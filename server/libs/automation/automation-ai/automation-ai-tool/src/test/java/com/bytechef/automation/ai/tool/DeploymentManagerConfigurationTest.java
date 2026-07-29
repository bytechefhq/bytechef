/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * Covers the deployment_manager subagent configuration: ChatClient construction succeeds against mocks and the delegate
 * ToolCallback carries the agent-type key the ai_hub BUILD prompt references.
 *
 * @author Ivica Cardic
 */
class DeploymentManagerConfigurationTest {

    @Test
    void testDeploymentManagerChatClientIsBuilt() {
        Resource promptResource = new ByteArrayResource(
            "You are the deployment_manager subagent.".getBytes(StandardCharsets.UTF_8), "test prompt resource");

        DeploymentManagerConfiguration configuration = new DeploymentManagerConfiguration();

        assertThatNoException().isThrownBy(
            () -> configuration.deploymentManagerChatClient(
                mock(ChatModel.class), mock(ProjectDeploymentFacade.class), promptResource));
    }

    @Test
    void testDeploymentManagerToolCallbackIsNamedCorrectly() {
        ToolCallback toolCallback = DeploymentManagerConfiguration.createDeploymentManagerToolCallback(
            mock(ChatClient.class));

        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("deployment_manager");
    }
}
