/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.ai.gateway.config.AiGatewayIntTestConfiguration;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySession;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpan;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpanType;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTraceSource;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiGatewayIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@AiGatewayIntTestConfigurationSharedMocks
public class AiObservabilityTraceServiceIntTest {

    private static final Long WORKSPACE_ID = 1L;

    @Autowired
    private AiObservabilityTraceService aiObservabilityTraceService;

    @Autowired
    private AiObservabilitySpanService aiObservabilitySpanService;

    @Autowired
    private AiObservabilitySessionService aiObservabilitySessionService;

    @Test
    public void testCreateAndRetrieveTrace() {
        AiObservabilityTrace trace = new AiObservabilityTrace(WORKSPACE_ID, AiObservabilityTraceSource.API);

        trace.setName("test-trace");
        trace.setExternalTraceId("ext-abc-123");

        aiObservabilityTraceService.create(trace);

        Long traceId = Validate.notNull(trace.getId(), "id");

        AiObservabilityTrace retrieved = aiObservabilityTraceService.getTrace(traceId);

        assertThat(retrieved)
            .hasFieldOrPropertyWithValue("name", "test-trace")
            .hasFieldOrPropertyWithValue("externalTraceId", "ext-abc-123")
            .hasFieldOrPropertyWithValue("workspaceId", WORKSPACE_ID);
    }

    @Test
    public void testSpansLinkedToTrace() {
        AiObservabilityTrace trace = new AiObservabilityTrace(WORKSPACE_ID, AiObservabilityTraceSource.API);

        trace.setName("trace-with-spans");

        aiObservabilityTraceService.create(trace);

        Long traceId = Validate.notNull(trace.getId(), "id");

        AiObservabilitySpan span = new AiObservabilitySpan(traceId, AiObservabilitySpanType.GENERATION);

        aiObservabilitySpanService.create(span);

        List<AiObservabilitySpan> spans = aiObservabilitySpanService.getSpansByTrace(traceId);

        assertThat(spans).hasSize(1);
    }

    @Test
    public void testSessionForeignKeyRelationship() {
        AiObservabilitySession session = new AiObservabilitySession(WORKSPACE_ID);

        session.setName("test-session");

        aiObservabilitySessionService.create(session);

        Long sessionId = Validate.notNull(session.getId(), "id");

        AiObservabilityTrace trace = new AiObservabilityTrace(WORKSPACE_ID, AiObservabilityTraceSource.API);

        trace.setName("session-linked-trace");
        trace.setSessionId(sessionId);

        aiObservabilityTraceService.create(trace);

        List<AiObservabilityTrace> traces = aiObservabilityTraceService.getTracesBySession(sessionId);

        assertThat(traces)
            .hasSize(1)
            .first()
            .hasFieldOrPropertyWithValue("sessionId", sessionId);
    }

    @Test
    public void testExternalTraceIdLookup() {
        AiObservabilityTrace trace = new AiObservabilityTrace(WORKSPACE_ID, AiObservabilityTraceSource.API);

        trace.setName("ext-trace");
        trace.setExternalTraceId("unique-external-id");

        aiObservabilityTraceService.create(trace);

        Optional<AiObservabilityTrace> found = aiObservabilityTraceService.findByExternalTraceId(
            WORKSPACE_ID, "unique-external-id");

        assertThat(found).isPresent();
        assertThat(found.get())
            .hasFieldOrPropertyWithValue("externalTraceId", "unique-external-id");
    }
}
