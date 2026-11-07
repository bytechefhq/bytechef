/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.mcp.server.spi.McpAppResources;
import com.bytechef.ai.mcp.server.spi.McpAppUiDescriptor;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.configuration.service.ProjectCodeWorkflowInfoSupplier;
import com.bytechef.ee.automation.configuration.facade.ProjectCodeWorkflowFacade;
import com.bytechef.ee.platform.customcomponent.configuration.facade.CustomComponentFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CodeCustomComponentViewerMcpContributorConfigurationTest {

    private final CodeCustomComponentViewerMcpContributorConfiguration configuration =
        new CodeCustomComponentViewerMcpContributorConfiguration();

    @Test
    void testContributesCodeAndCustomComponentToolsWithDescriptors() {
        McpServerToolCallbackContributor contributor = configuration.codeCustomComponentViewerMcpContributor(
            present(mock(ProjectCodeWorkflowFacade.class)), present(mock(ProjectCodeWorkflowInfoSupplier.class)),
            present(mock(CustomComponentFacade.class)));

        List<String> toolNames = contributor.getToolCallbacks()
            .stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();

        assertThat(toolNames).contains("getCodeWorkflowSource", "getCustomComponentSource");

        Map<String, McpAppUiDescriptor> descriptors = contributor.getMcpAppUiDescriptors();

        assertThat(descriptors.get("getCodeWorkflowSource")
            .resourceUri()).isEqualTo(McpAppResources.CODE_WORKFLOW_VIEWER_URI);
        assertThat(descriptors.get("getCustomComponentSource")
            .resourceUri()).isEqualTo(McpAppResources.CUSTOM_COMPONENT_VIEWER_URI);
    }

    @Test
    void testCustomComponentShaperOmitsLanguageForViewerAutoDetection() {
        McpServerToolCallbackContributor contributor = configuration.codeCustomComponentViewerMcpContributor(
            absent(), absent(), present(mock(CustomComponentFacade.class)));

        Map<String, Object> structuredContent = contributor.getMcpAppUiDescriptors()
            .get("getCustomComponentSource")
            .structuredContentShaper()
            .apply("const component = definition();");

        assertThat(structuredContent).containsEntry("source", "const component = definition();")
            .doesNotContainKey("language");
    }

    @Test
    void testShapersUnwrapTheJsonEncodedSourceTheToolCallbackReturns() {
        String source = "({\n    name: \"leadEnricher\"\n})";

        ProjectCodeWorkflowFacade projectCodeWorkflowFacade = mock(ProjectCodeWorkflowFacade.class);
        CustomComponentFacade customComponentFacade = mock(CustomComponentFacade.class);

        when(projectCodeWorkflowFacade.getCodeWorkflowSource(1056L)).thenReturn(source);
        when(customComponentFacade.getCustomComponentSource(1L)).thenReturn(source);

        McpServerToolCallbackContributor contributor = configuration.codeCustomComponentViewerMcpContributor(
            present(projectCodeWorkflowFacade), present(mock(ProjectCodeWorkflowInfoSupplier.class)),
            present(customComponentFacade));

        Map<String, McpAppUiDescriptor> descriptors = contributor.getMcpAppUiDescriptors();

        String codeWorkflowResult = findToolCallback(contributor, "getCodeWorkflowSource").call("{\"projectId\":1056}");
        String customComponentResult = findToolCallback(contributor, "getCustomComponentSource").call("{\"id\":1}");

        assertThat(descriptors.get("getCodeWorkflowSource")
            .structuredContentShaper()
            .apply(codeWorkflowResult)).containsEntry("source", source);
        assertThat(descriptors.get("getCustomComponentSource")
            .structuredContentShaper()
            .apply(customComponentResult)).containsEntry("source", source);
    }

    @Test
    void testSkipsToolsWhenFacadesAbsent() {
        McpServerToolCallbackContributor contributor = configuration.codeCustomComponentViewerMcpContributor(
            absent(), absent(), absent());

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    private static ToolCallback findToolCallback(McpServerToolCallbackContributor contributor, String toolName) {
        return contributor.getToolCallbacks()
            .stream()
            .filter(toolCallback -> toolName.equals(toolCallback.getToolDefinition()
                .name()))
            .findFirst()
            .orElseThrow();
    }

    private static <T> ObjectProvider<T> present(T value) {
        @SuppressWarnings("unchecked")
        ObjectProvider<T> provider = mock(ObjectProvider.class);

        Mockito.when(provider.getIfAvailable())
            .thenReturn(value);

        return provider;
    }

    private static <T> ObjectProvider<T> absent() {
        @SuppressWarnings("unchecked")
        ObjectProvider<T> provider = mock(ObjectProvider.class);

        return provider;
    }
}
