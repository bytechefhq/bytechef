/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.public_.web.rest;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.ai.gateway.public_.workspace.AiGatewayWorkspaceHeaderResolver;
import com.bytechef.ee.automation.ai.gateway.ratelimit.AiGatewayRateLimitChecker;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimitResult;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreTargetType;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreWorkspaceBoundaryException;
import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelSpanBatch;
import com.bytechef.ee.platform.ai.gateway.otlp.mapper.OtlpProtobufMapper;
import com.bytechef.ee.platform.ai.observability.facade.AiObservabilityOtlpIngestFacade;
import com.bytechef.ee.platform.ai.observability.facade.OtlpIngestResult;
import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
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
@ContextConfiguration(classes = AiGatewayOtlpControllerTest.TestConfig.class)
@SuppressWarnings("deprecation")
@WebMvcTest(AiGatewayOtlpController.class)
@TestPropertySource(properties = {
    "bytechef.edition=ee", "bytechef.ai.gateway.enabled=true"
})
class AiGatewayOtlpControllerTest {

    @EnableAutoConfiguration
    @SpringBootConfiguration
    static class TestConfig {

        @Bean
        AiGatewayOtlpController aiGatewayOtlpController(
            AiObservabilityOtlpIngestFacade aiObservabilityOtlpIngestFacade,
            AiGatewayRateLimitChecker aiGatewayRateLimitChecker,
            AiGatewayWorkspaceHeaderResolver workspaceHeaderResolver, ApplicationProperties applicationProperties,
            OtlpProtobufMapper otlpProtobufMapper) {

            return new AiGatewayOtlpController(
                aiObservabilityOtlpIngestFacade, aiGatewayRateLimitChecker, workspaceHeaderResolver,
                applicationProperties, otlpProtobufMapper);
        }

        @Bean
        AiGatewayExceptionHandler aiGatewayExceptionHandler() {
            return new AiGatewayExceptionHandler();
        }
    }

    private static final MediaType OTLP_PROTOBUF = MediaType.valueOf("application/x-protobuf");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiObservabilityOtlpIngestFacade aiObservabilityOtlpIngestFacade;

    @MockitoBean
    private AiGatewayRateLimitChecker aiGatewayRateLimitChecker;

    @MockitoBean
    private ApplicationProperties applicationProperties;

    @MockitoBean
    private OtlpProtobufMapper otlpProtobufMapper;

    @MockitoBean
    private AiGatewayWorkspaceHeaderResolver workspaceHeaderResolver;

    @BeforeEach
    void setUp() {
        when(aiGatewayRateLimitChecker.checkOtlpIngest(anyLong()))
            .thenReturn(AiGatewayRateLimitResult.allowed(10, System.currentTimeMillis() + 60_000));

        when(workspaceHeaderResolver.resolveAndVerify("42"))
            .thenReturn(42L);
        when(workspaceHeaderResolver.resolveAndVerify("not-a-number"))
            .thenReturn(null);
        when(workspaceHeaderResolver.resolveAndVerify((String) null))
            .thenReturn(null);

        ApplicationProperties.Ai ai = mock(ApplicationProperties.Ai.class);
        ApplicationProperties.Ai.Gateway gateway = mock(ApplicationProperties.Ai.Gateway.class);
        ApplicationProperties.Ai.Gateway.Otlp otlp = mock(ApplicationProperties.Ai.Gateway.Otlp.class);

        when(applicationProperties.getAi()).thenReturn(ai);
        when(ai.getGateway()).thenReturn(gateway);
        when(gateway.getOtlp()).thenReturn(otlp);
        when(otlp.getMaxSpansPerRequest()).thenReturn(1_000);
    }

    @Test
    void testAcceptsProtobufBody() throws Exception {
        when(otlpProtobufMapper.toBatch(ArgumentMatchers.any(ExportTraceServiceRequest.class)))
            .thenReturn(new OtelSpanBatch(List.of(), 0, 0));
        when(aiObservabilityOtlpIngestFacade.ingest(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(new OtlpIngestResult(1, 0, 0, List.of()));

        byte[] protobuf = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(
                ResourceSpans.newBuilder()
                    .addScopeSpans(
                        ScopeSpans.newBuilder()
                            .addSpans(
                                Span.newBuilder()
                                    .setTraceId(ByteString.copyFromUtf8("trace-1"))
                                    .setSpanId(ByteString.copyFromUtf8("span-1")))))
            .build()
            .toByteArray();

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/otlp/traces")
                    .header("X-ByteChef-Workspace-Id", "42")
                    .contentType(OTLP_PROTOBUF)
                    .content(protobuf))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.acceptedSpans").value(1))
            .andExpect(jsonPath("$.rejectedSpans").value(0));

        verify(aiObservabilityOtlpIngestFacade).ingest(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void testReturns400WhenWorkspaceHeaderMissing() throws Exception {
        byte[] protobuf = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder())
            .build()
            .toByteArray();

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/otlp/traces")
                    .contentType(OTLP_PROTOBUF)
                    .content(protobuf))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.type").value("missing_workspace_id"));
    }

    @Test
    void testReturns400WhenWorkspaceHeaderNotNumeric() throws Exception {
        byte[] protobuf = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder())
            .build()
            .toByteArray();

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/otlp/traces")
                    .header("X-ByteChef-Workspace-Id", "not-a-number")
                    .contentType(OTLP_PROTOBUF)
                    .content(protobuf))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.type").value("missing_workspace_id"));
    }

    @Test
    void testReturns429WhenRateLimited() throws Exception {
        when(aiGatewayRateLimitChecker.checkOtlpIngest(anyLong()))
            .thenReturn(AiGatewayRateLimitResult.rejected(0, System.currentTimeMillis() + 30_000));

        byte[] protobuf = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder())
            .build()
            .toByteArray();

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/otlp/traces")
                    .header("X-ByteChef-Workspace-Id", "42")
                    .contentType(OTLP_PROTOBUF)
                    .content(protobuf))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.error.type").value("rate_limit_exceeded"))
            .andExpect(jsonPath("$.error.message").exists());
    }

    @Test
    void testReturns400WhenProtobufMalformed() throws Exception {
        byte[] garbage = new byte[] {
            0x01, 0x02, 0x03, 0x04, (byte) 0xFF
        };

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/otlp/traces")
                    .header("X-ByteChef-Workspace-Id", "42")
                    .contentType(OTLP_PROTOBUF)
                    .content(garbage))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.type").value("invalid_protobuf"));
    }

    @Test
    void testReturns403WhenCallerNotMemberOfWorkspace() throws Exception {
        when(workspaceHeaderResolver.resolveAndVerify("999"))
            .thenThrow(AiScoreWorkspaceBoundaryException.forTarget(
                999L, AiScoreTargetType.WORKSPACE, 999L));

        byte[] protobuf = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder())
            .build()
            .toByteArray();

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/otlp/traces")
                    .header("X-ByteChef-Workspace-Id", "999")
                    .contentType(OTLP_PROTOBUF)
                    .content(protobuf))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.type").value("workspace_boundary"));
    }

    @Test
    void testReturns413WhenBatchExceedsLimit() throws Exception {
        // The 413 cap is enforced by counting protobuf spans BEFORE the mapper materializes DTOs. The test must
        // therefore put real spans on the wire — stubbing the mapper to return 1001 mocked OtelGenAiSpans does
        // NOT trigger the cap because countSpans(request) operates on the protobuf shape directly. Constructing
        // 1001 minimal protobuf Span entries is the smallest realistic input that exercises the cap.
        ScopeSpans.Builder scope = ScopeSpans.newBuilder();

        IntStream.rangeClosed(1, 1_001)
            .forEach(i -> scope.addSpans(
                Span.newBuilder()
                    .setTraceId(ByteString.copyFromUtf8(String.format("trace-%010d", i)))
                    .setSpanId(ByteString.copyFromUtf8(String.format("span-%03d", i)))
                    .setName("chat")
                    .build()));

        byte[] protobuf = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .addScopeSpans(scope))
            .build()
            .toByteArray();

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/otlp/traces")
                    .header("X-ByteChef-Workspace-Id", "42")
                    .contentType(OTLP_PROTOBUF)
                    .content(protobuf))
            .andExpect(status().isPayloadTooLarge())
            .andExpect(jsonPath("$.error.type").value("too_many_spans"));
    }

    /**
     * Boundary pin for {@code maxSpansPerRequest}: a request with exactly {@code cap} spans must NOT trigger 413.
     * Catches a regression that flips the cap check from {@code >} to {@code >=}. The companion test above pins the
     * {@code cap + 1} side; without this at-cap test the cap value itself is unverified, and the controller could
     * silently refuse the documented limit.
     */
    @Test
    void testAcceptsRequestAtExactlyMaxSpansPerRequest() throws Exception {
        when(otlpProtobufMapper.toBatch(ArgumentMatchers.any(ExportTraceServiceRequest.class)))
            .thenReturn(new OtelSpanBatch(List.of(), 0, 0));
        when(aiObservabilityOtlpIngestFacade.ingest(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(new OtlpIngestResult(0, 0, 0, List.of()));

        ScopeSpans.Builder scope = ScopeSpans.newBuilder();

        IntStream.rangeClosed(1, 1_000)
            .forEach(i -> scope.addSpans(
                Span.newBuilder()
                    .setTraceId(ByteString.copyFromUtf8(String.format("trace-%010d", i)))
                    .setSpanId(ByteString.copyFromUtf8(String.format("span-%03d", i)))
                    .setName("chat")
                    .build()));

        byte[] protobuf = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .addScopeSpans(scope))
            .build()
            .toByteArray();

        mockMvc
            .perform(
                post("/api/ai-gateway/v1/otlp/traces")
                    .header("X-ByteChef-Workspace-Id", "42")
                    .contentType(OTLP_PROTOBUF)
                    .content(protobuf))
            .andExpect(status().isAccepted());
    }
}
