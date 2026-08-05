/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.ai.mcp.server.spi.McpAppUiDescriptor;
import com.bytechef.ai.mcp.server.spi.McpAppViewerResources;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.configuration.service.ProjectCodeWorkflowInfoSupplier;
import com.bytechef.ee.automation.configuration.facade.ProjectCodeWorkflowFacade;
import com.bytechef.ee.platform.customcomponent.configuration.facade.CustomComponentFacade;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
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
            .resourceUri()).isEqualTo(McpAppViewerResources.CODE_WORKFLOW_VIEWER_URI);
        assertThat(descriptors.get("getCustomComponentSource")
            .resourceUri()).isEqualTo(McpAppViewerResources.CUSTOM_COMPONENT_VIEWER_URI);
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
    void testSkipsToolsWhenFacadesAbsent() {
        McpServerToolCallbackContributor contributor = configuration.codeCustomComponentViewerMcpContributor(
            absent(), absent(), absent());

        assertThat(contributor.getToolCallbacks()).isEmpty();
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
