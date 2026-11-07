/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.agui.spring.ai.SpringAIAgent;
import com.bytechef.ee.ai.hub.tool.AiHubToolInvocationContext;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.core.io.ByteArrayResource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubAutoMemoryToolsAdvisorTest {

    private final AiAutoMemoryService aiAutoMemoryService = mock(AiAutoMemoryService.class);

    @Test
    void testAddsEveryMemoryToolAndThePromptInBuildMode() {
        AiHubAutoMemoryToolsAdvisor advisor = newAdvisor(false);

        ChatClientRequest chatClientRequest =
            advisor.before(newRequest(ownerToolContext(), Map.of()), mock(AdvisorChain.class));

        assertThat(toolNames(chatClientRequest))
            .containsExactlyInAnyOrder(
                "existingTool", "MemoryView", "MemoryCreate", "MemoryStrReplace", "MemoryInsert", "MemoryDelete",
                "MemoryRename");
        assertThat(systemText(chatClientRequest)).contains("base prompt", "memory prompt");
    }

    @Test
    void testAddsOnlyMemoryViewWhenReadOnly() {
        AiHubAutoMemoryToolsAdvisor advisor = newAdvisor(true);

        ChatClientRequest chatClientRequest =
            advisor.before(newRequest(ownerToolContext(), Map.of()), mock(AdvisorChain.class));

        assertThat(toolNames(chatClientRequest)).containsExactlyInAnyOrder("existingTool", "MemoryView");
    }

    @Test
    void testPassesARequestWithoutAMemoryOwnerThrough() {
        AiHubAutoMemoryToolsAdvisor advisor = newAdvisor(false);

        Map<String, Object> toolContext = new AiHubToolInvocationContext(7L, null, null, null, 0L)
            .toToolContext();

        ChatClientRequest chatClientRequest = newRequest(toolContext, Map.of());

        assertThat(advisor.before(chatClientRequest, mock(AdvisorChain.class))).isSameAs(chatClientRequest);

        verifyNoInteractions(aiAutoMemoryService);
    }

    @Test
    void testWrapsAndDecoratesTheMemoryTools() {
        ToolCallback wrappedToolCallback = stubToolCallback("wrappedMemoryTool");
        ToolCallback decoratedToolCallback = stubToolCallback("decoratedMemoryTool");

        AiHubAutoMemoryToolsAdvisor advisor = AiHubAutoMemoryToolsAdvisor.builder()
            .aiAutoMemoryService(aiAutoMemoryService)
            .memorySystemPrompt(new ByteArrayResource("memory prompt".getBytes(StandardCharsets.UTF_8)))
            .readOnly(true)
            .toolCallbackWrapper(toolCallback -> wrappedToolCallback)
            .build();

        UnaryOperator<ToolCallback> toolCallbackDecorator = toolCallback -> toolCallback == wrappedToolCallback
            ? decoratedToolCallback
            : toolCallback;

        ChatClientRequest chatClientRequest = advisor.before(
            newRequest(ownerToolContext(), Map.of(SpringAIAgent.TOOL_CALLBACK_DECORATOR_KEY, toolCallbackDecorator)),
            mock(AdvisorChain.class));

        assertThat(toolNames(chatClientRequest)).containsExactlyInAnyOrder("existingTool", "decoratedMemoryTool");
    }

    private AiHubAutoMemoryToolsAdvisor newAdvisor(boolean readOnly) {
        return AiHubAutoMemoryToolsAdvisor.builder()
            .aiAutoMemoryService(aiAutoMemoryService)
            .memorySystemPrompt(new ByteArrayResource("memory prompt".getBytes(StandardCharsets.UTF_8)))
            .readOnly(readOnly)
            .build();
    }

    private static Map<String, Object> ownerToolContext() {
        return new AiHubToolInvocationContext(7L, 10L, null, null, 0L).toToolContext();
    }

    private static ChatClientRequest newRequest(Map<String, Object> toolContext, Map<String, Object> context) {
        ToolCallingChatOptions toolCallingChatOptions = ToolCallingChatOptions.builder()
            .toolCallbacks(List.of(stubToolCallback("existingTool")))
            .toolContext(toolContext)
            .build();

        Prompt prompt = new Prompt(
            List.of(new SystemMessage("base prompt"), new UserMessage("hello")), toolCallingChatOptions);

        return ChatClientRequest.builder()
            .prompt(prompt)
            .context(context)
            .build();
    }

    private static List<String> toolNames(ChatClientRequest chatClientRequest) {
        Prompt prompt = chatClientRequest.prompt();

        ToolCallingChatOptions toolCallingChatOptions =
            (ToolCallingChatOptions) Objects.requireNonNull(prompt.getOptions());

        List<ToolCallback> toolCallbacks = Objects.requireNonNull(toolCallingChatOptions.getToolCallbacks());

        return toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();
    }

    private static String systemText(ChatClientRequest chatClientRequest) {
        Prompt prompt = chatClientRequest.prompt();

        return prompt.getSystemMessage()
            .getText();
    }

    private static ToolCallback stubToolCallback(String name) {
        ToolCallback toolCallback = mock(ToolCallback.class);

        when(toolCallback.getToolDefinition())
            .thenReturn(
                ToolDefinition.builder()
                    .name(name)
                    .description(name)
                    .inputSchema("{}")
                    .build());

        return toolCallback;
    }
}
