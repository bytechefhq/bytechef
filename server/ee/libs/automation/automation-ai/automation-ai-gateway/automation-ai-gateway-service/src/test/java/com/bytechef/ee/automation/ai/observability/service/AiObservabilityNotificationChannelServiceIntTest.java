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
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityNotificationChannel;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityNotificationChannelType;
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
public class AiObservabilityNotificationChannelServiceIntTest {

    private static final Long WORKSPACE_ID = 1L;

    @Autowired
    private WorkspaceAiObservabilityNotificationChannelService workspaceAiObservabilityNotificationChannelService;

    @Test
    public void testCreateListAndDeleteChannel() {
        AiObservabilityNotificationChannel channel =
            workspaceAiObservabilityNotificationChannelService.createInWorkspace(
                new AiObservabilityNotificationChannel(
                    "ops-webhook",
                    AiObservabilityNotificationChannelType.WEBHOOK,
                    "{\"url\":\"https://example.com/hook\"}"),
                WORKSPACE_ID);

        Long channelId = Validate.notNull(channel.getId(), "id");

        List<AiObservabilityNotificationChannel> listed =
            workspaceAiObservabilityNotificationChannelService.getNotificationChannelsByWorkspace(WORKSPACE_ID);

        assertThat(listed)
            .extracting(AiObservabilityNotificationChannel::getName)
            .contains("ops-webhook");

        // Delete via the workspace-aware service so the workspace_ai_observability_notification_channel membership
        // row is removed before the parent ai_observability_notification_channel row. Calling the platform-only
        // AiObservabilityNotificationChannelService#delete directly would leave the membership row dangling and
        // fail with an FK constraint violation on the parent delete.
        workspaceAiObservabilityNotificationChannelService.delete(channelId);

        assertThat(workspaceAiObservabilityNotificationChannelService.getNotificationChannelsByWorkspace(WORKSPACE_ID))
            .extracting(AiObservabilityNotificationChannel::getId)
            .doesNotContain(channelId);
    }
}
