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

import com.bytechef.ai.copilot.tool.ConfigureMcpServerToolCallback;
import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolScope;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.copilot.tool.catalog.SimpleIntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contributes the {@code configureMcpServer} intelligent delegate tool to the
 * {@link com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog} — the automation-owned counterpart of the CE
 * {@code CopilotIntelligentToolContributorConfiguration}, living beside {@link McpServerSubAgentConfiguration} (which
 * declares the {@link IntelligentToolChatClientFactory} bean this contributor closes over) because the MCP registration
 * stack, and the {@link ConfigureMcpServerToolCallback}'s {@code ChatClient}, are automation-owned rather than EE-gated
 * — unlike {@code buildCustomComponent}/{@code buildCodeWorkflow}, MCP servers are a CE capability available whenever
 * either the Copilot panel or the AI Hub surface is enabled.
 *
 * <p>
 * There is no ASK subagent for this domain — mapping a server's tools is always a write — so
 * {@link IntelligentToolDefinition#chatClientFactory(IntelligentToolVariant)} always returns {@code null} for
 * {@link IntelligentToolVariant#ASK} and the catalog skips this definition for ASK surfaces.
 * {@code panelScopes = Set.of(IntelligentToolScope#MCP_SERVER)} is the one panel registration this promotion adds: the
 * MCP Servers panel's BUILD agent ({@code mcp_server_build}) fetches it via
 * {@link com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog#getForPanel}.
 * </p>
 *
 * @author Ivica Cardic
 */
@Configuration
class McpServerIntelligentToolContributorConfiguration {

    @Bean
    IntelligentToolContributor mcpServerIntelligentToolContributor(
        @Qualifier("mcpServerBuildSubAgentChatClientFactory") //
        ObjectProvider<IntelligentToolChatClientFactory> mcpServerBuildFactoryProvider,
        ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider) {

        SubAgentChatModelResolver chatModelResolver = chatModelResolverProvider.getIfAvailable();

        List<IntelligentToolDefinition> definitions = List.of(
            new SimpleIntelligentToolDefinition(
                "configureMcpServer", CopilotAgentType.CONFIGURE_MCP_SERVER.key(),
                Set.of(IntelligentToolScope.MCP_SERVER),
                variant -> variant == IntelligentToolVariant.BUILD
                    ? mcpServerBuildFactoryProvider.getIfAvailable() : null,
                chatClientFactory -> new ConfigureMcpServerToolCallback(chatClientFactory, chatModelResolver)));

        return () -> definitions;
    }
}
