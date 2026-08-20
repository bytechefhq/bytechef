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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolScope;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
final class McpServerIntelligentToolContributorConfigurationTest {

    private final McpServerIntelligentToolContributorConfiguration configuration =
        new McpServerIntelligentToolContributorConfiguration();

    @Test
    void testContributesOneDefinitionWithExpectedNameAndAgentTypeKey() {
        List<IntelligentToolDefinition> definitions = definitionsWithProviderPresent();

        assertThat(definitions).hasSize(1);

        IntelligentToolDefinition definition = definitions.get(0);

        assertThat(definition.name()).isEqualTo("configureMcpServer");
        assertThat(definition.agentTypeKey()).isEqualTo(CopilotAgentType.CONFIGURE_MCP_SERVER.key());
        assertThat(definition.panelScopes()).containsExactly(IntelligentToolScope.MCP_SERVER);
    }

    @Test
    void testChatClientFactoryIsNullForAskButPresentForBuild() {
        List<IntelligentToolDefinition> definitions = definitionsWithProviderPresent();

        IntelligentToolDefinition definition = definitions.get(0);

        assertThat(definition.chatClientFactory(IntelligentToolVariant.ASK)).isNull();
        assertThat(definition.chatClientFactory(IntelligentToolVariant.BUILD)).isNotNull();
    }

    @Test
    void testChatClientFactoryIsNullForBuildWhenProviderHasNoBean() {
        IntelligentToolContributor contributor =
            configuration.mcpServerIntelligentToolContributor(emptyProvider(), emptyProvider());

        List<IntelligentToolDefinition> definitions = contributor.getIntelligentToolDefinitions();

        IntelligentToolDefinition definition = definitions.get(0);

        assertThat(definition.chatClientFactory(IntelligentToolVariant.BUILD)).isNull();
    }

    @Test
    void testCreateBuildsConfigureMcpServerToolCallbackWithTheExpectedName() {
        List<IntelligentToolDefinition> definitions = definitionsWithProviderPresent();

        IntelligentToolDefinition definition = definitions.get(0);

        ToolCallback toolCallback = definition.create(chatModel -> mock(ChatClient.class));

        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("configureMcpServer");
    }

    private List<IntelligentToolDefinition> definitionsWithProviderPresent() {
        IntelligentToolContributor contributor = configuration.mcpServerIntelligentToolContributor(
            present(mock(IntelligentToolChatClientFactory.class)), present(mock(SubAgentChatModelResolver.class)));

        return contributor.getIntelligentToolDefinitions();
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> present(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(value);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        return mock(ObjectProvider.class);
    }
}
