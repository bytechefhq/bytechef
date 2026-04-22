/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.gateway.public_.config.AiGatewayPublicRestIntTestConfiguration;
import com.bytechef.ee.automation.ai.gateway.public_.config.AiGatewayPublicRestIntTestSharedMocks;
import com.bytechef.ee.automation.ai.gateway.public_.workspace.AiGatewayWorkspaceHeaderResolver;
import com.bytechef.ee.automation.ai.observability.repository.WorkspaceAiObservabilityTraceRepository;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.repository.AiObservabilityTraceRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

/**
 * Full HTTP-stack integration test for the OTLP traces ingest endpoint. Complements
 * {@code AiObservabilityOtlpIngestFacadeIntTest} (facade + DB) and {@code AiGatewayOtlpControllerTest} (MockMvc slice)
 * by exercising the real Spring {@code byte[]} message converter, controller header parsing, workspace routing, and
 * JSON response shape over a live Tomcat port.
 *
 * @author Ivica Cardic
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(
    classes = AiGatewayPublicRestIntTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgreSQLContainerConfiguration.class)
@AiGatewayPublicRestIntTestSharedMocks
public class AiGatewayOtlpControllerIntTest {

    /** Canonical OTel trace-id width per the OTLP spec — 16 bytes / 32 hex chars. */
    private static final int TRACE_ID_BYTES = 16;

    /** Canonical OTel span-id width per the OTLP spec — 8 bytes / 16 hex chars. */
    private static final int SPAN_ID_BYTES = 8;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Long WORKSPACE_ID = 1L;
    private static final MediaType OTLP_PROTOBUF = MediaType.valueOf("application/x-protobuf");

    @LocalServerPort
    private int port;

    @Autowired
    private AiObservabilityTraceRepository aiObservabilityTraceRepository;

    @Autowired
    private WorkspaceAiObservabilityTraceRepository workspaceAiObservabilityTraceRepository;

    @Autowired
    private AiGatewayWorkspaceHeaderResolver workspaceHeaderResolver;

    @BeforeEach
    public void setUpWorkspaceResolver() {
        // The resolver is a @MockitoBean (see AiGatewayPublicRestIntTestSharedMocks). The int test doesn't seed a
        // workspace-user row, so stub the mock to accept the expected header. The real membership enforcement is
        // covered by unit tests on the resolver's logic; here we care about the HTTP path.
        when(workspaceHeaderResolver.resolveAndVerify(String.valueOf(WORKSPACE_ID)))
            .thenReturn(WORKSPACE_ID);
        when(workspaceHeaderResolver.resolveAndVerify((String) null))
            .thenReturn(null);
    }

    @Test
    public void testAcceptsOtlpRequestAndPersistsTrace() {
        // Use canonical 16-byte / 8-byte random ids per the OTLP spec. Without exact-length seeds the trace id
        // is a variable-length ASCII string relying on byte-count coincidence — OtelGenAiSpan's compact
        // constructor enforces exactly 16 bytes / 32 hex chars and rejects almost any non-canonical input,
        // which would make the acceptedSpans=1 assertion silently load-bearing on the coincidence.
        ByteString traceIdBytes = randomBytes(TRACE_ID_BYTES);
        ByteString spanIdBytes = randomBytes(SPAN_ID_BYTES);
        String externalTraceIdHex = HexFormat.of()
            .formatHex(traceIdBytes.toByteArray());

        byte[] protobuf = buildValidGenAiProtobuf(traceIdBytes, spanIdBytes);

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(OTLP_PROTOBUF);
        headers.set("X-ByteChef-Workspace-Id", String.valueOf(WORKSPACE_ID));

        RestClient restClient = RestClient.create();

        ResponseEntity<String> response = restClient
            .method(HttpMethod.POST)
            .uri(baseUrl() + "/api/ai-gateway/v1/otlp/traces")
            .headers(httpHeaders -> httpHeaders.putAll(headers))
            .body(protobuf)
            .retrieve()
            .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        String body = response.getBody();

        assertThat(body)
            .isNotNull()
            .contains("\"acceptedSpans\":1")
            .contains("\"rejectedSpans\":0");

        Optional<AiObservabilityTrace> persisted = workspaceAiObservabilityTraceRepository
            .findByWorkspaceIdAndExternalTraceId(WORKSPACE_ID, externalTraceIdHex);

        // workspaceId is no longer a field on AiObservabilityTrace — it lives on the WorkspaceAiObservabilityTrace
        // membership row, which the repository's JOIN finder above already scoped by. Asserting the entity-level
        // fields is sufficient: a non-empty Optional from findByWorkspaceIdAndExternalTraceId means the trace IS
        // bound to WORKSPACE_ID via the workspace_ai_observability_trace row.
        assertThat(persisted)
            .isPresent()
            .get()
            .hasFieldOrPropertyWithValue("externalTraceId", externalTraceIdHex)
            .hasFieldOrPropertyWithValue("source", AiObservabilityTraceSource.OTLP);
    }

    @Test
    public void testReturns400WhenWorkspaceHeaderMissing() {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(OTLP_PROTOBUF);

        // A valid gen_ai protobuf body so the controller can parse it — the workspace-id check runs before the body
        // parse, but Spring's byte[] message converter rejects an empty body with HttpMessageNotReadableException
        // before the handler even runs. Sending a non-empty body keeps the message converter happy and forces the
        // controller's own workspace-missing branch.
        byte[] protobuf = buildValidGenAiProtobuf(randomBytes(TRACE_ID_BYTES), randomBytes(SPAN_ID_BYTES));

        RestClient restClient = RestClient.create();

        ResponseEntity<String> response = restClient
            .method(HttpMethod.POST)
            .uri(baseUrl() + "/api/ai-gateway/v1/otlp/traces")
            .headers(httpHeaders -> httpHeaders.putAll(headers))
            .body(protobuf)
            .retrieve()
            .onStatus(status -> status.value() == 400, (request, clientResponse) -> {
                // Swallow the 4xx so we can assert on the ResponseEntity instead of RestClient's default throw.
            })
            .toEntity(String.class);

        assertThat(response.getStatusCode()
            .value()).isEqualTo(400);
        assertThat(response.getBody())
            .isNotNull()
            .contains("missing_workspace_id");
    }

    @Test
    public void testReturns413WhenSpanCountExceedsMaxSpansPerRequest() {
        // Pins the count-before-mapper ordering through the real Spring byte[] message converter. The MockMvc
        // slice covers the same logic, but the integration stack would catch a regression where, e.g., the
        // mapper's toBatch ran first and OOM'd on a body crafted to defeat the cap before the 413 fired.
        // application-testint.yml sets max-spans-per-request=5, so a 6-span body must reject at the wire layer.
        int overCapSpanCount = 6;

        byte[] protobuf = buildProtobufWithNGenAiSpans(overCapSpanCount);

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(OTLP_PROTOBUF);
        headers.set("X-ByteChef-Workspace-Id", String.valueOf(WORKSPACE_ID));

        RestClient restClient = RestClient.create();

        ResponseEntity<String> response = restClient
            .method(HttpMethod.POST)
            .uri(baseUrl() + "/api/ai-gateway/v1/otlp/traces")
            .headers(httpHeaders -> httpHeaders.putAll(headers))
            .body(protobuf)
            .retrieve()
            .onStatus(status -> status.value() == 413, (request, clientResponse) -> {
                // Swallow the 4xx so we can assert on the ResponseEntity instead of RestClient's default throw.
            })
            .toEntity(String.class);

        assertThat(response.getStatusCode()
            .value()).isEqualTo(413);
        assertThat(response.getBody())
            .isNotNull()
            .contains("too_many_spans")
            .contains("max is 5");
    }

    @Test
    public void testReturns400WhenProtobufMalformed() {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(OTLP_PROTOBUF);
        headers.set("X-ByteChef-Workspace-Id", String.valueOf(WORKSPACE_ID));

        byte[] garbage = "this-is-not-valid-protobuf".getBytes(StandardCharsets.UTF_8);

        RestClient restClient = RestClient.create();

        ResponseEntity<String> response = restClient
            .method(HttpMethod.POST)
            .uri(baseUrl() + "/api/ai-gateway/v1/otlp/traces")
            .headers(httpHeaders -> httpHeaders.putAll(headers))
            .body(garbage)
            .retrieve()
            .onStatus(status -> status.value() == 400, (request, clientResponse) -> {
                // Swallow the 4xx so we can assert on the ResponseEntity.
            })
            .toEntity(String.class);

        assertThat(response.getStatusCode()
            .value()).isEqualTo(400);
        assertThat(response.getBody())
            .isNotNull()
            .contains("invalid_protobuf");
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Build an {@link ExportTraceServiceRequest} protobuf with a single gen_ai-tagged span. Matches the shape
     * {@code OtlpProtobufMapperImpl} expects: a resource + a scope + a span with {@code gen_ai.system} and
     * {@code gen_ai.request.model} attributes so the mapper doesn't drop it as non-GenAI. Trace and span ids must
     * already be canonical-length protobuf bytes (16 / 8 respectively) — see {@link #randomBytes}.
     */
    private static byte[] buildValidGenAiProtobuf(ByteString traceId, ByteString spanId) {
        Span span = Span.newBuilder()
            .setTraceId(traceId)
            .setSpanId(spanId)
            .setName("chat gpt-4o")
            .setStartTimeUnixNano(1_700_000_000_000_000_000L)
            .setEndTimeUnixNano(1_700_000_001_000_000_000L)
            .addAllAttributes(List.of(
                stringAttribute("gen_ai.system", "openai"),
                stringAttribute("gen_ai.request.model", "gpt-4o"),
                intAttribute("gen_ai.usage.input_tokens", 120L),
                intAttribute("gen_ai.usage.output_tokens", 64L)))
            .build();

        return ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(
                ResourceSpans.newBuilder()
                    .setResource(Resource.newBuilder()
                        .build())
                    .addScopeSpans(
                        ScopeSpans.newBuilder()
                            .addSpans(span)))
            .build()
            .toByteArray();
    }

    /**
     * Build an {@link ExportTraceServiceRequest} with {@code spanCount} gen_ai-tagged spans inside a single resource /
     * scope. Each span gets a freshly randomised id pair so a partial-success regression after the cap change cannot
     * mask a wrong count via dedup.
     */
    private static byte[] buildProtobufWithNGenAiSpans(int spanCount) {
        ScopeSpans.Builder scopeSpans = ScopeSpans.newBuilder();

        for (int spanIndex = 0; spanIndex < spanCount; spanIndex++) {
            scopeSpans.addSpans(Span.newBuilder()
                .setTraceId(randomBytes(TRACE_ID_BYTES))
                .setSpanId(randomBytes(SPAN_ID_BYTES))
                .setName("chat gpt-4o")
                .setStartTimeUnixNano(1_700_000_000_000_000_000L)
                .setEndTimeUnixNano(1_700_000_001_000_000_000L)
                .addAllAttributes(List.of(
                    stringAttribute("gen_ai.system", "openai"),
                    stringAttribute("gen_ai.request.model", "gpt-4o"))));
        }

        return ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(ResourceSpans.newBuilder()
                .setResource(Resource.newBuilder()
                    .build())
                .addScopeSpans(scopeSpans))
            .build()
            .toByteArray();
    }

    private static ByteString randomBytes(int length) {
        byte[] bytes = new byte[length];

        RANDOM.nextBytes(bytes);

        return ByteString.copyFrom(bytes);
    }

    private static KeyValue stringAttribute(String key, String value) {
        return KeyValue.newBuilder()
            .setKey(key)
            .setValue(AnyValue.newBuilder()
                .setStringValue(value))
            .build();
    }

    private static KeyValue intAttribute(String key, long value) {
        return KeyValue.newBuilder()
            .setKey(key)
            .setValue(AnyValue.newBuilder()
                .setIntValue(value))
            .build();
    }
}
