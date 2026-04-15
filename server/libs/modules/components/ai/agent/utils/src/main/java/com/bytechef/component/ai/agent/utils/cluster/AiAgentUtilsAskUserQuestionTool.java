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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * tool sends an {@code ask_user_question} SSE event to the chat client and suspends the workflow. The client then POSTs
 * the user's answers to the resume webhook URL, which resumes the AI agent action.
 *
 * @author Ivica Cardic
 */
public class AiAgentUtilsAskUserQuestionTool {

    private static final Logger logger = LoggerFactory.getLogger(AiAgentUtilsAskUserQuestionTool.class);

    public static final ClusterElementDefinition<ToolCallbackProviderFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ToolCallbackProviderFunction>clusterElement("askUserQuestionTool")
            .title("Ask User Question Tool")
            .description(
                "Ask the user clarifying questions to gather preferences, clarify instructions, or get decisions.")
            .type(TOOLS)
            .object(() -> AiAgentUtilsAskUserQuestionTool::apply);

    private static final String QUESTIONS = "questions";
    private static final ThreadLocal<ToolContext> TOOL_CONTEXT_HOLDER = new ThreadLocal<>();

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        AskUserQuestionTool askUserQuestionTool = AskUserQuestionTool.builder()
            .questionHandler(questions -> {
                ToolContext toolContext = TOOL_CONTEXT_HOLDER.get();

                if (toolContext == null) {
                    throw new IllegalStateException("ToolContext not available");
                }

                Map<String, Object> contextMap = toolContext.getContext();

                Object actionContextObj = contextMap.get(ACTION_CONTEXT);

                if (!(actionContextObj instanceof ActionContext actionContext)) {
                    throw new IllegalStateException("ActionContext not available in ToolContext");
                }

                String resumeUrl = ((ActionContextAware) actionContext).generateResumeUrl();

                sendQuestionEvent(toolContext, questions, resumeUrl);

                Map<String, Object> continueParameters = new HashMap<>();

                continueParameters.put(QUESTIONS, questions);

                Instant expiresAt = Instant.now()
                    .plus(30, ChronoUnit.DAYS);

                actionContext.suspend(new Suspend(continueParameters, expiresAt));

                Map<String, String> placeholderAnswers = new HashMap<>();

                for (AskUserQuestionTool.Question question : questions) {
                    placeholderAnswers.put(question.question(), "");
                }

                return placeholderAnswers;
            })
            .answersValidation(false)
            .build();

        ToolCallback[] originalCallbacks = ToolCallbacks.from(askUserQuestionTool);

        ToolCallback[] wrappedCallbacks = new ToolCallback[originalCallbacks.length];

        for (int i = 0; i < originalCallbacks.length; i++) {
            wrappedCallbacks[i] = new ToolContextAwareToolCallback(originalCallbacks[i]);
        }

        return ToolCallbackProvider.from(List.of(wrappedCallbacks));
    }

    @SuppressWarnings("unchecked")
    private static void sendQuestionEvent(
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

        Object emitterRefObj = toolContextMap.get(SSE_EMITTER_REFERENCE);
        Object bufferedEventsObj = toolContextMap.get(SSE_BUFFERED_EVENTS);

        if (emitterRefObj instanceof AtomicReference<?> emitterRef) {
            Object emitter = emitterRef.get();

            if (emitter instanceof SseEmitter sseEmitter) {
                try {
                    sseEmitter.send(eventData);

                    return;
                } catch (Exception exception) {
                    logger.warn("SSE send failed, falling back to buffering: {}", exception.getMessage());
                }
            }
        }

        if (bufferedEventsObj instanceof Queue<?> queue) {
            ((Queue<Map<String, Object>>) queue).add(eventData);

            return;
        }

        throw new IllegalStateException(
            "Failed to deliver question event: neither SSE emitter nor buffered events queue is available");
    }

    private static class ToolContextAwareToolCallback implements ToolCallback {

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
            TOOL_CONTEXT_HOLDER.remove();

            return delegate.call(toolInput);
        }

        @Override
        public String call(String toolInput, @Nullable ToolContext toolContext) {
            if (toolContext != null) {
                TOOL_CONTEXT_HOLDER.set(toolContext);
            }

            try {
                return delegate.call(toolInput, toolContext);
            } finally {
                TOOL_CONTEXT_HOLDER.remove();
            }
        }
    }
}
