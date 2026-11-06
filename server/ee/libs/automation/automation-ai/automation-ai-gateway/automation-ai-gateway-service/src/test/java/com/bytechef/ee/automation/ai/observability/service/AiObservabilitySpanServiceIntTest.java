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
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpan;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpanType;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilitySpanService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
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
public class AiObservabilitySpanServiceIntTest {

    private static final Long WORKSPACE_ID = 1L;

    @Autowired
    private AiObservabilitySpanService aiObservabilitySpanService;

    @Autowired
    private WorkspaceAiObservabilityWebhookSubscriptionService workspaceAiObservabilityWebhookSubscriptionService;

    @Autowired
    private WorkspaceAiObservabilityExportJobService workspaceAiObservabilityExportJobService;

    @Autowired
    private WorkspaceAiObservabilityAlertRuleService workspaceAiObservabilityAlertRuleService;

    @Autowired
    private WorkspaceAiObservabilitySessionService workspaceAiObservabilitySessionService;

    @Autowired
    private WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;

    @Autowired
    private AiObservabilityTraceService aiObservabilityTraceService;

    @Test
    public void testCreateAndFindSpansByTrace() {
        AiObservabilityTrace trace = new AiObservabilityTrace(AiObservabilityTraceSource.API);

        trace.setName("t1");

        workspaceAiObservabilityTraceService.createInWorkspace(trace, WORKSPACE_ID);

        Long traceId = Validate.notNull(trace.getId(), "trace id");

        AiObservabilitySpan span = new AiObservabilitySpan(traceId, AiObservabilitySpanType.GENERATION);

        span.setName("root-span");

        aiObservabilitySpanService.create(span);

        List<AiObservabilitySpan> retrieved = aiObservabilitySpanService.getSpansByTrace(traceId);

        assertThat(retrieved)
            .hasSize(1)
            .first()
            .hasFieldOrPropertyWithValue("name", "root-span");
    }
}
