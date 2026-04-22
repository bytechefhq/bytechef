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
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertCondition;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertMetric;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertRule;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertRuleChannel;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityNotificationChannel;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityNotificationChannelType;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityAlertRuleService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityNotificationChannelService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
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
public class AiObservabilityAlertRuleServiceIntTest {

    private static final Long WORKSPACE_ID = 1L;

    @Autowired
    private AiObservabilityAlertRuleService aiObservabilityAlertRuleService;

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
    private AiObservabilityNotificationChannelService aiObservabilityNotificationChannelService;

    @Test
    public void testCreateAlertRuleWithChannelAndSnoozeUnsnooze() {
        AiObservabilityNotificationChannel channel =
            workspaceAiObservabilityNotificationChannelService.createInWorkspace(
                new AiObservabilityNotificationChannel(
                    "ops-email",
                    AiObservabilityNotificationChannelType.EMAIL,
                    "{\"to\":\"ops@example.com\"}"),
                WORKSPACE_ID);

        Long channelId = Validate.notNull(channel.getId(), "id");

        AiObservabilityAlertRule rule = new AiObservabilityAlertRule(
            "high-error-rate",
            AiObservabilityAlertMetric.ERROR_RATE,
            AiObservabilityAlertCondition.GREATER_THAN,
            new BigDecimal("0.05"), 10, 30);

        Set<AiObservabilityAlertRuleChannel> channels = new HashSet<>();
        channels.add(new AiObservabilityAlertRuleChannel(channelId));

        rule.setChannels(channels);

        AiObservabilityAlertRule created =
            workspaceAiObservabilityAlertRuleService.createInWorkspace(rule, WORKSPACE_ID);

        Long ruleId = Validate.notNull(created.getId(), "id");

        AiObservabilityAlertRule retrieved = aiObservabilityAlertRuleService.getAlertRule(ruleId);

        assertThat(retrieved)
            .hasFieldOrPropertyWithValue("name", "high-error-rate");
        assertThat(retrieved.getChannels())
            .extracting(AiObservabilityAlertRuleChannel::notificationChannelId)
            .contains(channelId);

        Instant until = Instant.now()
            .plus(1, ChronoUnit.HOURS);

        AiObservabilityAlertRule snoozed = aiObservabilityAlertRuleService.snooze(ruleId, until);

        assertThat(snoozed.getSnoozedUntil()).isNotNull();

        AiObservabilityAlertRule unsnoozed = aiObservabilityAlertRuleService.unsnooze(ruleId);

        assertThat(unsnoozed.getSnoozedUntil()).isNull();
    }
}
