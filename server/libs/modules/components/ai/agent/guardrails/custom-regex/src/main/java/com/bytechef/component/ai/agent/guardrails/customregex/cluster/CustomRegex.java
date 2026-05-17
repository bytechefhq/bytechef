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
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_INPUT_PROPERTY;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_OUTPUT_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.agent.guardrails.util.RegexParserUtils;
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
 * Rule-based detection on operator-supplied regex patterns. Input is bounded via {@code RegexParser.bounded} to enforce
 * DoS caps.
 *
 * @author Ivica Cardic
 */
public final class CustomRegex {

    public static final String PATTERNS = "patterns";

    public static ClusterElementDefinition<GuardrailCheckFunction> ofCheck() {
        return ComponentDsl.<GuardrailCheckFunction>clusterElement("customRegexCheck")
            .title("Custom Regex")
            .description("User-defined regex — flags every match across all configured patterns.")
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
        return ComponentDsl.<GuardrailSanitizerFunction>clusterElement("customRegexSanitize")
            .title("Custom Regex")
            .description("User-defined regex — replaces matches with [name] placeholder.")
            .type(GuardrailSanitizerFunction.SANITIZE_TEXT)
            .properties(sharedProperties())
            .object(() -> CustomRegex::applySanitize);
    }

    private CustomRegex() {
    }

    public static void validateRegex(String regex) {
        compileOrThrow(regex);
    }

    private static Property[] sharedProperties() {
        return new Property[] {
            VALIDATE_INPUT_PROPERTY,
            VALIDATE_OUTPUT_PROPERTY,
            array(PATTERNS)
                .label("Patterns")
                .description("Named regex patterns. Each entry runs against the input; in sanitize mode, "
                    + "matches are replaced with the entry's [name] placeholder. Add at least one entry.")
                .items(
                    object()
                        .properties(
                            string(NAME)
                                .label("Name")
                                .description(
                                    "Used as the placeholder [name] in sanitize mode and as the violation "
                                        + "identifier when this pattern flags content.")
                                .required(true),
                            string(REGEX)
                                .label("Regex")
                                .description("Java regular-expression pattern. Use /pattern/flags literal syntax "
                                    + "for JS-style regex flags (e.g. /ssn-\\d{4}/i).")
                                .required(true)))
                .required(true)
        };
    }

    private static List<NamedRegex> namedRegexesOf(Parameters inputParameters) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> entries = (List<Map<String, String>>) (List<?>) inputParameters.getList(
            PATTERNS, Map.class, List.of());

        if (entries.isEmpty()) {
            throw new IllegalArgumentException(
                "Custom Regex requires at least one entry in 'Patterns'");
        }

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

    private static Pattern compileOrThrow(String regex) {
        try {
            return RegexParserUtils.compile(regex);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid regex: " + regex, exception);
        }
    }

    private static Optional<Violation> applyCheck(String text, GuardrailContext context) {
        Parameters inputParameters = context.inputParameters();
        List<NamedRegex> regexes = namedRegexesOf(inputParameters);

        List<String> matches = new ArrayList<>();
        ArrayList<String> triggeredNames = new ArrayList<>();
        List<RegexParserUtils.RegexExecutionLimitException> budgetFailures = new ArrayList<>();

        for (NamedRegex entry : regexes) {
            try {
                Matcher matcher = entry.pattern()
                    .matcher(RegexParserUtils.bounded(text));

                boolean anyMatch = false;

                while (matcher.find()) {
                    matches.add(matcher.group());

                    anyMatch = true;
                }

                if (anyMatch) {
                    triggeredNames.add(entry.name());
                }
            } catch (RegexParserUtils.RegexExecutionLimitException exception) {
                budgetFailures.add(new RegexParserUtils.RegexExecutionLimitException(
                    "customRegex entry '" + entry.name() + "': " + exception.getMessage(), exception));
            }
        }

        if (!budgetFailures.isEmpty()) {
            RegexParserUtils.RegexExecutionLimitException headline = budgetFailures.getFirst();

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
        List<RegexParserUtils.RegexExecutionLimitException> budgetFailures = new ArrayList<>();

        for (NamedRegex entry : regexes) {
            try {
                String replacement = Matcher.quoteReplacement("[" + entry.name() + "]");

                intermediate = entry.pattern()
                    .matcher(RegexParserUtils.bounded(intermediate))
                    .replaceAll(replacement);
            } catch (RegexParserUtils.RegexExecutionLimitException exception) {
                budgetFailures.add(new RegexParserUtils.RegexExecutionLimitException(
                    "customRegex entry '" + entry.name() + "': " + exception.getMessage(), exception));
            }
        }

        if (!budgetFailures.isEmpty()) {
            RegexParserUtils.RegexExecutionLimitException headline = budgetFailures.getFirst();

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
        List<RegexParserUtils.RegexExecutionLimitException> budgetFailures = new ArrayList<>();

        for (NamedRegex entry : regexes) {
            try {
                Matcher matcher = entry.pattern()
                    .matcher(RegexParserUtils.bounded(text));

                while (matcher.find()) {
                    grouped.computeIfAbsent(entry.name(), key -> new ArrayList<>())
                        .add(matcher.group());
                }
            } catch (RegexParserUtils.RegexExecutionLimitException exception) {
                budgetFailures.add(new RegexParserUtils.RegexExecutionLimitException(
                    "customRegex entry '" + entry.name() + "': " + exception.getMessage(), exception));
            }
        }

        if (!budgetFailures.isEmpty()) {
            RegexParserUtils.RegexExecutionLimitException headline = budgetFailures.getFirst();

            budgetFailures.stream()
                .skip(1)
                .forEach(headline::addSuppressed);

            throw headline;
        }

        return grouped;
    }

    private record NamedRegex(String name, Pattern pattern) {
    }
}
