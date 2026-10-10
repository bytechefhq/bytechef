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

package com.bytechef.ai.copilot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.agui.core.exception.AGUIException;
import com.bytechef.ai.copilot.agent.McpServerSpringAIAgent;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.connection.CopilotConnectionLister;
import com.bytechef.ai.copilot.tool.PropertyOptionsResolver;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.platform.ai.tool.WorkflowInstructionTools;
import com.bytechef.platform.ai.tool.WorkflowValidatorTools;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * @author Ivica Cardic
 */
class CopilotConfigurationTest {

    private static final Resource PROMPT_RESOURCE = new ByteArrayResource("prompt".getBytes(StandardCharsets.UTF_8));

    private final CopilotConfiguration copilotConfiguration = new CopilotConfiguration(
        PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE,
        PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE, PROMPT_RESOURCE, mock(WorkflowValidatorTools.class),
        mock(WorkflowInstructionTools.class), mock(ConnectionDefinitionService.class),
        mock(WorkspaceConnectionFacade.class), mock(ComponentDefinitionService.class),
        mock(ActionDefinitionService.class), mock(ActionDefinitionFacade.class), mock(TriggerDefinitionService.class),
        mock(TriggerDefinitionFacade.class), mock(PropertyOptionsResolver.class),
        CopilotConfigurationTest.<CopilotConnectionLister>emptyProvider());

    @Test
    void testMcpServerBuildAgentIsRegisteredUnderTheMcpServerSourceId() throws AGUIException {
        McpServerSpringAIAgent mcpServerSpringAIAgent = copilotConfiguration.mcpServerBuildSpringAIAgent(
            mock(ChatMemory.class), mock(ChatModel.class),
            CopilotConfigurationTest.<IntelligentToolChatClientFactory>emptyProvider(),
            PROMPT_RESOURCE, mock(SecurityContextRehydrator.class),
            CopilotConfigurationTest.<OverrideChatClientResolver>emptyProvider());

        assertThat(mcpServerSpringAIAgent.getAgentId()).isEqualTo("mcp_server_build");
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        return mock(ObjectProvider.class);
    }
}
