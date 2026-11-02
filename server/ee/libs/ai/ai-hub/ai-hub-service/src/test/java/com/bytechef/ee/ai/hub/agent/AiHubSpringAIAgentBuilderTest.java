/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tool objects registered through {@code tools(...)} or {@code tool(...)} would reach the chat request without the
 * approval gate, the context rehydrator or the AG-UI events, so the hub builder refuses them.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubSpringAIAgentBuilderTest {

    @Test
    void testToolsIsUnsupported() {
        AiHubSpringAIAgent.Builder builder = AiHubSpringAIAgent.builder();

        assertThatThrownBy(() -> builder.tools(List.of(new Object())))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("toolCallbacks");
    }

    @Test
    void testToolIsUnsupported() {
        AiHubSpringAIAgent.Builder builder = AiHubSpringAIAgent.builder();

        assertThatThrownBy(() -> builder.tool(new Object()))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("toolCallbacks");
    }
}
