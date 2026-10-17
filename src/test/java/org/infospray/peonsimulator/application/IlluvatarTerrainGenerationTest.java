package org.infospray.peonsimulator.application;

import org.infospray.peonsimulator.application.dto.CreateWorldRequest;
import org.infospray.peonsimulator.application.port.secondary.ActionRequestPublisher;
import org.infospray.peonsimulator.application.port.secondary.DomainEventPublisher;
import org.infospray.peonsimulator.application.port.secondary.EventRepository;
import org.infospray.peonsimulator.application.port.secondary.PeonDecisionProvider;
import org.infospray.peonsimulator.application.port.secondary.PeonFirstNameCatalog;
import org.infospray.peonsimulator.application.port.secondary.WorldRepository;
import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.model.Cell;
import org.infospray.peonsimulator.domain.model.TerrainType;
import org.infospray.peonsimulator.domain.model.World;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IlluvatarTerrainGenerationTest {
    @Test
    void shouldGenerateMostTreesAndRocksNextToSameTerrain() {
        World world = this.createWorld(73921L);

        assertThat(this.clusteredRatio(world, TerrainType.TREE)).isGreaterThanOrEqualTo(0.75);
        assertThat(this.clusteredRatio(world, TerrainType.ROCK)).isGreaterThanOrEqualTo(0.75);
    }

    @Test
    void shouldKeepClusteredTerrainReproducibleFromSeed() {
        World first = this.createWorld(19477L);
        World second = this.createWorld(19477L);

        Map<String, TerrainType> firstTerrain = first.getCells().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getTerrain()));
        Map<String, TerrainType> secondTerrain = second.getCells().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getTerrain()));
        assertThat(firstTerrain).isEqualTo(secondTerrain);
    }

    @Test
    void shouldGenerateOneCellWideRiverAcrossTheWholeMapWhenEnabled() {
        World world = this.createWorld(8128L, true);
        List<Cell> river = world.getCells().values().stream().filter(cell -> cell.getTerrain() == TerrainType.RIVER).toList();
        int minQ = world.getCells().values().stream().mapToInt(cell -> cell.getCoordinate().q()).min().orElseThrow();
        int maxQ = world.getCells().values().stream().mapToInt(cell -> cell.getCoordinate().q()).max().orElseThrow();

        assertThat(river).hasSize(maxQ - minQ + 1);
        assertThat(river).extracting(cell -> cell.getCoordinate().q()).containsExactlyInAnyOrderElementsOf(java.util.stream.IntStream.rangeClosed(minQ, maxQ).boxed().toList());
        assertThat(world.isRiverGenerationEnabled()).isTrue();
    }

    private World createWorld(long seed) {
        return this.createWorld(seed, false);
    }

    private World createWorld(long seed, boolean generateRiver) {
        WorldRepository worldRepository = mock(WorldRepository.class);
        PeonFirstNameCatalog names = mock(PeonFirstNameCatalog.class);
        when(worldRepository.findAll()).thenReturn(List.of());
        when(worldRepository.save(any(World.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(names.size()).thenReturn(1);
        when(names.nameAt(anyInt())).thenReturn("Borin");
        Illuvatar illuvatar = new Illuvatar(worldRepository, mock(EventRepository.class), mock(ActionRequestPublisher.class), mock(DomainEventPublisher.class), mock(PeonDecisionProvider.class), names, new GameDefaultsProperties());
        CreateWorldRequest request = new CreateWorldRequest("Amas-" + seed, 50, 50, 15, 20, 0, 1, 1, 0, 5, 10, seed, generateRiver, List.of(new CreateWorldRequest.TeamRequest("Test", 1)));
        return illuvatar.createWorld(request);
    }

    private double clusteredRatio(World world, TerrainType terrain) {
        List<Cell> cells = world.getCells().values().stream().filter(cell -> cell.getTerrain() == terrain).toList();
        long clustered = cells.stream().filter(cell -> cell.getCoordinate().neighbors().stream().filter(world::contains).map(world::cell).anyMatch(neighbor -> neighbor.getTerrain() == terrain)).count();
        return cells.isEmpty() ? 0 : (double) clustered / cells.size();
    }
}
