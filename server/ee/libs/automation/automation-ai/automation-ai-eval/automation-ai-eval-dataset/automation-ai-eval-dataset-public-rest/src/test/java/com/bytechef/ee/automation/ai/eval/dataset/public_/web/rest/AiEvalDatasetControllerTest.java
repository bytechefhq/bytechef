/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.dataset.public_.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.automation.ai.eval.dataset.service.WorkspaceAiEvalDatasetService;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.AiGatewayExceptionHandler;
import com.bytechef.ee.automation.ai.gateway.public_.workspace.AiGatewayWorkspaceHeaderResolver;
import com.bytechef.ee.automation.ai.gateway.ratelimit.AiGatewayRateLimitChecker;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDataset;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetItem;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetVersion;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetItemService;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetService;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetVersionService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimitResult;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreTargetType;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreWorkspaceBoundaryException;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import java.lang.reflect.Field;
import java.time.Instant;
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
@ContextConfiguration(classes = AiEvalDatasetControllerTest.TestConfig.class)
@WebMvcTest(AiEvalDatasetController.class)
@TestPropertySource(properties = {
    "bytechef.edition=ee", "bytechef.ai.gateway.enabled=true"
})
class AiEvalDatasetControllerTest {

    @EnableAutoConfiguration
    @SpringBootConfiguration
    static class TestConfig {

        @Bean
        AiEvalDatasetController aiEvalDatasetController(
            AiEvalDatasetService aiEvalDatasetService,
            WorkspaceAiEvalDatasetService workspaceAiEvalDatasetService,
            AiEvalDatasetVersionService aiEvalDatasetVersionService,
            AiEvalDatasetItemService aiEvalDatasetItemService,
            AiObservabilityTraceService aiObservabilityTraceService,
            AiGatewayRateLimitChecker aiGatewayRateLimitChecker,
            AiGatewayWorkspaceHeaderResolver workspaceHeaderResolver,
            ObjectMapper objectMapper,
            WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService) {

            return new AiEvalDatasetController(aiEvalDatasetService, workspaceAiObservabilityTraceService,
                workspaceAiEvalDatasetService, aiEvalDatasetVersionService, aiEvalDatasetItemService,
                aiObservabilityTraceService, aiGatewayRateLimitChecker, workspaceHeaderResolver, objectMapper);
        }

        @Bean
        AiGatewayExceptionHandler aiGatewayExceptionHandler() {
            return new AiGatewayExceptionHandler();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiEvalDatasetService aiEvalDatasetService;

    @MockitoBean
    private WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;

    @MockitoBean
    private WorkspaceAiEvalDatasetService workspaceAiEvalDatasetService;

    @MockitoBean
    private AiEvalDatasetVersionService aiEvalDatasetVersionService;

    @MockitoBean
    private AiEvalDatasetItemService aiEvalDatasetItemService;

    @MockitoBean
    private AiObservabilityTraceService aiObservabilityTraceService;

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
    void testCreateDatasetReturns201() throws Exception {
        AiEvalDataset created = buildDataset(10L, 42L, "My dataset");

        when(workspaceAiEvalDatasetService.createInWorkspace(any(), anyLong()))
            .thenReturn(created);

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/datasets")
                    .header("X-ByteChef-Workspace-Id", "42")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "name": "My dataset",
                            "description": "First dataset",
                            "tags": ["smoke", "v1"]
                        }
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.workspaceId").value(42))
            .andExpect(jsonPath("$.name").value("My dataset"));

        verify(workspaceAiEvalDatasetService).createInWorkspace(any(), anyLong());
    }

    @Test
    void testListDatasetsReturns200() throws Exception {
        when(workspaceAiEvalDatasetService.findAllByWorkspace(42L))
            .thenReturn(List.of(
                buildDataset(1L, 42L, "alpha"),
                buildDataset(2L, 42L, "beta")));

        mockMvc
            .perform(
                get("/api/ai-gateway/v1/datasets")
                    .header("X-ByteChef-Workspace-Id", "42"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("alpha"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].name").value("beta"));
    }

    @Test
    void testAddItemReturns201() throws Exception {
        when(aiEvalDatasetService.getDataset(10L))
            .thenReturn(buildDataset(10L, 42L, "d"));
        when(workspaceAiEvalDatasetService.getWorkspaceId(10L)).thenReturn(42L);

        AiEvalDatasetItem item = buildItem(100L, 10L, 5L, null);

        when(aiEvalDatasetItemService.addItem(eq(10L), any(), any(), any()))
            .thenReturn(item);

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/datasets/10/items")
                    .header("X-ByteChef-Workspace-Id", "42")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "input": {"messages": [{"role": "user", "content": "hi"}]},
                            "expectedOutput": "hello",
                            "metadata": {"tag": "greeting"}
                        }
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.datasetId").value(10))
            .andExpect(jsonPath("$.datasetVersionId").value(5));

        verify(aiEvalDatasetItemService).addItem(eq(10L), any(), any(), any());
    }

    @Test
    void testPromoteFromTraceReturns201() throws Exception {
        when(aiEvalDatasetService.getDataset(10L))
            .thenReturn(buildDataset(10L, 42L, "d"));
        when(workspaceAiEvalDatasetService.getWorkspaceId(10L)).thenReturn(42L);

        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.API);

        setField(trace, "id", 999L);
        setField(trace, "input", "trace input");

        when(aiObservabilityTraceService.getTrace(999L)).thenReturn(trace);
        when(workspaceAiObservabilityTraceService.getWorkspaceId(999L)).thenReturn(42L);

        AiEvalDatasetItem item = buildItem(101L, 10L, 5L, 999L);

        when(aiEvalDatasetItemService.addItemFromTrace(eq(10L), eq(999L), any(), any(), any()))
            .thenReturn(item);

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/datasets/10/items/from-trace")
                    .header("X-ByteChef-Workspace-Id", "42")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "traceId": 999,
                            "expectedOutput": "correct answer",
                            "metadata": {"promoted_by": "curator"}
                        }
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(101))
            .andExpect(jsonPath("$.sourceTraceId").value(999));

        verify(aiEvalDatasetItemService).addItemFromTrace(eq(10L), eq(999L), any(), any(), any());
    }

    @Test
    void testBulkAddReturns201WithCounts() throws Exception {
        when(aiEvalDatasetService.getDataset(10L))
            .thenReturn(buildDataset(10L, 42L, "d"));
        when(workspaceAiEvalDatasetService.getWorkspaceId(10L)).thenReturn(42L);

        when(aiEvalDatasetItemService.addItems(eq(10L), any()))
            .thenReturn(List.of(
                buildItem(201L, 10L, 5L, null),
                buildItem(202L, 10L, 5L, null)));

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/datasets/10/items/bulk")
                    .header("X-ByteChef-Workspace-Id", "42")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "items": [
                                {"input": "a"},
                                {"input": "b"}
                            ]
                        }
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.createdCount").value(2))
            .andExpect(jsonPath("$.ids.length()").value(2))
            .andExpect(jsonPath("$.ids[0]").value(201))
            .andExpect(jsonPath("$.ids[1]").value(202));
    }

    @Test
    void testFreezeReturns200() throws Exception {
        when(aiEvalDatasetService.getDataset(10L))
            .thenReturn(buildDataset(10L, 42L, "d"));
        when(workspaceAiEvalDatasetService.getWorkspaceId(10L)).thenReturn(42L);

        AiEvalDatasetVersion existing = new AiEvalDatasetVersion(10L, 1);

        setField(existing, "id", 5L);

        when(aiEvalDatasetVersionService.getVersion(5L))
            .thenReturn(existing);

        AiEvalDatasetVersion frozen = new AiEvalDatasetVersion(10L, 1);

        setField(frozen, "id", 5L);
        frozen.freeze();

        when(aiEvalDatasetVersionService.freeze(5L))
            .thenReturn(frozen);

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/datasets/10/versions/5/freeze")
                    .header("X-ByteChef-Workspace-Id", "42"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.versionId").value(5))
            .andExpect(jsonPath("$.frozen").value(true));
    }

    @Test
    void testFreezeReturns403WhenVersionBelongsToDifferentDataset() throws Exception {
        // Caller owns dataset 10 in workspace 42 ...
        when(aiEvalDatasetService.getDataset(10L))
            .thenReturn(buildDataset(10L, 42L, "d"));
        when(workspaceAiEvalDatasetService.getWorkspaceId(10L)).thenReturn(42L);

        // ... but the version they're trying to freeze belongs to dataset 999 (potentially in another workspace).
        // Without the parent-dataset check the controller would happily call freeze() on it.
        AiEvalDatasetVersion otherDatasetVersion = new AiEvalDatasetVersion(999L, 1);

        setField(otherDatasetVersion, "id", 5L);

        when(aiEvalDatasetVersionService.getVersion(5L))
            .thenReturn(otherDatasetVersion);

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/datasets/10/versions/5/freeze")
                    .header("X-ByteChef-Workspace-Id", "42"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.type").value("workspace_boundary"));
    }

    @Test
    void testReturns400WhenWorkspaceHeaderMissing() throws Exception {
        mockMvc
            .perform(
                post("/api/ai-gateway/v1/datasets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "name": "ds"
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
                post("/api/ai-gateway/v1/datasets")
                    .header("X-ByteChef-Workspace-Id", "999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "name": "ds"
                        }
                        """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.type").value("workspace_boundary"));
    }

    @Test
    void testAddItemReturns404ForMissingDataset() throws Exception {
        when(aiEvalDatasetService.getDataset(999L))
            .thenThrow(new IllegalArgumentException("AiEvalDataset not found with id: 999"));

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/datasets/999/items")
                    .header("X-ByteChef-Workspace-Id", "42")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "input": "anything"
                        }
                        """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.type").value("target_not_found"))
            .andExpect(jsonPath("$.error.message").value("Dataset 999 not found"));
    }

    @Test
    void testReturns429WhenRateLimited() throws Exception {
        when(aiGatewayRateLimitChecker.checkWorkspaceRequest(42L, "datasets"))
            .thenReturn(AiGatewayRateLimitResult.rejected(0, System.currentTimeMillis() + 30_000));

        mockMvc
            .perform(
                get("/api/ai-gateway/v1/datasets")
                    .header("X-ByteChef-Workspace-Id", "42"))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.error.type").value("rate_limit_exceeded"))
            .andExpect(jsonPath("$.error.message").exists());
    }

    @Test
    void testReturns403OnCrossWorkspace() throws Exception {
        when(aiEvalDatasetService.getDataset(anyLong()))
            .thenReturn(buildDataset(10L, 99L, "d"));
        when(workspaceAiEvalDatasetService.getWorkspaceId(10L)).thenReturn(99L);

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/datasets/10/items")
                    .header("X-ByteChef-Workspace-Id", "42")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "input": "some input"
                        }
                        """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.type").value("workspace_boundary"));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static AiEvalDataset buildDataset(long id, long workspaceId, String name) {
        AiEvalDataset dataset = new AiEvalDataset(name);

        setField(dataset, "id", id);
        // createdDate is normally populated by Spring Data JDBC auditing on insert; mock-context entities
        // skip that path, so seed it here so DatasetView's non-null contract holds.
        setField(dataset, "createdDate", Instant.parse("2026-04-22T10:00:00Z"));

        return dataset;
    }

    private static AiEvalDatasetItem buildItem(long id, long datasetId, long versionId, Long sourceTraceId) {
        AiEvalDatasetItem item = new AiEvalDatasetItem(datasetId, versionId, "{}");

        setField(item, "id", id);
        setField(item, "createdDate", Instant.parse("2026-04-22T10:00:00Z"));

        if (sourceTraceId != null) {
            item.linkSourceTrace(sourceTraceId);
        }

        return item;
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
