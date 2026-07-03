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
        // Scenario: a well-behaved tool returned its JSON envelope. The wrapper must not alter it — substituting
        // valid content with a placeholder would corrupt the agent's conversation history.
        ToolCallback delegate = mockCallback("askUserQuestion", "{\"kind\":\"ask-user-question\"}");

        ToolCallback wrapped = NonEmptyToolCallback.wrap(delegate);

        assertThat(wrapped.call("{}")).isEqualTo("{\"kind\":\"ask-user-question\"}");
    }

    @Test
    void testSubstitutesPlaceholderWhenResultIsEmpty() {
        // Scenario: a misbehaving tool returned "". Without this wrapper Spring AI would forward the empty
        // string as the tool_result content, and Anthropic would reject the next agent iteration with HTTP
        // 400 "messages.<N>: user messages must have non-empty content" — aborting the entire turn.
        ToolCallback delegate = mockCallback("badTool", "");

        ToolCallback wrapped = NonEmptyToolCallback.wrap(delegate);

        String result = wrapped.call("{}");

        assertThat(result)
            .isNotEmpty()
            .contains("no content");
    }

    @Test
    void testSubstitutesPlaceholderWhenResultIsNull() {
        // Scenario: a tool callback that returned null. Same failure mode as empty — Anthropic rejects the
        // next iteration. The wrapper turns it into a placeholder so the loop survives.
        ToolCallback delegate = mockCallback("nullTool", null);

        ToolCallback wrapped = NonEmptyToolCallback.wrap(delegate);

        assertThat(wrapped.call("{}"))
            .isNotEmpty()
            .contains("no content");
    }

    @Test
    void testSubstitutesPlaceholderWhenResultIsWhitespaceOnly() {
        // Scenario: a tool returned " \n " (whitespace, but not technically empty). Anthropic's validator
        // trims first then checks emptiness; a whitespace-only payload triggers the same 400. Treat as empty.
        ToolCallback delegate = mockCallback("whitespaceTool", "   \n\t  ");

        ToolCallback wrapped = NonEmptyToolCallback.wrap(delegate);

        assertThat(wrapped.call("{}"))
            .isNotEmpty()
            .contains("no content");
    }

    @Test
    void testWrapIsIdempotentForAlreadyWrappedDelegate() {
        // Scenario: a registration site wraps, then a different registration site wraps again. Double-wrapping
        // would attribute the WARN log to the outer wrapper instead of the actual offender. wrap() must short-
        // circuit when its argument is already a NonEmptyToolCallback so the inner one is preserved.
        ToolCallback inner = mockCallback("tool", "{}");

        ToolCallback wrapped = NonEmptyToolCallback.wrap(inner);
        ToolCallback doubleWrapped = NonEmptyToolCallback.wrap(wrapped);

        assertThat(doubleWrapped).isSameAs(wrapped);
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
