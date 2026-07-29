/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.automation.ai.gateway.config.AiGatewayIntTestConfiguration;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayIntTestConfigurationSharedMocks;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportFormat;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportJob;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportJobStatus;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportJobType;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportScope;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityExportJobService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
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
public class AiObservabilityExportJobServiceIntTest {

    private static final Long WORKSPACE_ID = 1L;

    @Autowired
    private AiObservabilityExportJobService aiObservabilityExportJobService;

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
    public void testCancelTransitionsStatus() {
        AiObservabilityExportJob job = new AiObservabilityExportJob(
            AiObservabilityExportJobType.ON_DEMAND,
            AiObservabilityExportFormat.JSON, AiObservabilityExportScope.TRACES, "tester");

        workspaceAiObservabilityExportJobService.createInWorkspace(job, WORKSPACE_ID);

        Long jobId = Validate.notNull(job.getId(), "id");

        AiObservabilityExportJob cancelled = aiObservabilityExportJobService.cancel(jobId);

        assertThat(cancelled.getStatus()).isEqualTo(AiObservabilityExportJobStatus.CANCELLED);
    }

    @Test
    public void testCancelRejectsTerminalStates() {
        AiObservabilityExportJob job = new AiObservabilityExportJob(
            AiObservabilityExportJobType.ON_DEMAND,
            AiObservabilityExportFormat.JSON, AiObservabilityExportScope.TRACES, "tester");

        workspaceAiObservabilityExportJobService.createInWorkspace(job, WORKSPACE_ID);

        Long jobId = Validate.notNull(job.getId(), "id");

        aiObservabilityExportJobService.cancel(jobId);

        assertThatThrownBy(() -> aiObservabilityExportJobService.cancel(jobId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("CANCELLED");
    }
}
