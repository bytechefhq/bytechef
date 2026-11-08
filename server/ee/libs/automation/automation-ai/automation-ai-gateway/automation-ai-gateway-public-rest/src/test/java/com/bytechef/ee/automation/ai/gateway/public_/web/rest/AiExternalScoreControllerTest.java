/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.ai.gateway.facade.AiExternalScoreFacade;
import com.bytechef.ee.automation.ai.gateway.public_.workspace.AiGatewayWorkspaceHeaderResolver;
import com.bytechef.ee.automation.ai.gateway.ratelimit.AiGatewayRateLimitChecker;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimitResult;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreBatchResult;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreResult;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreTargetNotFoundException;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreTargetType;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreWorkspaceBoundaryException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
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

/**
 * @author Ivica Cardic
 * @version ee
 */
@ContextConfiguration(classes = AiExternalScoreControllerTest.TestConfig.class)
@WebMvcTest(AiExternalScoreController.class)
@TestPropertySource(properties = {
    "bytechef.edition=ee", "bytechef.ai.gateway.enabled=true"
})
class AiExternalScoreControllerTest {

    @EnableAutoConfiguration
    @SpringBootConfiguration
    static class TestConfig {

        @Bean
        AiExternalScoreController aiExternalScoreController(
            AiExternalScoreFacade aiExternalScoreFacade, AiGatewayRateLimitChecker aiGatewayRateLimitChecker,
            AiGatewayWorkspaceHeaderResolver workspaceHeaderResolver, ApplicationProperties applicationProperties) {

            return new AiExternalScoreController(
                aiExternalScoreFacade, aiGatewayRateLimitChecker, workspaceHeaderResolver, applicationProperties);
        }

        @Bean
        ApplicationProperties applicationProperties() {
            ApplicationProperties applicationProperties = new ApplicationProperties();

            ApplicationProperties.Ai.Gateway.ExternalScores externalScores = applicationProperties.getAi()
                .getGateway()
                .getExternalScores();

            externalScores.setMaxBatchSize(1_000);

            return applicationProperties;
        }

        @Bean
        AiGatewayExceptionHandler aiGatewayExceptionHandler() {
            return new AiGatewayExceptionHandler();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationProperties applicationProperties;

    @MockitoBean
    private AiExternalScoreFacade aiExternalScoreFacade;

    @MockitoBean
    private AiGatewayRateLimitChecker aiGatewayRateLimitChecker;

    @MockitoBean
    private AiGatewayWorkspaceHeaderResolver workspaceHeaderResolver;

    private ListAppender<ILoggingEvent> exceptionHandlerLogs;

    @BeforeEach
    void setUp() {
        when(workspaceHeaderResolver.resolveAndVerify("42"))
            .thenReturn(42L);
        when(workspaceHeaderResolver.resolveAndVerify((String) null))
            .thenReturn(null);

        exceptionHandlerLogs = new ListAppender<>();
        exceptionHandlerLogs.start();

        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(AiGatewayExceptionHandler.class))
            .addAppender(exceptionHandlerLogs);

        when(aiGatewayRateLimitChecker.checkWorkspaceRequest(anyLong(), anyString()))
            .thenReturn(AiGatewayRateLimitResult.allowed(100, System.currentTimeMillis() + 60_000));
    }

    @AfterEach
    void detachLogAppender() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(AiGatewayExceptionHandler.class))
            .detachAppender(exceptionHandlerLogs);
        exceptionHandlerLogs.stop();
    }

    @Test
    void testRecordTraceScoreReturns200() throws Exception {
        when(aiExternalScoreFacade.recordTraceScore(eq(42L), eq(1L), any()))
            .thenReturn(AiExternalScoreResult.accepted(99L));

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/traces/1/scores")
                    .header("X-ByteChef-Workspace-Id", "42")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "name": "faithfulness",
                            "value": 0.87,
                            "dataType": "NUMERIC",
                            "source": "ragas@0.2.3"
                        }
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scoreId").value(99))
            .andExpect(jsonPath("$.rejectionReason").doesNotExist());

        verify(aiExternalScoreFacade).recordTraceScore(eq(42L), eq(1L), any());
    }

    @Test
    void testRecordSpanScoreReturns200() throws Exception {
        when(aiExternalScoreFacade.recordSpanScore(eq(42L), eq(5L), any()))
            .thenReturn(AiExternalScoreResult.accepted(101L));

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/spans/5/scores")
                    .header("X-ByteChef-Workspace-Id", "42")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "name": "relevance",
                            "value": 1,
                            "dataType": "BOOLEAN",
                            "source": "internal-judge"
                        }
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scoreId").value(101))
            .andExpect(jsonPath("$.rejectionReason").doesNotExist());

        verify(aiExternalScoreFacade).recordSpanScore(eq(42L), eq(5L), any());
    }

    @Test
    void testRecordBatchReturns200() throws Exception {
        when(aiExternalScoreFacade.recordBatch(eq(42L), any()))
            .thenReturn(new AiExternalScoreBatchResult(2, 0, 0, List.of()));

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/scores/batch")
                    .header("X-ByteChef-Workspace-Id", "42")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "scores": [
                                {
                                    "traceId": 1,
                                    "name": "faithfulness",
                                    "value": 0.87,
                                    "dataType": "NUMERIC",
                                    "source": "ragas"
                                },
                                {
                                    "spanId": 5,
                                    "name": "relevance",
                                    "value": 1,
                                    "dataType": "BOOLEAN",
                                    "source": "internal-judge"
                                }
                            ]
                        }
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.acceptedCount").value(2))
            .andExpect(jsonPath("$.rejectedCount").value(0));
    }

    @Test
    void testReturns400WhenWorkspaceHeaderMissing() throws Exception {
        mockMvc
            .perform(
                post("/api/ai-gateway/v1/traces/1/scores")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "name": "faithfulness",
                            "value": 0.87,
                            "dataType": "NUMERIC",
                            "source": "ragas"
                        }
                        """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.type").value("missing_workspace_id"))
            .andExpect(jsonPath("$.error.message").exists());
    }

    @Test
    void testReturns403OnCrossWorkspace() throws Exception {
        when(aiExternalScoreFacade.recordTraceScore(any(), any(), any()))
            .thenThrow(AiScoreWorkspaceBoundaryException.forTarget(
                42L, AiScoreTargetType.TRACE, 1L));

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/traces/1/scores")
                    .header("X-ByteChef-Workspace-Id", "42")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "name": "faithfulness",
                            "value": 0.87,
                            "dataType": "NUMERIC",
                            "source": "ragas"
                        }
                        """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.type").value("workspace_boundary"));
    }

    @Test
    void testReturns403WhenCallerNotMemberOfWorkspace() throws Exception {
        when(workspaceHeaderResolver.resolveAndVerify("999"))
            .thenThrow(AiScoreWorkspaceBoundaryException.forTarget(
                999L, AiScoreTargetType.WORKSPACE, 999L));

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/traces/1/scores")
                    .header("X-ByteChef-Workspace-Id", "999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "name": "faithfulness",
                            "value": 0.87,
                            "dataType": "NUMERIC",
                            "source": "ragas"
                        }
                        """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.type").value("workspace_boundary"));

        assertThat(exceptionHandlerLogs.list)
            .as("AiGatewayExceptionHandler must log workspace-boundary breaches at ERROR")
            .filteredOn(event -> event.getLevel() == Level.ERROR)
            .anyMatch(event -> event.getFormattedMessage()
                .contains("Cross-workspace request rejected"));
    }

    @Test
    void testReturns429WhenRateLimited() throws Exception {
        long resetAtEpochMs = System.currentTimeMillis() + 30_000;

        when(aiGatewayRateLimitChecker.checkWorkspaceRequest(42L, "scores"))
            .thenReturn(AiGatewayRateLimitResult.rejected(0, resetAtEpochMs));

        org.springframework.test.web.servlet.MvcResult mvcResult = mockMvc
            .perform(
                post("/api/ai-gateway/v1/scores/batch")
                    .header("X-ByteChef-Workspace-Id", "42")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "scores": []
                        }
                        """))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.error.type").value("rate_limit_exceeded"))
            .andExpect(jsonPath("$.error.message").exists())
            .andReturn();

        String retryAfterHeader = mvcResult.getResponse()
            .getHeader("Retry-After");

        assertThat(retryAfterHeader)
            .as("Retry-After must carry the resetAtEpochMs delta in seconds (RFC 7231 §7.1.3)")
            .isNotNull();

        long retryAfterSeconds = Long.parseLong(retryAfterHeader);

        assertThat(retryAfterSeconds)
            .as("Retry-After value derived from resetAtEpochMs (=now+30s) must fall within [25, 30] " +
                "after test-execution latency")
            .isBetween(25L, 30L);
    }

    @Test
    void testReturns413WhenBatchExceedsMaxBatchSize() throws Exception {
        int previousCap = applicationProperties.getAi()
            .getGateway()
            .getExternalScores()
            .getMaxBatchSize();

        applicationProperties.getAi()
            .getGateway()
            .getExternalScores()
            .setMaxBatchSize(2);

        try {
            mockMvc
                .perform(
                    post("/api/ai-gateway/v1/scores/batch")
                        .header("X-ByteChef-Workspace-Id", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "scores": [
                                    {"traceId": 1, "name": "a", "value": 1, "dataType": "NUMERIC", "source": "x"},
                                    {"traceId": 2, "name": "b", "value": 2, "dataType": "NUMERIC", "source": "x"},
                                    {"traceId": 3, "name": "c", "value": 3, "dataType": "NUMERIC", "source": "x"}
                                ]
                            }
                            """))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error.type").value("too_many_scores"))
                .andExpect(jsonPath("$.error.message").exists());
        } finally {
            applicationProperties.getAi()
                .getGateway()
                .getExternalScores()
                .setMaxBatchSize(previousCap);
        }
    }

    @Test
    void testAcceptsBatchAtExactlyMaxBatchSize() throws Exception {
        int previousCap = applicationProperties.getAi()
            .getGateway()
            .getExternalScores()
            .getMaxBatchSize();

        applicationProperties.getAi()
            .getGateway()
            .getExternalScores()
            .setMaxBatchSize(2);

        when(aiExternalScoreFacade.recordBatch(eq(42L), any()))
            .thenReturn(new AiExternalScoreBatchResult(2, 0, 0, List.of()));

        try {
            mockMvc
                .perform(
                    post("/api/ai-gateway/v1/scores/batch")
                        .header("X-ByteChef-Workspace-Id", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "scores": [
                                    {"traceId": 1, "name": "a", "value": 1, "dataType": "NUMERIC", "source": "x"},
                                    {"traceId": 2, "name": "b", "value": 2, "dataType": "NUMERIC", "source": "x"}
                                ]
                            }
                            """))
                .andExpect(status().isOk());
        } finally {
            applicationProperties.getAi()
                .getGateway()
                .getExternalScores()
                .setMaxBatchSize(previousCap);
        }
    }

    @Test
    void testReturns404WhenTargetNotFound() throws Exception {
        when(aiExternalScoreFacade.recordTraceScore(any(), any(), any()))
            .thenThrow(AiScoreTargetNotFoundException.forTarget(42L, AiScoreTargetType.TRACE, 999L));

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/traces/999/scores")
                    .header("X-ByteChef-Workspace-Id", "42")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "name": "faithfulness",
                            "value": 0.87,
                            "dataType": "NUMERIC",
                            "source": "ragas"
                        }
                        """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.type").value("target_not_found"))
            .andExpect(jsonPath("$.error.message").value("Trace 999 not found"));
    }
}
