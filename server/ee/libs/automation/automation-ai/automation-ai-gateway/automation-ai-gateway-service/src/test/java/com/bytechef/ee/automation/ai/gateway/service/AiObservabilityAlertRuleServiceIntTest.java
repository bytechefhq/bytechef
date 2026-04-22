/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.ai.gateway.config.AiGatewayIntTestConfiguration;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertCondition;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertMetric;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertRule;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertRuleChannel;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityNotificationChannel;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityNotificationChannelType;
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
    private AiObservabilityNotificationChannelService aiObservabilityNotificationChannelService;

    @Test
    public void testCreateAlertRuleWithChannelAndSnoozeUnsnooze() {
        AiObservabilityNotificationChannel channel = aiObservabilityNotificationChannelService.create(
            new AiObservabilityNotificationChannel(
                WORKSPACE_ID, "ops-email",
                AiObservabilityNotificationChannelType.EMAIL,
                "{\"to\":\"ops@example.com\"}"));

        Long channelId = Validate.notNull(channel.getId(), "id");

        AiObservabilityAlertRule rule = new AiObservabilityAlertRule(
            WORKSPACE_ID, "high-error-rate",
            AiObservabilityAlertMetric.ERROR_RATE,
            AiObservabilityAlertCondition.GREATER_THAN,
            new BigDecimal("0.05"), 10, 30);

        Set<AiObservabilityAlertRuleChannel> channels = new HashSet<>();
        channels.add(new AiObservabilityAlertRuleChannel(channelId));

        rule.setChannels(channels);

        AiObservabilityAlertRule created = aiObservabilityAlertRuleService.create(rule);

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
