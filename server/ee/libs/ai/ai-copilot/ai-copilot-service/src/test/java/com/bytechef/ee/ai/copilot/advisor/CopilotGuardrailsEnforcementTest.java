/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.RunAgentParameters;
import com.agui.core.event.RunErrorEvent;
import com.agui.core.event.RunFinishedEvent;
import com.agui.core.event.TextMessageContentEvent;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.advisor.CopilotGuardrailsAdvisorFactory;
import com.bytechef.ai.copilot.agent.JsonSchemaBuilderSpringAIAgent;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotGeneratorImpl;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotRequest;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionPromptBuilder;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.advisor.AiGuardrailsAdvisorProviderImpl;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsScope;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings.BlockingMode;
import com.bytechef.ee.platform.ai.guardrails.exception.AiGuardrailViolationException;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService;
import com.bytechef.ee.platform.ai.workspace.JobPrincipalWorkspaceResolver;
import com.bytechef.platform.ai.guardrails.AiGuardrailsAdvisorProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Flux;

/**
 * @version ee
 * @author Ivica Cardic
 */
class CopilotGuardrailsEnforcementTest {

    private static final long WORKSPACE_ID = 7L;
    private static final long OTHER_WORKSPACE_ID = 8L;
    private static final String BLOCKED_TERM = "classified";
    private static final String EMAIL = "bob@acme.io";

    private final AiGuardrailsWorkspaceSettingsService settingsService =
        mock(AiGuardrailsWorkspaceSettingsService.class);

    @Test
    void testCopilotAgentBlocksAWorkspaceBlockedTermAndNeverCallsTheModel() throws Exception {
        workspaceSettings(WORKSPACE_ID, null, BLOCKED_TERM);

        RecordingChatModel chatModel = new RecordingChatModel("Done.");

        AgentRun agentRun = runAgent(chatModel, provider(), WORKSPACE_ID, "Summarize the " + BLOCKED_TERM + " memo");

        assertThat(agentRun.runErrors()).hasSize(1);
        assertThat(agentRun.runErrors()
            .getFirst()).doesNotContain(BLOCKED_TERM);
        assertThat(chatModel.prompts()).isEmpty();
    }

    @Test
    void testCopilotAgentRedactsWorkspacePiiBeforeTheModelSeesIt() throws Exception {
        workspaceSettings(WORKSPACE_ID, true, null);

        RecordingChatModel chatModel = new RecordingChatModel("Noted.");

        AgentRun agentRun = runAgent(chatModel, provider(), WORKSPACE_ID, "Send the report to " + EMAIL);

        assertThat(agentRun.runErrors()).isEmpty();
        assertThat(chatModel.lastUserText()).isNotNull()
            .doesNotContain(EMAIL);
    }

    @Test
    void testCopilotAgentWithoutGuardrailsIsUnchanged() throws Exception {
        RecordingChatModel chatModel = new RecordingChatModel("Contact " + EMAIL);

        String userText = "Summarize the " + BLOCKED_TERM + " memo for " + EMAIL;

        AgentRun agentRun = runAgent(chatModel, null, WORKSPACE_ID, userText);

        assertThat(agentRun.runErrors()).isEmpty();
        assertThat(chatModel.lastUserText()).isEqualTo(userText);
        assertThat(agentRun.text()).isEqualTo("Contact " + EMAIL);
    }

    @Test
    void testCopilotAgentInAWorkspaceWithoutGuardrailsIsUnchanged() throws Exception {
        workspaceSettings(WORKSPACE_ID, true, BLOCKED_TERM);

        RecordingChatModel chatModel = new RecordingChatModel("Noted.");

        String userText = "Summarize the " + BLOCKED_TERM + " memo for " + EMAIL;

        AgentRun agentRun = runAgent(chatModel, provider(), OTHER_WORKSPACE_ID, userText);

        assertThat(agentRun.runErrors()).isEmpty();
        assertThat(chatModel.lastUserText()).isEqualTo(userText);
    }

    @Test
    void testWorkflowDescriptionGeneratorBlocksABlockedTermAndNeverCallsTheModel() {
        tenantDefaultSettings(null, BLOCKED_TERM);

        RecordingChatModel chatModel = new RecordingChatModel("A description.");

        WorkflowDescriptionCopilotGeneratorImpl generator = workflowDescriptionGenerator(
            chatModel, provider(), "{\"label\":\"" + BLOCKED_TERM + " sync\"}");

        assertThatThrownBy(() -> generator.generate(new WorkflowDescriptionCopilotRequest("workflow-1", null, 0)))
            .isInstanceOf(AiGuardrailViolationException.class);

        assertThat(chatModel.prompts()).isEmpty();
    }

    @Test
    void testWorkflowDescriptionGeneratorRedactsPiiBeforeTheModelSeesIt() {
        tenantDefaultSettings(true, null);

        RecordingChatModel chatModel = new RecordingChatModel("A description.");

        WorkflowDescriptionCopilotGeneratorImpl generator = workflowDescriptionGenerator(
            chatModel, provider(), "{\"label\":\"Mail " + EMAIL + "\"}");

        generator.generate(new WorkflowDescriptionCopilotRequest("workflow-1", null, 0));

        assertThat(chatModel.lastUserText()).isNotNull()
            .doesNotContain(EMAIL);
    }

    @Test
    void testWorkflowDescriptionGeneratorWithoutGuardrailsIsUnchanged() {
        RecordingChatModel chatModel = new RecordingChatModel("A description.");

        WorkflowDescriptionCopilotGeneratorImpl generator = workflowDescriptionGenerator(
            chatModel, null, "{\"label\":\"Mail " + EMAIL + "\"}");

        assertThat(generator.generate(new WorkflowDescriptionCopilotRequest("workflow-1", null, 0))
            .value()).isEqualTo("A description.");

        assertThat(chatModel.lastUserText()).contains(EMAIL);
    }

    private void workspaceSettings(long workspaceId, @Nullable Boolean redactPii, @Nullable String blockedTerms) {
        lenient().when(settingsService.fetchSettings(anyLong()))
            .thenReturn(Optional.empty());
        lenient().when(settingsService.fetchSettings(null))
            .thenReturn(Optional.empty());
        when(settingsService.fetchSettings(workspaceId)).thenReturn(Optional.of(
            new AiGuardrailsWorkspaceSettings(
                AiGuardrailsSettingsScope.WORKSPACE, workspaceId, redactPii, null, blockedTerms, null, null, null,
                BlockingMode.BLOCK, null, null, null)));
    }

    private void tenantDefaultSettings(@Nullable Boolean redactPii, @Nullable String blockedTerms) {
        when(settingsService.fetchSettings(null)).thenReturn(Optional.of(
            new AiGuardrailsWorkspaceSettings(
                AiGuardrailsSettingsScope.PLATFORM, null, redactPii, null, blockedTerms, null, null, null,
                BlockingMode.BLOCK, null, null, null)));
    }

    private AiGuardrailsAdvisorProvider provider() {
        AiGuardrails aiGuardrails = new AiGuardrails(
            settingsService, null, null, null, false, false, "", false, false, false, true);

        return new AiGuardrailsAdvisorProviderImpl(
            aiGuardrails, objectProvider(new SimpleMeterRegistry()), mock(JobPrincipalWorkspaceResolver.class));
    }

    private static CopilotGuardrailsAdvisorFactory factory(@Nullable AiGuardrailsAdvisorProvider provider) {
        return new CopilotGuardrailsAdvisorFactory(objectProvider(provider));
    }

    private static AgentRun runAgent(
        ChatModel chatModel, @Nullable AiGuardrailsAdvisorProvider provider, long workspaceId, String userText)
        throws AGUIException, InterruptedException {

        JsonSchemaBuilderSpringAIAgent agent = JsonSchemaBuilderSpringAIAgent.builder()
            .advisors(factory(provider).guardrailsAdvisors())
            .agentId("json-schema-builder-test")
            .chatModel(chatModel)
            .systemMessage("You build JSON schemas.")
            .state(new State())
            .build();

        State state = new State();

        state.set(CopilotConstants.STATE_TENANT_ID, "public");
        state.set(CopilotConstants.STATE_WORKSPACE_ID, workspaceId);

        UserMessage userMessage = new UserMessage();

        userMessage.setId("message-1");
        userMessage.setContent(userText);

        List<BaseMessage> messages = new ArrayList<>();

        messages.add(userMessage);

        AgentRun agentRun = new AgentRun();

        agent.runAgent(
            RunAgentParameters.builder()
                .threadId("thread")
                .runId("run")
                .messages(messages)
                .state(state)
                .build(),
            agentRun);

        assertThat(agentRun.await()).as("the agent run never finished")
            .isTrue();

        return agentRun;
    }

    private static WorkflowDescriptionCopilotGeneratorImpl workflowDescriptionGenerator(
        ChatModel chatModel, @Nullable AiGuardrailsAdvisorProvider provider, String definition) {

        OverrideChatClientResolver overrideChatClientResolver = mock(OverrideChatClientResolver.class);
        WorkflowService workflowService = mock(WorkflowService.class);
        Workflow workflow = mock(Workflow.class);

        when(overrideChatClientResolver.resolveDefaultChatModel(0)).thenReturn(chatModel);
        when(workflow.getDefinition()).thenReturn(definition);
        when(workflowService.getWorkflow("workflow-1")).thenReturn(workflow);

        ObjectProvider<MeterRegistry> meterRegistryProvider = objectProvider(null);

        return new WorkflowDescriptionCopilotGeneratorImpl(
            overrideChatClientResolver, workflowService, new WorkflowDescriptionPromptBuilder(), meterRegistryProvider,
            factory(provider));
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> objectProvider(@Nullable T value) {
        ObjectProvider<T> objectProvider = mock(ObjectProvider.class);

        lenient().when(objectProvider.getIfAvailable())
            .thenReturn(value);

        return objectProvider;
    }

    private static final class AgentRun implements AgentSubscriber {

        private final CountDownLatch finished = new CountDownLatch(1);
        private final List<String> runErrors = new CopyOnWriteArrayList<>();
        private final StringBuffer text = new StringBuffer();

        @Override
        public void onRunErrorEvent(RunErrorEvent event) {
            runErrors.add(event.getError());

            finished.countDown();
        }

        @Override
        public void onRunFinishedEvent(RunFinishedEvent event) {
            finished.countDown();
        }

        @Override
        public void onTextMessageContentEvent(TextMessageContentEvent event) {
            text.append(event.getDelta());
        }

        private boolean await() throws InterruptedException {
            return finished.await(10, TimeUnit.SECONDS);
        }

        private List<String> runErrors() {
            return runErrors;
        }

        private String text() {
            return text.toString();
        }
    }

    private static final class RecordingChatModel implements ChatModel {

        private final String reply;
        private final List<Prompt> prompts = new CopyOnWriteArrayList<>();

        private RecordingChatModel(String reply) {
            this.reply = reply;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);

            return response();
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            prompts.add(prompt);

            return Flux.just(response());
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder()
                .build();
        }

        private List<Prompt> prompts() {
            return prompts;
        }

        private @Nullable String lastUserText() {
            if (prompts.isEmpty()) {
                return null;
            }

            Prompt prompt = prompts.getLast();

            List<String> userTexts = prompt.getInstructions()
                .stream()
                .filter(message -> message.getMessageType() == MessageType.USER)
                .map(Message::getText)
                .toList();

            return userTexts.isEmpty() ? null : userTexts.getLast();
        }

        private ChatResponse response() {
            return ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage(reply))))
                .build();
        }
    }
}
