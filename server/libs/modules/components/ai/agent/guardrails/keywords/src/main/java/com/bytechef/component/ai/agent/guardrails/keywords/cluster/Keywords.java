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

package com.bytechef.component.ai.agent.guardrails.keywords.cluster;

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.CASE_SENSITIVE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.KEYWORDS;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_INPUT_PROPERTY;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_OUTPUT_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.agent.guardrails.util.KeywordMatcherUtils;
import com.bytechef.component.ai.agent.guardrails.util.KeywordMatcherUtils.KeywordMatchResult;
import com.bytechef.component.ai.agent.guardrails.util.MaskEntityMapUtils;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Rule-based keyword detection. Flags or masks content whose tokens match an operator-supplied keyword list
 * (case-sensitive or insensitive). Runs at the PREFLIGHT stage.
 *
 * @author Ivica Cardic
 */
public final class Keywords {

    public static final String KEYWORD_MASK_TYPE = "KEYWORD";

    public static ClusterElementDefinition<GuardrailCheckFunction> ofCheck() {
        return ComponentDsl.<GuardrailCheckFunction>clusterElement("keywordsCheck")
            .title("Keywords")
            .description("Flags the input if any listed keyword appears in it.")
            .type(GuardrailCheckFunction.CHECK_FOR_VIOLATIONS)
            .properties(sharedProperties())
            .object(() -> new PreflightCheckFunction() {

                @Override
                public Optional<Violation> apply(String text, GuardrailContext context) {
                    return applyCheck(text, context.inputParameters());
                }

                @Override
                public MaskResult mask(String text, GuardrailContext context) {
                    return MaskResult.unchanged();
                }
            });
    }

    public static ClusterElementDefinition<GuardrailSanitizerFunction> ofSanitize() {
        return ComponentDsl.<GuardrailSanitizerFunction>clusterElement("keywordsSanitize")
            .title("Keywords")
            .description("Masks any occurrences of listed keywords with a <KEYWORD> placeholder.")
            .type(GuardrailSanitizerFunction.SANITIZE_TEXT)
            .properties(sharedProperties())
            .object(() -> new PreflightSanitizerFunction() {

                @Override
                public String apply(String text, GuardrailContext context) {
                    return maskInline(text, context.inputParameters(), context.context());
                }

                @Override
                public MaskResult mask(String text, GuardrailContext context) {
                    return MaskResult.entities(collectMaskEntities(text, context.inputParameters()));
                }
            });
    }

    private Keywords() {
    }

    private static Property[] sharedProperties() {
        return new Property[] {
            VALIDATE_INPUT_PROPERTY,
            VALIDATE_OUTPUT_PROPERTY,
            array(KEYWORDS)
                .label("Keywords")
                .description("A list of words to detect.")
                .items(string())
                .required(true),
            bool(CASE_SENSITIVE)
                .label("Case Sensitive")
                .description("When off, matching is case-insensitive.")
                .defaultValue(false)
        };
    }

    private static Optional<Violation> applyCheck(String text, Parameters inputParameters) {
        List<String> keywords = inputParameters.getList(KEYWORDS, String.class, List.of());

        if (keywords.isEmpty()) {
            throw new IllegalArgumentException("Keywords guardrail requires at least one entry in 'Keywords'");
        }

        boolean caseSensitive = inputParameters.getBoolean(CASE_SENSITIVE, false);

        KeywordMatchResult result = KeywordMatcherUtils.match(text, keywords, caseSensitive);

        if (!result.matched()) {
            return Optional.empty();
        }

        List<String> matchedKeywords = result.matchedKeywords();

        return Optional.of(Violation.ofMatches("keywordsCheck", matchedKeywords));
    }

    private static Map<String, List<String>> collectMaskEntities(String text, Parameters inputParameters) {
        List<String> keywords = inputParameters.getList(KEYWORDS, String.class, List.of());

        if (keywords.isEmpty()) {
            throw new IllegalArgumentException("Keywords guardrail requires at least one entry in 'Keywords'");
        }

        boolean caseSensitive = inputParameters.getBoolean(CASE_SENSITIVE, false);

        List<String> matchedSubstrings = KeywordMatcherUtils.findMatchedSubstrings(text, keywords, caseSensitive);

        if (matchedSubstrings.isEmpty()) {
            return Map.of();
        }

        return Map.of(KEYWORD_MASK_TYPE, matchedSubstrings);
    }

    private static String maskInline(String text, Parameters inputParameters, Context context) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        Map<String, List<String>> entities = collectMaskEntities(text, inputParameters);

        if (entities.isEmpty()) {
            return text;
        }

        MaskEntityMapUtils maskEntityMap = new MaskEntityMapUtils(context);

        maskEntityMap.merge(entities);

        return maskEntityMap.applyTo(text);
    }
}
