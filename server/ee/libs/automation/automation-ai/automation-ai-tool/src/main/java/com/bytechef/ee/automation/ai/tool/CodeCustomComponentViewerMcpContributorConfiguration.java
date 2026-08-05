/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import com.bytechef.ai.mcp.server.spi.McpAppUiDescriptor;
import com.bytechef.ai.mcp.server.spi.McpAppViewerResources;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.configuration.service.ProjectCodeWorkflowInfoSupplier;
import com.bytechef.ee.automation.configuration.facade.ProjectCodeWorkflowFacade;
import com.bytechef.ee.platform.customcomponent.configuration.facade.CustomComponentFacade;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Surfaces the EE read-only viewer-backing tools ({@code getCodeWorkflowSource} → code-workflow viewer,
 * {@code getCustomComponentSource} → custom-component viewer) as first-class management MCP tools and declares their
 * MCP App UI descriptors. Their {@code list*} sibling tools are contributed too (useful for list-then-read) but carry
 * no viewer. Each backing facade is optional (via {@link ObjectProvider}) so a deployment lacking it exposes neither.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(name = "bytechef.ai.mcp.server.enabled", havingValue = "true", matchIfMissing = true)
public class CodeCustomComponentViewerMcpContributorConfiguration {

    @Bean
    McpServerToolCallbackContributor codeCustomComponentViewerMcpContributor(
        ObjectProvider<ProjectCodeWorkflowFacade> projectCodeWorkflowFacadeProvider,
        ObjectProvider<ProjectCodeWorkflowInfoSupplier> projectCodeWorkflowInfoSupplierProvider,
        ObjectProvider<CustomComponentFacade> customComponentFacadeProvider) {

        return new McpServerToolCallbackContributor() {

            @Override
            public List<ToolCallback> getToolCallbacks() {
                List<ToolCallback> toolCallbacks = new ArrayList<>();

                ProjectCodeWorkflowFacade projectCodeWorkflowFacade =
                    projectCodeWorkflowFacadeProvider.getIfAvailable();
                ProjectCodeWorkflowInfoSupplier projectCodeWorkflowInfoSupplier =
                    projectCodeWorkflowInfoSupplierProvider.getIfAvailable();

                if (projectCodeWorkflowFacade != null && projectCodeWorkflowInfoSupplier != null) {
                    toolCallbacks.addAll(List.of(ToolCallbacks.from(
                        new ReadCodeWorkflowTools(projectCodeWorkflowFacade, projectCodeWorkflowInfoSupplier))));
                }

                CustomComponentFacade customComponentFacade = customComponentFacadeProvider.getIfAvailable();

                if (customComponentFacade != null) {
                    toolCallbacks.addAll(List.of(ToolCallbacks.from(
                        new ReadCustomComponentTools(customComponentFacade))));
                }

                return toolCallbacks;
            }

            @Override
            public Map<String, McpAppUiDescriptor> getMcpAppUiDescriptors() {
                return Map.of(
                    "getCodeWorkflowSource",
                    new McpAppUiDescriptor(
                        McpAppViewerResources.CODE_WORKFLOW_VIEWER_URI,
                        CodeCustomComponentViewerMcpContributorConfiguration::shapeCodeWorkflow),
                    "getCustomComponentSource",
                    new McpAppUiDescriptor(
                        McpAppViewerResources.CUSTOM_COMPONENT_VIEWER_URI,
                        CodeCustomComponentViewerMcpContributorConfiguration::shapeCustomComponent));
            }
        };
    }

    // getCodeWorkflowSource returns the source as raw text; the code-workflow viewer reads {source}.
    private static @Nullable Map<String, Object> shapeCodeWorkflow(String resultText) {
        return Map.of("source", resultText);
    }

    // getCustomComponentSource returns polyglot source (JavaScript/Python/Ruby — Java components have no editable
    // source and are rejected by the facade) as raw text; the viewer reads {source} and auto-detects the language.
    private static @Nullable Map<String, Object> shapeCustomComponent(String resultText) {
        return Map.of("source", resultText);
    }
}
