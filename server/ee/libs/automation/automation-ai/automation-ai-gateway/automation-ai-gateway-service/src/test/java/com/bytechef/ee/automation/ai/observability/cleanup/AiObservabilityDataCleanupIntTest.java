/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.cleanup;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.ai.gateway.config.AiGatewayIntTestConfiguration;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayIntTestConfigurationSharedMocks;
import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayProjectService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityAlertRuleService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityExportJobService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityNotificationChannelService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilitySessionService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityWebhookSubscriptionService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProject;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiGatewayIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@AiGatewayIntTestConfigurationSharedMocks
public class AiObservabilityDataCleanupIntTest {

    private static final Long WORKSPACE_ID = 1L;

    @Autowired
    private WorkspaceAiGatewayProjectService workspaceAiGatewayProjectService;

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
    private AiObservabilityTraceService aiObservabilityTraceService;

    @Autowired
    private AiObservabilityDataCleanupService aiObservabilityDataCleanupService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testCleanupRespectsLogRetentionDays() {
        AiGatewayProject project = new AiGatewayProject("cleanup-project", "cleanup-slug");

        project.setLogRetentionDays(1);

        workspaceAiGatewayProjectService.createInWorkspace(project, WORKSPACE_ID);

        AiObservabilityTrace oldTrace = new AiObservabilityTrace(AiObservabilityTraceSource.API);

        oldTrace.setName("old-trace");

        workspaceAiObservabilityTraceService.createInWorkspace(oldTrace, WORKSPACE_ID);

        AiObservabilityTrace recentTrace = new AiObservabilityTrace(AiObservabilityTraceSource.API);

        recentTrace.setName("recent-trace");

        workspaceAiObservabilityTraceService.createInWorkspace(recentTrace, WORKSPACE_ID);

        Long oldTraceId = Validate.notNull(oldTrace.getId(), "id");

        Instant twoDaysAgo = Instant.now()
            .minus(2, ChronoUnit.DAYS);

        jdbcTemplate.update(
            "UPDATE ai_observability_trace SET created_date = ? WHERE id = ?",
            java.sql.Timestamp.from(twoDaysAgo), oldTraceId);

        aiObservabilityDataCleanupService.cleanup();

        List<AiObservabilityTrace> remaining = workspaceAiObservabilityTraceService.getTracesByWorkspace(
            WORKSPACE_ID,
            Instant.now()
                .minus(7, ChronoUnit.DAYS),
            Instant.now()
                .plus(1, ChronoUnit.DAYS));

        assertThat(remaining)
            .extracting(AiObservabilityTrace::getName)
            .contains("recent-trace")
            .doesNotContain("old-trace");
    }
}
