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

package com.bytechef.component.ai.agent.guardrails.pii.cluster;

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.ENTITIES;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.TYPE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.TYPE_ALL;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.TYPE_SELECTED;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_INPUT_PROPERTY;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_OUTPUT_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.agent.guardrails.util.MaskEntityMapUtils;
import com.bytechef.component.ai.agent.guardrails.util.PiiDetectorUtils;
import com.bytechef.component.ai.agent.guardrails.util.PiiDetectorUtils.PiiMatch;
import com.bytechef.component.ai.agent.guardrails.util.PiiDetectorUtils.PiiPattern;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.MaskResult;
import com.bytechef.platform.component.definition.ai.agent.guardrails.PreflightCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.PreflightSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Rule-based PII detection: emails, phone numbers, credit cards, IPs, IBANs, SSNs, and locale-specific identifiers.
 * Configurable via the {@code entities} parameter; runs at the PREFLIGHT stage so detected spans are masked before the
 * LLM stage.
 *
 * @author Ivica Cardic
 */
public final class Pii {

    public static ClusterElementDefinition<GuardrailCheckFunction> ofCheck() {
        return ComponentDsl.<GuardrailCheckFunction>clusterElement("piiCheck")
            .title("PII")
            .description("Flags the input when personally identifiable information is detected.")
            .type(GuardrailCheckFunction.CHECK_FOR_VIOLATIONS)
            .properties(sharedProperties())
            .object(() -> new PreflightCheckFunction() {

                @Override
                public Optional<Violation> apply(String text, GuardrailContext context) {
                    return applyCheck(text, resolvePatterns(context.inputParameters()));
                }

                @Override
                public MaskResult mask(String text, GuardrailContext context) {
                    return MaskResult.entities(collectMaskEntities(text, resolvePatterns(context.inputParameters())));
                }
            });
    }

    public static ClusterElementDefinition<GuardrailSanitizerFunction> ofSanitize() {
        return ComponentDsl.<GuardrailSanitizerFunction>clusterElement("piiSanitize")
            .title("PII")
            .description("Detects Personally Identifiable Information (email, phone number...).")
            .type(GuardrailSanitizerFunction.SANITIZE_TEXT)
            .properties(sharedProperties())
            .object(() -> new PreflightSanitizerFunction() {

                @Override
                public String apply(String text, GuardrailContext context) {
                    return maskInline(text, resolvePatterns(context.inputParameters()), context.context());
                }

                @Override
                public MaskResult mask(String text, GuardrailContext context) {
                    return MaskResult.entities(collectMaskEntities(text, resolvePatterns(context.inputParameters())));
                }
            });
    }

    private Pii() {
    }

    private static Property[] sharedProperties() {
        return new Property[] {
            VALIDATE_INPUT_PROPERTY,
            VALIDATE_OUTPUT_PROPERTY,
            string(TYPE)
                .label("Type")
                .description("Detect all PII types or only selected types.")
                .options(
                    option("All", TYPE_ALL),
                    option("Selected", TYPE_SELECTED))
                .defaultValue(TYPE_ALL)
                .required(true),
            array(ENTITIES)
                .label("Entities")
                .description("Which PII types to scan for.")
                .items(string())
                .options(PiiDetectorUtils.getPiiDetectionOptions())
                .displayCondition(TYPE + " == '" + TYPE_SELECTED + "'")
                .required(false)
        };
    }

    private static List<PiiPattern> resolvePatterns(Parameters params) {
        String type = params.getString(TYPE, TYPE_ALL);

        if (TYPE_SELECTED.equals(type)) {
            List<PiiPattern> selected = PiiDetectorUtils.filterByTypes(params.getList(ENTITIES, String.class));

            if (selected.isEmpty()) {
                throw new IllegalArgumentException(
                    "PII guardrail TYPE='SELECTED' requires at least one entity in 'Entities'.");
            }

            return selected;
        }

        return PiiDetectorUtils.DEFAULT_PII_PATTERNS;
    }

    private static Optional<Violation> applyCheck(String text, List<PiiPattern> patterns) {
        List<PiiMatch> matches = PiiDetectorUtils.detect(text, patterns, List.of());

        if (matches.isEmpty()) {
            return Optional.empty();
        }

        List<String> values = matches.stream()
            .map(PiiMatch::value)
            .toList();

        ArrayList<String> entityTypes = matches.stream()
            .map(PiiMatch::type)
            .distinct()
            .collect(Collectors.toCollection(ArrayList::new));

        return Optional.of(Violation.ofMatches("piiCheck", values, Map.of("entityTypes", entityTypes)));
    }

    private static String maskInline(String text, List<PiiPattern> patterns, Context context) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        Map<String, List<String>> entities = collectMaskEntities(text, patterns);

        if (entities.isEmpty()) {
            return text;
        }

        MaskEntityMapUtils maskEntityMap = new MaskEntityMapUtils(context);

        maskEntityMap.merge(entities);

        return maskEntityMap.applyTo(text);
    }

    private static Map<String, List<String>> collectMaskEntities(String text, List<PiiPattern> patterns) {
        List<PiiMatch> matches = PiiDetectorUtils.detect(text, patterns, List.of());

        if (matches.isEmpty()) {
            return Map.of();
        }

        Map<String, LinkedHashSet<String>> grouped = new LinkedHashMap<>();

        for (PiiMatch match : matches) {
            grouped.computeIfAbsent(match.type(), key -> new LinkedHashSet<>())
                .add(match.value());
        }

        Map<String, List<String>> result = new LinkedHashMap<>();

        grouped.forEach((type, values) -> result.put(type, new ArrayList<>(values)));

        return result;
    }
}
