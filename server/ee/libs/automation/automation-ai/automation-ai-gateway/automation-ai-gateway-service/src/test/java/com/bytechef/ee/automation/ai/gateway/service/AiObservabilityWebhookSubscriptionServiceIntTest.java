/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.ai.gateway.config.AiGatewayIntTestConfiguration;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityWebhookSubscription;
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

    @Test
    public void testOnlyEnabledSubscriptionsReturnedForEventDelivery() {
        AiObservabilityWebhookSubscription enabled = new AiObservabilityWebhookSubscription(
            WORKSPACE_ID, "primary", "https://example.com/hook", "[\"trace.completed\"]");

        aiObservabilityWebhookSubscriptionService.create(enabled);

        AiObservabilityWebhookSubscription disabled = new AiObservabilityWebhookSubscription(
            WORKSPACE_ID, "backup", "https://example.com/disabled", "[\"trace.completed\"]");

        disabled.setEnabled(false);

        aiObservabilityWebhookSubscriptionService.create(disabled);

        List<AiObservabilityWebhookSubscription> active =
            aiObservabilityWebhookSubscriptionService.getEnabledWebhookSubscriptionsByWorkspace(WORKSPACE_ID);

        assertThat(active)
            .extracting(AiObservabilityWebhookSubscription::getUrl)
            .contains("https://example.com/hook")
            .doesNotContain("https://example.com/disabled");
    }
}
