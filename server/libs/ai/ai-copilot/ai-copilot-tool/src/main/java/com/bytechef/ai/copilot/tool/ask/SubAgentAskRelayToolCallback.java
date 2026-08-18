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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Wraps one intelligent delegate {@code ToolCallback} so a question its specialist raised mid-delegation comes back as
 * <b>this delegate's own tool result</b> — which is what puts it on the parent agent's stream, because a delegate tool
 * result IS a main-agent tool result.
 *
 * <p>
 * Applied once, in {@code IntelligentToolCatalog}, to every callback the catalog builds. Wiring it there rather than
 * inside each of the delegate callback classes is the point: every intelligent delegate gains the capability from one
 * edit, and one added later inherits it instead of having to remember.
 * </p>
 *
 * <p>
 * {@link #getToolDefinition()} returns the wrapped callback's definition verbatim — name, description and input schema
 * alike. Anything else would advertise a name the catalog does not know the delegate by, and the model would call a
 * tool that never resolves.
 * </p>
 *
 * <p>
 * The specialist's own text summary is discarded whenever a question was raised. That is by design: the summary is a
 * one-line "I asked the user something", and rendering it beside the question card would restate the question in prose.
 * The check runs before the null-result guard for the same reason — a specialist that asked has already produced the
 * thing the user needs to see, whether or not it also managed a summary.
 * </p>
 *
 * <p>
 * An <em>error</em>-shaped result is not a summary, though, and dropping one silently is a real failure: a specialist
 * that asks and then hits an {@code AccessDeniedException} from an admin-guarded facade would return only the question,
 * the user would answer it, and the re-delegation would be denied again with nothing ever explaining why. Such a result
 * is therefore always logged at WARN against the delegate's name, and — on the
 * {@link SubAgentQuestionRenderer#PLAIN_TEXT} surfaces — appended to the rendered question so the caller sees it. It is
 * deliberately NOT appended under {@link SubAgentQuestionRenderer#JSON}: that payload is parsed by the client as the
 * {@code ask-user-question} envelope, and trailing prose would make the parse fail and cost the user the choice card
 * entirely.
 * </p>
 *
 * @author Ivica Cardic
 */
public final class SubAgentAskRelayToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(SubAgentAskRelayToolCallback.class);

    private static final JsonMapper JSON_MAPPER = new JsonMapper();

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
            return renderQuestion(toolName, pendingQuestion, result);
        }

        if (result == null) {
            return ToolErrors.toolError(toolName + " subagent returned null");
        }

        return result;
    }

    /**
     * Renders a raised question, keeping an error-shaped result from vanishing with the discarded summary. Under
     * {@link SubAgentQuestionRenderer#JSON} the envelope must stay byte-identical for the client's parser, so the error
     * reaches the operator through the log only.
     */
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

    /**
     * The result when it is a {@link ToolErrors}-shaped {@code {"error": ...}} payload, {@code null} otherwise. Matched
     * structurally rather than by substring so an ordinary result that merely mentions the word "error" is not
     * misreported as a failure.
     */
    private static @Nullable String errorResultOrNull(@Nullable String result) {
        if (result == null) {
            return null;
        }

        String trimmedResult = result.strip();

        if (!trimmedResult.startsWith("{")) {
            return null;
        }

        try {
            JsonNode resultJsonNode = JSON_MAPPER.readTree(trimmedResult);

            JsonNode errorJsonNode = resultJsonNode.get("error");

            return errorJsonNode == null || errorJsonNode.isNull() ? null : trimmedResult;
        } catch (JacksonException exception) {
            return null;
        }
    }
}
