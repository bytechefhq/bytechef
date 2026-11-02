/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.copilot.config;

import com.bytechef.ai.copilot.tool.ask.SubAgentQuestionRenderer;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
public class EmbeddedCopilotMcpContributorConfiguration {

    public static final Set<String> INTELLIGENT_TOOL_NAMES = Set.of("buildIntegrationWorkflow");

    @Bean
    McpServerToolCallbackContributor embeddedWorkflowEditorMcpToolCallbackContributor(
        IntelligentToolCatalog intelligentToolCatalog, WorkspaceFacade workspaceFacade) {

        return () -> intelligentToolCatalog.getByNames(
            INTELLIGENT_TOOL_NAMES, IntelligentToolVariant.BUILD, (chatClient, definition) -> chatClient,
            (toolCallback, definition) -> new WorkspaceScopedSubAgentToolCallback(toolCallback, workspaceFacade),
            SubAgentQuestionRenderer.PLAIN_TEXT);
    }
}
