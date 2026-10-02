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

import com.bytechef.component.ai.agent.guardrails.util.PiiEntityOptions;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Property;
import com.bytechef.platform.ai.sensitivedata.PiiPatternCatalog;
import com.bytechef.platform.ai.sensitivedata.RegexPiiDetector;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataRedactor;
import com.bytechef.platform.ai.sensitivedata.SensitiveKind;
import com.bytechef.platform.ai.sensitivedata.SensitiveSpan;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.MaskResult;
import com.bytechef.platform.component.definition.ai.agent.guardrails.PreflightCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.PreflightSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
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
                    return applyCheck(text, context);
                }

                @Override
                public MaskResult mask(String text, GuardrailContext context) {
                    return Pii.mask(text, context);
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
                    MaskResult result = Pii.mask(text, context);

                    return result instanceof MaskResult.Masked masked ? masked.text() : text;
                }

                @Override
                public MaskResult mask(String text, GuardrailContext context) {
                    return Pii.mask(text, context);
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
                .options(PiiEntityOptions.getPiiDetectionOptions())
                .displayCondition(TYPE + " == '" + TYPE_SELECTED + "'")
                .required(false)
        };
    }

    private static Optional<Violation> applyCheck(String text, GuardrailContext context) {
        List<PiiPatternCatalog.PiiPattern> patterns = PiiEntityOptions.selectedPatterns(context.inputParameters());
        Set<String> selectedTypes = patterns.stream()
            .map(PiiPatternCatalog.PiiPattern::type)
            .collect(Collectors.toSet());
        String type = context.inputParameters()
            .getString(TYPE, TYPE_ALL);

        List<SensitiveSpan> own = detect(text, patterns);
        List<SensitiveSpan> published = context.publishedInputSpans()
            .stream()
            .filter(span -> span.kind() == SensitiveKind.PII)
            .filter(span -> !TYPE_SELECTED.equals(type) || selectedTypes.contains(span.category()))
            .toList();

        if (own.isEmpty() && published.isEmpty()) {
            return Optional.empty();
        }

        ArrayList<String> entityTypes = Stream.concat(published.stream(), own.stream())
            .map(SensitiveSpan::category)
            .distinct()
            .collect(Collectors.toCollection(ArrayList::new));

        if (published.isEmpty()) {
            List<String> values = own.stream()
                .map(span -> text.substring(span.start(), span.end()))
                .toList();

            return Optional.of(Violation.ofMatches("piiCheck", values, Map.of("entityTypes", entityTypes)));
        }

        return Optional.of(
            Violation.ofSpans("piiCheck", published.size() + own.size(), Map.of("entityTypes", entityTypes)));
    }

    private static MaskResult mask(String text, GuardrailContext context) {
        if (text == null || text.isEmpty()) {
            return MaskResult.unchanged();
        }

        SensitiveDataRedactor.RedactionResult result =
            redactor(PiiEntityOptions.selectedPatterns(context.inputParameters()))
                .redactWithSpans(
                    text, Set.of(SensitiveKind.PII), SensitiveDataRedactor.DEFAULT_MIN_CONFIDENCE, null, List.of(),
                    span -> "<" + span.category() + ">");

        if (result.accepted()
            .isEmpty()) {
            return MaskResult.unchanged();
        }

        return MaskResult.masked(result.text(), text);
    }

    private static List<SensitiveSpan> detect(String text, List<PiiPatternCatalog.PiiPattern> patterns) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        return redactor(patterns)
            .redactWithSpans(text, Set.of(SensitiveKind.PII), SensitiveDataRedactor.DEFAULT_MIN_CONFIDENCE, null)
            .accepted();
    }

    private static SensitiveDataRedactor redactor(List<PiiPatternCatalog.PiiPattern> patterns) {
        return new SensitiveDataRedactor(List.of(new RegexPiiDetector(patterns)));
    }
}
