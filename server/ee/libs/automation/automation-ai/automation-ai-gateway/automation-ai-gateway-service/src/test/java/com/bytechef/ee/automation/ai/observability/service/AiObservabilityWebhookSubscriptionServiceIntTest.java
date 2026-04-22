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
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityWebhookSubscription;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityWebhookSubscriptionService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
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
public class AiObservabilityWebhookSubscriptionServiceIntTest {

    private static final Long WORKSPACE_ID = 1L;

    @Autowired
    private AiObservabilityWebhookSubscriptionService aiObservabilityWebhookSubscriptionService;

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

    @Test
    public void testOnlyEnabledSubscriptionsReturnedForEventDelivery() {
        AiObservabilityWebhookSubscription enabled = new AiObservabilityWebhookSubscription(
            "primary", "https://example.com/hook", "[\"trace.completed\"]");

        workspaceAiObservabilityWebhookSubscriptionService.createInWorkspace(enabled, WORKSPACE_ID);

        AiObservabilityWebhookSubscription disabled = new AiObservabilityWebhookSubscription(
            "backup", "https://example.com/disabled", "[\"trace.completed\"]");

        disabled.setEnabled(false);

        workspaceAiObservabilityWebhookSubscriptionService.createInWorkspace(disabled, WORKSPACE_ID);

        List<AiObservabilityWebhookSubscription> active =
            workspaceAiObservabilityWebhookSubscriptionService.getEnabledWebhookSubscriptionsByWorkspace(WORKSPACE_ID);

        assertThat(active)
            .extracting(AiObservabilityWebhookSubscription::getUrl)
            .contains("https://example.com/hook")
            .doesNotContain("https://example.com/disabled");
    }
}
