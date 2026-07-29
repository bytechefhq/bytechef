/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class MapToolCallbackResolverTest {

    @Test
    void testResolveReturnsMappedCallbackWithoutTouchingToolDefinition() {
        ToolCallback toolCallback = mock(ToolCallback.class);

        MapToolCallbackResolver resolver = new MapToolCallbackResolver(Map.of("slack_sendMessage", toolCallback));

        assertThat(resolver.resolve("slack_sendMessage")).isSameAs(toolCallback);
        assertThat(resolver.resolve("missing")).isNull();

        // Resolution must key off the map, never call getToolDefinition() (which would force a lazy schema/component
        // load for a ClusterElementToolCallback).
        verifyNoInteractions(toolCallback);
    }
}
