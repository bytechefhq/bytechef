/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.ai.guardrails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
class RedactingToolCallbackTest {

    private static final String SURFACE = "mcp_automation";
    private static final ToolContext TOOL_CONTEXT = new ToolContext(Map.<String, Object>of("tenantId", "public"));

    @Test
    void testRedactsTheDelegatesResult() {
        ToolCallback toolCallback = wrap(
            new FixedToolCallback("{\"email\":\"bob@acme.io\"}"),
            providerReturning(result -> result.replace("bob@acme.io", "[REDACTED_EMAIL_ADDRESS]")));

        assertThat(toolCallback.call("{}", TOOL_CONTEXT)).isEqualTo("{\"email\":\"[REDACTED_EMAIL_ADDRESS]\"}");
    }

    @Test
    void testRedactsTheDelegatesResultOnTheSingleArgumentOverload() {
        ToolCallback toolCallback = wrap(
            new FixedToolCallback("{\"email\":\"bob@acme.io\"}"),
            providerReturning(result -> result.replace("bob@acme.io", "[REDACTED_EMAIL_ADDRESS]")));

        assertThat(toolCallback.call("{}")).isEqualTo("{\"email\":\"[REDACTED_EMAIL_ADDRESS]\"}");
    }

    @Test
    void testReturnsTheResultUntouchedWhenNoRedactorResolves() {
        ToolCallback toolCallback = wrap(new FixedToolCallback("{\"email\":\"bob@acme.io\"}"), emptyProvider());

        assertThat(toolCallback.call("{}", TOOL_CONTEXT)).isEqualTo("{\"email\":\"bob@acme.io\"}");
    }

    @Test
    void testFailsClosedWhenRedactionThrows() {
        ToolCallback toolCallback = wrap(
            new FixedToolCallback("{\"email\":\"bob@acme.io\"}"), providerReturning(result -> {
                throw new IllegalStateException("detector exploded");
            }));

        assertThatThrownBy(() -> toolCallback.call("{}", TOOL_CONTEXT))
            .as("the raw payload must never be returned when redaction fails")
            .isInstanceOf(McpOutboundRedactionException.class);
    }

    @Test
    void testFailsClosedWhenResolvingTheRedactorThrows() {
        ToolCallback toolCallback = wrap(
            new FixedToolCallback("{\"email\":\"bob@acme.io\"}"), staticProvider((workspaceId, surface) -> {
                throw new IllegalStateException("settings lookup failed");
            }));

        assertThatThrownBy(() -> toolCallback.call("{}", TOOL_CONTEXT))
            .as("a settings lookup failure must not be indistinguishable from redaction being off")
            .isInstanceOf(McpOutboundRedactionException.class);
    }

    @Test
    void testRedactsTheMessageOfADelegateThatThrows() {
        ToolCallback toolCallback = wrap(
            new ThrowingToolCallback(new IllegalStateException("rejected contact bob@acme.io")),
            providerReturning(result -> result.replace("bob@acme.io", "[REDACTED_EMAIL_ADDRESS]")));

        assertThatThrownBy(() -> toolCallback.call("{}", TOOL_CONTEXT))
            .as("the MCP layer builds its tool-error text from the exception message")
            .isInstanceOf(RedactedToolExecutionException.class)
            .hasMessage("rejected contact [REDACTED_EMAIL_ADDRESS]");
    }

    @Test
    void testFailsClosedWhenRedactingTheMessageOfADelegateThatThrows() {
        ToolCallback toolCallback = wrap(
            new ThrowingToolCallback(new IllegalStateException("rejected contact bob@acme.io")),
            providerReturning(result -> {
                throw new IllegalStateException("detector exploded");
            }));

        assertThatThrownBy(() -> toolCallback.call("{}", TOOL_CONTEXT))
            .as("the raw failure message must never be the fallback when redacting it fails")
            .isInstanceOf(McpOutboundRedactionException.class)
            .hasMessageNotContaining("bob@acme.io");
    }

    @Test
    void testResolvesTheRedactorPerCallNotAtConstruction() {
        AtomicReference<McpOutboundRedactor> current = new AtomicReference<>();

        McpOutboundRedactorProvider provider =
            (workspaceId, surface) -> Optional.ofNullable(current.get());

        ToolCallback toolCallback = wrap(new FixedToolCallback("bob@acme.io"), staticProvider(provider));

        assertThat(toolCallback.call("{}", TOOL_CONTEXT)).isEqualTo("bob@acme.io");

        current.set(result -> "[REDACTED_EMAIL_ADDRESS]");

        assertThat(toolCallback.call("{}", TOOL_CONTEXT))
            .as("a client's cached tools/list must not pin the redactor resolved when the list was built")
            .isEqualTo("[REDACTED_EMAIL_ADDRESS]");
    }

    @Test
    void testDelegatesTheToolDefinitionAndMetadata() {
        FixedToolCallback delegate = new FixedToolCallback("{}");

        ToolCallback toolCallback = wrap(delegate, emptyProvider());

        ToolDefinition toolDefinition = toolCallback.getToolDefinition();

        assertThat(toolDefinition.name()).isEqualTo("sendEmail");
        assertThat(toolCallback.getToolMetadata()
            .returnDirect())
                .as("the decorator must be a pass-through for everything but the result")
                .isEqualTo(delegate.getToolMetadata()
                    .returnDirect());
    }

    private static ToolCallback wrap(
        ToolCallback delegate, ObjectProvider<McpOutboundRedactorProvider> providerProvider) {

        return RedactingToolCallback.wrap(delegate, providerProvider, 1L, SURFACE);
    }

    private static ObjectProvider<McpOutboundRedactorProvider> providerReturning(McpOutboundRedactor redactor) {
        return staticProvider((workspaceId, surface) -> Optional.of(redactor));
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<McpOutboundRedactorProvider> emptyProvider() {
        ObjectProvider<McpOutboundRedactorProvider> providerProvider = mock(ObjectProvider.class);

        when(providerProvider.getIfAvailable()).thenReturn(null);

        return providerProvider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<McpOutboundRedactorProvider> staticProvider(
        McpOutboundRedactorProvider provider) {

        ObjectProvider<McpOutboundRedactorProvider> providerProvider = mock(ObjectProvider.class);

        when(providerProvider.getIfAvailable()).thenReturn(provider);

        return providerProvider;
    }

    private record FixedToolCallback(String result) implements ToolCallback {

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name("sendEmail")
                .description("sends an email")
                .inputSchema("{}")
                .build();
        }

        @Override
        public ToolMetadata getToolMetadata() {
            return ToolMetadata.builder()
                .returnDirect(true)
                .build();
        }

        @Override
        public String call(String toolInput) {
            return result;
        }
    }

    private record ThrowingToolCallback(RuntimeException failure) implements ToolCallback {

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name("sendEmail")
                .description("sends an email")
                .inputSchema("{}")
                .build();
        }

        @Override
        public String call(String toolInput) {
            throw failure;
        }
    }
}
