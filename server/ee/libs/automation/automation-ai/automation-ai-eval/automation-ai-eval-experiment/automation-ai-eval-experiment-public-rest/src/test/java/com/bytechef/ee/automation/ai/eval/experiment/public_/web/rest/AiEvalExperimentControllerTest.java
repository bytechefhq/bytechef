/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.public_.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.automation.ai.eval.dataset.service.WorkspaceAiEvalDatasetService;
import com.bytechef.ee.automation.ai.eval.experiment.executor.AiEvalExperimentExecutor;
import com.bytechef.ee.automation.ai.eval.experiment.service.WorkspaceAiEvalExperimentService;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.AiGatewayExceptionHandler;
import com.bytechef.ee.automation.ai.gateway.public_.workspace.AiGatewayWorkspaceHeaderResolver;
import com.bytechef.ee.automation.ai.gateway.ratelimit.AiGatewayRateLimitChecker;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDataset;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetVersion;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetService;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetVersionService;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperiment;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRun;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRunStatus;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentRunService;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimitResult;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreTargetType;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreWorkspaceBoundaryException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 * @version ee
 */
@ContextConfiguration(classes = AiEvalExperimentControllerTest.TestConfig.class)
@WebMvcTest(AiEvalExperimentController.class)
@TestPropertySource(properties = {
    "bytechef.edition=ee", "bytechef.ai.gateway.enabled=true"
})
class AiEvalExperimentControllerTest {

    @EnableAutoConfiguration
    @SpringBootConfiguration
    static class TestConfig {

        @Bean
        AiEvalExperimentController aiEvalExperimentController(
            AiEvalExperimentService aiEvalExperimentService,
            WorkspaceAiEvalExperimentService workspaceAiEvalExperimentService,
            AiEvalExperimentRunService aiEvalExperimentRunService,
            AiEvalExperimentExecutor aiEvalExperimentExecutor,
            AiEvalDatasetService aiEvalDatasetService,
            WorkspaceAiEvalDatasetService workspaceAiEvalDatasetService,
            AiEvalDatasetVersionService aiEvalDatasetVersionService,
            AiGatewayRateLimitChecker aiGatewayRateLimitChecker,
            AiGatewayWorkspaceHeaderResolver workspaceHeaderResolver,
            ObjectMapper objectMapper) {

            return new AiEvalExperimentController(
                aiEvalExperimentService, workspaceAiEvalExperimentService, aiEvalExperimentRunService,
                aiEvalExperimentExecutor, aiEvalDatasetService, workspaceAiEvalDatasetService,
                aiEvalDatasetVersionService, aiGatewayRateLimitChecker, workspaceHeaderResolver, objectMapper);
        }

        @Bean
        AiGatewayExceptionHandler aiGatewayExceptionHandler() {
            return new AiGatewayExceptionHandler();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiEvalExperimentService aiEvalExperimentService;

    @MockitoBean
    private WorkspaceAiEvalExperimentService workspaceAiEvalExperimentService;

    @MockitoBean
    private AiEvalExperimentRunService aiEvalExperimentRunService;

    @MockitoBean
    private AiEvalExperimentExecutor aiEvalExperimentExecutor;

    @MockitoBean
    private AiEvalDatasetService aiEvalDatasetService;

    @MockitoBean
    private WorkspaceAiEvalDatasetService workspaceAiEvalDatasetService;

    @MockitoBean
    private AiEvalDatasetVersionService aiEvalDatasetVersionService;

    @MockitoBean
    private AiGatewayRateLimitChecker aiGatewayRateLimitChecker;

    @MockitoBean
    private AiGatewayWorkspaceHeaderResolver workspaceHeaderResolver;

    @BeforeEach
    void setUp() {
        when(workspaceHeaderResolver.resolveAndVerify("42"))
            .thenReturn(42L);
        when(workspaceHeaderResolver.resolveAndVerify((String) null))
            .thenReturn(null);

        when(aiGatewayRateLimitChecker.checkWorkspaceRequest(anyLong(), anyString()))
            .thenReturn(AiGatewayRateLimitResult.allowed(100, System.currentTimeMillis() + 60_000));
    }

    @Test
    void testCreateReturns202() throws Exception {
        AiEvalExperiment created = buildExperiment(123L, 42L, 7L, 100L, "gpt-4o");

        stubDatasetVersion(7L, 99L, 42L);

        when(workspaceAiEvalExperimentService.createInWorkspace(any(), anyLong()))
            .thenReturn(created);

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/experiments")
                    .header("X-ByteChef-Workspace-Id", "42")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "datasetVersionId": 7,
                            "promptVersionId": 100,
                            "model": "gpt-4o",
                            "metadata": {"notes": "baseline run"}
                        }
                        """))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.id").value(123))
            .andExpect(jsonPath("$.workspaceId").value(42))
            .andExpect(jsonPath("$.datasetVersionId").value(7))
            .andExpect(jsonPath("$.promptVersionId").value(100))
            .andExpect(jsonPath("$.model").value("gpt-4o"))
            .andExpect(jsonPath("$.status").value("PENDING"));

        verify(workspaceAiEvalExperimentService).createInWorkspace(any(), anyLong());
        verify(aiEvalExperimentExecutor).execute(123L);
    }

    @Test
    void testCreateReturns403WhenDatasetVersionBelongsToDifferentWorkspace() throws Exception {
        // Caller is in workspace 42, but datasetVersionId 7 -> dataset 99 -> workspace 88. Without the boundary check
        // the controller would happily replay workspace 88's items into workspace 42.
        stubDatasetVersion(7L, 99L, 88L);

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/experiments")
                    .header("X-ByteChef-Workspace-Id", "42")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "datasetVersionId": 7,
                            "promptVersionId": 100,
                            "model": "gpt-4o"
                        }
                        """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.type").value("workspace_boundary"));

        // The experiment must not be persisted nor the executor kicked off when the boundary check fails.
        verify(workspaceAiEvalExperimentService, org.mockito.Mockito.never()).createInWorkspace(any(), anyLong());
        verify(aiEvalExperimentExecutor, org.mockito.Mockito.never()).execute(anyLong());
    }

    private void stubDatasetVersion(long versionId, long datasetId, long datasetWorkspaceId) {
        AiEvalDatasetVersion datasetVersion = new AiEvalDatasetVersion(datasetId, 1);

        setField(datasetVersion, "id", versionId);

        when(aiEvalDatasetVersionService.getVersion(versionId)).thenReturn(datasetVersion);

        AiEvalDataset dataset = new AiEvalDataset("ds");

        setField(dataset, "id", datasetId);

        when(aiEvalDatasetService.getDataset(datasetId)).thenReturn(dataset);
        when(workspaceAiEvalDatasetService.getWorkspaceId(datasetId)).thenReturn(datasetWorkspaceId);
    }

    @Test
    void testGetSummaryReturns200WithRunCounts() throws Exception {
        AiEvalExperiment experiment = buildExperiment(123L, 42L, 7L, 100L, "gpt-4o");

        when(aiEvalExperimentService.getExperiment(123L))
            .thenReturn(experiment);
        when(aiEvalExperimentRunService.countByExperiment(123L))
            .thenReturn(10L);
        when(aiEvalExperimentRunService.countByExperimentAndStatus(123L, AiEvalExperimentRunStatus.COMPLETED))
            .thenReturn(8L);
        when(aiEvalExperimentRunService.countByExperimentAndStatus(123L, AiEvalExperimentRunStatus.FAILED))
            .thenReturn(2L);

        mockMvc
            .perform(
                get("/api/ai-gateway/v1/experiments/123")
                    .header("X-ByteChef-Workspace-Id", "42"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(123))
            .andExpect(jsonPath("$.workspaceId").value(42))
            .andExpect(jsonPath("$.datasetVersionId").value(7))
            .andExpect(jsonPath("$.promptVersionId").value(100))
            .andExpect(jsonPath("$.model").value("gpt-4o"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.totalRuns").value(10))
            .andExpect(jsonPath("$.completedRuns").value(8))
            .andExpect(jsonPath("$.failedRuns").value(2));
    }

    @Test
    void testListRunsReturns200WithRunArray() throws Exception {
        AiEvalExperiment experiment = buildExperiment(123L, 42L, 7L, 100L, "gpt-4o");

        when(aiEvalExperimentService.getExperiment(123L))
            .thenReturn(experiment);
        when(aiEvalExperimentRunService.findAllByExperiment(123L))
            .thenReturn(List.of(
                buildRun(501L, 123L, 11L, 900L, AiEvalExperimentRunStatus.COMPLETED, 120, new BigDecimal("0.0012")),
                buildRun(502L, 123L, 12L, null, AiEvalExperimentRunStatus.PENDING, null, null)));

        mockMvc
            .perform(
                get("/api/ai-gateway/v1/experiments/123/runs")
                    .header("X-ByteChef-Workspace-Id", "42"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(501))
            .andExpect(jsonPath("$[0].experimentId").value(123))
            .andExpect(jsonPath("$[0].datasetItemId").value(11))
            .andExpect(jsonPath("$[0].traceId").value(900))
            .andExpect(jsonPath("$[0].status").value("COMPLETED"))
            .andExpect(jsonPath("$[0].latencyMs").value(120))
            .andExpect(jsonPath("$[1].id").value(502))
            .andExpect(jsonPath("$[1].status").value("PENDING"));
    }

    @Test
    void testReturns400WhenWorkspaceHeaderMissing() throws Exception {
        mockMvc
            .perform(
                post("/api/ai-gateway/v1/experiments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "datasetVersionId": 7,
                            "model": "gpt-4o"
                        }
                        """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.type").value("missing_workspace_id"))
            .andExpect(jsonPath("$.error.message").exists());
    }

    @Test
    void testReturns403WhenCallerNotMemberOfWorkspace() throws Exception {
        when(workspaceHeaderResolver.resolveAndVerify("999"))
            .thenThrow(AiScoreWorkspaceBoundaryException.forTarget(
                999L, AiScoreTargetType.WORKSPACE, 999L));

        mockMvc
            .perform(
                get("/api/ai-gateway/v1/experiments/123")
                    .header("X-ByteChef-Workspace-Id", "999"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.type").value("workspace_boundary"));
    }

    @Test
    void testReturns403OnCrossWorkspace() throws Exception {
        AiEvalExperiment foreignExperiment = buildExperiment(123L, 99L, 7L, 100L, "gpt-4o");

        when(aiEvalExperimentService.getExperiment(anyLong()))
            .thenReturn(foreignExperiment);

        mockMvc
            .perform(
                get("/api/ai-gateway/v1/experiments/123")
                    .header("X-ByteChef-Workspace-Id", "42"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.type").value("workspace_boundary"));
    }

    @Test
    void testStopReturns202() throws Exception {
        AiEvalExperiment experiment = buildExperiment(123L, 42L, 7L, 100L, "gpt-4o");

        when(aiEvalExperimentService.getExperiment(123L))
            .thenReturn(experiment);
        when(aiEvalExperimentService.requestStop(123L))
            .thenReturn(experiment);

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/experiments/123/stop")
                    .header("X-ByteChef-Workspace-Id", "42"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.id").value(123))
            .andExpect(jsonPath("$.stopRequested").value(true));

        verify(aiEvalExperimentService).requestStop(123L);
    }

    @Test
    void testStopReturns400WhenWorkspaceHeaderMissing() throws Exception {
        mockMvc
            .perform(
                post("/api/ai-gateway/v1/experiments/123/stop"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.type").value("missing_workspace_id"))
            .andExpect(jsonPath("$.error.message").exists());
    }

    @Test
    void testStopReturns403OnCrossWorkspace() throws Exception {
        AiEvalExperiment foreignExperiment = buildExperiment(123L, 99L, 7L, 100L, "gpt-4o");

        when(aiEvalExperimentService.getExperiment(anyLong()))
            .thenReturn(foreignExperiment);

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/experiments/123/stop")
                    .header("X-ByteChef-Workspace-Id", "42"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.type").value("workspace_boundary"));
    }

    /**
     * Stop is a security-adjacent control surface — operators rely on it to halt runaway experiments. It must be
     * idempotent: a second call (or a call after the experiment has already terminated) must NOT 500, must NOT change
     * the response shape, and must continue to delegate to the service so the FSM-level idempotency
     * (already-stop-requested or already-terminal) is exercised end-to-end. A regression that 500-ed on the second call
     * would silently strand operators trying to confirm a stop landed.
     */
    @Test
    void testStopIsIdempotent() throws Exception {
        AiEvalExperiment experiment = buildExperiment(123L, 42L, 7L, 100L, "gpt-4o");

        when(aiEvalExperimentService.getExperiment(123L))
            .thenReturn(experiment);
        when(aiEvalExperimentService.requestStop(123L))
            .thenReturn(experiment);

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/experiments/123/stop")
                    .header("X-ByteChef-Workspace-Id", "42"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.id").value(123))
            .andExpect(jsonPath("$.stopRequested").value(true));

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/experiments/123/stop")
                    .header("X-ByteChef-Workspace-Id", "42"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.id").value(123))
            .andExpect(jsonPath("$.stopRequested").value(true));

        verify(aiEvalExperimentService, org.mockito.Mockito.times(2))
            .requestStop(123L);
    }

    @Test
    void testGetSummaryReturns404ForMissingExperiment() throws Exception {
        when(aiEvalExperimentService.getExperiment(999L))
            .thenThrow(new IllegalArgumentException("AiEvalExperiment not found with id: 999"));

        mockMvc
            .perform(
                get("/api/ai-gateway/v1/experiments/999")
                    .header("X-ByteChef-Workspace-Id", "42"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.type").value("target_not_found"))
            .andExpect(jsonPath("$.error.message").value("Experiment 999 not found"));
    }

    @Test
    void testReturns429WhenRateLimited() throws Exception {
        when(aiGatewayRateLimitChecker.checkWorkspaceRequest(42L, "experiments"))
            .thenReturn(AiGatewayRateLimitResult.rejected(0, System.currentTimeMillis() + 30_000));

        mockMvc
            .perform(
                get("/api/ai-gateway/v1/experiments/123")
                    .header("X-ByteChef-Workspace-Id", "42"))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.error.type").value("rate_limit_exceeded"))
            .andExpect(jsonPath("$.error.message").exists());
    }

    private AiEvalExperiment buildExperiment(
        long id, long workspaceId, long datasetVersionId, Long promptVersionId, String model) {

        AiEvalExperiment experiment = new AiEvalExperiment(datasetVersionId);

        experiment.setPromptVersionId(promptVersionId);
        experiment.setModel(model);

        setField(experiment, "id", id);

        // Workspace assignment lives on workspace_ai_eval_experiment now; stub the service helper that the boundary
        // check
        // queries.
        org.mockito.Mockito.lenient()
            .when(workspaceAiEvalExperimentService.getWorkspaceId(id))
            .thenReturn(workspaceId);

        return experiment;
    }

    private static AiEvalExperimentRun buildRun(
        long id, long experimentId, long datasetItemId, Long traceId, AiEvalExperimentRunStatus status,
        Integer latencyMs, BigDecimal cost) {

        AiEvalExperimentRun run = new AiEvalExperimentRun(experimentId, datasetItemId);

        // Domain mutators are package-private and owned by the FSM. Tests build canonical states by writing
        // the persisted ordinal/columns directly via reflection — same mechanism Spring Data JDBC uses on read.
        setField(run, "status", status.ordinal());

        if (traceId != null) {
            setField(run, "traceId", traceId);
        }

        if (latencyMs != null) {
            setField(run, "latencyMs", latencyMs);
        }

        if (cost != null) {
            setField(run, "cost", cost);
        }

        setField(run, "id", id);

        return run;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass()
                .getDeclaredField(fieldName);

            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException(reflectiveOperationException);
        }
    }
}
