/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentScheduleService;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentService;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * Covers the personal-agent manager subagent configuration (the one management manager that stays AI-hub-specific):
 * ChatClient construction succeeds against mocks and the delegate ToolCallback carries the agent-type key the ai_hub
 * BUILD prompt references. The mcp_manager / deployment_manager / api_collection_manager configurations moved to
 * automation-ai-tool and are covered there.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class PersonalAgentManagerConfigurationTest {

    @Test
    void testPersonalAgentManagerChatClientIsBuilt() {
        Resource promptResource = new ByteArrayResource(
            "You are the personal_agent_manager subagent.".getBytes(StandardCharsets.UTF_8), "test prompt resource");

        PersonalAgentManagerConfiguration configuration = new PersonalAgentManagerConfiguration();

        assertThatNoException().isThrownBy(
            () -> configuration.personalAgentManagerChatClient(
                mock(AiHubPersonalAgentService.class), mock(AiHubPersonalAgentScheduleService.class),
                mock(AiHubTaskService.class), mock(ChatModel.class), promptResource));
    }

    @Test
    void testPersonalAgentManagerToolCallbackIsNamedCorrectly() {
        ToolCallback toolCallback = PersonalAgentManagerConfiguration.createPersonalAgentManagerToolCallback(
            mock(ChatClient.class));

        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("personal_agent_manager");
    }
}
