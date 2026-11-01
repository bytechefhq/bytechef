/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.public_.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.embedded.execution.facade.ToolInvocationFacade;
import com.bytechef.ee.embedded.execution.public_.web.rest.config.EmbeddedExecutionPublicRestTestConfiguration;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLog;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = EmbeddedExecutionPublicRestTestConfiguration.class)
@TestPropertySource(properties = "bytechef.edition=ee")
@WebMvcTest(ToolInvocationApiController.class)
@WithMockUser(username = ToolInvocationApiControllerIntTest.EXTERNAL_USER_ID)
class ToolInvocationApiControllerIntTest {

    static final String EXTERNAL_USER_ID = "external-user-1";

    private static final Instant CREATED_DATE = Instant.parse("2026-07-18T10:30:00Z");
    private static final List<ToolExecutionSurface> EMBEDDED_SURFACES = List.of(
        ToolExecutionSurface.MCP_EMBEDDED, ToolExecutionSurface.EMBEDDED_API_ACTION,
        ToolExecutionSurface.EMBEDDED_API_TOOL);
    private static final Instant END_DATE = Instant.parse("2026-07-18T11:00:00Z");
    private static final Instant START_DATE = Instant.parse("2026-07-18T10:00:00Z");

    @MockitoBean
    private EnvironmentService environmentService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ToolInvocationFacade toolInvocationFacade;

    private final AtomicLong toolInvocationLogId = new AtomicLong();

    @Test
    void testGetToolInvocationsPagePassesTheFiltersAndMapsTheInvocations() throws Exception {
        ToolInvocationLog toolInvocationLog = createToolInvocationLog(
            ToolExecutionSurface.EMBEDDED_API_ACTION, ToolExecutionKind.COMPONENT, ToolExecutionOutcome.ERROR);

        toolInvocationLog.setErrorMessage("Channel not found");
        toolInvocationLog.setErrorType("IllegalStateException");
        toolInvocationLog.setIntegrationInstanceId(4L);

        when(environmentService.getEnvironment("STAGING")).thenReturn(Environment.STAGING);
        when(
            toolInvocationFacade.getToolInvocations(
                EXTERNAL_USER_ID, Environment.STAGING, ToolExecutionSurface.EMBEDDED_API_ACTION,
                ToolExecutionOutcome.ERROR, START_DATE, END_DATE, 2))
                    .thenReturn(new PageImpl<>(List.of(toolInvocationLog), PageRequest.of(2, 20), 41));

        mockMvc
            .perform(
                get("/v1/{externalUserId}/tool-invocations", EXTERNAL_USER_ID)
                    .header("X-Environment", "STAGING")
                    .param("surface", "EMBEDDED_API_ACTION")
                    .param("outcome", "ERROR")
                    .param("startDate", START_DATE.toString())
                    .param("endDate", END_DATE.toString())
                    .param("pageNumber", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.number").value(2))
            .andExpect(jsonPath("$.numberOfElements").value(1))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(41))
            .andExpect(jsonPath("$.totalPages").value(3))
            .andExpect(jsonPath("$.pageable").doesNotExist())
            .andExpect(jsonPath("$.sort").doesNotExist())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].componentName").value("slack"))
            .andExpect(jsonPath("$.content[0].componentVersion").value(1))
            .andExpect(jsonPath("$.content[0].durationMs").value(42))
            .andExpect(jsonPath("$.content[0].errorMessage").value("Channel not found"))
            .andExpect(jsonPath("$.content[0].errorType").value("IllegalStateException"))
            .andExpect(jsonPath("$.content[0].integrationInstanceId").value(4))
            .andExpect(jsonPath("$.content[0].kind").value("COMPONENT"))
            .andExpect(jsonPath("$.content[0].operationName").value("sendMessage"))
            .andExpect(jsonPath("$.content[0].outcome").value("ERROR"))
            .andExpect(jsonPath("$.content[0].surface").value("EMBEDDED_API_ACTION"))
            .andExpect(jsonPath("$.content[0].toolName").value("sendMessage"));
    }

    @Test
    void testGetToolInvocationsPageDefaultsToTheDefaultEnvironmentAndTheFirstPage() throws Exception {
        when(environmentService.getEnvironment((String) null)).thenReturn(Environment.PRODUCTION);
        when(
            toolInvocationFacade.getToolInvocations(
                eq(EXTERNAL_USER_ID), eq(Environment.PRODUCTION), isNull(), isNull(), isNull(), isNull(), eq(0)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc
            .perform(get("/v1/{externalUserId}/tool-invocations", EXTERNAL_USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0));

        verify(toolInvocationFacade).getToolInvocations(
            eq(EXTERNAL_USER_ID), eq(Environment.PRODUCTION), isNull(), isNull(), isNull(), isNull(), eq(0));
    }

    @Test
    void testGetToolInvocationsPageReturnsInvocationsOfEveryOutcome() throws Exception {
        List<ToolInvocationLog> toolInvocationLogs = Arrays.stream(ToolExecutionOutcome.values())
            .map(outcome -> createToolInvocationLog(
                ToolExecutionSurface.MCP_EMBEDDED, ToolExecutionKind.WORKFLOW, outcome))
            .toList();

        mockToolInvocations(toolInvocationLogs);

        mockMvc
            .perform(get("/v1/{externalUserId}/tool-invocations", EXTERNAL_USER_ID))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content[*].outcome").value(
                    Matchers.contains(
                        Arrays.stream(ToolExecutionOutcome.values())
                            .map(ToolExecutionOutcome::name)
                            .toArray())));
    }

    @Test
    void testGetToolInvocationsPageReturnsInvocationsOfEveryKind() throws Exception {
        List<ToolInvocationLog> toolInvocationLogs = Arrays.stream(ToolExecutionKind.values())
            .map(kind -> createToolInvocationLog(ToolExecutionSurface.MCP_EMBEDDED, kind, ToolExecutionOutcome.SUCCESS))
            .toList();

        mockToolInvocations(toolInvocationLogs);

        mockMvc
            .perform(get("/v1/{externalUserId}/tool-invocations", EXTERNAL_USER_ID))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content[*].kind").value(
                    Matchers.contains(
                        Arrays.stream(ToolExecutionKind.values())
                            .map(ToolExecutionKind::name)
                            .toArray())));
    }

    @Test
    void testGetToolInvocationsPageReturnsInvocationsOfEveryEmbeddedSurface() throws Exception {
        List<ToolInvocationLog> toolInvocationLogs = EMBEDDED_SURFACES.stream()
            .map(surface -> createToolInvocationLog(surface, ToolExecutionKind.COMPONENT, ToolExecutionOutcome.SUCCESS))
            .toList();

        mockToolInvocations(toolInvocationLogs);

        mockMvc
            .perform(get("/v1/{externalUserId}/tool-invocations", EXTERNAL_USER_ID))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content[*].surface").value(
                    Matchers.contains(
                        EMBEDDED_SURFACES.stream()
                            .map(ToolExecutionSurface::name)
                            .toArray())));
    }

    @Test
    void testGetToolInvocationsPageAcceptsEveryOutcomeAsAFilter() throws Exception {
        when(environmentService.getEnvironment((String) null)).thenReturn(Environment.PRODUCTION);
        when(toolInvocationFacade.getToolInvocations(any(), any(), any(), any(), any(), any(), anyInt()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        for (ToolExecutionOutcome outcome : ToolExecutionOutcome.values()) {
            mockMvc
                .perform(
                    get("/v1/{externalUserId}/tool-invocations", EXTERNAL_USER_ID)
                        .param("outcome", outcome.name()))
                .andExpect(status().isOk());

            verify(toolInvocationFacade).getToolInvocations(
                eq(EXTERNAL_USER_ID), eq(Environment.PRODUCTION), isNull(), eq(outcome), isNull(), isNull(), eq(0));
        }
    }

    private ToolInvocationLog createToolInvocationLog(
        ToolExecutionSurface surface, ToolExecutionKind kind, ToolExecutionOutcome outcome) {

        ToolInvocationLog toolInvocationLog = new ToolInvocationLog();

        toolInvocationLog.setId(toolInvocationLogId.incrementAndGet());
        toolInvocationLog.setComponentName("slack");
        toolInvocationLog.setComponentVersion(1);
        toolInvocationLog.setCreatedDate(CREATED_DATE);
        toolInvocationLog.setDurationMs(42);
        toolInvocationLog.setKind(kind);
        toolInvocationLog.setOperationName("sendMessage");
        toolInvocationLog.setOutcome(outcome);
        toolInvocationLog.setSurface(surface);
        toolInvocationLog.setToolName("sendMessage");

        return toolInvocationLog;
    }

    private void mockToolInvocations(List<ToolInvocationLog> toolInvocationLogs) {
        when(environmentService.getEnvironment((String) null)).thenReturn(Environment.PRODUCTION);
        when(
            toolInvocationFacade.getToolInvocations(
                eq(EXTERNAL_USER_ID), eq(Environment.PRODUCTION), isNull(), isNull(), isNull(), isNull(), eq(0)))
                    .thenReturn(new PageImpl<>(toolInvocationLogs, PageRequest.of(0, 20), toolInvocationLogs.size()));
    }
}
