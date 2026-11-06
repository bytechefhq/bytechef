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
import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.external.invocation.ExternalInvocationKind;
import com.bytechef.platform.external.invocation.ExternalInvocationOutcome;
import com.bytechef.platform.external.invocation.ExternalInvocationSurface;
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
    private static final List<ExternalInvocationSurface> EMBEDDED_SURFACES = List.of(
        ExternalInvocationSurface.MCP_EMBEDDED, ExternalInvocationSurface.EMBEDDED_API_ACTION,
        ExternalInvocationSurface.EMBEDDED_API_TOOL);
    private static final Instant END_DATE = Instant.parse("2026-07-18T11:00:00Z");
    private static final Instant START_DATE = Instant.parse("2026-07-18T10:00:00Z");

    @MockitoBean
    private EnvironmentService environmentService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ToolInvocationFacade toolInvocationFacade;

    private final AtomicLong externalInvocationLogId = new AtomicLong();

    @Test
    void testGetToolInvocationsPagePassesTheFiltersAndMapsTheInvocations() throws Exception {
        ExternalInvocationLog externalInvocationLog = createExternalInvocationLog(
            ExternalInvocationSurface.EMBEDDED_API_ACTION, ExternalInvocationKind.COMPONENT,
            ExternalInvocationOutcome.ERROR);

        externalInvocationLog.setErrorMessage("Channel not found");
        externalInvocationLog.setErrorType("IllegalStateException");
        externalInvocationLog.setIntegrationInstanceId(4L);

        when(environmentService.getEnvironment("STAGING")).thenReturn(Environment.STAGING);
        when(
            toolInvocationFacade.getToolInvocations(
                EXTERNAL_USER_ID, Environment.STAGING, ExternalInvocationSurface.EMBEDDED_API_ACTION,
                ExternalInvocationOutcome.ERROR, START_DATE, END_DATE, 2))
                    .thenReturn(new PageImpl<>(List.of(externalInvocationLog), PageRequest.of(2, 20), 41));

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
        List<ExternalInvocationLog> externalInvocationLogs = Arrays.stream(ExternalInvocationOutcome.values())
            .map(outcome -> createExternalInvocationLog(
                ExternalInvocationSurface.MCP_EMBEDDED, ExternalInvocationKind.WORKFLOW, outcome))
            .toList();

        mockToolInvocations(externalInvocationLogs);

        mockMvc
            .perform(get("/v1/{externalUserId}/tool-invocations", EXTERNAL_USER_ID))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content[*].outcome").value(
                    Matchers.contains(
                        Arrays.stream(ExternalInvocationOutcome.values())
                            .map(ExternalInvocationOutcome::name)
                            .toArray())));
    }

    @Test
    void testGetToolInvocationsPageReturnsInvocationsOfEveryKind() throws Exception {
        List<ExternalInvocationLog> externalInvocationLogs = Arrays.stream(ExternalInvocationKind.values())
            .map(kind -> createExternalInvocationLog(ExternalInvocationSurface.MCP_EMBEDDED, kind,
                ExternalInvocationOutcome.SUCCESS))
            .toList();

        mockToolInvocations(externalInvocationLogs);

        mockMvc
            .perform(get("/v1/{externalUserId}/tool-invocations", EXTERNAL_USER_ID))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content[*].kind").value(
                    Matchers.contains(
                        Arrays.stream(ExternalInvocationKind.values())
                            .map(ExternalInvocationKind::name)
                            .toArray())));
    }

    @Test
    void testGetToolInvocationsPageReturnsInvocationsOfEveryEmbeddedSurface() throws Exception {
        List<ExternalInvocationLog> externalInvocationLogs = EMBEDDED_SURFACES.stream()
            .map(surface -> createExternalInvocationLog(surface, ExternalInvocationKind.COMPONENT,
                ExternalInvocationOutcome.SUCCESS))
            .toList();

        mockToolInvocations(externalInvocationLogs);

        mockMvc
            .perform(get("/v1/{externalUserId}/tool-invocations", EXTERNAL_USER_ID))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content[*].surface").value(
                    Matchers.contains(
                        EMBEDDED_SURFACES.stream()
                            .map(ExternalInvocationSurface::name)
                            .toArray())));
    }

    @Test
    void testGetToolInvocationsPageAcceptsEveryOutcomeAsAFilter() throws Exception {
        when(environmentService.getEnvironment((String) null)).thenReturn(Environment.PRODUCTION);
        when(toolInvocationFacade.getToolInvocations(any(), any(), any(), any(), any(), any(), anyInt()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        for (ExternalInvocationOutcome outcome : ExternalInvocationOutcome.values()) {
            mockMvc
                .perform(
                    get("/v1/{externalUserId}/tool-invocations", EXTERNAL_USER_ID)
                        .param("outcome", outcome.name()))
                .andExpect(status().isOk());

            verify(toolInvocationFacade).getToolInvocations(
                eq(EXTERNAL_USER_ID), eq(Environment.PRODUCTION), isNull(), eq(outcome), isNull(), isNull(), eq(0));
        }
    }

    private ExternalInvocationLog createExternalInvocationLog(
        ExternalInvocationSurface surface, ExternalInvocationKind kind, ExternalInvocationOutcome outcome) {

        ExternalInvocationLog externalInvocationLog = new ExternalInvocationLog();

        externalInvocationLog.setId(externalInvocationLogId.incrementAndGet());
        externalInvocationLog.setComponentName("slack");
        externalInvocationLog.setComponentVersion(1);
        externalInvocationLog.setCreatedDate(CREATED_DATE);
        externalInvocationLog.setDurationMs(42);
        externalInvocationLog.setKind(kind);
        externalInvocationLog.setOperationName("sendMessage");
        externalInvocationLog.setOutcome(outcome);
        externalInvocationLog.setSurface(surface);
        externalInvocationLog.setToolName("sendMessage");

        return externalInvocationLog;
    }

    private void mockToolInvocations(List<ExternalInvocationLog> externalInvocationLogs) {
        when(environmentService.getEnvironment((String) null)).thenReturn(Environment.PRODUCTION);
        when(
            toolInvocationFacade.getToolInvocations(
                eq(EXTERNAL_USER_ID), eq(Environment.PRODUCTION), isNull(), isNull(), isNull(), isNull(), eq(0)))
                    .thenReturn(
                        new PageImpl<>(externalInvocationLogs, PageRequest.of(0, 20), externalInvocationLogs.size()));
    }
}
