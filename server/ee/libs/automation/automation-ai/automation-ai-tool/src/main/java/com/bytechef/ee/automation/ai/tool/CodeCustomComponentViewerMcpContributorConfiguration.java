/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import com.bytechef.ai.mcp.server.spi.McpAppResources;
import com.bytechef.ai.mcp.server.spi.McpAppUiDescriptor;
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
 * @version ee
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
                        McpAppResources.CODE_WORKFLOW_VIEWER_URI,
                        CodeCustomComponentViewerMcpContributorConfiguration::shapeCodeWorkflow),
                    "getCustomComponentSource",
                    new McpAppUiDescriptor(
                        McpAppResources.CUSTOM_COMPONENT_VIEWER_URI,
                        CodeCustomComponentViewerMcpContributorConfiguration::shapeCustomComponent));
            }
        };
    }

    private static @Nullable Map<String, Object> shapeCodeWorkflow(String resultText) {
        return Map.of("source", resultText);
    }

    private static @Nullable Map<String, Object> shapeCustomComponent(String resultText) {
        return Map.of("source", resultText);
    }
}
