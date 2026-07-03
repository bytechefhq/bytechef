/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.ai.hub.toolsearch.AiHubGlobalToolCatalog;
import com.bytechef.ee.ai.hub.toolsearch.ToolSearchCatalogFeeder;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubGlobalToolCatalogTest {

    @Test
    void testNoDuplicateToolNamesWithinAMode() {
        List<String> names = sampleAskCatalog().toolCallbacks()
            .stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();

        assertThat(names).doesNotHaveDuplicates();
    }

    @Test
    void testAskCatalogUsesAskSession() {
        assertThat(sampleAskCatalog().sessionId()).isEqualTo(ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID);
    }

    private static AiHubGlobalToolCatalog sampleAskCatalog() {
        ToolCallback listProjects = org.mockito.Mockito.mock(ToolCallback.class);

        org.mockito.Mockito.when(listProjects.getToolDefinition())
            .thenReturn(org.springframework.ai.tool.definition.ToolDefinition.builder()
                .name("listProjects")
                .description("List all projects")
                .inputSchema("{}")
                .build());

        return new AiHubGlobalToolCatalog(ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID, List.of(listProjects));
    }
}
