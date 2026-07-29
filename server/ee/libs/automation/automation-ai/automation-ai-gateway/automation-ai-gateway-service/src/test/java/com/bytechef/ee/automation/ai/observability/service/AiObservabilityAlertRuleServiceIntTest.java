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
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityAlertRuleService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private JdbcTemplate jdbcTemplate;

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

    @Test
    public void testCreateAlertRuleWithChannelAndSnoozeUnsnooze() {
        // Delivery targets are platform Notification rows post channel migration; insert one directly since the
        // gateway test context does not wire the CE notification service stack.
        Long notificationId = jdbcTemplate.queryForObject(
            """
                INSERT INTO notification (
                    name, type, settings, created_by, created_date, last_modified_by, last_modified_date, version)
                VALUES ('ops-email', 0, '{"email": "ops@example.com"}', 'system', now(), 'system', now(), 1)
                RETURNING id
                """,
            Long.class);

        AiObservabilityAlertRule rule = new AiObservabilityAlertRule(
            "high-error-rate",
            AiObservabilityAlertMetric.ERROR_RATE,
            AiObservabilityAlertCondition.GREATER_THAN,
            new BigDecimal("0.05"), 10, 30);

        Set<AiObservabilityAlertRuleChannel> channels = new HashSet<>();
        channels.add(new AiObservabilityAlertRuleChannel(notificationId));

        rule.setChannels(channels);

        AiObservabilityAlertRule created =
            workspaceAiObservabilityAlertRuleService.createInWorkspace(rule, WORKSPACE_ID);

        Long ruleId = Validate.notNull(created.getId(), "id");

        AiObservabilityAlertRule retrieved = aiObservabilityAlertRuleService.getAlertRule(ruleId);

        assertThat(retrieved)
            .hasFieldOrPropertyWithValue("name", "high-error-rate");
        assertThat(retrieved.getChannels())
            .extracting(AiObservabilityAlertRuleChannel::notificationId)
            .contains(notificationId);

        Instant until = Instant.now()
            .plus(1, ChronoUnit.HOURS);

        AiObservabilityAlertRule snoozed = aiObservabilityAlertRuleService.snooze(ruleId, until);

        assertThat(snoozed.getSnoozedUntil()).isNotNull();

        AiObservabilityAlertRule unsnoozed = aiObservabilityAlertRuleService.unsnooze(ruleId);

        assertThat(unsnoozed.getSnoozedUntil()).isNull();
    }
}
