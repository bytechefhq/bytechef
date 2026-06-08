/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.config;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.ee.ai.copilot.tool.ClusterElementAgentToolCallback;
import com.bytechef.ee.ai.copilot.tool.CodeEditorAgentToolCallback;
import com.bytechef.ee.ai.copilot.tool.ConverterAgentToolCallback;
import com.bytechef.ee.ai.copilot.tool.SkillsAgentToolCallback;
import com.bytechef.ee.ai.copilot.tool.WorkflowEditorAgentToolCallback;
import com.bytechef.ee.automation.ai.tool.SkillsTools;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
class ToolCallbackContributorConfiguration {

    @Bean
    McpServerToolCallbackContributor copilotAgentToolCallbackContributor(
        ObjectProvider<SkillsTools> skillsToolsProvider,
        @Qualifier("workflowEditorBuildSubAgentChatClient") ObjectProvider<ChatClient> workflowEditorProvider,
        @Qualifier("codeEditorBuildSubAgentChatClient") ObjectProvider<ChatClient> codeEditorProvider,
        @Qualifier("clusterElementBuildSubAgentChatClient") ObjectProvider<ChatClient> clusterElementProvider,
        @Qualifier("skillsBuildSubAgentChatClient") ObjectProvider<ChatClient> skillsProvider,
        @Qualifier("converterBuildSubAgentChatClient") ObjectProvider<ChatClient> converterProvider) {

        return () -> {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            skillsToolsProvider.ifAvailable(
                skillsTools -> toolCallbacks.addAll(List.of(ToolCallbacks.from(skillsTools))));

            workflowEditorProvider.ifAvailable(
                chatClient -> toolCallbacks.add(new WorkflowEditorAgentToolCallback(chatClient)));
            codeEditorProvider.ifAvailable(
                chatClient -> toolCallbacks.add(new CodeEditorAgentToolCallback(chatClient)));
            clusterElementProvider.ifAvailable(
                chatClient -> toolCallbacks.add(new ClusterElementAgentToolCallback(chatClient)));
            skillsProvider.ifAvailable(
                chatClient -> toolCallbacks.add(new SkillsAgentToolCallback(chatClient)));
            converterProvider.ifAvailable(
                chatClient -> toolCallbacks.add(new ConverterAgentToolCallback(chatClient)));

            return toolCallbacks;
        };
    }
}
