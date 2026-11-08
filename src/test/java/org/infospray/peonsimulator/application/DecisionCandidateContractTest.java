package org.infospray.peonsimulator.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.infospray.peonsimulator.adapter.primary.rest.ApiExceptionHandler;
import org.infospray.peonsimulator.adapter.primary.rest.WorldController;
import org.infospray.peonsimulator.adapter.secondary.decision.PeonDecisionCandidateRegistry;
import org.infospray.peonsimulator.adapter.secondary.decision.RuleBasedPeonDecisionProvider;
import org.infospray.peonsimulator.adapter.secondary.persistence.JdbcWorldRepository;
import org.infospray.peonsimulator.application.command.ChangeWorldStatusCommand;
import org.infospray.peonsimulator.application.command.CreateWorldCommand;
import org.infospray.peonsimulator.application.command.GetWorldStateCommand;
import org.infospray.peonsimulator.application.dto.PeonDecision;
import org.infospray.peonsimulator.application.dto.PeonDecisionContext;
import org.infospray.peonsimulator.application.port.secondary.*;
import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.action.PeonAction;
import org.infospray.peonsimulator.domain.model.PeonPurpose;
import org.infospray.peonsimulator.domain.model.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DecisionCandidateContractTest {
    private static final String CANDIDATE = "CANDIDATE-8c361e13-f35f-44d4-9142-82179434acdf";
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private EmbeddedDatabase database;
    private JdbcWorldRepository repository;
    private Illuvatar illuvatar;
    private MockMvc api;
    private RecordingCandidate baseline;
    private RecordingCandidate alternative;
    private ActionRequestPublisher actions;

    @BeforeEach
    void setUp() {
        this.database = new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).addScript("schema.sql").build();
        this.repository = new JdbcWorldRepository(new JdbcTemplate(this.database), this.mapper);
        this.baseline = new RecordingCandidate("baseline");
        this.alternative = new RecordingCandidate(CANDIDATE);
        this.actions = mock(ActionRequestPublisher.class);
        PeonFirstNameCatalog names = mock(PeonFirstNameCatalog.class);
        when(names.size()).thenReturn(1);
        when(names.nameAt(anyInt())).thenReturn("Borin");
        this.illuvatar = new Illuvatar(this.repository, mock(EventRepository.class), this.actions, mock(DomainEventPublisher.class), new PeonDecisionCandidateRegistry(List.of(this.alternative, this.baseline)), names, new GameDefaultsProperties());
        WorldController controller = new WorldController(this.illuvatar, new CreateWorldCommand(this.illuvatar), null, new ChangeWorldStatusCommand(this.illuvatar), new GetWorldStateCommand(this.illuvatar), null, null, null, null, null, null, null, null, null, null, null, null);
        this.api = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @AfterEach
    void tearDown() { this.database.shutdown(); }

    @Test
    void defaultsToBaselineAndLoadsLegacyWorlds() throws Exception {
        World world = this.create("default", null);
        assertThat(world.getDecisionCandidateId()).isEqualTo("baseline");
        assertThat(this.repository.findById(world.getId()).orElseThrow().getDecisionCandidateId()).isEqualTo("baseline");
        assertThat(this.mapper.readValue("{}", World.class).getDecisionCandidateId()).isEqualTo("baseline");
        assertThat(this.mapper.readValue("{\"decisionCandidateId\":null}", World.class).getDecisionCandidateId()).isEqualTo("baseline");
        assertThat(new RuleBasedPeonDecisionProvider(new GameDefaultsProperties()).candidateId()).isEqualTo("baseline");
    }

    @Test
    void persistsAndReturnsCandidateThroughAllWorldEndpoints() throws Exception {
        World world = this.create("candidate", CANDIDATE);
        assertThat(world.getDecisionCandidateId()).isEqualTo(CANDIDATE);
        JdbcWorldRepository reloaded = new JdbcWorldRepository(new JdbcTemplate(this.database), this.mapper);
        assertThat(reloaded.findById(world.getId()).orElseThrow().getDecisionCandidateId()).isEqualTo(CANDIDATE);
        assertThat(reloaded.findAtSequence(world.getId(), 0).orElseThrow().getDecisionCandidateId()).isEqualTo(CANDIDATE);
        this.api.perform(get("/api/worlds/{id}", world.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.decisionCandidateId").value(CANDIDATE));
        this.api.perform(get("/api/worlds/{id}?sequence=0", world.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.decisionCandidateId").value(CANDIDATE));
        this.api.perform(get("/api/worlds")).andExpect(status().isOk()).andExpect(jsonPath("$[0].decisionCandidateId").value(CANDIDATE));
        this.api.perform(post("/api/worlds/{id}/start", world.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.decisionCandidateId").value(CANDIDATE));
        verifyNoInteractions(this.actions);
    }

    @Test
    void rejectsUnknownOrEmptyCandidateBeforeCreatingOrStartingWorld() throws Exception {
        for (String id : List.of("unknown", "", "BASELINE", " baseline")) {
            this.api.perform(post("/api/worlds").contentType("application/json").content(this.request("bad", id))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Unknown decisionCandidateId")));
        }
        assertThat(this.repository.findAll()).isEmpty();
        World world = this.create("removed", null);
        world.setDecisionCandidateId("removed-candidate");
        this.repository.save(world);
        this.api.perform(post("/api/worlds/{id}/start", world.getId())).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("removed-candidate")));
        verifyNoInteractions(this.actions);
    }

    @Test
    void usesSelectedProviderForTurnsAndReplayWithoutLeakingBetweenWorlds() throws Exception {
        World first = this.create("first", null);
        World second = this.create("second", CANDIDATE);
        this.illuvatar.advance(first.getId(), true);
        this.illuvatar.advance(second.getId(), true);
        UUID firstPeon = first.getPeons().keySet().iterator().next();
        UUID secondPeon = second.getPeons().keySet().iterator().next();
        assertThat(this.baseline.peons).containsExactly(firstPeon);
        assertThat(this.alternative.peons).containsExactly(secondPeon);
        assertThat(this.illuvatar.replayDecision(second.getId(), secondPeon, 1).action().reason()).isEqualTo(CANDIDATE);
        assertThat(this.illuvatar.replayDecision(first.getId(), firstPeon, 1).action().reason()).isEqualTo("baseline");
        assertThat(this.baseline.peons).containsExactly(firstPeon, firstPeon);
        assertThat(this.alternative.peons).containsExactly(secondPeon, secondPeon);
        verify(this.actions, times(2)).publish(any());
    }

    @Test
    void rejectsDuplicatesDuringApplicationContextStartup() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(PeonDecisionCandidateRegistry.class, () -> new PeonDecisionCandidateRegistry(List.of(this.baseline, new RecordingCandidate("baseline"))));
            assertThatThrownBy(context::refresh).hasRootCauseInstanceOf(IllegalStateException.class).hasStackTraceContaining("Duplicate decision candidate identifier: baseline");
        }
    }

    @Test
    void resolvesExactlyRegardlessOfRegistrationOrderAndReportsSortedIds() {
        PeonDecisionCandidateRegistry first = new PeonDecisionCandidateRegistry(List.of(this.baseline, this.alternative));
        PeonDecisionCandidateRegistry second = new PeonDecisionCandidateRegistry(List.of(this.alternative, this.baseline));
        assertThat(first.resolve(CANDIDATE)).isSameAs(this.alternative);
        assertThat(second.resolve("baseline")).isSameAs(this.baseline);
        assertThatThrownBy(() -> first.resolve("missing")).hasMessage("Unknown decisionCandidateId: missing; available: [" + CANDIDATE + ", baseline]");
        assertThatThrownBy(() -> new PeonDecisionCandidateRegistry(List.of(this.alternative))).isInstanceOf(IllegalStateException.class).hasMessageContaining("Missing baseline");
        assertThatThrownBy(() -> new PeonDecisionCandidateRegistry(List.of(this.baseline, new RecordingCandidate(" ")))).isInstanceOf(IllegalStateException.class);
    }

    private World create(String name, String candidateId) throws Exception {
        String json = this.api.perform(post("/api/worlds").contentType("application/json").content(this.request(name, candidateId))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return this.mapper.readValue(json, World.class);
    }

    private String request(String name, String candidateId) throws Exception {
        var json = this.mapper.createObjectNode();
        json.put("name", name).put("width", 5).put("height", 5).put("rockPercentage", 0).put("treePercentage", 0).put("sheepCount", 0).put("seed", 42L);
        json.putArray("teams").addObject().put("name", "team").put("peonCount", 1);
        if (candidateId != null) { json.put("decisionCandidateId", candidateId); }
        return this.mapper.writeValueAsString(json);
    }

    private static final class RecordingCandidate implements PeonDecisionCandidate {
        private final String id;
        private final List<UUID> peons = new ArrayList<>();

        private RecordingCandidate(String id) { this.id = id; }
        @Override
        public String candidateId() { return this.id; }
        @Override
        public PeonDecision decide(PeonDecisionContext context) {
            this.peons.add(context.peonId());
            return new PeonDecision(PeonAction.idle(PeonPurpose.SURVIVRE, this.id), "test", 1, false, List.of());
        }
    }
}
