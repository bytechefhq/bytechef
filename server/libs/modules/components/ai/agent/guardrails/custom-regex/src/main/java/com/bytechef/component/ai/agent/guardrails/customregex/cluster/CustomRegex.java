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

package com.bytechef.component.ai.agent.guardrails.customregex.cluster;

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.NAME;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.REGEX;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.agent.guardrails.util.GuardrailProperties;
import com.bytechef.component.ai.agent.guardrails.util.RegexParser;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.MaskResult;
import com.bytechef.platform.component.definition.ai.agent.guardrails.PreflightCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rule-based detection on operator-supplied regex patterns. Pattern input is wrapped in {@code RegexParser.bounded} to
 * enforce DoS caps, and compilation errors surface as configuration errors (fail-closed regardless of fail mode).
 *
 * @author Ivica Cardic
 */
public final class CustomRegex {

    /** Array-of-objects property name for the multi-entry form: {@code [{name, regex}, ...]}. */
    public static final String PATTERNS = "patterns";

    public static ClusterElementDefinition<GuardrailCheckFunction> ofCheck() {
        return ComponentDsl.<GuardrailCheckFunction>clusterElement("customRegexCheck")
            .title("Custom Regex")
            .description("User-defined regex — flags the first match.")
            .type(GuardrailCheckFunction.CHECK_FOR_VIOLATIONS)
            .properties(sharedProperties())
            .object(() -> new PreflightCheckFunction() {

                @Override
                public Optional<Violation> apply(String text, GuardrailContext context) {
                    return applyCheck(text, context);
                }

                @Override
                public MaskResult mask(String text, GuardrailContext context) {
                    return MaskResult.entities(collectMaskEntities(text, context));
                }
            });
    }

    public static ClusterElementDefinition<GuardrailSanitizerFunction> ofSanitize() {
        // CustomRegex sanitize uses the user-chosen [name] placeholder, not the shared <TYPE> convention, so it stays
        // on the apply() path (no PreflightMasking). The check-side still opts into mask(-entities) because that mask
        // is consumed only by downstream LLM checks where the <name> format is equivalent.
        return ComponentDsl.<GuardrailSanitizerFunction>clusterElement("customRegexSanitize")
            .title("Custom Regex")
            .description("User-defined regex — replaces matches with [name] placeholder.")
            .type(GuardrailSanitizerFunction.SANITIZE_TEXT)
            .properties(sharedProperties())
            .object(() -> CustomRegex::applySanitize);
    }

    private CustomRegex() {
    }

    /**
     * Validate a user-supplied regex ahead of workflow execution. Intended call-site is a workflow-save-time validator
     * hook so that a bad pattern fails fast in the editor instead of surfacing as a generic "request blocked" at
     * runtime. The guardrail module itself does not invoke this from the save path yet; runtime compilation in
     * {@link #namedRegexesOf} still catches the error and routes it through the advisor's fail-closed path, but it does
     * so on every request until the configuration is fixed.
     *
     * <p>
     * This method also constitutes the operator-facing defense against compile-time regex pathology:
     * {@link RegexParser#compile} rejects expressions longer than {@link RegexParser#MAX_EXPRESSION_LENGTH} up front,
     * but that bound is deliberately coarse. A future workflow-save validator should call this method and can add
     * stricter per-workflow limits if needed.
     */
    public static void validateRegex(String regex) {
        compileOrThrow(regex);
    }

    private static Property[] sharedProperties() {
        return new Property[] {
            string(NAME)
                .label("Name")
                .description(
                    "Used as the placeholder [name] in sanitize mode. Ignored when 'Patterns' below is populated."),
            string(REGEX)
                .label("Regex")
                .description(
                    "Java regular-expression pattern. Ignored when 'Patterns' below is populated."),
            array(PATTERNS)
                .label("Patterns")
                .description("Multiple named regex patterns that all run together. When non-empty, this takes "
                    + "precedence over the single Name/Regex pair above.")
                .items(
                    object()
                        .properties(
                            string(NAME)
                                .label("Name")
                                .required(true),
                            string(REGEX)
                                .label("Regex")
                                .description("Java regular-expression pattern. Use /pattern/flags literal syntax "
                                    + "for JS-style regex flags (e.g. /ssn-\\d{4}/i).")
                                .required(true)))
                .required(false),
            GuardrailProperties.failMode()
        };
    }

    private record NamedRegex(String name, Pattern pattern) {
    }

    private static List<NamedRegex> namedRegexesOf(Parameters inputParameters) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> entries = (List<Map<String, String>>) (List<?>) inputParameters.getList(
            PATTERNS, Map.class, List.of());

        if (!entries.isEmpty()) {
            List<NamedRegex> list = new ArrayList<>(entries.size());

            for (Map<String, String> entry : entries) {
                String entryName = entry.get(NAME);
                String entryRegex = entry.get(REGEX);

                if (entryName == null || entryName.isBlank() || entryRegex == null || entryRegex.isBlank()) {
                    throw new IllegalArgumentException(
                        "Each custom-regex pattern requires non-empty 'name' and 'regex' fields");
                }

                list.add(new NamedRegex(entryName, compileOrThrow(entryRegex)));
            }

            return list;
        }

        String singleName = inputParameters.getString(NAME);
        String singleRegex = inputParameters.getString(REGEX);

        if (singleName == null || singleName.isBlank() || singleRegex == null || singleRegex.isBlank()) {
            throw new IllegalArgumentException(
                "Custom Regex requires either a 'patterns' array or both 'name' and 'regex'");
        }

        return List.of(new NamedRegex(singleName, compileOrThrow(singleRegex)));
    }

    private static Pattern compileOrThrow(String regex) {
        try {
            return RegexParser.compile(regex);
        } catch (IllegalArgumentException e) {
            // RegexParser.compile throws PatternSyntaxException (a subtype of IllegalArgumentException) for invalid
            // regex syntax and plain IllegalArgumentException for unsupported flags / empty expression — the single
            // IAE catch covers both so the message always includes the raw input for operator correlation.
            throw new IllegalArgumentException("Invalid regex: " + regex, e);
        }
    }

    private static Optional<Violation> applyCheck(String text, GuardrailContext context) {
        Parameters inputParameters = context.inputParameters();
        List<NamedRegex> regexes = namedRegexesOf(inputParameters);

        List<String> matches = new ArrayList<>();
        List<String> triggeredNames = new ArrayList<>();
        List<RegexParser.RegexExecutionLimitException> budgetFailures = new ArrayList<>();

        // Per-entry try/catch mirrors PiiDetector/SecretKeyDetector: a single pathological user-supplied regex must not
        // silently prevent subsequent entries from running. All failures are aggregated so operators see every bad
        // entry, not just the first.
        for (NamedRegex entry : regexes) {
            try {
                Matcher matcher = entry.pattern()
                    .matcher(RegexParser.bounded(text));

                boolean anyMatch = false;

                while (matcher.find()) {
                    matches.add(matcher.group());

                    anyMatch = true;
                }

                if (anyMatch) {
                    triggeredNames.add(entry.name());
                }
            } catch (RegexParser.RegexExecutionLimitException e) {
                budgetFailures.add(new RegexParser.RegexExecutionLimitException(
                    "customRegex entry '" + entry.name() + "': " + e.getMessage(), e));
            }
        }

        if (!budgetFailures.isEmpty()) {
            // Surface one aggregated exception; the advisor's isConfigurationError treats
            // RegexExecutionLimitException as fail-closed regardless of fail mode.
            RegexParser.RegexExecutionLimitException headline = budgetFailures.getFirst();

            budgetFailures.stream()
                .skip(1)
                .forEach(headline::addSuppressed);

            throw headline;
        }

        if (matches.isEmpty()) {
            return Optional.empty();
        }

        String violationName = triggeredNames.size() == 1 ? triggeredNames.getFirst() : "customRegex";

        return Optional.of(Violation.ofMatches(violationName, matches, Map.of("patternNames", triggeredNames)));
    }

    private static String applySanitize(String text, GuardrailContext context) {
        return mask(text, context);
    }

    private static String mask(String text, GuardrailContext context) {
        List<NamedRegex> regexes = namedRegexesOf(context.inputParameters());

        String intermediate = text;
        List<RegexParser.RegexExecutionLimitException> budgetFailures = new ArrayList<>();

        for (NamedRegex entry : regexes) {
            try {
                // Matcher.quoteReplacement prevents `$` / `\` in `name` from being interpreted as replacement syntax.
                String replacement = Matcher.quoteReplacement("[" + entry.name() + "]");

                intermediate = entry.pattern()
                    .matcher(RegexParser.bounded(intermediate))
                    .replaceAll(replacement);
            } catch (RegexParser.RegexExecutionLimitException e) {
                budgetFailures.add(new RegexParser.RegexExecutionLimitException(
                    "customRegex entry '" + entry.name() + "': " + e.getMessage(), e));
            }
        }

        if (!budgetFailures.isEmpty()) {
            RegexParser.RegexExecutionLimitException headline = budgetFailures.getFirst();

            budgetFailures.stream()
                .skip(1)
                .forEach(headline::addSuppressed);

            throw headline;
        }

        return intermediate;
    }

    private static Map<String, List<String>> collectMaskEntities(String text, GuardrailContext context) {
        List<NamedRegex> regexes = namedRegexesOf(context.inputParameters());

        Map<String, List<String>> grouped = new LinkedHashMap<>();
        List<RegexParser.RegexExecutionLimitException> budgetFailures = new ArrayList<>();

        for (NamedRegex entry : regexes) {
            try {
                Matcher matcher = entry.pattern()
                    .matcher(RegexParser.bounded(text));

                while (matcher.find()) {
                    grouped.computeIfAbsent(entry.name(), key -> new ArrayList<>())
                        .add(matcher.group());
                }
            } catch (RegexParser.RegexExecutionLimitException e) {
                budgetFailures.add(new RegexParser.RegexExecutionLimitException(
                    "customRegex entry '" + entry.name() + "': " + e.getMessage(), e));
            }
        }

        if (!budgetFailures.isEmpty()) {
            RegexParser.RegexExecutionLimitException headline = budgetFailures.getFirst();

            budgetFailures.stream()
                .skip(1)
                .forEach(headline::addSuppressed);

            throw headline;
        }

        return grouped;
    }
}
