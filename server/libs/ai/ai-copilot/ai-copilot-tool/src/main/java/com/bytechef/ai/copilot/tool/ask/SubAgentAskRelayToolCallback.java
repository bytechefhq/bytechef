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

package com.bytechef.ai.copilot.tool.ask;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.commons.util.JsonUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

/**
 * @author Ivica Cardic
 */
public final class SubAgentAskRelayToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(SubAgentAskRelayToolCallback.class);

    private final ToolCallback delegate;
    private final SubAgentAskRelay askRelay;
    private final SubAgentQuestionRenderer questionRenderer;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SubAgentAskRelayToolCallback(
        ToolCallback delegate, SubAgentAskRelay askRelay, SubAgentQuestionRenderer questionRenderer) {

        this.delegate = delegate;
        this.askRelay = askRelay;
        this.questionRenderer = questionRenderer;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        SubAgentAskRelay.AskOutcome<String> askOutcome =
            askRelay.runWithChannel(() -> delegate.call(toolInput, toolContext));

        String pendingQuestion = askOutcome.pendingQuestion();
        String result = askOutcome.result();

        ToolDefinition toolDefinition = delegate.getToolDefinition();

        String toolName = toolDefinition.name();

        if (pendingQuestion != null) {
            SubAgentPendingQuestionSignal.markPending(toolContext);

            return renderQuestion(toolName, pendingQuestion, result);
        }

        if (result == null) {
            return ToolErrors.toolError(toolName + " subagent returned null");
        }

        return result;
    }

    private String renderQuestion(String toolName, String pendingQuestion, @Nullable String result) {
        String errorResult = errorResultOrNull(result);

        if (errorResult != null) {
            log.warn(
                "{} raised a question and also returned an error result, which the question replaces: {}", toolName,
                errorResult);
        }

        if (questionRenderer != SubAgentQuestionRenderer.PLAIN_TEXT) {
            return pendingQuestion;
        }

        String formattedQuestion = SubAgentQuestionFormatter.format(toolName, pendingQuestion);

        if (errorResult == null) {
            return formattedQuestion;
        }

        return formattedQuestion + "\n\nNote: " + toolName
            + " also reported an error on the call that asked — it may fail again for the same reason once answered: "
            + errorResult;
    }

    private static @Nullable String errorResultOrNull(@Nullable String result) {
        if (result == null) {
            return null;
        }

        String trimmedResult = result.strip();

        if (!trimmedResult.startsWith("{")) {
            return null;
        }

        try {
            JsonNode resultJsonNode = JsonUtils.readTree(trimmedResult);

            JsonNode errorJsonNode = resultJsonNode.get("error");

            return errorJsonNode == null || errorJsonNode.isNull() ? null : trimmedResult;
        } catch (JacksonException exception) {
            return null;
        }
    }
}
