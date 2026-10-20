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

package com.bytechef.ai.copilot.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.context.Context;
import com.agui.core.event.RunErrorEvent;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.SystemMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.agui.spring.ai.SpringAIAgent;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.platform.ai.llm.NoChatProviderActivatedException;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.task.TaskExecutor;

/**
 * @author Ivica Cardic
 */
class CopilotSpringAIAgentTest {

    private static final String CALLER_TENANT_ID = "acme";

    @BeforeEach
    @AfterEach
    void resetContexts() {
        TenantContext.resetCurrentTenantId();
        EnvironmentContext.clear();
    }

    @Nested
    class ConversationIdTest {

        @Test
        void testConversationIdDerivesAUuidFromTheServerScopeAndThreadId() throws AGUIException {
            State state = new State();

            state.set(CopilotConstants.STATE_CONVERSATION_SCOPE, "embedded:000001:user-1:workflow-1");

            assertThat(newConverterAgent(null).conversationId(newInput(state)))
                .isEqualTo(
                    UUID.nameUUIDFromBytes(
                        "embedded:000001:user-1:workflow-1:thread".getBytes(StandardCharsets.UTF_8))
                        .toString());
        }

        @Test
        void testConversationIdFitsTheChatMemoryColumnForALongScope() throws AGUIException {
            State state = new State();

            state.set(
                CopilotConstants.STATE_CONVERSATION_SCOPE,
                "embedded:000001:1234567890:3971538b-be9a-485d-89bb-fc5eb98f8033");

            assertThat(newConverterAgent(null).conversationId(newInput(state))).hasSizeLessThanOrEqualTo(36);
        }

        @Test
        void testConversationIdSeparatesScopesThatShareAThreadId() throws AGUIException {
            State victimState = new State();

            victimState.set(CopilotConstants.STATE_CONVERSATION_SCOPE, "embedded:000001:victim:workflow-1");

            State attackerState = new State();

            attackerState.set(CopilotConstants.STATE_CONVERSATION_SCOPE, "embedded:000001:attacker:workflow-1");

            assertThat(newConverterAgent(null).conversationId(newInput(attackerState)))
                .isNotEqualTo(newConverterAgent(null).conversationId(newInput(victimState)));
        }

        @Test
        void testConversationIdFallsBackToTheThreadIdWithoutAScope() throws AGUIException {
            assertThat(newConverterAgent(null).conversationId(newInput(new State()))).isEqualTo("thread");
        }
    }

    @Nested
    class PrepareChatMemoryTest {

        private static final String CONVERSATION_ID = "conversation";

        private ChatMemory chatMemory;

        @BeforeEach
        void beforeEach() {
            chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(100)
                .build();

            chatMemory.add(
                CONVERSATION_ID,
                List.of(
                    new org.springframework.ai.chat.messages.UserMessage("Post a standup reminder"),
                    new AssistantMessage("Which channel?"),
                    new org.springframework.ai.chat.messages.UserMessage("Use #general"),
                    new AssistantMessage("Done, posting to #general.")));
        }

        @Test
        void testRewindsTheMemoryOfAnUnscopedRunToTheEditedTurn() throws AGUIException {
            newPlainAgent().prepareChatMemory(
                newInput(new State(), "Post a standup reminder", "Use #random"), chatMemory, CONVERSATION_ID);

            assertThat(chatMemory.get(CONVERSATION_ID))
                .extracting(Message::getText)
                .containsExactly("Post a standup reminder", "Which channel?");
        }

        @Test
        void testRewindsTheMemoryOfAnUnscopedRunForARegeneratedReply() throws AGUIException {
            newPlainAgent().prepareChatMemory(
                newInput(new State(), "Post a standup reminder", "Use #general"), chatMemory, CONVERSATION_ID);

            assertThat(chatMemory.get(CONVERSATION_ID))
                .extracting(Message::getText)
                .containsExactly("Post a standup reminder", "Which channel?");
        }

        @Test
        void testRunsUseAConversationLockingChatMemory() throws AGUIException {
            assertThat(newPlainAgent().chatMemoryForRun(newInput(new State()), chatMemory))
                .isInstanceOf(ConversationLockingChatMemory.class);
        }

        @Test
        void testResetsTheMemoryWhenTheFirstMessageIsEdited() throws AGUIException {
            newPlainAgent().prepareChatMemory(
                newInput(new State(), "Post a standup reminder at 9am"), chatMemory, CONVERSATION_ID);

            assertThat(chatMemory.get(CONVERSATION_ID)).isEmpty();
        }

        @Test
        void testKeepsTheMemoryWhenALongerHistoryDoesNotMatch() throws AGUIException {
            newPlainAgent().prepareChatMemory(
                newInput(new State(), "Something the server never saw", "Use #random"), chatMemory,
                CONVERSATION_ID);

            assertThat(chatMemory.get(CONVERSATION_ID)).hasSize(4);
        }

        @Test
        void testKeepsTheMemoryOfAnUnscopedRunThatContinuesTheConversation() throws AGUIException {
            newPlainAgent().prepareChatMemory(
                newInput(new State(), "Post a standup reminder", "Use #general", "Make it 10am"), chatMemory,
                CONVERSATION_ID);

            assertThat(chatMemory.get(CONVERSATION_ID)).hasSize(4);
        }

        private static CopilotSpringAIAgent newPlainAgent() throws AGUIException {
            return new PlainAgent(newBuilder(mock(ChatModel.class)), null);
        }
    }

    @Nested
    @ExtendWith(ObjectMapperSetupExtension.class)
    class EndsTurnTest {

        @Test
        void testEndsTurnForAResultThatAwaitsTheUser() throws AGUIException {
            assertThat(newConverterAgent(null)
                .endsTurn(newToolResponse("{\"kind\":\"select-connection\",\"awaitingAnswer\":true}")))
                    .isTrue();
        }

        @Test
        void testKeepsTheTurnForAResultThatDoesNotAwaitTheUser() throws AGUIException {
            assertThat(
                newConverterAgent(null)
                    .endsTurn(newToolResponse("{\"kind\":\"select-property-option\",\"awaitingAnswer\":false}")))
                        .isFalse();
            assertThat(newConverterAgent(null).endsTurn(newToolResponse("{\"error\":\"componentName is required\"}")))
                .isFalse();
        }

        @Test
        void testKeepsTheTurnForAResultThatIsNotJsonButMentionsTheKey() throws AGUIException {
            assertThat(newConverterAgent(null).endsTurn(newToolResponse("awaitingAnswer")))
                .isFalse();
        }

        private static ToolResponseMessage.ToolResponse newToolResponse(String responseData) {
            return new ToolResponseMessage.ToolResponse("call-1", "selectConnection", responseData);
        }
    }

    @Nested
    class RunErrorMessageTest {

        @Test
        void testRunErrorMessageHidesTheCauseFromAScopedRun() throws AGUIException {
            State state = new State();

            state.set(CopilotConstants.STATE_SCOPED_PROJECT_ID, 1114L);

            String message = newConverterAgent(null).runErrorMessage(
                newInput(state), new IllegalStateException("relation \"workflow\" does not exist"));

            assertThat(message).isEqualTo("The copilot run failed. Reference: run");
        }

        @Test
        void testRunErrorMessageDescribesAKnownFailureToAScopedRun() throws AGUIException {
            State state = new State();

            state.set(CopilotConstants.STATE_SCOPED_PROJECT_ID, 1114L);

            String message = newConverterAgent(null).runErrorMessage(
                newInput(state), new RuntimeException("HTTP 429: rate_limit_error for key sk-ant-1234"));

            assertThat(message).isEqualTo(CopilotRunFailures.RATE_LIMITED + " Reference: run")
                .doesNotContain("sk-ant");
        }

        @Test
        void testRunErrorMessageKeepsTheCauseForAnUnscopedRun() throws AGUIException {
            String message = newConverterAgent(null).runErrorMessage(
                newInput(new State()), new IllegalStateException("No API key configured"));

            assertThat(message).isEqualTo("No API key configured");
        }

        @Test
        void testRunErrorMessageShowsTheCauseHiddenBehindAStreamWrapperForAnUnscopedRun() throws AGUIException {
            String message = newConverterAgent(null).runErrorMessage(
                newInput(new State()),
                new RuntimeException("Stream processing failed",
                    new IllegalStateException("Your credit balance is too low")));

            assertThat(message).isEqualTo("Your credit balance is too low");
        }

        @Test
        void testRunErrorMessageNeverReturnsNull() throws AGUIException {
            String message = newConverterAgent(null).runErrorMessage(newInput(new State()), new NullPointerException());

            assertThat(message).isEqualTo("The copilot run failed. Reference: run");
        }
    }

    @Nested
    class ResolveChatClientTest {

        @Test
        void testUnavailableSelectedModelFailsTheRun() throws AGUIException {
            OverrideChatClientResolver overrideChatClientResolver = mock(OverrideChatClientResolver.class);

            when(overrideChatClientResolver.resolve(any()))
                .thenThrow(new SelectedChatModelUnavailableException("openAi", "gpt-4o", 1));

            CopilotSpringAIAgent agent = newConverterAgent(overrideChatClientResolver);

            assertThatThrownBy(() -> agent.resolveChatClient(newInput(new State())))
                .isInstanceOf(AGUIException.class)
                .hasMessageContaining("gpt-4o");
        }

        @Test
        void testNoEnabledProviderFailsTheRun() throws AGUIException {
            OverrideChatClientResolver overrideChatClientResolver = mock(OverrideChatClientResolver.class);

            when(overrideChatClientResolver.resolve(any()))
                .thenThrow(ChatModelUnavailableException.noProviderEnabled(1));

            CopilotSpringAIAgent agent = newConverterAgent(overrideChatClientResolver);

            assertThatThrownBy(() -> agent.resolveChatClient(newInput(new State())))
                .isInstanceOf(AGUIException.class)
                .hasMessageContaining("No AI provider is enabled in environment 1");
        }

        @Test
        void testOtherResolverFailuresFailTheRun() throws AGUIException {
            OverrideChatClientResolver overrideChatClientResolver = mock(OverrideChatClientResolver.class);

            when(overrideChatClientResolver.resolve(any())).thenThrow(new IllegalStateException("catalog down"));

            CopilotSpringAIAgent agent = newConverterAgent(overrideChatClientResolver);

            assertThatThrownBy(() -> agent.resolveChatClient(newInput(new State())))
                .isInstanceOf(AGUIException.class)
                .hasMessageContaining("catalog down");
        }

        @Test
        void testNullOverrideUsesTheStartupChatClient() throws AGUIException {
            OverrideChatClientResolver overrideChatClientResolver = mock(OverrideChatClientResolver.class);

            CopilotSpringAIAgent agent = newConverterAgent(overrideChatClientResolver);

            ChatClient chatClient = agent.resolveChatClient(newInput(new State()));

            assertThat(chatClient).isNotNull();
        }

        @Test
        void testUnresolvableModelEmitsExactlyOneRunErrorAndNeverCallsTheStartupModel() throws AGUIException {
            ChatModel startupChatModel = mock(ChatModel.class);
            OverrideChatClientResolver overrideChatClientResolver = mock(OverrideChatClientResolver.class);

            when(overrideChatClientResolver.resolve(any()))
                .thenThrow(ChatModelUnavailableException.noProviderEnabled(1));

            PlainAgent agent = new PlainAgent(newBuilder(startupChatModel), overrideChatClientResolver);

            List<RunErrorEvent> runErrorEvents = new ArrayList<>();

            agent.run(newInput(stateWithTenantId(CALLER_TENANT_ID)), newRunErrorCollector(runErrorEvents));

            assertThat(runErrorEvents).hasSize(1);
            assertThat(runErrorEvents.getFirst()
                .getError()).contains("No AI provider is enabled in environment 1");

            verify(startupChatModel, never()).call(any(Prompt.class));
            verify(startupChatModel, never()).stream(any(Prompt.class));
        }
    }

    @Nested
    class RunErrorCodeTest {

        @Test
        void testNoEnabledProviderCarriesTheNoAiProviderCode() throws AGUIException {
            assertThat(runErrorFor(ChatModelUnavailableException.noProviderEnabled(1)).getCode())
                .isEqualTo("NO_AI_PROVIDER");
        }

        @Test
        void testNoActivatedCatalogProviderCarriesTheNoAiProviderCode() throws AGUIException {
            assertThat(runErrorFor(new NoChatProviderActivatedException("No chat provider is activated")).getCode())
                .isEqualTo("NO_AI_PROVIDER");
        }

        @Test
        void testUnavailableSelectedModelCarriesTheSelectedModelUnavailableCode() throws AGUIException {
            assertThat(runErrorFor(new SelectedChatModelUnavailableException("openai", "gpt-4o", 1)).getCode())
                .isEqualTo("SELECTED_MODEL_UNAVAILABLE");
        }

        @Test
        void testIncompleteSelectionCarriesTheModelSelectionIncompleteCode() throws AGUIException {
            RunErrorEvent runErrorEvent = runErrorFor(new IncompleteChatModelSelectionException("openai", null));

            assertThat(runErrorEvent.getCode()).isEqualTo("MODEL_SELECTION_INCOMPLETE");
            assertThat(runErrorEvent.getError()).contains("The model selection is incomplete");
        }

        @Test
        void testOtherModelFailuresCarryNoCode() throws AGUIException {
            assertThat(runErrorFor(new ChatModelUnavailableException("The run carries no environment.")).getCode())
                .isNull();
        }

        @Test
        void testUnknownFailuresCarryNoCode() throws AGUIException {
            assertThat(runErrorFor(new IllegalStateException("catalog down")).getCode()).isNull();
        }

        private static RunErrorEvent runErrorFor(RuntimeException exception) throws AGUIException {
            OverrideChatClientResolver overrideChatClientResolver = mock(OverrideChatClientResolver.class);

            when(overrideChatClientResolver.resolve(any())).thenThrow(exception);

            PlainAgent agent = new PlainAgent(newBuilder(mock(ChatModel.class)), overrideChatClientResolver);

            List<RunErrorEvent> runErrorEvents = new ArrayList<>();

            agent.run(newInput(stateWithTenantId(CALLER_TENANT_ID)), newRunErrorCollector(runErrorEvents));

            assertThat(runErrorEvents).hasSize(1);

            return runErrorEvents.getFirst();
        }
    }

    @Nested
    class TenantContextTest {

        @Test
        void testRunBindsTheCarriedTenantOnTheAgentThread() throws AGUIException {
            TenantCapturingAgent agent = newTenantCapturingAgent();

            runOnWorkerThread(agent, newInput(stateWithTenantId(CALLER_TENANT_ID)));

            assertThat(agent.getCapturedTenantId()).isEqualTo(CALLER_TENANT_ID);
        }

        @Test
        void testRunLeavesTheWorkerThreadOnItsPreviousTenant() throws AGUIException {
            TenantCapturingAgent agent = newTenantCapturingAgent();

            runOnWorkerThread(agent, newInput(stateWithTenantId(CALLER_TENANT_ID)));

            assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
        }

        @Test
        void testRunRestoresAPreExistingTenant() throws AGUIException {
            TenantCapturingAgent agent = newTenantCapturingAgent();

            TenantContext.setCurrentTenantId("other");

            agent.runWithTenant(CALLER_TENANT_ID, () -> {});

            assertThat(TenantContext.getCurrentTenantId()).isEqualTo("other");
        }

        @Test
        void testRunToleratesANestedBindingAroundToolCalls() throws AGUIException {
            TenantCapturingAgent agent = newTenantCapturingAgent();

            AtomicReference<String> tenantIdAfterNestedBinding = new AtomicReference<>();

            agent.runWithTenant(CALLER_TENANT_ID, () -> {
                String previousTenantId = TenantContext.getCurrentTenantId();

                TenantContext.setCurrentTenantId("nested");
                TenantContext.setCurrentTenantId(previousTenantId);

                tenantIdAfterNestedBinding.set(TenantContext.getCurrentTenantId());
            });

            assertThat(tenantIdAfterNestedBinding.get()).isEqualTo(CALLER_TENANT_ID);
            assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
        }

        @Test
        void testRunFailsWhenStateCarriesNoTenant() throws AGUIException {
            TenantCapturingAgent agent = newTenantCapturingAgent();

            List<RunErrorEvent> runErrorEvents = new ArrayList<>();

            agent.run(newInput(new State()), newRunErrorCollector(runErrorEvents));

            assertThat(runErrorEvents).hasSize(1);
            assertThat(runErrorEvents.getFirst()
                .getError()).contains("no tenant");
            assertThat(agent.getCapturedTenantId()).isNull();
        }

        @Test
        void testRunIsSealedAgainstOverrides() throws NoSuchMethodException {
            Method runMethod = CopilotSpringAIAgent.class.getDeclaredMethod(
                "run", RunAgentInput.class, AgentSubscriber.class);

            assertThat(Modifier.isFinal(runMethod.getModifiers()))
                .as("CopilotSpringAIAgent.run must stay final; use decorateSubscriber or another hook instead")
                .isTrue();
        }

        @Test
        void testConverterAgentDecoratesTheSubscriber() throws AGUIException {
            CopilotSpringAIAgent agent = newConverterAgent(null);

            AgentSubscriber subscriber = new AgentSubscriber() {};

            assertThat(agent.decorateSubscriber(subscriber)).isNotSameAs(subscriber);
        }
    }

    @Nested
    class EnvironmentContextTest {

        @Test
        void testRunWithEnvironmentBindsEnvironmentFromState() throws AGUIException {
            CopilotSpringAIAgent agent = newConverterAgent(null);

            AtomicReference<Environment> capturedEnvironment = new AtomicReference<>();

            agent.runWithEnvironment(
                newInput(stateWithEnvironmentId(0L)),
                () -> capturedEnvironment.set(EnvironmentContext.getCurrentEnvironment()));

            assertThat(capturedEnvironment.get()).isEqualTo(Environment.DEVELOPMENT);
            assertThat(EnvironmentContext.fetchCurrentEnvironment()).isNull();
        }

        @Test
        void testRunWithEnvironmentRestoresPreviousEnvironment() throws AGUIException {
            CopilotSpringAIAgent agent = newConverterAgent(null);

            EnvironmentContext.set(Environment.STAGING);

            agent.runWithEnvironment(newInput(stateWithEnvironmentId(0L)), () -> {});

            assertThat(EnvironmentContext.fetchCurrentEnvironment()).isEqualTo(Environment.STAGING);
        }

        @Test
        void testRunWithEnvironmentLeavesContextUntouchedWhenStateHasNoEnvironment() throws AGUIException {
            CopilotSpringAIAgent agent = newConverterAgent(null);

            AtomicReference<Environment> capturedEnvironment = new AtomicReference<>();

            agent.runWithEnvironment(
                newInput(new State()), () -> capturedEnvironment.set(EnvironmentContext.fetchCurrentEnvironment()));

            assertThat(capturedEnvironment.get()).isNull();
            assertThat(EnvironmentContext.fetchCurrentEnvironment()).isNull();
        }

        @Test
        void testRunWithEnvironmentLeavesContextUntouchedWhenEnvironmentIsOutOfRange() throws AGUIException {
            CopilotSpringAIAgent agent = newConverterAgent(null);

            AtomicReference<Environment> capturedEnvironment = new AtomicReference<>();

            agent.runWithEnvironment(
                newInput(stateWithEnvironmentId(Environment.values().length)),
                () -> capturedEnvironment.set(EnvironmentContext.fetchCurrentEnvironment()));

            assertThat(capturedEnvironment.get()).isNull();
            assertThat(EnvironmentContext.fetchCurrentEnvironment()).isNull();
        }
    }

    private static AgentSubscriber newRunErrorCollector(List<RunErrorEvent> runErrorEvents) {
        return new AgentSubscriber() {

            @Override
            public void onRunErrorEvent(RunErrorEvent event) {
                runErrorEvents.add(event);
            }
        };
    }

    private static void runOnWorkerThread(TenantCapturingAgent agent, RunAgentInput input) {
        TaskExecutor taskExecutor = workerThreadExecutor();

        List<RunErrorEvent> runErrorEvents = new ArrayList<>();

        taskExecutor.execute(() -> agent.run(input, newRunErrorCollector(runErrorEvents)));

        assertThat(runErrorEvents)
            .extracting(RunErrorEvent::getError)
            .containsExactly(SystemMessageReachedException.MESSAGE);
    }

    private static TaskExecutor workerThreadExecutor() {
        return runnable -> {
            TenantContext.resetCurrentTenantId();

            runnable.run();
        };
    }

    private static State stateWithEnvironmentId(long environmentId) {
        State state = new State();

        state.set(CopilotConstants.STATE_ENVIRONMENT_ID, environmentId);

        return state;
    }

    private static State stateWithTenantId(String tenantId) {
        State state = new State();

        state.set(CopilotConstants.STATE_TENANT_ID, tenantId);

        return state;
    }

    private static RunAgentInput newInput(State state) {
        UserMessage userMessage = new UserMessage();

        userMessage.setId("message-1");
        userMessage.setContent("build me a workflow");

        List<BaseMessage> messages = new ArrayList<>();

        messages.add(userMessage);

        return new RunAgentInput("thread", "run", state, messages, List.of(), List.of(), null);
    }

    private static RunAgentInput newInput(State state, String... userTexts) {
        List<BaseMessage> messages = new ArrayList<>();

        for (int index = 0; index < userTexts.length; index++) {
            UserMessage userMessage = new UserMessage();

            userMessage.setId("message-" + index);
            userMessage.setContent(userTexts[index]);

            messages.add(userMessage);
        }

        return new RunAgentInput("thread", "run", state, messages, List.of(), List.of(), null);
    }

    private static SpringAIAgent.Builder newBuilder(ChatModel chatModel) {
        return SpringAIAgent.builder()
            .agentId("test")
            .chatModel(chatModel)
            .systemMessage("system")
            .state(new State());
    }

    private static CopilotSpringAIAgent newConverterAgent(
        @Nullable OverrideChatClientResolver overrideChatClientResolver) throws AGUIException {

        return ConverterSpringAIAgent.builder()
            .agentId("test")
            .chatModel(mock(ChatModel.class))
            .systemMessage("system")
            .state(new State())
            .overrideChatClientResolver(overrideChatClientResolver)
            .build();
    }

    private static TenantCapturingAgent newTenantCapturingAgent() throws AGUIException {
        return new TenantCapturingAgent(newBuilder(mock(ChatModel.class)));
    }

    private static final class PlainAgent extends CopilotSpringAIAgent {

        private PlainAgent(SpringAIAgent.Builder builder, OverrideChatClientResolver overrideChatClientResolver)
            throws AGUIException {

            super(builder, overrideChatClientResolver);
        }
    }

    private static final class TenantCapturingAgent extends CopilotSpringAIAgent {

        private final AtomicReference<String> capturedTenantId = new AtomicReference<>();

        private TenantCapturingAgent(SpringAIAgent.Builder builder) throws AGUIException {
            super(builder, null);
        }

        private @Nullable String getCapturedTenantId() {
            return capturedTenantId.get();
        }

        @Override
        protected SystemMessage createSystemMessage(State state, List<Context> contexts) {
            capturedTenantId.set(TenantContext.getCurrentTenantId());

            throw new SystemMessageReachedException();
        }
    }

    private static final class SystemMessageReachedException extends RuntimeException {

        private static final String MESSAGE = "system message reached";

        private SystemMessageReachedException() {
            super(MESSAGE);
        }
    }
}
