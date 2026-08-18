/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Surfaces the EE write tools for code workflows and custom components ({@code createCodeWorkflow},
 * {@code updateCodeWorkflowSource}, {@code createCustomComponent}, {@code updateCustomComponentSource},
 * {@code publishCustomComponent}, {@code deleteCustomComponent}) as first-class management MCP tools — the write
 * counterpart of {@link CodeCustomComponentViewerMcpContributorConfiguration}'s read-only viewer tools. This lets an
 * MCP client author code directly with a read → edit → update loop instead of delegating to the one-shot
 * {@code buildCustomComponent}/{@code buildCodeWorkflow} subagents. All writes are draft-safe: editor-path saves create
 * or update drafts and can never modify a published version; publishing is a separate explicit tool call
 * ({@code publishCustomComponent} here, {@code publishProject} from the core management tool set for code workflows).
 * Each backing tools bean is optional (via {@link ObjectProvider}) so a deployment lacking it exposes nothing.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(name = "bytechef.ai.mcp.server.enabled", havingValue = "true", matchIfMissing = true)
public class CodeCustomComponentWriteMcpContributorConfiguration {

    @Bean
    McpServerToolCallbackContributor codeCustomComponentWriteMcpContributor(
        ObjectProvider<CodeWorkflowTools> codeWorkflowToolsProvider,
        ObjectProvider<CustomComponentTools> customComponentToolsProvider) {

        return () -> {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            CodeWorkflowTools codeWorkflowTools = codeWorkflowToolsProvider.getIfAvailable();

            if (codeWorkflowTools != null) {
                toolCallbacks.addAll(List.of(ToolCallbacks.from(codeWorkflowTools)));
            }

            CustomComponentTools customComponentTools = customComponentToolsProvider.getIfAvailable();

            if (customComponentTools != null) {
                toolCallbacks.addAll(List.of(ToolCallbacks.from(customComponentTools)));
            }

            return toolCallbacks;
        };
    }
}
