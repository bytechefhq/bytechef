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

package com.bytechef.component.ai.agent.utils.cluster;

import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;
import static com.bytechef.platform.ai.constant.AiAgentSseEventType.ASK_USER_QUESTION;
import static com.bytechef.platform.ai.constant.AiAgentSseEventType.EVENT_TYPE;
import static com.bytechef.platform.ai.constant.AiAgentToolContextKey.ACTION_CONTEXT;
import static com.bytechef.platform.ai.constant.AiAgentToolContextKey.SSE_BUFFERED_EVENTS;
import static com.bytechef.platform.ai.constant.AiAgentToolContextKey.SSE_EMITTER_REFERENCE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler.SseEmitter;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Provides a tool that enables the AI agent to ask the user clarifying questions during execution. When invoked, the
 * tool sends an {@code ask_user_question} SSE event to the chat client.
 *
 * <p>
 * When the action context has a resume URL, the event carries it and the tool requests a suspend of the agent's
 * workflow task, so the client can POST the user's answers to the resume URL.
 *
 * <p>
 * Without a resume URL (for example, an editor test run that has no job) the questions are still shown, but nothing can
 * carry the answers back to this tool call, so the model is told to end its turn and read the answers from the user's
 * next message. When the questions cannot be delivered, or no SSE transport is present in the {@link ToolContext} (for
 * example, the non-streaming chat action), the tool neither suspends nor waits, and tells the model to proceed without
 * asking.
 *
 * @author Ivica Cardic
 */
public class AiAgentUtilsAskUserQuestionTool {

    private static final Logger log = LoggerFactory.getLogger(AiAgentUtilsAskUserQuestionTool.class);

    public static final ClusterElementDefinition<ToolCallbackProviderFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ToolCallbackProviderFunction>clusterElement("askUserQuestionTool")
            .title("Ask User Question Tool")
            .description(
                "Ask the user clarifying questions to gather preferences, clarify instructions, or get decisions.")
            .type(TOOLS)
            .object(() -> AiAgentUtilsAskUserQuestionTool::apply);

    static final String NO_EVENT_TRANSPORT_RESULT =
        "The user cannot be asked questions in this context because the conversation is not streamed. Do not call " +
            "this tool again; proceed using your best judgment and state the assumptions you made in your response.";

    static final String NO_RESUME_URL_RESULT =
        "The questions were shown to the user, but their answers cannot be returned to this tool call. Do not " +
            "assume any answers; end your response now, and the user will answer in their next message.";

    static final String QUESTIONS_NOT_DELIVERED_RESULT =
        "The questions could not be delivered to the user. Do not call this tool again; proceed using your best " +
            "judgment and state the assumptions you made in your response.";

    static final Duration SUSPEND_TIMEOUT = Duration.ofDays(30);

    private static final String QUESTIONS = "questions";
    private static final ThreadLocal<ToolInvocation> TOOL_INVOCATION_HOLDER = new ThreadLocal<>();

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        AskUserQuestionTool askUserQuestionTool = AskUserQuestionTool.builder()
            .questionHandler(AiAgentUtilsAskUserQuestionTool::askQuestions)
            .answersValidation(false)
            .build();

        List<ToolCallback> toolCallbacks = Arrays.stream(ToolCallbacks.from(askUserQuestionTool))
            .<ToolCallback>map(ToolContextAwareToolCallback::new)
            .toList();

        return ToolCallbackProvider.from(toolCallbacks);
    }

    private static Map<String, String> askQuestions(List<AskUserQuestionTool.Question> questions) {
        ToolInvocation toolInvocation = TOOL_INVOCATION_HOLDER.get();

        if (toolInvocation == null) {
            throw new IllegalStateException("ToolContext not available");
        }

        Map<String, Object> toolContextMap = toolInvocation.toolContext.getContext();

        if (!(toolContextMap.get(ACTION_CONTEXT) instanceof ActionContext actionContext) ||
            !(actionContext instanceof ActionContextAware actionContextAware)) {

            throw new IllegalStateException("ActionContext not available in ToolContext");
        }

        String resumeUrl = actionContextAware.getResumeUrl();

        if (!sendQuestionEvent(toolInvocation.toolContext, questions, resumeUrl)) {
            toolInvocation.result = QUESTIONS_NOT_DELIVERED_RESULT;

            return Map.of();
        }

        if (resumeUrl == null) {
            toolInvocation.result = NO_RESUME_URL_RESULT;

            return Map.of();
        }

        Map<String, Object> continueParameters = new HashMap<>();

        continueParameters.put(QUESTIONS, questions);

        Instant expiresAt = Instant.now()
            .plus(SUSPEND_TIMEOUT);

        actionContext.suspend(new Suspend(continueParameters, expiresAt));

        return Map.of();
    }

    private static boolean hasEventTransport(ToolContext toolContext) {
        Map<String, Object> toolContextMap = toolContext.getContext();

        return toolContextMap.get(SSE_EMITTER_REFERENCE) instanceof AtomicReference<?> ||
            toolContextMap.get(SSE_BUFFERED_EVENTS) instanceof Queue<?>;
    }

    @SuppressWarnings("unchecked")
    private static boolean sendQuestionEvent(
        ToolContext toolContext, List<AskUserQuestionTool.Question> questions, @Nullable String resumeUrl) {

        Map<String, Object> eventData = new LinkedHashMap<>();

        eventData.put(EVENT_TYPE, ASK_USER_QUESTION);

        if (resumeUrl != null) {
            eventData.put("resumeUrl", resumeUrl);
        }

        List<Map<String, Object>> questionList = questions.stream()
            .map(question -> {
                Map<String, Object> questionMap = new LinkedHashMap<>();

                questionMap.put("header", question.header());
                questionMap.put("multiSelect", question.multiSelect());

                List<Map<String, String>> optionList = question.options()
                    .stream()
                    .map(option -> Map.of("description", option.description(), "label", option.label()))
                    .toList();

                questionMap.put("options", optionList);
                questionMap.put("question", question.question());

                return questionMap;
            })
            .toList();

        eventData.put(QUESTIONS, questionList);

        Map<String, Object> toolContextMap = toolContext.getContext();

        if (toolContextMap.get(SSE_EMITTER_REFERENCE) instanceof AtomicReference<?> emitterReference &&
            emitterReference.get() instanceof SseEmitter sseEmitter) {

            try {
                sseEmitter.send(eventData);

                return true;
            } catch (Exception exception) {
                log.error("Failed to send the ask_user_question event", exception);

                return false;
            }
        }

        if (toolContextMap.get(SSE_BUFFERED_EVENTS) instanceof Queue<?> bufferedEvents) {
            ((Queue<Map<String, Object>>) bufferedEvents).add(eventData);

            return true;
        }

        log.error("Failed to send the ask_user_question event: neither an SSE emitter nor a buffered events queue is " +
            "available");

        return false;
    }

    private static final class ToolInvocation {

        private final ToolContext toolContext;
        private @Nullable String result;

        private ToolInvocation(ToolContext toolContext) {
            this.toolContext = toolContext;
        }
    }

    static class ToolContextAwareToolCallback implements ToolCallback {

        private final ToolCallback delegate;

        ToolContextAwareToolCallback(ToolCallback delegate) {
            this.delegate = delegate;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return delegate.getToolDefinition();
        }

        @Override
        public String call(String toolInput) {
            return NO_EVENT_TRANSPORT_RESULT;
        }

        @Override
        public String call(String toolInput, @Nullable ToolContext toolContext) {
            if (toolContext == null || !hasEventTransport(toolContext)) {
                return NO_EVENT_TRANSPORT_RESULT;
            }

            ToolInvocation toolInvocation = new ToolInvocation(toolContext);

            TOOL_INVOCATION_HOLDER.set(toolInvocation);

            try {
                String result = delegate.call(toolInput, toolContext);

                if (toolInvocation.result != null) {
                    return toolInvocation.result;
                }

                return result;
            } catch (RuntimeException exception) {
                log.warn("Failed to ask the user questions", exception);

                throw exception;
            } finally {
                TOOL_INVOCATION_HOLDER.remove();
            }
        }
    }
}
