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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.JSON_SCHEMA_BUILDER;

import com.bytechef.component.ai.agent.guardrails.GuardrailUnavailableException;
import com.bytechef.component.ai.agent.guardrails.MissingModelChildException;
import com.bytechef.component.ai.agent.guardrails.util.GuardrailProperties;
import com.bytechef.component.ai.agent.guardrails.util.LlmClassifier;
import com.bytechef.component.ai.agent.guardrails.util.LlmClassifier.Verdict;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * LLM-based custom classifier accepting multiple operator-defined entries (prompt + threshold + optional schema). Each
 * entry is classified independently; any firing entry becomes a violation. When multiple entries fail the LLM call the
 * headline cause is thrown and the remaining causes are attached as suppressed exceptions.
 *
 * @author Ivica Cardic
 */
@Component("custom_v1_ClusterElement")
public final class Custom {

    /** Array-of-objects property name for the multi-entry form: {@code [{name, prompt, responseSchema, threshold}]}. */
    public static final String GUARDRAILS = "guardrails";

    public ClusterElementDefinition<GuardrailCheckFunction> of() {
        return ComponentDsl.<GuardrailCheckFunction>clusterElement("custom")
            .title("Custom")
            .description("LLM-based guardrail with user-defined classification prompts. One cluster element may "
                + "hold a single prompt (Name + Prompt) or an array of named prompts (Guardrails).")
            .type(GuardrailCheckFunction.CHECK_FOR_VIOLATIONS)
            .properties(
                string(NAME)
                    .label("Name")
                    .description("Unique identifier for this guardrail. Ignored when Guardrails below is populated."),
                string(PROMPT)
                    .label("Prompt")
                    .description("Classification instructions for the LLM. Ignored when Guardrails below is "
                        + "populated."),
                string(RESPONSE_SCHEMA)
                    .label("Response Schema")
                    .description("Optional JSON schema extending the required {flagged: boolean, "
                        + "confidenceScore: number} response. Extra fields you define (e.g. reason, "
                        + "category) are surfaced in the violation's diagnostic info. Applies only to the "
                        + "single-prompt form.")
                    .controlType(JSON_SCHEMA_BUILDER)
                    .required(false),
                number(THRESHOLD)
                    .label("Threshold")
                    .description("Minimum confidence score (0.0-1.0) required to flag. Applies to the "
                        + "single-prompt form; each entry in the multi-entry form has its own threshold.")
                    .defaultValue(DEFAULT_THRESHOLD),
                array(GUARDRAILS)
                    .label("Guardrails")
                    .description("Multiple named LLM guardrails that all run together. When non-empty, this "
                        + "takes precedence over the single Name/Prompt pair above.")
                    .items(
                        object()
                            .properties(
                                string(NAME)
                                    .label("Name")
                                    .required(true),
                                string(PROMPT)
                                    .label("Prompt")
                                    .description("Classification instructions for the LLM.")
                                    .required(true),
                                string(RESPONSE_SCHEMA)
                                    .label("Response Schema")
                                    .description("Optional JSON schema; extra fields surface in the "
                                        + "violation's diagnostic info.")
                                    .controlType(JSON_SCHEMA_BUILDER)
                                    .required(false),
                                number(THRESHOLD)
                                    .label("Threshold")
                                    .description("Minimum confidence score (0.0-1.0) required to flag.")
                                    .defaultValue(DEFAULT_THRESHOLD)))
                    .required(false),
                GuardrailProperties.failMode())
            .object(() -> new GuardrailCheckFunction() {

                @Override
                public Optional<Violation> apply(String text, GuardrailContext context) throws Exception {
                    return Custom.this.apply(text, context);
                }

                @Override
                public GuardrailStage stage() {
                    return GuardrailStage.LLM;
                }
            });
    }

    private Optional<Violation> apply(String text, GuardrailContext context) throws Exception {
        // Fail-closed via exception: advisor converts this to a blocking violation.
        ChatClient chatClient = context.chatClient()
            .orElseThrow(() -> new MissingModelChildException("custom"));

        Parameters inputParameters = context.inputParameters();

        String systemMessage = GuardrailProperties.resolveSystemMessage(context.parentParameters());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) (List<?>) inputParameters.getList(
            GUARDRAILS, Map.class, List.of());

        if (!entries.isEmpty()) {
            return applyMultiple(chatClient, systemMessage, text, entries);
        }

        String guardrailName = inputParameters.getRequiredString(NAME);
        String userPrompt = inputParameters.getRequiredString(PROMPT);
        double threshold = inputParameters.getDouble(THRESHOLD, DEFAULT_THRESHOLD);
        String responseSchema = inputParameters.getString(RESPONSE_SCHEMA);

        if (responseSchema != null && !responseSchema.isBlank()) {
            return classifyWithSchema(
                chatClient, guardrailName, userPrompt, threshold, systemMessage, text, responseSchema);
        }

        return classifyWith(chatClient, guardrailName, userPrompt, threshold, systemMessage, text);
    }

    /**
     * When the user populates the Guardrails array, each entry is classified independently. A runtime failure in one
     * entry (LLM outage, parse failure) must not silently disable the remaining entries — every entry runs and
     * successes are aggregated. Failure policy:
     * <ul>
     * <li>If <b>any</b> entry fails, the first failure is rethrown as {@link GuardrailUnavailableException}. The
     * advisor's fail-mode then decides: FAIL_CLOSED blocks; FAIL_OPEN records-and-forwards. This preserves the security
     * posture that a partial pass is effectively a complete failure — if we cannot guarantee the guardrail was fully
     * effective, the operator's fail-mode choice must be honoured. Silently surfacing the failure only as
     * {@code info.failedEntries} would have hidden the failure from the advisor's fail-mode branch.</li>
     * <li>If every entry succeeded with no violations, returns {@link Optional#empty()}.</li>
     * <li>If every entry succeeded and one or more fired, returns the headline violation. When multiple entries fired,
     * the headline carries {@code flaggedEntries} in its info map so operators see the full picture.</li>
     * </ul>
     * Configuration errors (missing/blank name/prompt) still propagate immediately because they represent an operator
     * bug rather than a transient per-entry failure.
     */
    private static Optional<Violation> applyMultiple(
        ChatClient chatClient, String systemMessage, String text, List<Map<String, Object>> entries) {

        // Validate every entry up front before any LLM call. Otherwise a bad entry in position N would abort the loop
        // after entries 0..N-1 had already made LLM requests whose results we'd then discard — expensive, and the
        // inconsistent execution model ("entries 1-2 ran but their verdicts are thrown away; entries 4-5 never ran")
        // is harder to reason about than an atomic fail-fast for configuration.
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

        for (ValidatedEntry entry : validated) {
            // Check before dispatching: LlmClassifier restores the interrupt flag on wrapped InterruptedException
            // before rethrowing as GuardrailUnavailableException, so the flag is still set when we re-enter the loop
            // after a cancelled call. Without this short-circuit the remaining entries would fire real LLM requests
            // despite the caller having asked to cancel.
            if (Thread.currentThread()
                .isInterrupted()) {
                entryFailures.put(entry.name, new GuardrailUnavailableException(
                    "custom", "Interrupted before entry '" + entry.name + "' could run",
                    new InterruptedException()));

                break;
            }

            try {
                Optional<Violation> violation = entry.schema != null && !entry.schema.isBlank()
                    ? classifyWithSchema(
                        chatClient, entry.name, entry.prompt, entry.threshold, systemMessage, text, entry.schema)
                    : classifyWith(chatClient, entry.name, entry.prompt, entry.threshold, systemMessage, text);

                violation.ifPresent(violations::add);
            } catch (OutOfMemoryError e) {
                // OOM must propagate unchanged so the JVM-level handler runs.
                throw e;
            } catch (Throwable e) {
                // Mirror the advisor / LlmClassifier discipline: catch Throwable minus OOM. Errors thrown from inside
                // the classifier helpers (StackOverflowError on a pathological prompt, LinkageError, etc.) would
                // otherwise abort the loop with no record of which entry failed and no aggregated message — operators
                // would see only the raw Error with no list of which entries did or did not run.
                entryFailures.put(entry.name, e);
            }
        }

        if (!entryFailures.isEmpty()) {
            Map.Entry<String, Throwable> first = entryFailures.entrySet()
                .iterator()
                .next();
            String summary = violations.isEmpty()
                ? "Every Custom guardrail entry failed"
                : "Custom guardrail had " + entryFailures.size() + " entry failure(s) alongside "
                    + violations.size() + " flagged entry/entries (flagged: "
                    + violations.stream()
                        .map(Violation::guardrail)
                        .toList()
                    + ", failed: " + List.copyOf(entryFailures.keySet()) + ")";

            GuardrailUnavailableException aggregated = new GuardrailUnavailableException(
                "custom",
                summary + "; first failure from '" + first.getKey() + "'",
                first.getValue());

            // Preserve every non-headline entry failure as a suppressed cause so operators looking at the exception
            // stack see the full picture — otherwise only the first entry's stack trace reaches Sentry / the logs,
            // and debugging "which of my three custom entries broke" requires reproducing the run.
            entryFailures.entrySet()
                .stream()
                .skip(1)
                .forEach(entry -> aggregated.addSuppressed(entry.getValue()));

            throw aggregated;
        }

        if (violations.isEmpty()) {
            return Optional.empty();
        }

        Violation headline = violations.getFirst();

        if (violations.size() == 1) {
            return Optional.of(headline);
        }

        List<String> flaggedNames = violations.stream()
            .map(Violation::guardrail)
            .toList();

        // classifyWith/classifyWithSchema always produce a ClassifiedViolation, so the pattern match resolves on the
        // first arm in practice; the 1.0 fallback guards against a future refactor that might introduce other variants
        // into the violations list without updating this synthesis step.
        double headlineScore = headline instanceof Violation.ClassifiedViolation classified
            ? classified.confidenceScore()
            : 1.0;

        return Optional.of(Violation.ofClassification(
            headline.guardrail(),
            headlineScore,
            Map.of("flaggedEntries", flaggedNames)));
    }

    /** Package-private seam for testing. */
    static Optional<Violation> classifyWith(
        ChatClient chatClient, String guardrailName, String userPrompt, double threshold,
        String systemMessage, String text) {

        Verdict verdict = LlmClassifier.classify(
            guardrailName, chatClient, systemMessage, userPrompt, text, threshold);

        if (!verdict.violated()) {
            return Optional.empty();
        }

        return Optional.of(Violation.ofClassification(guardrailName, verdict.confidenceScore()));
    }

    /** Package-private seam for testing. */
    static Optional<Violation> classifyWithSchema(
        ChatClient chatClient, String guardrailName, String userPrompt, double threshold,
        String systemMessage, String text, String responseSchema) {

        LlmClassifier.SchemaVerdict verdict = LlmClassifier.classifyWithSchema(
            guardrailName, chatClient, systemMessage, userPrompt, text, threshold, responseSchema);

        if (!verdict.violated()) {
            return Optional.empty();
        }

        return Optional.of(
            Violation.ofClassification(guardrailName, verdict.confidenceScore(), verdict.extraFields()));
    }

    /**
     * Parse the per-entry {@code threshold} field with the same string-tolerant semantics as the single-prompt path
     * ({@code Parameters.getDouble}). The single-prompt path converts a {@code "0.7"} form value via
     * {@code Parameters}; the multi-entry path historically silently fell back to
     * {@link com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants#DEFAULT_THRESHOLD} on any
     * non-{@code
     * Number} value, which let an unparseable threshold mask itself as "loose default" rather than as a configuration
     * error. Configuration errors are routed to fail-closed by the advisor's {@code isConfigurationError} branch, so
     * surfacing this as {@link IllegalArgumentException} is the security-preserving choice.
     */
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
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                        "Custom guardrail entry '" + entryName + "' has an unparseable threshold value: '"
                            + string + "'",
                        e);
                }
            }
        }

        // Mirror ClassifiedViolation's [0.0, 1.0] invariant at parse time so an out-of-range threshold surfaces
        // as a configuration error with the entry name in context, rather than later as an always-fail comparison
        // inside LlmClassifier where the entry name is lost.
        if (Double.isNaN(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(
                "Custom guardrail entry '" + entryName + "' has an out-of-range threshold: " + value
                    + " (must be in [0.0, 1.0])");
        }

        return value;
    }

    private record ValidatedEntry(String name, String prompt, double threshold, String schema) {
    }
}
