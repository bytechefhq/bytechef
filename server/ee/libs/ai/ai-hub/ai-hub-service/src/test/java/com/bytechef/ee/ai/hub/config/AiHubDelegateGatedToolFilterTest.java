/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalPolicy;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 * The {@code buildWorkflow} and {@code importWorkflow} delegates run their own sub-agent, whose tool calls never pass
 * the hub's approval gate. The hub therefore hands those sub-agents a chat client that never offers a tool the gate
 * gates, by default or by a workspace REQUIRE rule, so a gated tool such as {@code deleteWorkflow} cannot be reached
 * through a delegation.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiHubDelegateGatedToolFilterTest {

    private static final List<String> GATED_TOOL_NAMES = List.of(
        "deleteWorkflow", "deleteProject", "deleteProjectDeployment", "rollbackProjectDeployment", "promoteWorkflow",
        "dropTable");

    private static final List<String> UNGATED_TOOL_NAMES = List.of("createWorkflow", "updateWorkflow", "validate");

    private static final long WORKSPACE_ID = 7L;

    @Test
    void testHubDelegateSubAgentNeverOffersAGatedTool() throws Exception {
        AiHubAgentFixtures fixtures = AiHubAgentFixtures.withBuildWorkflow();

        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator =
            captureChatClientDecorator(fixtures);

        PromptCapturingChatModel chatModel = new PromptCapturingChatModel();
        AtomicInteger gatedToolInvocations = new AtomicInteger();

        List<ToolCallback> subAgentToolCallbacks = subAgentToolCallbacks(gatedToolInvocations);

        ChatClient subAgentChatClient = ChatClient.builder(chatModel)
            .defaultTools(subAgentToolCallbacks.toArray())
            .build();

        ChatClient hubSubAgentChatClient = chatClientDecorator.apply(subAgentChatClient, null);

        hubSubAgentChatClient.prompt("Remove the Orders workflow")
            .call()
            .content();

        assertThat(chatModel.lastOfferedToolNames()).containsExactlyInAnyOrderElementsOf(UNGATED_TOOL_NAMES);
        assertThat(gatedToolInvocations).hasValue(0);

        subAgentChatClient.prompt("Remove the Orders workflow")
            .call()
            .content();

        assertThat(chatModel.lastOfferedToolNames()).containsAll(GATED_TOOL_NAMES);
    }

    @Test
    void testHubDelegateSubAgentNeverOffersAToolTheWorkspaceRequiresApprovalFor() throws Exception {
        AiHubAgentFixtures fixtures = AiHubAgentFixtures.withBuildWorkflow();

        when(fixtures.policy.decide(WORKSPACE_ID))
            .thenReturn(new AiHubToolApprovalPolicy.Decision(Set.of("saveWorkflowTestConnection"), Set.of()));

        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator =
            captureChatClientDecorator(fixtures);

        PromptCapturingChatModel chatModel = new PromptCapturingChatModel();
        AtomicInteger requiredToolInvocations = new AtomicInteger();

        ChatClient subAgentChatClient = ChatClient.builder(chatModel)
            .defaultTools(
                toolCallback("saveWorkflowTestConnection", requiredToolInvocations),
                toolCallback("createWorkflow", new AtomicInteger()))
            .build();

        ChatClient hubSubAgentChatClient = chatClientDecorator.apply(subAgentChatClient, null);

        Map<String, Object> toolContext = AgentToolInvocationContext.builder()
            .workspaceId(WORKSPACE_ID)
            .build()
            .toToolContext();

        hubSubAgentChatClient.prompt("Bind the Slack connection")
            .toolContext(toolContext)
            .call()
            .content();

        assertThat(chatModel.lastOfferedToolNames()).containsExactly("createWorkflow");
        assertThat(requiredToolInvocations).hasValue(0);
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private static BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> captureChatClientDecorator(
        AiHubAgentFixtures fixtures) throws Exception {

        fixtures.buildAgent();

        ArgumentCaptor<BiFunction> chatClientDecoratorCaptor = ArgumentCaptor.forClass(BiFunction.class);

        verify(fixtures.intelligentToolCatalog)
            .getByNames(anySet(), eq(IntelligentToolVariant.BUILD), chatClientDecoratorCaptor.capture(), any());

        return chatClientDecoratorCaptor.getValue();
    }

    private static List<ToolCallback> subAgentToolCallbacks(AtomicInteger gatedToolInvocations) {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (String name : GATED_TOOL_NAMES) {
            toolCallbacks.add(toolCallback(name, gatedToolInvocations));
        }

        for (String name : UNGATED_TOOL_NAMES) {
            toolCallbacks.add(toolCallback(name, new AtomicInteger()));
        }

        return toolCallbacks;
    }

    private static ToolCallback toolCallback(String name, AtomicInteger invocations) {
        return FunctionToolCallback.builder(name, (String input) -> {
            invocations.incrementAndGet();

            return "{}";
        })
            .description(name)
            .inputType(String.class)
            .build();
    }

    private static final class PromptCapturingChatModel implements ChatModel {

        private final List<Prompt> prompts = new ArrayList<>();

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);

            return new ChatResponse(List.of(new Generation(new AssistantMessage("done"))));
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder()
                .build();
        }

        List<String> lastOfferedToolNames() {
            Prompt prompt = prompts.getLast();

            if (!(prompt.getOptions() instanceof ToolCallingChatOptions chatOptions)) {
                return List.of();
            }

            List<ToolCallback> toolCallbacks = chatOptions.getToolCallbacks();

            return toolCallbacks == null ? List.of()
                : toolCallbacks.stream()
                    .map(toolCallback -> toolCallback.getToolDefinition()
                        .name())
                    .toList();
        }
    }
}
