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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.agent.guardrails.util.GuardrailProperties;
import com.bytechef.component.ai.agent.guardrails.util.KeywordMatcher;
import com.bytechef.component.ai.agent.guardrails.util.KeywordMatcher.KeywordMatchResult;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import java.util.List;
import java.util.Optional;

/**
 * Rule-based keyword detection. Flags content whose tokens match an operator-supplied keyword list (case-sensitive or
 * insensitive). Runs at the post-mask stage so preflight masking (PII, URLs) does not hide legitimate keyword matches.
 *
 * @author Ivica Cardic
 */
public final class Keywords {

    public static ClusterElementDefinition<GuardrailCheckFunction> of() {
        return ComponentDsl.<GuardrailCheckFunction>clusterElement("keywords")
            .title("Keywords")
            .description("Flags the input if any listed keyword appears in it.")
            .type(GuardrailCheckFunction.CHECK_FOR_VIOLATIONS)
            .properties(
                array(KEYWORDS)
                    .label("Keywords")
                    .description("A list of words to detect.")
                    .items(string())
                    .required(true),
                bool(CASE_SENSITIVE)
                    .label("Case Sensitive")
                    .description("When off, matching is case-insensitive.")
                    .defaultValue(false),
                GuardrailProperties.failMode())
            .object(() -> new GuardrailCheckFunction() {

                @Override
                public Optional<Violation> apply(String text, GuardrailContext context) {
                    return Keywords.apply(text, context);
                }

                @Override
                public GuardrailStage stage() {
                    // LLM stage (n8n parity): keyword match runs on already-masked text so keyword lists don't need
                    // to anticipate every e-mail local-part or URL fragment variant. Users who need raw-text
                    // keyword matching can place Keywords under SanitizeText instead of CheckForViolations.
                    return GuardrailStage.LLM;
                }
            });
    }

    private Keywords() {
    }

    private static Optional<Violation> apply(String text, GuardrailContext context) {
        Parameters inputParameters = context.inputParameters();
        List<String> keywords = inputParameters.getList(KEYWORDS, String.class);
        boolean caseSensitive = inputParameters.getBoolean(CASE_SENSITIVE, false);

        KeywordMatchResult result = KeywordMatcher.match(text, keywords, caseSensitive);

        if (!result.matched()) {
            return Optional.empty();
        }

        List<String> matchedKeywords = result.matchedKeywords();

        // Raw keywords are carried on matchedSubstrings only. Do NOT duplicate them into info: the advisor's
        // public-view
        // projection reduces matchedSubstrings to matchCount so operator-configured keyword lists never cross the
        // advisor boundary; placing the same list in info would bypass that scrubbing because toPublicView copies info
        // verbatim.
        return Optional.of(Violation.ofMatches("keywords", matchedKeywords));
    }
}
