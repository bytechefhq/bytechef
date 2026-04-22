/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.ai.gateway.config.AiGatewayIntTestConfiguration;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayIntTestConfigurationSharedMocks;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySession;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpan;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpanType;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilitySessionService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilitySpanService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
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
    private WorkspaceAiObservabilityWebhookSubscriptionService workspaceAiObservabilityWebhookSubscriptionService;

    @Autowired
    private WorkspaceAiObservabilityExportJobService workspaceAiObservabilityExportJobService;

    @Autowired
    private WorkspaceAiObservabilityNotificationChannelService workspaceAiObservabilityNotificationChannelService;

    @Autowired
    private WorkspaceAiObservabilityAlertRuleService workspaceAiObservabilityAlertRuleService;

    @Autowired
    private WorkspaceAiObservabilitySessionService workspaceAiObservabilitySessionService;

    @Autowired
    private WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;

    @Autowired
    private AiObservabilitySpanService aiObservabilitySpanService;

    @Autowired
    private AiObservabilitySessionService aiObservabilitySessionService;

    @Test
    public void testCreateAndRetrieveTrace() {
        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.API);

        trace.setName("test-trace");
        trace.setExternalTraceId("ext-abc-123");

        workspaceAiObservabilityTraceService.createInWorkspace(trace, WORKSPACE_ID);

        Long traceId = Validate.notNull(trace.getId(), "id");

        AiObservabilityTrace retrieved = aiObservabilityTraceService.getTrace(traceId);

        assertThat(retrieved)
            .hasFieldOrPropertyWithValue("name", "test-trace")
            .hasFieldOrPropertyWithValue("externalTraceId", "ext-abc-123");
        assertThat(workspaceAiObservabilityTraceService.getWorkspaceId(traceId)).isEqualTo(WORKSPACE_ID);
    }

    @Test
    public void testSpansLinkedToTrace() {
        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.API);

        trace.setName("trace-with-spans");

        workspaceAiObservabilityTraceService.createInWorkspace(trace, WORKSPACE_ID);

        Long traceId = Validate.notNull(trace.getId(), "id");

        AiObservabilitySpan span = new AiObservabilitySpan(traceId, AiObservabilitySpanType.GENERATION);

        aiObservabilitySpanService.create(span);

        List<AiObservabilitySpan> spans = aiObservabilitySpanService.getSpansByTrace(traceId);

        assertThat(spans).hasSize(1);
    }

    @Test
    public void testSessionForeignKeyRelationship() {
        AiObservabilitySession session = new AiObservabilitySession();

        session.setName("test-session");

        workspaceAiObservabilitySessionService.createInWorkspace(session, WORKSPACE_ID);

        Long sessionId = Validate.notNull(session.getId(), "id");

        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.API);

        trace.setName("session-linked-trace");
        trace.setSessionId(sessionId);

        workspaceAiObservabilityTraceService.createInWorkspace(trace, WORKSPACE_ID);

        List<AiObservabilityTrace> traces = aiObservabilityTraceService.getTracesBySession(sessionId);

        assertThat(traces)
            .hasSize(1)
            .first()
            .hasFieldOrPropertyWithValue("sessionId", sessionId);
    }

    @Test
    public void testExternalTraceIdLookup() {
        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.API);

        trace.setName("ext-trace");
        trace.setExternalTraceId("unique-external-id");

        workspaceAiObservabilityTraceService.createInWorkspace(trace, WORKSPACE_ID);

        Optional<AiObservabilityTrace> found = workspaceAiObservabilityTraceService.findByExternalTraceId(
            WORKSPACE_ID, "unique-external-id");

        assertThat(found).isPresent();
        assertThat(found.get())
            .hasFieldOrPropertyWithValue("externalTraceId", "unique-external-id");
    }
}
