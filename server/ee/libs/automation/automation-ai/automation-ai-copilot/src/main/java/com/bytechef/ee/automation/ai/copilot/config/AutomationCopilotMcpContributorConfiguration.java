/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.copilot.config;

import com.bytechef.ee.automation.ai.copilot.tool.CodeWorkflowAgentToolCallback;
import com.bytechef.ee.automation.ai.copilot.tool.ContextStoreAgentToolCallback;
import com.bytechef.ee.automation.ai.copilot.tool.CustomComponentAgentToolCallback;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contributes the EE Copilot domain subagents ({@code context_store_agent}, {@code custom_component_agent},
 * {@code code_workflow_agent}) to the management MCP server through the {@link McpServerToolCallbackContributor} SPI —
 * the EE counterpart of the CE {@code ToolCallbackContributorConfiguration}, which contributes only CE-domain
 * subagents. Each domain exposes its BUILD subagent {@link ChatClient} (defined in
 * {@link AutomationCopilotConfiguration} / {@link ContextStoreAgentConfiguration}), whose tool set includes the
 * read-only tools, so the single delegate tool serves both ask-style and build-style requests. An absent chat client
 * bean (surface toggles off, context-store feature disabled) skips silently.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class AutomationCopilotMcpContributorConfiguration {

    @Bean
    McpServerToolCallbackContributor automationCopilotAgentToolCallbackContributor(
        @Qualifier("contextStoreBuildSubAgentChatClient") ObjectProvider<ChatClient> contextStoreProvider,
        @Qualifier("customComponentBuildSubAgentChatClient") ObjectProvider<ChatClient> customComponentProvider,
        @Qualifier("codeWorkflowBuildSubAgentChatClient") ObjectProvider<ChatClient> codeWorkflowProvider) {

        return () -> {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            contextStoreProvider.ifAvailable(
                chatClient -> toolCallbacks.add(new ContextStoreAgentToolCallback(chatClient)));
            customComponentProvider.ifAvailable(
                chatClient -> toolCallbacks.add(new CustomComponentAgentToolCallback(chatClient)));
            codeWorkflowProvider.ifAvailable(
                chatClient -> toolCallbacks.add(new CodeWorkflowAgentToolCallback(chatClient)));

            return toolCallbacks;
        };
    }
}
