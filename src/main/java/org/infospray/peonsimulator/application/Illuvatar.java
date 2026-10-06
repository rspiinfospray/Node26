package org.infospray.peonsimulator.application;

import org.infospray.peonsimulator.application.dto.ActionRequest;
import org.infospray.peonsimulator.application.dto.CreateWorldRequest;
import org.infospray.peonsimulator.application.dto.PeonDecisionContext;
import org.infospray.peonsimulator.application.dto.PeonDecision;
import org.infospray.peonsimulator.application.port.secondary.ActionRequestPublisher;
import org.infospray.peonsimulator.application.port.secondary.DomainEventPublisher;
import org.infospray.peonsimulator.application.port.secondary.EventRepository;
import org.infospray.peonsimulator.application.port.secondary.PeonDecisionProvider;
import org.infospray.peonsimulator.application.port.secondary.PeonFirstNameCatalog;
import org.infospray.peonsimulator.application.port.secondary.WorldRepository;
import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.action.PeonAction;
import org.infospray.peonsimulator.domain.event.SimulationEvent;
import org.infospray.peonsimulator.domain.model.ActionType;
import org.infospray.peonsimulator.domain.model.Cell;
import org.infospray.peonsimulator.domain.model.HexCoordinate;
import org.infospray.peonsimulator.domain.model.Grave;
import org.infospray.peonsimulator.domain.model.House;
import org.infospray.peonsimulator.domain.model.ItemType;
import org.infospray.peonsimulator.domain.model.Peon;
import org.infospray.peonsimulator.domain.model.ActionExperience;
import org.infospray.peonsimulator.domain.model.PeonPersonality;
import org.infospray.peonsimulator.domain.model.PeonRelation;
import org.infospray.peonsimulator.domain.model.RememberedCell;
import org.infospray.peonsimulator.domain.model.RememberedGraveObservation;
import org.infospray.peonsimulator.domain.model.RememberedPeonObservation;
import org.infospray.peonsimulator.domain.model.Team;
import org.infospray.peonsimulator.domain.model.TerrainType;
import org.infospray.peonsimulator.domain.model.World;
import org.infospray.peonsimulator.domain.model.WorldStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Service
public class Illuvatar {
    private static final List<String> TEAM_COLORS = List.of("#e85d75", "#4ea8de", "#80b918", "#ffb703", "#9d4edd", "#f77f00");
    private final WorldRepository worldRepository;
    private final EventRepository eventRepository;
    private final ActionRequestPublisher actionRequestPublisher;
    private final DomainEventPublisher domainEventPublisher;
    private final PeonDecisionProvider decisionProvider;
    private final PeonFirstNameCatalog firstNameCatalog;
    private final GameDefaultsProperties defaults;

    public Illuvatar(WorldRepository worldRepository, EventRepository eventRepository, ActionRequestPublisher actionRequestPublisher, DomainEventPublisher domainEventPublisher, PeonDecisionProvider decisionProvider, PeonFirstNameCatalog firstNameCatalog, GameDefaultsProperties defaults) {
        this.worldRepository = worldRepository;
        this.eventRepository = eventRepository;
        this.actionRequestPublisher = actionRequestPublisher;
        this.domainEventPublisher = domainEventPublisher;
        this.decisionProvider = decisionProvider;
        this.firstNameCatalog = firstNameCatalog;
        this.defaults = defaults;
    }

    @Transactional
    public synchronized World createWorld(CreateWorldRequest request) {
        String requestedName = request.name() == null ? "" : request.name().trim();
        boolean nameAlreadyUsed = !requestedName.isBlank() && this.worldRepository.findAll().stream().map(World::getName).filter(java.util.Objects::nonNull).map(String::trim).anyMatch(requestedName::equalsIgnoreCase);
        if (nameAlreadyUsed) {
            throw new IllegalArgumentException("Un monde nomm\u00e9 \"" + requestedName + "\" existe d\u00e9j\u00e0");
        }
        int minFood = request.minFoodPerCell() == null ? this.defaults.getMinFoodPerCell() : request.minFoodPerCell();
        int maxFood = request.maxFoodPerCell() == null ? this.defaults.getMaxFoodPerCell() : request.maxFoodPerCell();
        if (minFood > maxFood) {
            throw new IllegalArgumentException("minFoodPerCell ne peut pas dépasser maxFoodPerCell");
        }
        int requestedPeons = request.teams().stream().mapToInt(CreateWorldRequest.TeamRequest::peonCount).sum();
        World world = this.newWorld(request);
        Random random = new Random(world.getSeed());
        this.generateTerrain(world, random);
        List<Cell> traversableCells = world.getCells().values().stream().filter(cell -> cell.getTerrain() != TerrainType.ROCK).toList();
        if (traversableCells.size() < requestedPeons) {
            throw new IllegalArgumentException("Le monde ne contient pas assez de cases accessibles pour tous les peons");
        }
        this.generateFood(world, traversableCells, random);
        this.generateTeamsAndPeons(world, request.teams(), traversableCells, random);
        World saved = this.worldRepository.save(world);
        this.worldRepository.saveSnapshot(world);
        SimulationEvent event = SimulationEvent.of(null, "WORLD_CREATED", world.getId(), null, null, 0, 0, 0, Map.of("name", world.getName(), "width", world.getWidth(), "height", world.getHeight(), "peonCount", requestedPeons));
        this.eventRepository.appendAll(List.of(event));
        this.afterCommit(() -> this.domainEventPublisher.publish(List.of(event)));
        return saved;
    }

    @Transactional
    public synchronized World setStatus(UUID worldId, WorldStatus status) {
        World world = this.getWorld(worldId);
        if (world.getStatus() != WorldStatus.FINISHED) {
            world.setStatus(status);
            this.worldRepository.save(world);
        }
        return world;
    }

    @Transactional
    public synchronized World advance(UUID worldId, boolean manual) {
        World world = this.getWorld(worldId);
        if ((!manual && world.getStatus() != WorldStatus.RUNNING) || world.getStatus() == WorldStatus.FINISHED || world.getPendingActionId() != null) {
            return world;
        }
        Peon actor = world.nextActor();
        if (actor == null) {
            this.worldRepository.save(world);
            return world;
        }
        long sequence = world.getSequenceNumber() + 1;
        world.setSequenceNumber(sequence);
        PeonDecisionContext context = this.buildDecisionContext(world, actor);
        PeonDecision peonDecision = this.decisionProvider.decide(context);
        PeonAction action = peonDecision.action();
        actor.beginDecision(peonDecision.situationKey(), action.type());
        UUID actionId = UUID.randomUUID();
        world.setPendingActionId(actionId);
        this.worldRepository.save(world);
        Map<String, Object> decisionPayload = new LinkedHashMap<>();
        decisionPayload.put("actionType", action.type().name());
        decisionPayload.put("purpose", action.purpose().name());
        decisionPayload.put("reason", action.reason());
        decisionPayload.put("situationKey", peonDecision.situationKey());
        decisionPayload.put("selectedScore", peonDecision.selectedScore());
        decisionPayload.put("explorationChoice", peonDecision.explorationChoice());
        decisionPayload.put("candidates", peonDecision.candidates());
        if (action.destination() != null) { decisionPayload.put("destination", action.destination()); }
        if (action.targetPeonId() != null) { decisionPayload.put("targetPeonId", action.targetPeonId()); }
        SimulationEvent decision = SimulationEvent.of(actionId, "PEON_DECISION_MADE", world.getId(), actor.getTeamId(), actor.getId(), world.getCurrentRound(), sequence, 0, decisionPayload);
        this.eventRepository.appendAll(List.of(decision));
        ActionRequest actionRequest = new ActionRequest(actionId, world.getId(), actor.getId(), sequence, action);
        this.afterCommit(() -> {
            this.domainEventPublisher.publish(List.of(decision));
            this.actionRequestPublisher.publish(actionRequest);
        });
        return this.getWorld(worldId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized void processAction(ActionRequest request) {
        if (this.eventRepository.actionWasProcessed(request.actionId())) {
            return;
        }
        World world = this.getWorld(request.worldId());
        if (!request.actionId().equals(world.getPendingActionId()) || request.sequenceNumber() != world.getSequenceNumber()) {
            return;
        }
        Peon actor = world.getPeons().get(request.peonId());
        List<SimulationEvent> events = new ArrayList<>();
        int[] eventIndex = {1};
        if (actor == null || !actor.isAlive()) {
            events.add(this.event(world, request, actor, "PEON_ACTION_REJECTED", eventIndex, Map.of("reason", "PEON_NOT_ALIVE")));
        } else {
            this.resolveAction(world, actor, request, events, eventIndex);
            if (actor.isAlive()) {
                int before = actor.getHealthPoints();
                House shelter = actor.getInsideHouseId() == null ? null : world.getHouses().get(actor.getInsideHouseId());
                int hungerLoss = shelter != null && shelter.contains(actor.getId()) ? (int) Math.ceil(world.getHungerHealthLossPerTurn() * this.defaults.getHouseHungerPercentage() / 100.0) : world.getHungerHealthLossPerTurn();
                actor.hurt(hungerLoss);
                events.add(this.event(world, request, actor, "PEON_HUNGER_APPLIED", eventIndex, Map.of("healthBefore", before, "healthAfter", actor.getHealthPoints(), "loss", hungerLoss, "sheltered", shelter != null)));
                if (!actor.isAlive()) {
                    this.removeDeadPeon(world, actor);
                    events.add(this.event(world, request, actor, "PEON_DIED", eventIndex, Map.of("reason", "HUNGER", "position", actor.getPosition())));
                }
            }
            this.restoreLeveledPeonsToMaximumHealth(world, request, events, eventIndex);
            if (actor.getPendingDecisionAction() != null) { this.learnFromOutcome(world, actor, request, events, eventIndex); }
        }
        world.completeTurn();
        events.add(this.event(world, request, actor, "PEON_TURN_COMPLETED", eventIndex, Map.of("worldStatus", world.getStatus().name(), "nextRound", world.getCurrentRound())));
        if (world.getStatus() == WorldStatus.FINISHED) {
            events.add(this.event(world, request, actor, "WORLD_FINISHED", eventIndex, Map.of("reason", world.alivePeons().isEmpty() ? "NO_SURVIVORS" : "MAX_ROUNDS_REACHED")));
        }
        this.worldRepository.save(world);
        this.worldRepository.saveSnapshot(world);
        this.eventRepository.appendAll(events);
        this.eventRepository.markActionProcessed(request.actionId());
        this.afterCommit(() -> this.domainEventPublisher.publish(events));
    }

    public World getWorld(UUID worldId) {
        return this.worldRepository.findById(worldId).orElseThrow(() -> new IllegalArgumentException("Monde introuvable: " + worldId));
    }

    public List<World> getWorlds() { return this.worldRepository.findAll(); }
    public World getWorldAtSequence(UUID worldId, long sequenceNumber) { return this.worldRepository.findAtSequence(worldId, sequenceNumber).orElseThrow(() -> new IllegalArgumentException("Aucun état historique pour la séquence " + sequenceNumber)); }
    public List<SimulationEvent> getEvents(UUID worldId) { return this.eventRepository.findByWorldId(worldId); }

    @Transactional
    public synchronized void deleteAllWorlds() {
        this.eventRepository.deleteAll();
        this.worldRepository.deleteAll();
    }

    private World newWorld(CreateWorldRequest request) {
        World world = new World();
        world.setId(UUID.randomUUID());
        String requestedName = request.name() == null ? "" : request.name().trim();
        world.setName(requestedName.isBlank() ? "Monde " + world.getId().toString().substring(0, 8) : requestedName);
        world.setWidth(request.width() == null ? this.defaults.getWorldWidth() : request.width());
        world.setHeight(request.height() == null ? this.defaults.getWorldHeight() : request.height());
        world.setRockPercentage(request.rockPercentage() == null ? this.defaults.getRockPercentage() : request.rockPercentage());
        world.setTreePercentage(request.treePercentage() == null ? this.defaults.getTreePercentage() : request.treePercentage());
        world.setFoodCellPercentage(request.foodCellPercentage() == null ? this.defaults.getFoodCellPercentage() : request.foodCellPercentage());
        world.setMinFoodPerCell(request.minFoodPerCell() == null ? this.defaults.getMinFoodPerCell() : request.minFoodPerCell());
        world.setMaxFoodPerCell(request.maxFoodPerCell() == null ? this.defaults.getMaxFoodPerCell() : request.maxFoodPerCell());
        world.setHungerHealthLossPerTurn(request.hungerHealthLossPerTurn() == null ? this.defaults.getHungerHealthLossPerTurn() : request.hungerHealthLossPerTurn());
        world.setMaxRounds(request.maxRounds() == null ? this.defaults.getMaxRounds() : request.maxRounds());
        world.setSeed(request.resolvedSeed());
        return world;
    }

    private void generateTerrain(World world, Random random) {
        int centerQ = world.getWidth() / 2;
        int centerR = world.getHeight() / 2;
        int radius = (Math.min(world.getWidth(), world.getHeight()) - 1) / 2;
        for (int q = 0; q < world.getWidth(); q++) {
            for (int r = 0; r < world.getHeight(); r++) {
                if (new HexCoordinate(centerQ, centerR).distanceTo(new HexCoordinate(q, r)) > radius) { continue; }
                double terrainRoll = random.nextDouble();
                double rockProbability = world.getRockPercentage() / 100.0;
                double treeProbability = world.getTreePercentage() / 100.0;
                TerrainType terrain = terrainRoll < rockProbability ? TerrainType.ROCK : terrainRoll < rockProbability + treeProbability ? TerrainType.TREE : TerrainType.PLAIN;
                Cell cell = new Cell(new HexCoordinate(q, r), terrain, 0);
                if (terrain == TerrainType.TREE) { cell.setTreeHealthPoints(this.defaults.getTreeHealthPoints()); }
                world.addCell(cell);
            }
        }
        Cell center = world.cell(new HexCoordinate(centerQ, centerR));
        center.setTerrain(TerrainType.PLAIN);
        Set<String> reachable = this.reachablePlainKeys(world, center.getCoordinate());
        world.getCells().forEach((key, cell) -> { if (cell.getTerrain() != TerrainType.ROCK && !reachable.contains(key)) { cell.setTerrain(TerrainType.ROCK); } });
    }

    private Set<String> reachablePlainKeys(World world, HexCoordinate start) {
        Set<String> visited = new HashSet<>();
        Queue<HexCoordinate> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            HexCoordinate coordinate = queue.remove();
            if (!world.contains(coordinate) || !visited.add(World.key(coordinate)) || world.cell(coordinate).getTerrain() == TerrainType.ROCK) { continue; }
            coordinate.neighbors().forEach(queue::add);
        }
        return visited;
    }

    private void generateFood(World world, List<Cell> traversableCells, Random random) {
        for (Cell cell : traversableCells) {
            if (random.nextDouble() < world.getFoodCellPercentage() / 100.0) {
                cell.setFoodQuantity(world.getMinFoodPerCell() + random.nextInt(world.getMaxFoodPerCell() - world.getMinFoodPerCell() + 1));
            }
        }
    }

    private void generateTeamsAndPeons(World world, List<CreateWorldRequest.TeamRequest> requests, List<Cell> traversableCells, Random random) {
        List<Cell> shuffled = new ArrayList<>(traversableCells);
        java.util.Collections.shuffle(shuffled, random);
        int cellIndex = 0;
        int nameIndex = random.nextInt(this.firstNameCatalog.size());
        int teamIndex = 0;
        int globalPeonIndex = 0;
        for (CreateWorldRequest.TeamRequest request : requests) {
            UUID teamId = UUID.randomUUID();
            world.getTeams().put(teamId, new Team(teamId, request.name(), TEAM_COLORS.get(teamIndex++ % TEAM_COLORS.size())));
            for (int index = 0; index < request.peonCount(); index++) {
                Cell spawn = shuffled.get(cellIndex++);
                String baseName = this.firstNameCatalog.nameAt(nameIndex++);
                String firstName = globalPeonIndex < this.firstNameCatalog.size() ? baseName : baseName + "-" + (globalPeonIndex / this.firstNameCatalog.size() + 1);
                Peon peon = new Peon(UUID.randomUUID(), firstName, teamId, spawn.getCoordinate());
                peon.setMaxHealthPoints(this.defaults.getMaxHealthPoints());
                peon.setHealthPoints(this.defaults.getInitialHealthPoints());
                peon.setExperiencePerLevel(this.defaults.getExperiencePerLevel());
                peon.setExperiencePoints(this.defaults.getInitialExperiencePoints());
                peon.setAttackDamage(this.defaults.getAttackDamage());
                int prudence = 5 + random.nextInt(81);
                int aggressiveness = random.nextInt(101 - prudence);
                peon.setPersonality(new PeonPersonality(prudence, aggressiveness, 20 + random.nextInt(71), 20 + random.nextInt(71), 20 + random.nextInt(71)));
                world.getPeons().put(peon.getId(), peon);
                spawn.addOccupant(peon.getId());
                this.remember(world, peon, spawn, true, 0);
                this.observe(world, peon, 0);
                globalPeonIndex++;
            }
        }
    }

    private PeonDecisionContext buildDecisionContext(World world, Peon peon) {
        Cell current = world.cell(peon.getPosition());
        House house = world.houseAt(peon.getPosition());
        List<PeonDecisionContext.ObservedPeon> occupants = current.getOccupantPeonIds().stream().filter(id -> !id.equals(peon.getId())).map(world.getPeons()::get).filter(java.util.Objects::nonNull).filter(Peon::isAlive).filter(other -> other.getInsideHouseId() == null || other.getInsideHouseId().equals(peon.getInsideHouseId())).map(other -> new PeonDecisionContext.ObservedPeon(other.getId(), other.getTeamId(), other.getHealthPoints(), other.getLevel())).toList();
        List<HexCoordinate> neighbors = peon.getPosition().neighbors().stream().filter(world::contains).filter(coordinate -> { RememberedCell remembered = peon.getMentalMap().get(World.key(coordinate)); return remembered != null && remembered.getTerrain() != TerrainType.ROCK; }).toList();
        Peon houseHost = house == null || house.getHostPeonId() == null ? null : world.getPeons().get(house.getHostPeonId());
        PeonDecisionContext.ObservedHouse observedHouse = house == null ? null : new PeonDecisionContext.ObservedHouse(house.getId(), house.getHostPeonId(), houseHost == null ? null : houseHost.getTeamId(), house.contains(peon.getId()), !house.isEmpty());
        boolean buildable = current.getTerrain() == TerrainType.PLAIN && house == null && current.getOccupantPeonIds().stream().allMatch(peon.getId()::equals) && world.getGraves().values().stream().noneMatch(grave -> grave.position().equals(peon.getPosition()));
        return new PeonDecisionContext(peon.getId(), peon.getTeamId(), peon.getHealthPoints(), peon.getMaxHealthPoints(), peon.getExperiencePoints(), peon.getLevel(), peon.getPosition(), current.getTerrain() == TerrainType.TREE, world.getSequenceNumber(), world.getSeed(), peon.getPersonality(), Map.copyOf(peon.getLearnedActions()), Map.copyOf(peon.getMentalMap()), occupants, current.getFoodQuantity(), neighbors, world.getCurrentRound(), peon.getLastObservationRound(), current.getTerrain(), current.getTreeHealthPoints(), peon.itemCount(ItemType.WOOD), buildable, observedHouse);
    }

    private void resolveAction(World world, Peon actor, ActionRequest request, List<SimulationEvent> events, int[] index) {
        PeonAction action = request.action();
        switch (action.type()) {
            case VOIR -> { this.observe(world, actor, world.getSequenceNumber()); events.add(this.event(world, request, actor, "PEON_SAW", index, Map.of("visibleCells", this.visibleCoordinates(world, actor).size()))); }
            case MANGER -> this.eat(world, actor, request, events, index);
            case SE_DEPLACER -> this.move(world, actor, request, events, index);
            case ATTAQUER -> this.attack(world, actor, request, events, index);
            case COMMUNIQUER -> this.communicate(world, actor, request, events, index);
            case COUPER_DU_BOIS -> this.chopWood(world, actor, request, events, index);
            case CONSTRUIRE_MAISON -> this.buildHouse(world, actor, request, events, index);
            case NE_RIEN_FAIRE -> events.add(this.event(world, request, actor, "PEON_IDLED", index, Map.of()));
        }
    }

    private void eat(World world, Peon actor, ActionRequest request, List<SimulationEvent> events, int[] index) {
        Cell cell = world.cell(actor.getPosition());
        if (cell.getFoodQuantity() <= 0) { this.reject(world, actor, request, events, index, "NO_FOOD"); return; }
        int foodBefore = cell.getFoodQuantity();
        int healthBefore = actor.getHealthPoints();
        cell.consumeFood();
        actor.heal(this.defaults.getEatHealthGain());
        events.add(this.event(world, request, actor, "FOOD_CONSUMED", index, Map.of("position", actor.getPosition(), "quantityBefore", foodBefore, "quantityAfter", cell.getFoodQuantity())));
        events.add(this.event(world, request, actor, "PEON_ATE", index, Map.of("healthBefore", healthBefore, "healthAfter", actor.getHealthPoints())));
        this.gainExperience(world, actor, request, events, index, this.defaults.getEatExperienceGain());
        this.remember(world, actor, cell, true, world.getSequenceNumber());
    }

    private void move(World world, Peon actor, ActionRequest request, List<SimulationEvent> events, int[] index) {
        HexCoordinate destination = request.action().destination();
        if (destination == null || actor.getPosition().distanceTo(destination) != 1 || !world.contains(destination) || world.cell(destination).getTerrain() == TerrainType.ROCK) { this.reject(world, actor, request, events, index, "INVALID_DESTINATION"); return; }
        HexCoordinate origin = actor.getPosition();
        this.leaveHouse(world, actor, request, events, index);
        world.cell(origin).removeOccupant(actor.getId());
        world.cell(destination).addOccupant(actor.getId());
        actor.setPosition(destination);
        House destinationHouse = world.houseAt(destination);
        if (destinationHouse != null && destinationHouse.isEmpty()) {
            destinationHouse.claim(actor.getId());
            actor.setInsideHouseId(destinationHouse.getId());
            events.add(this.event(world, request, actor, "HOUSE_CLAIMED", index, Map.of("houseId", destinationHouse.getId(), "position", destination)));
            events.add(this.event(world, request, actor, "PEON_ENTERED_HOUSE", index, Map.of("houseId", destinationHouse.getId())));
        }
        this.remember(world, actor, world.cell(destination), true, world.getSequenceNumber());
        events.add(this.event(world, request, actor, "PEON_MOVED", index, Map.of("from", origin, "to", destination)));
        this.gainExperience(world, actor, request, events, index, this.defaults.getMoveExperienceGain());
        int healthBeforeMovementCost = actor.getHealthPoints();
        actor.hurt(this.defaults.getMoveHealthCost());
        events.add(this.event(world, request, actor, "PEON_MOVEMENT_COST_APPLIED", index, Map.of("healthBefore", healthBeforeMovementCost, "healthAfter", actor.getHealthPoints(), "loss", this.defaults.getMoveHealthCost())));
        if (!actor.isAlive()) {
            this.removeDeadPeon(world, actor);
            events.add(this.event(world, request, actor, "PEON_DIED", index, Map.of("reason", "MOVEMENT", "position", actor.getPosition())));
            return;
        }
        this.observe(world, actor, world.getSequenceNumber());
        events.add(this.event(world, request, actor, "PEON_SAW", index, Map.of("visibleCells", this.visibleCoordinates(world, actor).size())));
    }

    private void attack(World world, Peon actor, ActionRequest request, List<SimulationEvent> events, int[] index) {
        Peon target = world.getPeons().get(request.action().targetPeonId());
        if (target == null || !target.isAlive() || target.getTeamId().equals(actor.getTeamId()) || !target.getPosition().equals(actor.getPosition()) || target.getInsideHouseId() != null && !target.getInsideHouseId().equals(actor.getInsideHouseId())) { this.reject(world, actor, request, events, index, "INVALID_TARGET"); return; }
        int before = target.getHealthPoints();
        int damage = actor.getAttackDamage();
        target.hurt(damage);
        boolean lethal = !target.isAlive();
        int experienceGain = lethal ? this.defaults.getLethalAttackExperienceGain() : this.defaults.getAttackExperienceGain();
        events.add(this.event(world, request, actor, "PEON_ATTACKED", index, Map.of("targetPeonId", target.getId(), "healthBefore", before, "healthAfter", target.getHealthPoints(), "damage", damage, "lethal", lethal, "attackerExperienceGain", experienceGain)));
        this.gainExperience(world, actor, request, events, index, experienceGain);
        if (!target.isAlive()) {
            this.removeDeadPeon(world, target);
            events.add(this.event(world, request, target, "PEON_DIED", index, Map.of("reason", "ATTACK", "position", target.getPosition(), "attackerPeonId", actor.getId())));
        } else {
            this.gainExperience(world, target, request, events, index, this.defaults.getAttackExperienceGain());
        }
    }

    private void communicate(World world, Peon actor, ActionRequest request, List<SimulationEvent> events, int[] index) {
        Collection<Peon> allies = world.cell(actor.getPosition()).getOccupantPeonIds().stream().map(world.getPeons()::get).filter(java.util.Objects::nonNull).filter(peon -> !peon.getId().equals(actor.getId()) && peon.isAlive() && peon.getTeamId().equals(actor.getTeamId())).toList();
        House house = world.houseAt(actor.getPosition());
        boolean houseInteraction = false;
        if (house != null && !house.isEmpty() && !house.contains(actor.getId())) {
            Peon host = world.getPeons().get(house.getHostPeonId());
            if (host != null && host.isAlive() && host.getTeamId().equals(actor.getTeamId())) {
                houseInteraction = true;
                events.add(this.event(world, request, actor, "HOUSE_ENTRY_REQUESTED", index, Map.of("houseId", house.getId(), "hostPeonId", host.getId())));
                double acceptance = host.getPersonality().getSolidarity() + host.getPersonality().getIngenuity() * 0.25 - host.getPersonality().getPrudence() * 0.4;
                if (acceptance >= 30) {
                    house.enter(actor.getId());
                    actor.setInsideHouseId(house.getId());
                    events.add(this.event(world, request, host, "HOUSE_ENTRY_ACCEPTED", index, Map.of("houseId", house.getId(), "guestPeonId", actor.getId())));
                    events.add(this.event(world, request, actor, "PEON_ENTERED_HOUSE", index, Map.of("houseId", house.getId())));
                } else { events.add(this.event(world, request, host, "HOUSE_ENTRY_REFUSED", index, Map.of("houseId", house.getId(), "guestPeonId", actor.getId()))); }
            }
        } else if (house != null && actor.getId().equals(house.getHostPeonId())) {
            for (Peon ally : allies) {
                if (ally.getInsideHouseId() == null) { house.enter(ally.getId()); ally.setInsideHouseId(house.getId()); houseInteraction = true; events.add(this.event(world, request, actor, "HOUSE_ENTRY_INVITED", index, Map.of("houseId", house.getId(), "guestPeonId", ally.getId()))); events.add(this.event(world, request, ally, "PEON_ENTERED_HOUSE", index, Map.of("houseId", house.getId()))); }
            }
        }
        if (allies.isEmpty() && !houseInteraction) { this.reject(world, actor, request, events, index, "NO_ALLY_IN_RANGE"); return; }
        for (Peon ally : allies) {
            actor.getMentalMap().forEach((key, memory) -> {
                RememberedCell existing = ally.getMentalMap().get(key);
                if (existing == null || existing.getLastObservedAtSequence() < memory.getLastObservedAtSequence()) { ally.getMentalMap().put(key, memory); }
            });
        }
        events.add(this.event(world, request, actor, "PEON_COMMUNICATED", index, Map.of("allyCount", allies.size())));
    }

    private void chopWood(World world, Peon actor, ActionRequest request, List<SimulationEvent> events, int[] index) {
        Cell cell = world.cell(actor.getPosition());
        if (cell.getTerrain() != TerrainType.TREE || cell.getTreeHealthPoints() <= 0) { this.reject(world, actor, request, events, index, "NO_TREE"); return; }
        int healthBefore = cell.getTreeHealthPoints();
        int collected = cell.chopTree(this.defaults.getWoodPerChop());
        actor.addItem(ItemType.WOOD, collected);
        events.add(this.event(world, request, actor, "PEON_CHOPPED_WOOD", index, Map.of("position", actor.getPosition(), "treeHealthBefore", healthBefore, "treeHealthAfter", cell.getTreeHealthPoints(), "woodCollected", collected, "woodAfter", actor.itemCount(ItemType.WOOD))));
        if (cell.getTerrain() == TerrainType.PLAIN) { events.add(this.event(world, request, actor, "TREE_CUT_DOWN", index, Map.of("position", actor.getPosition()))); }
        this.gainExperience(world, actor, request, events, index, this.defaults.getChopWoodExperienceGain());
        this.observe(world, actor, world.getSequenceNumber());
    }

    private void buildHouse(World world, Peon actor, ActionRequest request, List<SimulationEvent> events, int[] index) {
        Cell cell = world.cell(actor.getPosition());
        boolean occupiedByOther = cell.getOccupantPeonIds().stream().anyMatch(id -> !id.equals(actor.getId()));
        boolean gravePresent = world.getGraves().values().stream().anyMatch(grave -> grave.position().equals(actor.getPosition()));
        if (cell.getTerrain() != TerrainType.PLAIN || world.houseAt(actor.getPosition()) != null || occupiedByOther || gravePresent) { this.reject(world, actor, request, events, index, "INVALID_BUILD_SITE"); return; }
        if (!actor.removeItem(ItemType.WOOD, this.defaults.getWoodRequiredForHouse())) { this.reject(world, actor, request, events, index, "NOT_ENOUGH_WOOD"); return; }
        House house = new House(UUID.randomUUID(), actor.getPosition(), actor.getId());
        world.getHouses().put(house.getId(), house);
        actor.setInsideHouseId(house.getId());
        events.add(this.event(world, request, actor, "HOUSE_BUILT", index, Map.of("houseId", house.getId(), "position", actor.getPosition(), "woodSpent", this.defaults.getWoodRequiredForHouse(), "woodAfter", actor.itemCount(ItemType.WOOD))));
        events.add(this.event(world, request, actor, "PEON_ENTERED_HOUSE", index, Map.of("houseId", house.getId())));
        this.gainExperience(world, actor, request, events, index, this.defaults.getBuildHouseExperienceGain());
    }

    private void leaveHouse(World world, Peon actor, ActionRequest request, List<SimulationEvent> events, int[] index) {
        if (actor.getInsideHouseId() == null) { return; }
        House house = world.getHouses().get(actor.getInsideHouseId());
        actor.setInsideHouseId(null);
        if (house == null) { return; }
        if (actor.getId().equals(house.getHostPeonId())) {
            for (UUID occupantId : List.copyOf(house.getOccupantPeonIds())) { Peon occupant = world.getPeons().get(occupantId); if (occupant != null) { occupant.setInsideHouseId(null); } }
            house.empty();
            events.add(this.event(world, request, actor, "HOUSE_EMPTIED", index, Map.of("houseId", house.getId())));
        } else { house.leave(actor.getId()); }
        events.add(this.event(world, request, actor, "PEON_LEFT_HOUSE", index, Map.of("houseId", house.getId())));
    }

    private void gainExperience(World world, Peon peon, ActionRequest request, List<SimulationEvent> events, int[] index, int amount) {
        int healthBefore = peon.getHealthPoints();
        int maxHealthBefore = peon.getMaxHealthPoints();
        int attackDamageBefore = peon.getAttackDamage();
        int levelBefore = peon.gainExperience(amount, this.defaults.getMaxHealthGainPerLevel(), this.defaults.getAttackDamageGainPerLevel());
        events.add(this.event(world, request, peon, "PEON_EXPERIENCE_GAINED", index, Map.of("amount", amount, "experienceAfter", peon.getExperiencePoints())));
        if (peon.getLevel() > levelBefore) { events.add(this.event(world, request, peon, "PEON_LEVELED_UP", index, Map.of("levelBefore", levelBefore, "levelAfter", peon.getLevel(), "healthBefore", healthBefore, "healthAfter", peon.getHealthPoints(), "maxHealthBefore", maxHealthBefore, "maxHealthAfter", peon.getMaxHealthPoints(), "attackDamageBefore", attackDamageBefore, "attackDamageAfter", peon.getAttackDamage()))); }
    }

    private void restoreLeveledPeonsToMaximumHealth(World world, ActionRequest request, List<SimulationEvent> events, int[] index) {
        List<UUID> leveledPeonIds = events.stream().filter(event -> "PEON_LEVELED_UP".equals(event.eventType())).map(SimulationEvent::peonId).filter(java.util.Objects::nonNull).distinct().toList();
        for (UUID peonId : leveledPeonIds) {
            Peon peon = world.getPeons().get(peonId);
            if (peon == null || !peon.isAlive()) { continue; }
            int healthBefore = peon.getHealthPoints();
            peon.setHealthPoints(peon.getMaxHealthPoints());
            events.add(this.event(world, request, peon, "PEON_LEVEL_UP_HEAL_APPLIED", index, Map.of("level", peon.getLevel(), "healthBefore", healthBefore, "healthAfter", peon.getHealthPoints(), "maxHealthPoints", peon.getMaxHealthPoints())));
        }
    }

    private void learnFromOutcome(World world, Peon actor, ActionRequest request, List<SimulationEvent> events, int[] index) {
        boolean rejected = events.stream().anyMatch(event -> "PEON_ACTION_REJECTED".equals(event.eventType()) && actor.getId().equals(event.peonId()));
        int healthDelta = actor.getHealthPoints() - actor.getHealthBeforeDecision();
        int experienceDelta = actor.getExperiencePoints() - actor.getExperienceBeforeDecision();
        int knownCellsDelta = actor.getMentalMap().size() - actor.getKnownCellsBeforeDecision();
        double healthPercentageDelta = healthDelta * 100.0 / actor.getMaxHealthPoints();
        double reward = healthPercentageDelta * 1.2 + experienceDelta * 0.5 + knownCellsDelta * 4.0 + (actor.isAlive() ? 5 : -100) + (rejected ? -25 : 0);
        if (events.stream().anyMatch(event -> "PEON_ATE".equals(event.eventType()))) { reward += 15; }
        if (events.stream().anyMatch(event -> "PEON_ATTACKED".equals(event.eventType()))) { reward += 20; }
        if (events.stream().anyMatch(event -> "PEON_COMMUNICATED".equals(event.eventType()))) { reward += 10; }
        ActionExperience experience = actor.learnFromDecision(world.getSequenceNumber(), Math.round(reward * 10.0) / 10.0, rejected, this.defaults.getLearningRate(), this.defaults.getActionHistoryLimit());
        String knowledgeKey = experience.situationKey() + "|" + experience.actionType().name();
        events.add(this.event(world, request, actor, "PEON_LEARNED", index, Map.of("actionType", experience.actionType().name(), "situationKey", experience.situationKey(), "reward", experience.reward(), "healthDelta", experience.healthDelta(), "experienceDelta", experience.experienceDelta(), "knownCellsDelta", experience.knownCellsDelta(), "rejected", experience.rejected(), "survived", experience.survived(), "expectedReward", actor.getLearnedActions().get(knowledgeKey).getExpectedReward())));
    }

    private void reject(World world, Peon actor, ActionRequest request, List<SimulationEvent> events, int[] index, String reason) { events.add(this.event(world, request, actor, "PEON_ACTION_REJECTED", index, Map.of("reason", reason, "actionType", request.action().type().name()))); }
    private void removeDeadPeon(World world, Peon peon) {
        if (peon.getInsideHouseId() != null) {
            House house = world.getHouses().get(peon.getInsideHouseId());
            if (house != null && peon.getId().equals(house.getHostPeonId())) { for (UUID occupantId : List.copyOf(house.getOccupantPeonIds())) { Peon occupant = world.getPeons().get(occupantId); if (occupant != null) { occupant.setInsideHouseId(null); } } house.empty(); } else if (house != null) { house.leave(peon.getId()); }
            peon.setInsideHouseId(null);
        }
        world.cell(peon.getPosition()).removeOccupant(peon.getId());
        world.getGraves().putIfAbsent(peon.getId(), new Grave(peon.getId(), peon.getPosition(), world.getCurrentRound(), world.getSequenceNumber()));
    }

    private void observe(World world, Peon peon, long sequence) {
        for (HexCoordinate coordinate : this.visibleCoordinates(world, peon)) { this.remember(world, peon, world.cell(coordinate), coordinate.equals(peon.getPosition()), sequence); }
        peon.setLastObservationRound(sequence == 0 ? 0 : world.getCurrentRound());
    }

    private void remember(World world, Peon observer, Cell cell, boolean visited, long sequence) {
        boolean occupantsHiddenByTrees = cell.getTerrain() == TerrainType.TREE && !cell.getCoordinate().equals(observer.getPosition());
        List<RememberedPeonObservation> observations = occupantsHiddenByTrees ? List.of() : cell.getOccupantPeonIds().stream().map(world.getPeons()::get).filter(java.util.Objects::nonNull).filter(observed -> observed.getInsideHouseId() == null || observed.getInsideHouseId().equals(observer.getInsideHouseId())).map(observed -> new RememberedPeonObservation(observed.getId(), observed.getTeamId(), observed.getTeamId().equals(observer.getTeamId()) ? PeonRelation.ALLY : PeonRelation.ENEMY, cell.getCoordinate(), observed.getHealthPoints(), observed.getLevel(), sequence)).toList();
        List<RememberedGraveObservation> graveObservations = world.getGraves().values().stream().filter(grave -> grave.position().equals(cell.getCoordinate())).map(grave -> world.getPeons().get(grave.peonId())).filter(java.util.Objects::nonNull).map(dead -> {
            Grave grave = world.getGraves().get(dead.getId());
            return new RememberedGraveObservation(dead.getId(), dead.getTeamId(), dead.getTeamId().equals(observer.getTeamId()) ? PeonRelation.ALLY : PeonRelation.ENEMY, grave.position(), grave.deathRound(), grave.deathSequence(), sequence);
        }).toList();
        observer.remember(cell, visited, sequence, observations, graveObservations);
    }

    private List<HexCoordinate> visibleCoordinates(World world, Peon peon) {
        List<HexCoordinate> visible = new ArrayList<>();
        for (Cell cell : world.getCells().values()) {
            HexCoordinate target = cell.getCoordinate();
            if (peon.getPosition().distanceTo(target) <= peon.getLevel() && this.hasLineOfSight(world, peon.getPosition(), target)) { visible.add(target); }
        }
        return visible;
    }

    private boolean hasLineOfSight(World world, HexCoordinate from, HexCoordinate to) {
        int distance = from.distanceTo(to);
        if (distance <= 1) { return true; }
        for (int step = 1; step < distance; step++) {
            double ratio = (double) step / distance;
            HexCoordinate point = this.roundAxial(from.q() + (to.q() - from.q()) * ratio, from.r() + (to.r() - from.r()) * ratio);
            if (world.cell(point) != null && (world.cell(point).getTerrain() == TerrainType.ROCK || world.cell(point).getTerrain() == TerrainType.TREE)) { return false; }
        }
        return true;
    }

    private HexCoordinate roundAxial(double q, double r) {
        double x = q;
        double z = r;
        double y = -x - z;
        int rx = (int) Math.round(x);
        int ry = (int) Math.round(y);
        int rz = (int) Math.round(z);
        double dx = Math.abs(rx - x);
        double dy = Math.abs(ry - y);
        double dz = Math.abs(rz - z);
        if (dx > dy && dx > dz) { rx = -ry - rz; } else if (dy > dz) { ry = -rx - rz; } else { rz = -rx - ry; }
        return new HexCoordinate(rx, rz);
    }

    private SimulationEvent event(World world, ActionRequest request, Peon peon, String type, int[] index, Map<String, Object> payload) {
        UUID teamId = peon == null ? null : peon.getTeamId();
        UUID peonId = peon == null ? request.peonId() : peon.getId();
        return SimulationEvent.of(request.actionId(), type, world.getId(), teamId, peonId, world.getCurrentRound(), world.getSequenceNumber(), index[0]++, payload);
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) { action.run(); return; }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() { action.run(); }
        });
    }
}
