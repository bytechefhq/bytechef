/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.tool.DelegatingToolCallback;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class NonEmptyToolCallbackTest {

    @Test
    void testPassesThroughNonEmptyResult() {
        ToolCallback delegate = mockCallback("askUserQuestion", "{\"kind\":\"ask-user-question\"}");

        ToolCallback wrapped = NonEmptyToolCallback.wrap(delegate);

        assertThat(wrapped.call("{}")).isEqualTo("{\"kind\":\"ask-user-question\"}");
    }

    @Test
    void testSubstitutesPlaceholderWhenResultIsEmpty() {
        ToolCallback delegate = mockCallback("badTool", "");

        ToolCallback wrapped = NonEmptyToolCallback.wrap(delegate);

        String result = wrapped.call("{}");

        assertThat(result)
            .isNotEmpty()
            .contains("no content");
    }

    @Test
    void testSubstitutesPlaceholderWhenResultIsNull() {
        ToolCallback delegate = mockCallback("nullTool", null);

        ToolCallback wrapped = NonEmptyToolCallback.wrap(delegate);

        assertThat(wrapped.call("{}"))
            .isNotEmpty()
            .contains("no content");
    }

    @Test
    void testSubstitutesPlaceholderWhenResultIsWhitespaceOnly() {
        ToolCallback delegate = mockCallback("whitespaceTool", "   \n\t  ");

        ToolCallback wrapped = NonEmptyToolCallback.wrap(delegate);

        assertThat(wrapped.call("{}"))
            .isNotEmpty()
            .contains("no content");
    }

    @Test
    void testWrapIsIdempotentForAlreadyWrappedDelegate() {
        ToolCallback inner = mockCallback("tool", "{}");

        ToolCallback wrapped = NonEmptyToolCallback.wrap(inner);
        ToolCallback doubleWrapped = NonEmptyToolCallback.wrap(wrapped);

        assertThat(doubleWrapped).isSameAs(wrapped);
    }

    @Test
    void testExposesTheWrappedCallbackAsItsDelegate() {
        ToolCallback inner = mockCallback("tool", "{}");

        ToolCallback wrapped = NonEmptyToolCallback.wrap(inner);

        assertThat(wrapped).isInstanceOf(DelegatingToolCallback.class);
        assertThat(DelegatingToolCallback.unwrap(wrapped)).isSameAs(inner);
    }

    private static ToolCallback mockCallback(String name, String returnValue) {
        ToolCallback callback = mock(ToolCallback.class);
        ToolDefinition definition = mock(ToolDefinition.class);

        when(definition.name()).thenReturn(name);
        when(callback.getToolDefinition()).thenReturn(definition);
        when(callback.call("{}")).thenReturn(returnValue);

        return callback;
    }
}
