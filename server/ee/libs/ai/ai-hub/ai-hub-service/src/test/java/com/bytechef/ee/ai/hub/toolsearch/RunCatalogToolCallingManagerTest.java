/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class RunCatalogToolCallingManagerTest {

    @Test
    void testAToolTheModelCallsWithoutSearchingRunsTheRunsCatalogCallback() {
        ToolCallback staticCatalogTool = toolCallback("listComponents", "static");
        ToolCallback runCatalogTool = toolCallback("listComponents", "run");

        RunCatalogToolCallingManager toolCallingManager = new RunCatalogToolCallingManager(
            new DefaultToolCallingManager(
                ObservationRegistry.NOOP, new MapToolCallbackResolver(Map.of("listComponents", staticCatalogTool)),
                new DefaultToolExecutionExceptionProcessor(false)));

        Prompt prompt = new Prompt(
            List.of(new UserMessage("hi")),
            ToolCallingChatOptions.builder()
                .toolCallbacks(toolCallback("askUserQuestion", "pinned"))
                .toolContext("workspaceId", 7L)
                .toolContext(
                    RunCatalogToolCallingManager.RUN_CATALOG_TOOL_CALLBACKS_KEY,
                    Map.of("listComponents", runCatalogTool))
                .build());

        ChatResponse chatResponse = new ChatResponse(
            List.of(
                new Generation(
                    AssistantMessage.builder()
                        .content("")
                        .toolCalls(
                            List.of(new AssistantMessage.ToolCall("call-1", "function", "listComponents", "{}")))
                        .build())));

        toolCallingManager.executeToolCalls(prompt, chatResponse);

        verify(staticCatalogTool, never()).call(anyString(), any());

        ArgumentCaptor<ToolContext> toolContextCaptor = ArgumentCaptor.forClass(ToolContext.class);

        verify(runCatalogTool).call(anyString(), toolContextCaptor.capture());

        assertThat(toolContextCaptor.getValue()
            .getContext()).containsEntry("workspaceId", 7L)
                .doesNotContainKey(RunCatalogToolCallingManager.RUN_CATALOG_TOOL_CALLBACKS_KEY);
    }

    private static ToolCallback toolCallback(String name, String result) {
        ToolCallback toolCallback = mock(ToolCallback.class);

        when(toolCallback.getToolDefinition()).thenReturn(
            ToolDefinition.builder()
                .name(name)
                .description(name)
                .inputSchema("{\"type\":\"object\",\"properties\":{}}")
                .build());
        when(toolCallback.getToolMetadata()).thenReturn(ToolMetadata.builder()
            .build());
        when(toolCallback.call(anyString(), any())).thenReturn("\"" + result + "\"");

        return toolCallback;
    }
}
