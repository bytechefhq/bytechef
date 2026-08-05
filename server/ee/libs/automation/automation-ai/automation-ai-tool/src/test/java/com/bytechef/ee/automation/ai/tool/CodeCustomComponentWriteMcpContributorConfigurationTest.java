/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Pins the management MCP exposure of the code-workflow and custom-component WRITE tools: each present tools bean
 * contributes its full tool set, and a missing bean skips silently.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class CodeCustomComponentWriteMcpContributorConfigurationTest {

    private final CodeCustomComponentWriteMcpContributorConfiguration configuration =
        new CodeCustomComponentWriteMcpContributorConfiguration();

    @Test
    void testContributesWriteToolsWhenBeansPresent() {
        McpServerToolCallbackContributor contributor = configuration.codeCustomComponentWriteMcpContributor(
            toPresentProvider(new CodeWorkflowTools(mock())),
            toPresentProvider(new CustomComponentTools(mock())));

        List<String> toolNames = contributor.getToolCallbacks()
            .stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();

        assertThat(toolNames).contains(
            "createCodeWorkflow", "updateCodeWorkflowSource", "createCustomComponent", "updateCustomComponentSource",
            "publishCustomComponent", "deleteCustomComponent");
    }

    @Test
    void testMissingBeansAreSkipped() {
        McpServerToolCallbackContributor contributor = configuration.codeCustomComponentWriteMcpContributor(
            toAbsentProvider(), toAbsentProvider());

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @Test
    void testPartialAvailabilityContributesOnlyPresentBean() {
        McpServerToolCallbackContributor contributor = configuration.codeCustomComponentWriteMcpContributor(
            toAbsentProvider(), toPresentProvider(new CustomComponentTools(mock())));

        List<String> toolNames = contributor.getToolCallbacks()
            .stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();

        assertThat(toolNames).contains("createCustomComponent")
            .doesNotContain("createCodeWorkflow");
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> toPresentProvider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(value);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> toAbsentProvider() {
        return mock(ObjectProvider.class);
    }
}
