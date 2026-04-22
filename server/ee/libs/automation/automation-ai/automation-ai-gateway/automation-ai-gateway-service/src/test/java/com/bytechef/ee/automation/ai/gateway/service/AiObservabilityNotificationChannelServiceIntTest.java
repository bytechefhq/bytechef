/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.ai.gateway.config.AiGatewayIntTestConfiguration;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityNotificationChannel;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityNotificationChannelType;
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
    private AiObservabilityNotificationChannelService aiObservabilityNotificationChannelService;

    @Test
    public void testCreateListAndDeleteChannel() {
        AiObservabilityNotificationChannel channel = aiObservabilityNotificationChannelService.create(
            new AiObservabilityNotificationChannel(
                WORKSPACE_ID, "ops-webhook",
                AiObservabilityNotificationChannelType.WEBHOOK,
                "{\"url\":\"https://example.com/hook\"}"));

        Long channelId = Validate.notNull(channel.getId(), "id");

        List<AiObservabilityNotificationChannel> listed =
            aiObservabilityNotificationChannelService.getNotificationChannelsByWorkspace(WORKSPACE_ID);

        assertThat(listed)
            .extracting(AiObservabilityNotificationChannel::getName)
            .contains("ops-webhook");

        aiObservabilityNotificationChannelService.delete(channelId);

        assertThat(aiObservabilityNotificationChannelService.getNotificationChannelsByWorkspace(WORKSPACE_ID))
            .extracting(AiObservabilityNotificationChannel::getId)
            .doesNotContain(channelId);
    }
}
