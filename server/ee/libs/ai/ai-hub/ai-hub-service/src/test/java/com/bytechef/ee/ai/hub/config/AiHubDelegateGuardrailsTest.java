/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.agent.tool.AgentType;
import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.ai.tool.AutomationToolInvocationContext;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalPolicy;
import com.bytechef.ee.ai.hub.subagent.SubAgentAdvisorContributor;
import com.bytechef.ee.ai.hub.subagent.SubAgentSessionMemoryContributor;
import com.bytechef.ee.ai.hub.subagent.WorkspaceAdvisorContributor;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsScope;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings.BlockingMode;
import com.bytechef.ee.platform.ai.guardrails.exception.AiGuardrailViolationException;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService;
import com.bytechef.platform.ai.guardrails.GuardrailSurface;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiHubDelegateGuardrailsTest {

    private static final String THREAD_ID = "thread-1";
    private static final long WORKSPACE_ID = 7L;
    private static final long OTHER_WORKSPACE_ID = 8L;
    private static final String BLOCKED_TERM = "classified";
    private static final String EMAIL = "bob@acme.io";

    private final PromptCapturingChatModel chatModel = new PromptCapturingChatModel();
    private final AiGuardrailsWorkspaceSettingsService settingsService =
        mock(AiGuardrailsWorkspaceSettingsService.class);

    @Test
    void testWorkspaceBlockedTermBlocksTheDelegateCall() throws Exception {
        workspaceSettings(WORKSPACE_ID, null, BLOCKED_TERM, BlockingMode.BLOCK);

        AiHubAgentFixtures fixtures = fixtures(guardrails());

        ChatClient delegateChatClient = decorate(captureChatClientDecorator(fixtures));

        assertThatExceptionOfType(AiGuardrailViolationException.class)
            .isThrownBy(() -> callWith(delegateChatClient, "Summarize the " + BLOCKED_TERM + " memo", WORKSPACE_ID))
            .satisfies(exception -> {
                assertThat(exception.getCategory()).isEqualTo("blocked_term");
                assertThat(exception.getMessage()).doesNotContain(BLOCKED_TERM);
            });

        assertThat(chatModel.prompts).isEmpty();
        assertThat(sessionEvents(fixtures)).isEmpty();
    }

    @Test
    void testWorkspaceBlockedTermBecomesADelegateToolError() throws Exception {
        workspaceSettings(WORKSPACE_ID, null, BLOCKED_TERM, BlockingMode.BLOCK);

        AiHubAgentFixtures fixtures = fixtures(guardrails());

        ChatClient delegateChatClient = decorate(captureChatClientDecorator(fixtures));
        ToolCallback delegateToolCallback = captureCallbackDecorator(fixtures).apply(
            new ChatClientToolCallback(delegateChatClient), mock(IntelligentToolDefinition.class));

        Map<String, Object> toolContext = new HashMap<>(
            AgentToolInvocationContext.builder()
                .workspaceId(WORKSPACE_ID)
                .conversationId(THREAD_ID)
                .build()
                .toToolContext());

        toolContext.put(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID);

        String result = delegateToolCallback.call(
            "{\"request\": \"Summarize the " + BLOCKED_TERM + " memo\"}", new ToolContext(toolContext));

        assertThat(result)
            .contains("\"error\"")
            .contains(AiGuardrailViolationException.class.getSimpleName())
            .doesNotContain(BLOCKED_TERM);
        assertThat(chatModel.prompts).isEmpty();
        assertThat(sessionEvents(fixtures)).isEmpty();
    }

    @Test
    void testWorkspaceBlockedTermDoesNotApplyToAnotherWorkspacesDelegateCall() throws Exception {
        workspaceSettings(WORKSPACE_ID, null, BLOCKED_TERM, BlockingMode.BLOCK);

        when(settingsService.fetchSettings(OTHER_WORKSPACE_ID)).thenReturn(Optional.empty());

        AiHubAgentFixtures fixtures = fixtures(guardrails());

        when(fixtures.policy.decide(OTHER_WORKSPACE_ID))
            .thenReturn(new AiHubToolApprovalPolicy.Decision(Set.of(), Set.of()));

        ChatClient delegateChatClient = decorate(captureChatClientDecorator(fixtures));

        callWith(delegateChatClient, "Summarize the " + BLOCKED_TERM + " memo", OTHER_WORKSPACE_ID);

        assertThat(chatModel.lastConversationTexts()).containsExactly("Summarize the " + BLOCKED_TERM + " memo");
    }

    @Test
    void testWorkspacePiiRedactionRedactsTheDelegateRequestBeforeTheModelAndTheSessionSeeIt() throws Exception {
        workspaceSettings(WORKSPACE_ID, true, null, BlockingMode.BLOCK);

        AiHubAgentFixtures fixtures = fixtures(guardrails());

        ChatClient delegateChatClient = decorate(captureChatClientDecorator(fixtures));

        callWith(delegateChatClient, "Send the report to " + EMAIL, WORKSPACE_ID);

        assertThat(chatModel.lastConversationTexts()).singleElement()
            .satisfies(text -> assertThat(text).doesNotContain(EMAIL));

        assertThat(sessionEvents(fixtures))
            .extracting(sessionEvent -> sessionEvent.getMessage()
                .getText())
            .isNotEmpty()
            .noneMatch(text -> text.contains(EMAIL));
    }

    @Test
    void testDelegateWithoutAiGuardrailsBehavesAsBefore() throws Exception {
        AiHubAgentFixtures fixtures = fixtures(null);

        ChatClient delegateChatClient = decorate(captureChatClientDecorator(fixtures));

        callWith(delegateChatClient, "Summarize the " + BLOCKED_TERM + " memo for " + EMAIL, WORKSPACE_ID);
        callWith(delegateChatClient, "make it shorter", WORKSPACE_ID);

        assertThat(chatModel.lastConversationTexts()).containsExactly(
            "Summarize the " + BLOCKED_TERM + " memo for " + EMAIL,
            "reply to: Summarize the " + BLOCKED_TERM + " memo for " + EMAIL, "make it shorter");
    }

    @Test
    void testDelegateWithInactiveGuardrailsBehavesAsBefore() throws Exception {
        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiHubAgentFixtures fixtures = fixtures(guardrails());

        ChatClient delegateChatClient = decorate(captureChatClientDecorator(fixtures));

        callWith(delegateChatClient, "Summarize the " + BLOCKED_TERM + " memo for " + EMAIL, WORKSPACE_ID);
        callWith(delegateChatClient, "make it shorter", WORKSPACE_ID);

        assertThat(chatModel.lastConversationTexts()).containsExactly(
            "Summarize the " + BLOCKED_TERM + " memo for " + EMAIL,
            "reply to: Summarize the " + BLOCKED_TERM + " memo for " + EMAIL, "make it shorter");
    }

    @Test
    void testDelegateContributorsPutWorkspaceGuardrailsBeforeSessionMemory() {
        List<SubAgentAdvisorContributor> contributors = AiHubConfiguration.delegateContributors(
            CopilotAgentType.BUILD_WORKFLOW.key(), AiHubAgentFixtures.withBuildWorkflow().sessionMemory, guardrails(),
            new AiGuardrailMetrics(new SimpleMeterRegistry(), GuardrailSurface.AI_HUB));

        assertThat(contributors)
            .extracting(Object::getClass)
            .containsExactly(WorkspaceAdvisorContributor.class, SubAgentSessionMemoryContributor.class);
    }

    @Test
    void testDelegateContributorsWithoutAiGuardrailsAreSessionMemoryOnly() {
        List<SubAgentAdvisorContributor> contributors = AiHubConfiguration.delegateContributors(
            CopilotAgentType.BUILD_WORKFLOW.key(), AiHubAgentFixtures.withBuildWorkflow().sessionMemory, null, null);

        assertThat(contributors)
            .extracting(Object::getClass)
            .containsExactly(SubAgentSessionMemoryContributor.class);
    }

    private void workspaceSettings(
        long workspaceId, Boolean redactPii, String blockedTerms, BlockingMode blockingMode) {

        when(settingsService.fetchSettings(workspaceId)).thenReturn(Optional.of(
            new AiGuardrailsWorkspaceSettings(
                AiGuardrailsSettingsScope.WORKSPACE, workspaceId, redactPii, null, blockedTerms, null, null, null,
                blockingMode, null, null, null)));
    }

    private AiGuardrails guardrails() {
        return new AiGuardrails(settingsService, null, null, null, false, false, "", false, false, false, false);
    }

    private static AiHubAgentFixtures fixtures(AiGuardrails aiGuardrails) {
        AiHubAgentFixtures fixtures = AiHubAgentFixtures.withBuildWorkflow();

        if (aiGuardrails != null) {
            fixtures.withAiGuardrails(aiGuardrails);
        }

        when(fixtures.policy.decide(WORKSPACE_ID))
            .thenReturn(new AiHubToolApprovalPolicy.Decision(Set.of(), Set.of()));

        return fixtures;
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

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private static BiFunction<ToolCallback, IntelligentToolDefinition, ToolCallback> captureCallbackDecorator(
        AiHubAgentFixtures fixtures) {

        ArgumentCaptor<BiFunction> callbackDecoratorCaptor = ArgumentCaptor.forClass(BiFunction.class);

        verify(fixtures.intelligentToolCatalog)
            .getByNames(anySet(), eq(IntelligentToolVariant.BUILD), any(), callbackDecoratorCaptor.capture());

        return callbackDecoratorCaptor.getValue();
    }

    private ChatClient decorate(BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator) {
        IntelligentToolDefinition definition = mock(IntelligentToolDefinition.class);
        AgentType agentType = CopilotAgentType.BUILD_WORKFLOW;

        when(definition.agentType()).thenReturn(agentType);

        return chatClientDecorator.apply(
            ChatClient.builder(chatModel)
                .build(),
            definition);
    }

    private static List<SessionEvent> sessionEvents(AiHubAgentFixtures fixtures) {
        return fixtures.sessionMemory.sessionService()
            .getEvents(
                SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, CopilotAgentType.BUILD_WORKFLOW.key()),
                EventFilter.all());
    }

    private static void callWith(ChatClient chatClient, String request, long workspaceId) {
        Map<String, Object> toolContext = AgentToolInvocationContext.builder()
            .workspaceId(workspaceId)
            .conversationId(THREAD_ID)
            .build()
            .toToolContext();

        chatClient.prompt(request)
            .toolContext(toolContext)
            .call()
            .content();
    }

    private static final class ChatClientToolCallback implements ToolCallback {

        private final ChatClient chatClient;

        private ChatClientToolCallback(ChatClient chatClient) {
            this.chatClient = chatClient;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name("buildWorkflow")
                .description("Builds a workflow")
                .inputSchema(
                    "{\"type\": \"object\", \"properties\": {\"request\": {\"type\": \"string\"}}, " +
                        "\"required\": [\"request\"]}")
                .build();
        }

        @Override
        public String call(String toolInput) {
            return call(toolInput, null);
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            Map<String, ?> input = JsonUtils.readMap(toolInput);

            return chatClient.prompt((String) input.get("request"))
                .toolContext(toolContext == null ? Map.of() : toolContext.getContext())
                .call()
                .content();
        }
    }

    private static final class PromptCapturingChatModel implements ChatModel {

        private final List<Prompt> prompts = new ArrayList<>();

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);

            UserMessage userMessage = prompt.getUserMessage();

            return new ChatResponse(
                List.of(new Generation(new AssistantMessage("reply to: " + userMessage.getText()))));
        }

        List<String> lastConversationTexts() {
            Prompt prompt = prompts.getLast();

            return prompt.getInstructions()
                .stream()
                .filter(message -> message.getMessageType() != MessageType.SYSTEM)
                .map(Message::getText)
                .toList();
        }
    }
}
