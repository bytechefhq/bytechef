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

package com.bytechef.component.ai.agent.action;

import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.CONVERSATION_ID;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.FAILING_TOOL_NAME;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.LOOK_UP_TOOL_NAME;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.LOOK_UP_TOOL_RESULT;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.SUSPENDING_TOOL_NAME;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.USER_PROMPT;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.createActionContext;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.createChatModel;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.createClusterElementDefinitionService;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.createConnectionParameters;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.createExtensions;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.createInputParameters;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.createToolCallingManager;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.findLastToolResponseMessage;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.mockJson;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.textResponse;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.toPersistedContinueParameters;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.toolCall;
import static com.bytechef.component.ai.agent.action.AiAgentResumeTestSupport.toolCallResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.agent.tool.AgentToolSuspension;
import com.bytechef.component.ai.agent.tool.SuspendableToolCallingManager;
import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.ai.tool.ToolSuspensionException;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsResumePerformFunction;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiAgentChatActionResumeTest {

    private static final String FINAL_ANSWER = "final answer";
    private static final String FIRST_TOOL_CALL_ID = "call_1";
    private static final String SECOND_TOOL_CALL_ID = "call_2";
    private static final String THIRD_TOOL_CALL_ID = "call_3";

    @Test
    void testPerformSuspendsAndResumeContinuesFromPersistedState() throws Exception {
        List<Prompt> prompts = new ArrayList<>();

        AiAgentChatAction.ChatActionDefinitionWrapper chatActionDefinition = createChatActionDefinition(
            prompts, callNumber -> callNumber == 1 ? suspendingToolCallResponse(FIRST_TOOL_CALL_ID) : finalAnswer());

        AtomicReference<ActionContext.Suspend> firstTurnSuspend = new AtomicReference<>();

        Object performResult = getPerformFunction(chatActionDefinition).apply(
            createInputParameters("TEXT"), createConnectionParameters(), createExtensions(false),
            createActionContext(firstTurnSuspend));

        assertThat(performResult).isNull();

        ActionContext.Suspend suspend = firstTurnSuspend.get();

        assertThat(suspend).isNotNull();

        AgentToolSuspension agentToolSuspension = getAgentToolSuspension(suspend);

        assertThat(agentToolSuspension.pendingToolCallId()).isEqualTo(FIRST_TOOL_CALL_ID);

        Object resumeResult = getResumePerformFunction(chatActionDefinition).apply(
            createInputParameters("TEXT"), createConnectionParameters(), createExtensions(false),
            toPersistedContinueParameters(suspend), MockParametersFactory.create(Map.of("approved", true)),
            createActionContext(new AtomicReference<>()));

        assertThat(prompts).hasSize(2);

        ToolResponseMessage resumedToolResponseMessage = findLastToolResponseMessage(prompts.get(1)
            .getInstructions());

        assertThat(resumedToolResponseMessage).isNotNull();
        assertThat(resumedToolResponseMessage.getResponses()).containsExactly(
            new ToolResponseMessage.ToolResponse(FIRST_TOOL_CALL_ID, SUSPENDING_TOOL_NAME, "{\"approved\":true}"));

        assertThat(resumeResult).isEqualTo(FINAL_ANSWER);
    }

    @Test
    void testResumeOutputHasTheSameShapeAsPerformOutput() throws Exception {
        List<Prompt> performPrompts = new ArrayList<>();

        AiAgentChatAction.ChatActionDefinitionWrapper performChatActionDefinition = createChatActionDefinition(
            performPrompts, callNumber -> callNumber == 1 ? lookUpToolCallResponse() : finalAnswer());

        ActionContextAware performActionContext = createActionContext(new AtomicReference<>());

        when(performActionContext.isEditorEnvironment()).thenReturn(true);
        when(performActionContext.getJobId()).thenReturn(null);

        Object performResult = getPerformFunction(performChatActionDefinition).apply(
            createInputParameters("TEXT"), createConnectionParameters(), createExtensions(false),
            performActionContext);

        List<Prompt> prompts = new ArrayList<>();

        AiAgentChatAction.ChatActionDefinitionWrapper chatActionDefinition = createChatActionDefinition(
            prompts, callNumber -> switch (callNumber) {
                case 1 -> suspendingToolCallResponse(FIRST_TOOL_CALL_ID);
                case 2 -> lookUpToolCallResponse();
                default -> finalAnswer();
            });

        AtomicReference<ActionContext.Suspend> suspendReference = new AtomicReference<>();

        getPerformFunction(chatActionDefinition).apply(
            createInputParameters("TEXT"), createConnectionParameters(), createExtensions(false),
            createActionContext(suspendReference));

        ActionContextAware resumeActionContext = createActionContext(new AtomicReference<>());

        when(resumeActionContext.isEditorEnvironment()).thenReturn(true);
        when(resumeActionContext.getJobId()).thenReturn(null);

        Object resumeResult = getResumePerformFunction(chatActionDefinition).apply(
            createInputParameters("TEXT"), createConnectionParameters(), createExtensions(false),
            toPersistedContinueParameters(suspendReference.get()),
            MockParametersFactory.create(Map.of("approved", true)), resumeActionContext);

        assertThat(performResult).isInstanceOf(Map.class);
        assertThat(resumeResult).isInstanceOf(Map.class);

        Map<String, Object> performOutput = toMap(performResult);
        Map<String, Object> resumeOutput = toMap(resumeResult);

        assertThat(resumeOutput.keySet()).containsExactlyElementsOf(performOutput.keySet());
        assertThat(resumeOutput).containsEntry("response", FINAL_ANSWER);
        assertThat(resumeOutput.get("toolExecutions")).isEqualTo(performOutput.get("toolExecutions"));

        List<?> toolExecutions = (List<?>) resumeOutput.get("toolExecutions");

        assertThat(toolExecutions).hasSize(1);
        assertThat(toMap(toolExecutions.getFirst())).containsEntry("toolName", LOOK_UP_TOOL_NAME)
            .containsEntry("output", LOOK_UP_TOOL_RESULT);
    }

    @Test
    void testResumeWithEmptyDataSendsTheNoHumanResponseToolResult() throws Exception {
        List<Prompt> prompts = new ArrayList<>();

        AiAgentChatAction.ChatActionDefinitionWrapper chatActionDefinition = createChatActionDefinition(
            prompts, callNumber -> callNumber == 1 ? suspendingToolCallResponse(FIRST_TOOL_CALL_ID) : finalAnswer());

        AtomicReference<ActionContext.Suspend> suspendReference = new AtomicReference<>();

        getPerformFunction(chatActionDefinition).apply(
            createInputParameters("TEXT"), createConnectionParameters(), createExtensions(false),
            createActionContext(suspendReference));

        Object resumeResult = getResumePerformFunction(chatActionDefinition).apply(
            createInputParameters("TEXT"), createConnectionParameters(), createExtensions(false),
            toPersistedContinueParameters(suspendReference.get()), MockParametersFactory.create(Map.of()),
            createActionContext(new AtomicReference<>()));

        assertThat(resumeResult).isEqualTo(FINAL_ANSWER);

        ToolResponseMessage resumedToolResponseMessage = findLastToolResponseMessage(prompts.get(1)
            .getInstructions());

        assertThat(resumedToolResponseMessage).isNotNull();
        assertThat(resumedToolResponseMessage.getResponses()).containsExactly(
            new ToolResponseMessage.ToolResponse(
                FIRST_TOOL_CALL_ID, SUSPENDING_TOOL_NAME, AbstractAiAgentChatAction.NO_HUMAN_RESPONSE_TOOL_RESULT));
        assertThat(AbstractAiAgentChatAction.NO_HUMAN_RESPONSE_TOOL_RESULT).contains("NO_RESPONSE");
    }

    @Test
    void testResumeWithoutAgentToolSuspensionThrowsAndNeverCallsTheModel() throws Exception {
        List<Prompt> prompts = new ArrayList<>();

        AiAgentChatAction.ChatActionDefinitionWrapper chatActionDefinition = createChatActionDefinition(
            prompts, callNumber -> finalAnswer());

        MultipleConnectionsResumePerformFunction resumePerformFunction = getResumePerformFunction(
            chatActionDefinition);

        assertThatThrownBy(() -> resumePerformFunction.apply(
            createInputParameters("TEXT"), createConnectionParameters(), createExtensions(false),
            ParametersFactory.create(Map.of("formUrl", "https://example.com")),
            MockParametersFactory.create(Map.of("approved", true)), createActionContext(new AtomicReference<>())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(AgentToolSuspension.CONTINUE_PARAMETER_KEY);

        assertThat(prompts).isEmpty();
    }

    @Test
    void testResumeWithJsonResponseFormatValidatesTheStructuredOutput() throws Exception {
        List<Prompt> prompts = new ArrayList<>();

        AiAgentChatAction.ChatActionDefinitionWrapper chatActionDefinition = createChatActionDefinition(
            prompts, callNumber -> {
                List<Prompt> resumedPrompts = getResumedPrompts(prompts);

                if (resumedPrompts.isEmpty()) {
                    return suspendingToolCallResponse(FIRST_TOOL_CALL_ID);
                }

                return resumedPrompts.size() == 1
                    ? textResponse("this is not JSON") : textResponse("{\"answer\":\"approved\"}");
            });

        AtomicReference<ActionContext.Suspend> suspendReference = new AtomicReference<>();

        ActionContextAware performActionContext = createActionContext(suspendReference);

        mockJson(performActionContext);

        Object performResult = getPerformFunction(chatActionDefinition).apply(
            createInputParameters("JSON"), createConnectionParameters(), createExtensions(false),
            performActionContext);

        assertThat(performResult).isNull();
        assertThat(suspendReference.get()).isNotNull();

        ActionContextAware resumeActionContext = createActionContext(new AtomicReference<>());

        mockJson(resumeActionContext);

        Object resumeResult = getResumePerformFunction(chatActionDefinition).apply(
            createInputParameters("JSON"), createConnectionParameters(), createExtensions(false),
            toPersistedContinueParameters(suspendReference.get()),
            MockParametersFactory.create(Map.of("approved", true)), resumeActionContext);

        List<Prompt> resumedPrompts = getResumedPrompts(prompts);

        assertThat(resumedPrompts)
            .as("the invalid answer is sent back to the model by the structured output validation")
            .hasSize(2);
        assertThat(resumeResult).isEqualTo(Map.of("answer", "approved"));

        List<Message> resumedInstructions = resumedPrompts.getFirst()
            .getInstructions();

        assertThat(resumedInstructions.getLast()).isInstanceOf(ToolResponseMessage.class);
        assertThat(countUserMessages(resumedInstructions)).isEqualTo(
            countUserMessages(
                prompts.getFirst()
                    .getInstructions()));
    }

    @Test
    void testResumedTurnThatSuspendsAgainRecordsTheNewPendingToolCall() throws Exception {
        List<Prompt> prompts = new ArrayList<>();

        AiAgentChatAction.ChatActionDefinitionWrapper chatActionDefinition = createChatActionDefinition(
            prompts,
            callNumber -> suspendingToolCallResponse(callNumber == 1 ? FIRST_TOOL_CALL_ID : SECOND_TOOL_CALL_ID));

        AtomicReference<ActionContext.Suspend> firstTurnSuspend = new AtomicReference<>();

        getPerformFunction(chatActionDefinition).apply(
            createInputParameters("TEXT"), createConnectionParameters(), createExtensions(false),
            createActionContext(firstTurnSuspend));

        AtomicReference<ActionContext.Suspend> secondTurnSuspend = new AtomicReference<>();

        Object resumeResult = getResumePerformFunction(chatActionDefinition).apply(
            createInputParameters("TEXT"), createConnectionParameters(), createExtensions(false),
            toPersistedContinueParameters(firstTurnSuspend.get()),
            MockParametersFactory.create(Map.of("approved", true)), createActionContext(secondTurnSuspend));

        assertThat(resumeResult).isNull();

        ActionContext.Suspend suspend = secondTurnSuspend.get();

        assertThat(suspend).isNotNull();

        AgentToolSuspension agentToolSuspension = getAgentToolSuspension(suspend);

        assertThat(agentToolSuspension.pendingToolCallId()).isEqualTo(SECOND_TOOL_CALL_ID);

        List<Message> resumedConversation = agentToolSuspension.resumeConversation("{\"approved\":false}");
        ToolResponseMessage firstToolResponseMessage = (ToolResponseMessage) resumedConversation.stream()
            .filter(message -> message instanceof ToolResponseMessage)
            .findFirst()
            .orElseThrow();

        assertThat(firstToolResponseMessage.getResponses()).containsExactly(
            new ToolResponseMessage.ToolResponse(FIRST_TOOL_CALL_ID, SUSPENDING_TOOL_NAME, "{\"approved\":true}"));
    }

    @Test
    void testToolSuspensionExceptionFailsTheResumedAgent() throws Exception {
        List<Prompt> prompts = new ArrayList<>();

        AiAgentChatAction.ChatActionDefinitionWrapper chatActionDefinition = createChatActionDefinition(
            prompts, callNumber -> switch (callNumber) {
                case 1 -> suspendingToolCallResponse(FIRST_TOOL_CALL_ID);
                case 2 -> toolCallResponse(FAILING_TOOL_NAME, SECOND_TOOL_CALL_ID);
                default -> finalAnswer();
            });

        AtomicReference<ActionContext.Suspend> suspendReference = new AtomicReference<>();

        getPerformFunction(chatActionDefinition).apply(
            createInputParameters("TEXT"), createConnectionParameters(), createExtensions(false),
            createActionContext(suspendReference));

        MultipleConnectionsResumePerformFunction resumePerformFunction = getResumePerformFunction(
            chatActionDefinition);

        assertThatThrownBy(() -> resumePerformFunction.apply(
            createInputParameters("TEXT"), createConnectionParameters(), createExtensions(false),
            toPersistedContinueParameters(suspendReference.get()),
            MockParametersFactory.create(Map.of("approved", true)), createActionContext(new AtomicReference<>())))
                .isInstanceOf(ToolSuspensionException.class)
                .hasMessage("The approval request could not be sent");

        assertThat(prompts).as("the failure must not be sent to the model as a tool result")
            .hasSize(2);
    }

    @Test
    void testToolSuspensionExceptionFailsTheAgent() throws Exception {
        List<Prompt> prompts = new ArrayList<>();

        AiAgentChatAction.ChatActionDefinitionWrapper chatActionDefinition = createChatActionDefinition(
            prompts,
            callNumber -> callNumber == 1 ? toolCallResponse(FAILING_TOOL_NAME, FIRST_TOOL_CALL_ID) : finalAnswer());

        MultipleConnectionsPerformFunction performFunction = getPerformFunction(chatActionDefinition);

        assertThatThrownBy(() -> performFunction.apply(
            createInputParameters("TEXT"), createConnectionParameters(), createExtensions(false),
            createActionContext(new AtomicReference<>())))
                .isInstanceOf(ToolSuspensionException.class);

        assertThat(prompts).hasSize(1);
    }

    @Test
    void testResumeStoresOnlyTheFinalAssistantMessageInChatMemory() throws Exception {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .build();

        chatMemory.add(
            CONVERSATION_ID, List.of(new UserMessage("earlier question"), new AssistantMessage("earlier answer")));

        ChatMemoryFunction.Result chatMemoryResult = ChatMemoryFunction.Result.of(
            MessageChatMemoryAdvisor.builder(chatMemory)
                .build(),
            chatMemory);

        List<Prompt> prompts = new ArrayList<>();

        AiAgentChatAction.ChatActionDefinitionWrapper chatActionDefinition = createChatActionDefinition(
            prompts, callNumber -> callNumber == 1 ? suspendingToolCallResponse(FIRST_TOOL_CALL_ID) : finalAnswer(),
            chatMemoryResult);

        AtomicReference<ActionContext.Suspend> suspendReference = new AtomicReference<>();

        getPerformFunction(chatActionDefinition).apply(
            createInputParameters("TEXT"), createConnectionParameters(), createExtensions(true),
            createActionContext(suspendReference));

        List<Message> memoryAfterSuspend = List.copyOf(chatMemory.get(CONVERSATION_ID));

        assertThat(getTexts(memoryAfterSuspend)).containsExactly("earlier question", "earlier answer", USER_PROMPT);

        Object resumeResult = getResumePerformFunction(chatActionDefinition).apply(
            createInputParameters("TEXT"), createConnectionParameters(), createExtensions(true),
            toPersistedContinueParameters(suspendReference.get()),
            MockParametersFactory.create(Map.of("approved", true)), createActionContext(new AtomicReference<>()));

        assertThat(resumeResult).isEqualTo(FINAL_ANSWER);

        List<Message> memoryAfterResume = chatMemory.get(CONVERSATION_ID);

        assertThat(memoryAfterResume).hasSize(memoryAfterSuspend.size() + 1)
            .noneMatch(message -> message instanceof ToolResponseMessage);
        assertThat(memoryAfterResume.getLast()).isInstanceOf(AssistantMessage.class);
        assertThat(memoryAfterResume.getLast()
            .getText()).isEqualTo(FINAL_ANSWER);

        List<String> resumedPromptTexts = getTexts(
            prompts.get(1)
                .getInstructions());

        assertThat(resumedPromptTexts).as("the memory loaded when the turn started is not prepended again")
            .containsOnlyOnce("earlier question", "earlier answer", USER_PROMPT);
    }

    @Test
    void testJsonResponseFormatSuspendsWithoutParsingTheToolResult() throws Exception {
        List<Prompt> prompts = new ArrayList<>();

        AiAgentChatAction.ChatActionDefinitionWrapper chatActionDefinition = createChatActionDefinition(
            prompts, callNumber -> suspendingToolCallResponse(FIRST_TOOL_CALL_ID));

        AtomicReference<ActionContext.Suspend> suspendReference = new AtomicReference<>();

        ActionContextAware actionContext = createActionContext(suspendReference);

        mockJson(actionContext);

        Object performResult = getPerformFunction(chatActionDefinition).apply(
            createInputParameters("JSON"), createConnectionParameters(), createExtensions(false), actionContext);

        assertThat(performResult).isNull();
        assertThat(suspendReference.get()).isNotNull();
        assertThat(prompts).hasSize(1);
    }

    @Test
    void testRoundWithASiblingToolCallResumesWithBothToolResponses() throws Exception {
        List<Prompt> prompts = new ArrayList<>();

        AiAgentChatAction.ChatActionDefinitionWrapper chatActionDefinition = createChatActionDefinition(
            prompts, callNumber -> callNumber == 1
                ? toolCallResponse(
                    toolCall(LOOK_UP_TOOL_NAME, FIRST_TOOL_CALL_ID),
                    toolCall(SUSPENDING_TOOL_NAME, SECOND_TOOL_CALL_ID),
                    toolCall(LOOK_UP_TOOL_NAME, THIRD_TOOL_CALL_ID))
                : finalAnswer());

        AtomicReference<ActionContext.Suspend> firstTurnSuspend = new AtomicReference<>();

        Object performResult = getPerformFunction(chatActionDefinition).apply(
            createInputParameters("TEXT"), createConnectionParameters(), createExtensions(false),
            createActionContext(firstTurnSuspend));

        assertThat(performResult).isNull();

        ActionContext.Suspend suspend = firstTurnSuspend.get();

        assertThat(suspend).isNotNull();
        assertThat(getAgentToolSuspension(suspend).pendingToolCallId()).isEqualTo(SECOND_TOOL_CALL_ID);

        Object resumeResult = getResumePerformFunction(chatActionDefinition).apply(
            createInputParameters("TEXT"), createConnectionParameters(), createExtensions(false),
            toPersistedContinueParameters(suspend), MockParametersFactory.create(Map.of("approved", true)),
            createActionContext(new AtomicReference<>()));

        assertThat(resumeResult).isEqualTo(FINAL_ANSWER);
        assertThat(prompts).hasSize(2);

        ToolResponseMessage resumedToolResponseMessage = findLastToolResponseMessage(prompts.get(1)
            .getInstructions());

        assertThat(resumedToolResponseMessage).isNotNull();
        assertThat(resumedToolResponseMessage.getResponses()).containsExactly(
            new ToolResponseMessage.ToolResponse(FIRST_TOOL_CALL_ID, LOOK_UP_TOOL_NAME, LOOK_UP_TOOL_RESULT),
            new ToolResponseMessage.ToolResponse(SECOND_TOOL_CALL_ID, SUSPENDING_TOOL_NAME, "{\"approved\":true}"),
            new ToolResponseMessage.ToolResponse(
                THIRD_TOOL_CALL_ID, LOOK_UP_TOOL_NAME, SuspendableToolCallingManager.NOT_EXECUTED_TOOL_RESULT));
    }

    @Test
    void testPerformWithAContextThatIsNotActionContextAware() throws Exception {
        List<Prompt> prompts = new ArrayList<>();

        AiAgentChatAction.ChatActionDefinitionWrapper chatActionDefinition = createChatActionDefinition(
            prompts, callNumber -> finalAnswer());

        Object performResult = getPerformFunction(chatActionDefinition).apply(
            createInputParameters("TEXT"), createConnectionParameters(), createExtensions(false),
            mock(ActionContext.class));

        assertThat(performResult).isEqualTo(FINAL_ANSWER);
    }

    private static AiAgentChatAction.ChatActionDefinitionWrapper createChatActionDefinition(
        List<Prompt> prompts, Function<Integer, ChatResponse> responseFunction) throws Exception {

        return createChatActionDefinition(prompts, responseFunction, null);
    }

    private static AiAgentChatAction.ChatActionDefinitionWrapper createChatActionDefinition(
        List<Prompt> prompts, Function<Integer, ChatResponse> responseFunction,
        ChatMemoryFunction.@Nullable Result chatMemoryResult) throws Exception {

        return AiAgentChatAction.of(
            mock(AiAgentToolFacade.class),
            createClusterElementDefinitionService(createChatModel(prompts, responseFunction), chatMemoryResult),
            createToolCallingManager());
    }

    private static MultipleConnectionsPerformFunction getPerformFunction(
        AiAgentChatAction.ChatActionDefinitionWrapper chatActionDefinition) {

        return (MultipleConnectionsPerformFunction) chatActionDefinition.getPerform()
            .orElseThrow();
    }

    private static MultipleConnectionsResumePerformFunction getResumePerformFunction(
        AiAgentChatAction.ChatActionDefinitionWrapper chatActionDefinition) {

        return (MultipleConnectionsResumePerformFunction) chatActionDefinition.getResumePerform()
            .orElseThrow();
    }

    private static AgentToolSuspension getAgentToolSuspension(ActionContext.Suspend suspend) {
        Map<String, ?> continueParameters = suspend.continueParameters();

        assertThat(continueParameters).containsKey(AgentToolSuspension.CONTINUE_PARAMETER_KEY);

        return (AgentToolSuspension) continueParameters.get(AgentToolSuspension.CONTINUE_PARAMETER_KEY);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static List<Prompt> getResumedPrompts(List<Prompt> prompts) {
        return prompts.stream()
            .filter(prompt -> findLastToolResponseMessage(prompt.getInstructions()) != null)
            .toList();
    }

    private static long countUserMessages(List<Message> messages) {
        return messages.stream()
            .filter(message -> message instanceof UserMessage)
            .count();
    }

    private static List<String> getTexts(List<Message> messages) {
        return messages.stream()
            .map(Message::getText)
            .filter(text -> text != null && !text.isEmpty())
            .toList();
    }

    private static ChatResponse finalAnswer() {
        return textResponse(FINAL_ANSWER);
    }

    private static ChatResponse lookUpToolCallResponse() {
        return toolCallResponse(LOOK_UP_TOOL_NAME, "call_look_up");
    }

    private static ChatResponse suspendingToolCallResponse(String toolCallId) {
        return toolCallResponse(SUSPENDING_TOOL_NAME, toolCallId);
    }
}
