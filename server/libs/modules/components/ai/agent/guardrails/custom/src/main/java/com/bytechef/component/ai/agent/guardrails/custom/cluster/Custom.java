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

package com.bytechef.component.ai.agent.guardrails.custom.cluster;

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.DEFAULT_THRESHOLD;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.NAME;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.PROMPT;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.RESPONSE_SCHEMA;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.THRESHOLD;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_INPUT_PROPERTY;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_OUTPUT_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.JSON_SCHEMA_BUILDER;

import com.bytechef.component.ai.agent.guardrails.GuardrailUnavailableException;
import com.bytechef.component.ai.agent.guardrails.MissingModelChildException;
import com.bytechef.component.ai.agent.guardrails.util.LlmClassifierUtils;
import com.bytechef.component.ai.agent.guardrails.util.LlmClassifierUtils.Verdict;
import com.bytechef.component.ai.agent.guardrails.util.ParametersUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.chat.client.ChatClient;

/**
 * LLM-based custom classifier accepting multiple operator-defined entries (prompt + threshold + optional schema). Each
 * entry classifies independently; any firing entry becomes a violation.
 *
 * @author Ivica Cardic
 */
public final class Custom {

    private static final String GUARDRAILS = "guardrails";

    private Custom() {
    }

    public static ClusterElementDefinition<GuardrailCheckFunction> of() {
        return ComponentDsl.<GuardrailCheckFunction>clusterElement("custom")
            .title("Custom")
            .description(
                "Create your custom LLM-based guardrail with one or more user-defined classifier prompts." +
                    " Each entry in Classifiers runs independently")
            .type(GuardrailCheckFunction.CHECK_FOR_VIOLATIONS)
            .properties(
                VALIDATE_INPUT_PROPERTY,
                VALIDATE_OUTPUT_PROPERTY,
                array(GUARDRAILS)
                    .label("Classifiers")
                    .description("Named LLM classifier prompts that all run together. Add at least one entry.")
                    .items(
                        object()
                            .properties(
                                string(NAME)
                                    .label("Name")
                                    .description("Unique identifier for this classifier")
                                    .required(true),
                                string(PROMPT)
                                    .label("Prompt")
                                    .description("Classification instructions for the LLM.")
                                    .required(true),
                                string(RESPONSE_SCHEMA)
                                    .label("Response Schema")
                                    .description(
                                        "JSON schema extending the required. Defined are attached to the violation diagnostic for downstream tools and logs")
                                    .controlType(JSON_SCHEMA_BUILDER)
                                    .required(false),
                                number(THRESHOLD)
                                    .label("Threshold")
                                    .description("Confidence score required to flag as violation (0.0-1.0)")
                                    .defaultValue(DEFAULT_THRESHOLD)
                                    .minValue(0)
                                    .maxValue(1)))
                    .required(true))
            .object(() -> new GuardrailCheckFunction() {

                @Override
                public Optional<Violation> apply(String text, GuardrailContext context) throws Exception {
                    List<Violation> violations = applyAll(text, context);

                    if (violations.isEmpty()) {
                        return Optional.empty();
                    }

                    return Optional.of(violations.getFirst());
                }

                @Override
                public List<Violation> applyAll(String text, GuardrailContext context) throws Exception {
                    return Custom.applyAll(text, context);
                }

                @Override
                public GuardrailStage stage() {
                    return GuardrailStage.LLM;
                }

                @Override
                public boolean requiresChatClient() {
                    return true;
                }
            });
    }

    private static List<Violation> applyAll(String text, GuardrailContext context) {
        ChatClient chatClient = context.requireChatClient(() -> new MissingModelChildException("custom"));

        Parameters inputParameters = context.inputParameters();

        String systemMessage = ParametersUtils.resolveSystemMessage(context.parentParameters());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) (List<?>) inputParameters.getList(
            GUARDRAILS, Map.class, List.of());

        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Custom guardrail requires at least one entry in 'Classifiers'");
        }

        return applyMultiple(chatClient, systemMessage, text, entries);
    }

    private static List<Violation> applyMultiple(
        ChatClient chatClient, String systemMessage, String text, List<Map<String, Object>> entries) {

        List<ValidatedEntry> validated = new ArrayList<>(entries.size());

        for (Map<String, Object> entry : entries) {
            String entryName = entry.get(NAME) == null ? null : entry.get(NAME)
                .toString();
            String entryPrompt = entry.get(PROMPT) == null ? null : entry.get(PROMPT)
                .toString();

            if (entryName == null || entryName.isBlank() || entryPrompt == null || entryPrompt.isBlank()) {
                throw new IllegalArgumentException(
                    "Each custom guardrail entry requires non-empty 'name' and 'prompt' fields");
            }

            double entryThreshold = parseThreshold(entry.get(THRESHOLD), entryName);
            Object entrySchemaObj = entry.get(RESPONSE_SCHEMA);
            String entrySchema = entrySchemaObj == null ? null : entrySchemaObj.toString();

            validated.add(new ValidatedEntry(entryName, entryPrompt, entryThreshold, entrySchema));
        }

        List<Violation> violations = new ArrayList<>();
        Map<String, Throwable> entryFailures = new LinkedHashMap<>();

        for (int index = 0; index < validated.size(); index++) {
            ValidatedEntry entry = validated.get(index);

            Thread thread = Thread.currentThread();

            if (thread.isInterrupted()) {
                InterruptedException interruptedException = new InterruptedException();

                for (int remaining = index; remaining < validated.size(); remaining++) {
                    ValidatedEntry skipped = validated.get(remaining);

                    entryFailures.put(
                        skipped.name,
                        new GuardrailUnavailableException(
                            "custom", "Interrupted before entry '" + skipped.name + "' could run",
                            interruptedException));
                }

                break;
            }

            try {
                Optional<Violation> violation = entry.schema != null && !entry.schema.isBlank()
                    ? classifyWithSchema(
                        chatClient, entry.name, entry.prompt, entry.threshold, systemMessage, text, entry.schema)
                    : classifyWith(chatClient, entry.name, entry.prompt, entry.threshold, systemMessage, text);

                violation.ifPresent(violations::add);
            } catch (OutOfMemoryError error) {
                throw error;
            } catch (Throwable throwable) {
                entryFailures.put(entry.name, throwable);
            }
        }

        if (!entryFailures.isEmpty() && violations.isEmpty()) {
            Map.Entry<String, Throwable> first = entryFailures.entrySet()
                .iterator()
                .next();

            GuardrailUnavailableException aggregated = new GuardrailUnavailableException(
                "custom",
                "Every Custom guardrail entry failed; first failure from '" + first.getKey() + "'",
                first.getValue());

            entryFailures.entrySet()
                .stream()
                .skip(1)
                .forEach(entry -> aggregated.addSuppressed(entry.getValue()));

            throw aggregated;
        }

        if (!entryFailures.isEmpty()) {
            entryFailures.forEach((entryName, cause) -> violations.add(
                Violation.ofExecutionFailure("custom:" + entryName, cause)));
        }

        return List.copyOf(violations);
    }

    static Optional<Violation> classifyWith(
        ChatClient chatClient, String guardrailName, String userPrompt, double threshold,
        String systemMessage, String text) {

        Verdict verdict = LlmClassifierUtils.classify(
            guardrailName, chatClient, systemMessage, userPrompt, text, threshold);

        if (!verdict.violated()) {
            return Optional.empty();
        }

        return Optional.of(Violation.ofClassification(guardrailName, verdict.confidenceScore()));
    }

    static Optional<Violation> classifyWithSchema(
        ChatClient chatClient, String guardrailName, String userPrompt, double threshold,
        String systemMessage, String text, String responseSchema) {

        LlmClassifierUtils.SchemaVerdict verdict = LlmClassifierUtils.classifyWithSchema(
            guardrailName, chatClient, systemMessage, userPrompt, text, threshold, responseSchema);

        if (!verdict.violated()) {
            return Optional.empty();
        }

        LinkedHashMap<String, Serializable> serializableExtras = new LinkedHashMap<>();

        verdict.extraFields()
            .forEach((key, value) -> {
                if (value instanceof Serializable serializableValue) {
                    serializableExtras.put(key, serializableValue);
                }
            });

        return Optional.of(
            Violation.ofClassification(guardrailName, verdict.confidenceScore(), serializableExtras));
    }

    private static double parseThreshold(Object raw, String entryName) {
        double value;

        if (raw == null) {
            value = DEFAULT_THRESHOLD;
        } else if (raw instanceof Number number) {
            value = number.doubleValue();
        } else {
            String string = raw.toString();

            if (string.isBlank()) {
                value = DEFAULT_THRESHOLD;
            } else {
                try {
                    value = Double.parseDouble(string.trim());
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException(
                        "Custom guardrail entry '" + entryName + "' has an unparseable threshold value: '"
                            + string + "'",
                        exception);
                }
            }
        }

        if (Double.isNaN(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(
                "Custom guardrail entry '" + entryName + "' has an out-of-range threshold: " + value +
                    " (must be in [0.0, 1.0])");
        }

        return value;
    }

    private record ValidatedEntry(String name, String prompt, double threshold, String schema) {
    }
}
