/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentParameters;
import com.agui.core.event.BaseEvent;
import com.agui.core.event.CustomEvent;
import com.agui.core.event.RunErrorEvent;
import com.agui.core.event.RunStartedEvent;
import com.agui.core.event.ToolCallResultEvent;
import com.agui.core.event.ToolCallStartEvent;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.ProjectWorkflowAgentToolCallback;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolScope;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.copilot.tool.catalog.SimpleIntelligentToolDefinition;
import com.bytechef.ee.ai.hub.agent.AiHubSpringAIAgent;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalPolicy;
import com.bytechef.ee.ai.hub.config.AiHubAgentFixtures;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiHubSubagentProgressStreamTest {

    private static final String REQUEST = "Build a workflow that posts every new Gmail message to Slack";
    private static final String THREAD_ID = "thread-subagent-progress";
    private static final long USER_ID = 1L;
    private static final long WORKSPACE_ID = 7L;

    private final AiHubAgentFixtures fixtures = AiHubAgentFixtures.withBuildWorkflow();
    private final List<Thread> innerToolThreads = new CopyOnWriteArrayList<>();
    private final MainAgentChatModel mainAgentChatModel = new MainAgentChatModel();
    private final SpecialistChatModel specialistChatModel = new SpecialistChatModel();

    @BeforeEach
    void setUp() {
        when(fixtures.policy.decide(anyLong())).thenReturn(new AiHubToolApprovalPolicy.Decision(Set.of(), Set.of()));
    }

    @Test
    void testTheStreamCarriesTheDelegateStartItsInnerStepsInOrderAndThenDone() throws Exception {
        RunResult runResult = runTurn();

        assertThat(runResult.events()).noneMatch(RunErrorEvent.class::isInstance);

        ToolCallStartEvent buildWorkflowStart = runResult.events()
            .stream()
            .filter(ToolCallStartEvent.class::isInstance)
            .map(ToolCallStartEvent.class::cast)
            .filter(event -> "buildWorkflow".equals(event.getToolCallName()))
            .findFirst()
            .orElseThrow();

        List<CustomEvent> progressEvents = progressEvents(runResult.events());

        assertThat(progressEvents).extracting(AiHubSubagentProgressStreamTest::phaseAndText)
            .containsExactly(
                "started: " + REQUEST,
                "step: Reading the build instructions",
                "step: Searching components",
                "step: Adding the trigger \"gmail › newEmail\"",
                "step: Configuring \"slack › sendChannelMessage\"",
                "step: Saving the workflow",
                "done: done");

        assertThat(progressEvents).allSatisfy(event -> assertThat(value(event))
            .containsEntry("toolCallId", buildWorkflowStart.getToolCallId())
            .containsEntry("subagentName", "buildWorkflow"));

        ToolCallResultEvent buildWorkflowResult = runResult.events()
            .stream()
            .filter(ToolCallResultEvent.class::isInstance)
            .map(ToolCallResultEvent.class::cast)
            .filter(event -> buildWorkflowStart.getToolCallId()
                .equals(event.getToolCallId()))
            .findFirst()
            .orElseThrow();

        assertThat(runResult.events()
            .indexOf(progressEvents.getFirst()))
                .isGreaterThan(runResult.events()
                    .indexOf(buildWorkflowStart));
        assertThat(runResult.events()
            .indexOf(progressEvents.getLast()))
                .isLessThan(runResult.events()
                    .indexOf(buildWorkflowResult));

        assertThat(innerToolThreads).isNotEmpty()
            .allSatisfy(innerToolThread -> assertThat(innerToolThread).isNotSameAs(runResult.runThread()));
    }

    @Test
    void testProgressNeverReachesTheModelOrTheToolResult() throws Exception {
        RunResult runResult = runTurn();

        String buildWorkflowResponse = mainAgentChatModel.toolResponses()
            .stream()
            .filter(toolResponse -> "buildWorkflow".equals(toolResponse.name()))
            .map(ToolResponseMessage.ToolResponse::responseData)
            .findFirst()
            .orElseThrow();

        assertThat(buildWorkflowResponse).contains("Workflow saved")
            .doesNotContain("Searching components")
            .doesNotContain(SubagentProgressEmitter.EVENT_NAME);
        assertThat(specialistChatModel.prompts).allSatisfy(prompt -> assertThat(prompt.getContents())
            .doesNotContain("Searching components"));
        assertThat(progressEvents(runResult.events())).isNotEmpty();
    }

    private RunResult runTurn() throws Exception {
        AiHubSpringAIAgent agent = fixtures.buildAgent(mainAgentChatModel, intelligentToolCatalog());

        List<BaseEvent> events = new CopyOnWriteArrayList<>();
        AtomicReference<Thread> runThread = new AtomicReference<>();
        CountDownLatch finalized = new CountDownLatch(1);

        agent.runAgent(parameters(), new AgentSubscriber() {

            @Override
            public void onEvent(BaseEvent event) {
                if (event instanceof RunStartedEvent) {
                    runThread.set(Thread.currentThread());
                }

                events.add(event);
            }

            @Override
            public void onRunFinalized(AgentSubscriberParams params) {
                finalized.countDown();
            }
        });

        assertThat(finalized.await(30, TimeUnit.SECONDS)).isTrue();

        return new RunResult(events, runThread.get());
    }

    private record RunResult(List<BaseEvent> events, Thread runThread) {
    }

    private static List<CustomEvent> progressEvents(List<BaseEvent> events) {
        return events.stream()
            .filter(CustomEvent.class::isInstance)
            .map(CustomEvent.class::cast)
            .filter(event -> SubagentProgressEmitter.EVENT_NAME.equals(event.getName()))
            .toList();
    }

    private static String phaseAndText(CustomEvent event) {
        Map<String, Object> value = value(event);

        return value.get("phase") + ": " + value.get("text");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> value(CustomEvent event) {
        return (Map<String, Object>) event.getValue();
    }

    private static RunAgentParameters parameters() {
        State state = new State();

        state.set("mode", "BUILD");
        state.set(AiHubStateKeys.AUTHENTICATED_USER_ID, USER_ID);
        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, WORKSPACE_ID);
        state.set(AiHubStateKeys.VERIFIED_THREAD_ID, THREAD_ID);

        UserMessage userMessage = new UserMessage();

        userMessage.setId("message-1");
        userMessage.setContent(REQUEST);

        List<BaseMessage> messages = new ArrayList<>();

        messages.add(userMessage);

        return RunAgentParameters.builder()
            .threadId(THREAD_ID)
            .runId("run-1")
            .messages(messages)
            .tools(List.of())
            .context(List.of())
            .state(state)
            .build();
    }

    @SuppressWarnings("unchecked")
    private IntelligentToolCatalog intelligentToolCatalog() {
        IntelligentToolDefinition buildWorkflowDefinition = new SimpleIntelligentToolDefinition(
            "buildWorkflow", CopilotAgentType.BUILD_WORKFLOW, Set.of(IntelligentToolScope.PROJECT),
            variant -> variant == IntelligentToolVariant.BUILD
                ? candidateChatModel -> ChatClient.builder(specialistChatModel)
                    .defaultToolCallbacks(innerToolCallbacks().toArray(new ToolCallback[0]))
                    .build()
                : null,
            chatClientFactory -> new ProjectWorkflowAgentToolCallback(chatClientFactory, null));

        IntelligentToolContributor contributor = () -> List.of(buildWorkflowDefinition);

        ObjectProvider<IntelligentToolContributor> contributorsProvider = mock(ObjectProvider.class);

        when(contributorsProvider.orderedStream()).thenAnswer(invocation -> Stream.of(contributor));

        return new IntelligentToolCatalog(contributorsProvider);
    }

    private List<ToolCallback> innerToolCallbacks() {
        return List.of(
            innerToolCallback("getWorkflowBuildInstructions", "{\"instructions\":\"Use a trigger first.\"}"),
            innerToolCallback("searchComponents", "[{\"name\":\"gmail\"},{\"name\":\"slack\"}]"),
            innerToolCallback("getTask", "{\"name\":\"found\"}"),
            innerToolCallback("updateWorkflow", "{\"saved\":true}"));
    }

    private ToolCallback innerToolCallback(String name, String result) {
        return FunctionToolCallback.builder(name, (Map<String, Object> input) -> {
            innerToolThreads.add(Thread.currentThread());

            return result;
        })
            .description(name)
            .inputType(Map.class)
            .build();
    }

    private static final class MainAgentChatModel implements ChatModel {

        private final AtomicInteger streamCalls = new AtomicInteger();
        private final List<Prompt> prompts = new CopyOnWriteArrayList<>();

        @Override
        public ChatResponse call(Prompt prompt) {
            throw new UnsupportedOperationException("The AI Hub agent streams");
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            prompts.add(prompt);

            if (streamCalls.incrementAndGet() == 1) {
                return Flux
                    .just(toolCalls(toolCall("call-build", "buildWorkflow", "{\"request\":\"" + REQUEST + "\"}")));
            }

            return Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("Built it.")))));
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder()
                .build();
        }

        List<ToolResponseMessage.ToolResponse> toolResponses() {
            return prompts.stream()
                .flatMap(prompt -> prompt.getInstructions()
                    .stream())
                .filter(ToolResponseMessage.class::isInstance)
                .map(ToolResponseMessage.class::cast)
                .flatMap(toolResponseMessage -> toolResponseMessage.getResponses()
                    .stream())
                .toList();
        }
    }

    private static final class SpecialistChatModel implements ChatModel {

        private static final List<ChatResponse> SCRIPT = List.of(
            toolCalls(toolCall("inner-1", "getWorkflowBuildInstructions", "{}")),
            toolCalls(
                toolCall("inner-2", "searchComponents", "{\"query\":\"gmail\"}"),
                toolCall("inner-3", "searchComponents", "{\"query\":\"slack\"}")),
            toolCalls(
                toolCall("inner-4", "getTask",
                    "{\"type\":\"trigger\",\"name\":\"newEmail\",\"componentName\":\"gmail\"}")),
            toolCalls(
                toolCall(
                    "inner-5", "getTask",
                    "{\"type\":\"action\",\"name\":\"sendChannelMessage\",\"componentName\":\"slack\"}"),
                toolCall(
                    "inner-6", "getTask",
                    "{\"type\":\"action\",\"name\":\"sendChannelMessage\",\"componentName\":\"slack\"}")),
            toolCalls(toolCall("inner-7", "updateWorkflow", "{\"workflowId\":\"workflow-1\",\"definition\":\"{}\"}")),
            new ChatResponse(List.of(new Generation(new AssistantMessage("Workflow saved.")))));

        private final AtomicInteger calls = new AtomicInteger();
        private final List<Prompt> prompts = new CopyOnWriteArrayList<>();

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);

            return SCRIPT.get(Math.min(calls.getAndIncrement(), SCRIPT.size() - 1));
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder()
                .build();
        }
    }

    private static ChatResponse toolCalls(AssistantMessage.ToolCall... toolCalls) {
        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(toolCalls))
            .build();

        return new ChatResponse(List.of(new Generation(assistantMessage)));
    }

    private static AssistantMessage.ToolCall toolCall(String id, String name, @Nullable String arguments) {
        return new AssistantMessage.ToolCall(id, "function", name, arguments);
    }
}
