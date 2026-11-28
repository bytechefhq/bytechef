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

package com.bytechef.automation.configuration.tool.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.automation.configuration.tool.ReadProjectWorkflowTools;
import com.bytechef.platform.ai.tool.WebSearchToolCallbackProvider;
import com.bytechef.platform.component.tool.ComponentTools;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.tool.WorkflowInstructionTools;
import com.bytechef.platform.workflow.tool.WorkflowValidatorTools;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CodeEditorSubAgentConfigurationTest {

    @Test
    void testTheAskFactoryAddsTheWebSearchToolsOfTheCurrentEnvironment() {
        CodeEditorAgentConfiguration codeEditorAgentConfiguration = realCopilotConfiguration();
        ChatModel chatModel = mock(ChatModel.class);
        ComponentTools componentTools = mock(ComponentTools.class);
        ReadProjectWorkflowTools readProjectWorkflowTools = mock(ReadProjectWorkflowTools.class);
        WebSearchToolCallbackProvider webSearchToolCallbackProvider = mock(WebSearchToolCallbackProvider.class);

        when(webSearchToolCallbackProvider.getToolCallbacks(Environment.STAGING.ordinal()))
            .thenReturn(List.of(mock(ToolCallback.class)));

        ChatClient beanClient = codeEditorAgentConfiguration.codeEditorAskSubAgentChatClient(
            chatModel, readProjectWorkflowTools, componentTools);

        IntelligentToolChatClientFactory factory = codeEditorAgentConfiguration.codeEditorAskSubAgentChatClientFactory(
            beanClient, chatModel, readProjectWorkflowTools, componentTools, webSearchToolCallbackProvider);

        EnvironmentContext.set(Environment.STAGING);

        try {
            assertThat(factory.get(null)).isNotSameAs(beanClient);
        } finally {
            EnvironmentContext.clear();
        }

        assertThat(factory.get(null)).isSameAs(beanClient);

        verify(webSearchToolCallbackProvider).getToolCallbacks(Environment.STAGING.ordinal());
    }

    private static CodeEditorAgentConfiguration realCopilotConfiguration() {
        Resource promptResource = new ByteArrayResource("prompt".getBytes(StandardCharsets.UTF_8));

        WorkflowValidatorTools workflowValidatorTools = mock(WorkflowValidatorTools.class);
        WorkflowInstructionTools workflowInstructionTools = mock(WorkflowInstructionTools.class);

        return new CodeEditorAgentConfiguration(promptResource, promptResource, workflowValidatorTools,
            workflowInstructionTools);
    }
}
