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

package com.bytechef.ai.copilot.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.commons.util.JsonUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

/**
 * Bridges spring-ai-agent-utils' {@link AskUserQuestionTool} into the shared streaming agent (AI Hub and the in-editor
 * Copilot). Used by the LLM to pose a multi-choice clarification ("Which Slack action did you mean?" / "Pick a
 * connection"), with the actual UI buttons rendered by the chat client via a {@code kind: "ask-user-question"} data
 * part.
 *
 * @author Ivica Cardic
 */
public final class AskUserQuestionToolCallback implements ToolCallback {

    static final String TOOL_NAME = "askUserQuestion";
    static final String KIND = "ask-user-question";

    private static final String INPUT_CONSTRAINTS_GUIDANCE =
        " Input rules: provide 1-4 questions; each question must have 2-4 options. A free-text \"Other\" answer is" +
            " always offered to the user automatically — never add \"Other\", \"None\", \"Custom\", or a similar" +
            " catch-all as a hard-coded option. Keep each question's \"header\" to 12 characters or fewer (a short" +
            " chip label such as \"Channel\" or \"Timezone\"). Every option needs a non-blank \"label\" and" +
            " \"description\".";

    private static final Logger log = LoggerFactory.getLogger(AskUserQuestionToolCallback.class);

    private final ThreadLocal<List<AskUserQuestionTool.Question>> capturedQuestions = new ThreadLocal<>();

    private final ToolCallback delegate;
    private final ToolStateVisibilityMetrics metrics;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AskUserQuestionToolCallback(ToolStateVisibilityMetrics metrics) {
        this.metrics = metrics;

        AskUserQuestionTool askUserQuestionTool = AskUserQuestionTool.builder()
            .questionHandler(this::captureQuestionsReturningPlaceholders)
            .answersValidation(false)
            .build();

        ToolCallback[] callbacks = ToolCallbacks.from(askUserQuestionTool);

        if (callbacks.length != 1) {
            throw new IllegalStateException(
                "Expected exactly one ToolCallback from AskUserQuestionTool, got " + callbacks.length +
                    " — library shape changed; wrapper needs updating");
        }

        this.delegate = callbacks[0];
    }

    @Override
    public ToolDefinition getToolDefinition() {
        ToolDefinition toolDefinition = delegate.getToolDefinition();

        return ToolDefinition.builder()
            .name(TOOL_NAME)
            .description(toolDefinition.description() + INPUT_CONSTRAINTS_GUIDANCE)
            .inputSchema(toolDefinition.inputSchema())
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        capturedQuestions.remove();

        String validationError = validateInputShape(toolInput);

        if (validationError != null) {
            metrics.recordAskUserQuestion("error");

            log.warn(
                "askUserQuestion rejected by pre-validation: {} — first 300 chars of input: {}",
                validationError,
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 300)));

            return toolError(validationError);
        }

        try {
            delegate.call(toolInput, toolContext);

            List<AskUserQuestionTool.Question> questions = capturedQuestions.get();

            if (questions == null || questions.isEmpty()) {
                metrics.recordAskUserQuestion("empty");

                return toolError("No questions captured from askUserQuestion input");
            }

            Map<String, Object> envelope = new LinkedHashMap<>();

            envelope.put("kind", KIND);
            envelope.put("questions", serialiseQuestions(questions));
            envelope.put("awaitingAnswer", true);

            metrics.recordAskUserQuestion("success");

            return JsonUtils.write(envelope);
        } catch (RuntimeException exception) {
            metrics.recordAskUserQuestion("error");

            log.warn(
                "askUserQuestion failed: {} — first 200 chars of input: {}",
                exception.getMessage(),
                toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200)));

            return ToolErrors.runtimeFailure(AskUserQuestionToolCallback.class, TOOL_NAME, exception);
        } finally {
            capturedQuestions.remove();
        }
    }

    private Map<String, String> captureQuestionsReturningPlaceholders(
        List<AskUserQuestionTool.Question> questions) {

        capturedQuestions.set(questions);

        Map<String, String> placeholders = new HashMap<>();

        for (AskUserQuestionTool.Question question : questions) {
            placeholders.put(question.question(), "");
        }

        return placeholders;
    }

    private static List<Map<String, Object>> serialiseQuestions(List<AskUserQuestionTool.Question> questions) {
        List<Map<String, Object>> serialised = new ArrayList<>(questions.size());

        for (AskUserQuestionTool.Question question : questions) {
            Map<String, Object> row = new LinkedHashMap<>();

            row.put("question", question.question());
            row.put("header", question.header());
            row.put("multiSelect", question.multiSelect());

            List<Map<String, Object>> options = new ArrayList<>();

            for (AskUserQuestionTool.Question.Option option : question.options()) {
                Map<String, Object> optionRow = new LinkedHashMap<>();

                optionRow.put("label", option.label());
                optionRow.put("description", option.description());

                options.add(optionRow);
            }

            row.put("options", options);

            serialised.add(row);
        }

        return serialised;
    }

    /**
     * Validates the LLM's input against the library's documented constraints — strictly, with hard rejection instead of
     * the library's WARN-and-continue behaviour. Returns a human-readable error string when a violation is found, or
     * {@code null} when the input is well-formed.
     *
     * <p>
     * Constraints enforced:
     * </p>
     * <ul>
     * <li>{@code questions} must be a non-empty array with 1-4 entries.</li>
     * <li>Each question's {@code header} must be present, non-blank, and {@code <= 12} characters.</li>
     * <li>Each question's {@code question} text must be present and non-blank.</li>
     * <li>Each question's {@code options} array must contain 2-4 entries.</li>
     * <li>Each option's {@code label} and {@code description} must be present and non-blank.</li>
     * </ul>
     */
    private @Nullable String validateInputShape(@Nullable String toolInput) {
        if (toolInput == null || toolInput.isBlank()) {
            return "askUserQuestion input is empty";
        }

        JsonNode rootJsonNode;

        try {
            rootJsonNode = JsonUtils.readTree(toolInput);
        } catch (JacksonException exception) {
            return "askUserQuestion input is not valid JSON: " + exception.getMessage();
        }

        JsonNode questionsJsonNode = rootJsonNode.get("questions");

        if (questionsJsonNode == null || !questionsJsonNode.isArray()) {
            return "askUserQuestion requires a 'questions' array";
        }

        int questionsCount = questionsJsonNode.size();

        if (questionsCount < 1 || questionsCount > 4) {
            return "askUserQuestion 'questions' must contain 1-4 items, got: " + questionsCount;
        }

        for (int i = 0; i < questionsCount; i++) {
            String questionError = validateQuestionNode(questionsJsonNode.get(i), i);

            if (questionError != null) {
                return questionError;
            }
        }

        return null;
    }

    private @Nullable String validateQuestionNode(JsonNode questionJsonNode, int index) {
        if (questionJsonNode == null || !questionJsonNode.isObject()) {
            return "askUserQuestion 'questions[" + index + "]' must be an object";
        }

        String question = textOrNull(questionJsonNode.get("question"));

        if (question == null || question.isBlank()) {
            return "askUserQuestion 'questions[" + index + "].question' is required and must be non-blank";
        }

        String header = textOrNull(questionJsonNode.get("header"));

        if (header == null || header.isBlank()) {
            return "askUserQuestion 'questions[" + index + "].header' is required and must be non-blank";
        }

        if (header.length() > 12) {
            return "askUserQuestion 'questions[" + index + "].header' is " + header.length() +
                " characters; the limit is 12 (use a shorter chip label like 'Channel' or 'Auth')";
        }

        JsonNode optionsNode = questionJsonNode.get("options");

        if (optionsNode == null || !optionsNode.isArray()) {
            return "askUserQuestion 'questions[" + index + "].options' must be an array of 2-4 items";
        }

        int optionsCount = optionsNode.size();

        if (optionsCount < 2 || optionsCount > 4) {
            return "askUserQuestion 'questions[" + index + "].options' must contain 2-4 items, got: " +
                optionsCount +
                " (the user always sees an 'Other' fallback automatically — do not include it as a hard-coded option)";
        }

        for (int j = 0; j < optionsCount; j++) {
            JsonNode optionJsonNode = optionsNode.get(j);

            if (optionJsonNode == null || !optionJsonNode.isObject()) {
                return "askUserQuestion 'questions[" + index + "].options[" + j + "]' must be an object";
            }

            String label = textOrNull(optionJsonNode.get("label"));

            if (label == null || label.isBlank()) {
                return "askUserQuestion 'questions[" + index + "].options[" + j +
                    "].label' is required and must be non-blank";
            }

            String description = textOrNull(optionJsonNode.get("description"));

            if (description == null || description.isBlank()) {
                return "askUserQuestion 'questions[" + index + "].options[" + j +
                    "].description' is required and must be non-blank";
            }
        }

        return null;
    }

    private static @Nullable String textOrNull(@Nullable JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return null;
        }

        return jsonNode.asString();
    }

    private String toolError(String message) {
        return ToolErrors.toolError(message);
    }
}
