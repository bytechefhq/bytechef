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

package com.bytechef.component.ai.agent.guardrails.llmpii.cluster;

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.ENTITIES;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction.CHECK_FOR_VIOLATIONS;
import static com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction.SANITIZE_TEXT;

import com.bytechef.component.ai.agent.guardrails.MissingModelChildException;
import com.bytechef.component.ai.agent.guardrails.util.GuardrailProperties;
import com.bytechef.component.ai.agent.guardrails.util.LlmPiiDetector;
import com.bytechef.component.ai.agent.guardrails.util.MaskEntityMap;
import com.bytechef.component.ai.agent.guardrails.util.PiiDetector;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Property;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.chat.client.ChatClient;

/**
 * @author Ivica Cardic
 */
public final class LlmPii {

    // Distinct cluster-element names so ClusterElementDefinitionService.findFirst() returns the correct variant for
    // each parent (CHECK_FOR_VIOLATIONS vs SANITIZE_TEXT). Also used verbatim as the public violation-guardrail tag so
    // operators can correlate "llmPiiCheck" log lines with the violation metadata they see.
    public static final String LLM_PII_CHECK_NAME = "llmPiiCheck";
    public static final String LLM_PII_SANITIZE_NAME = "llmPiiSanitize";

    private LlmPii() {
    }

    public static ClusterElementDefinition<GuardrailCheckFunction> ofCheck() {
        return ComponentDsl.<GuardrailCheckFunction>clusterElement(LLM_PII_CHECK_NAME)
            .title("LLM PII")
            .description(
                "LLM-assisted detection of personally identifiable information (names, emails, addresses, …).")
            .type(CHECK_FOR_VIOLATIONS)
            .properties(sharedProperties())
            .object(() -> new GuardrailCheckFunction() {

                @Override
                public Optional<Violation> apply(String text, GuardrailContext context) {
                    return check(LLM_PII_CHECK_NAME, text, context);
                }

                @Override
                public GuardrailStage stage() {
                    return GuardrailStage.LLM;
                }
            });
    }

    public static ClusterElementDefinition<GuardrailSanitizerFunction> ofSanitize() {
        return ComponentDsl.<GuardrailSanitizerFunction>clusterElement(LLM_PII_SANITIZE_NAME)
            .title("LLM PII")
            .description("LLM-assisted masking of PII in model responses.")
            .type(SANITIZE_TEXT)
            .properties(sharedProperties())
            .object(() -> new GuardrailSanitizerFunction() {

                @Override
                public String apply(String text, GuardrailContext context) {
                    return mask(LLM_PII_SANITIZE_NAME, text, context);
                }

                @Override
                public GuardrailStage stage() {
                    return GuardrailStage.LLM;
                }
            });
    }

    private static Property[] sharedProperties() {
        return new Property[] {
            array(ENTITIES)
                .label("Entities")
                .description("PII entity types the LLM should look for.")
                .items(string())
                .options(PiiDetector.getPiiDetectionOptions()),
            GuardrailProperties.failMode()
        };
    }

    private static Optional<Violation> check(String guardrailName, String text, GuardrailContext context) {
        List<LlmPiiDetector.Span> spans = detectSpans(guardrailName, text, context);

        if (spans.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(Violation.ofMatches(guardrailName,
            spans.stream()
                .map(LlmPiiDetector.Span::value)
                .toList()));
    }

    private static String mask(String guardrailName, String text, GuardrailContext context) {
        List<LlmPiiDetector.Span> spans = detectSpans(guardrailName, text, context);

        if (spans.isEmpty() || text == null || text.isEmpty()) {
            return text;
        }

        // Mask longest spans first so inner substrings (e.g. "John") don't preempt outer spans that contain them
        // ("John Smith"). Mirrors the longest-match-wins contract of PiiDetector.deduplicateOverlaps and
        // MaskEntityMap.applyTo.
        List<LlmPiiDetector.Span> orderedSpans = spans.stream()
            .filter(span -> span.value() != null && !span.value()
                .isEmpty())
            .sorted(Comparator.comparingInt((LlmPiiDetector.Span span) -> span.value()
                .length())
                .reversed())
            .toList();

        String result = text;

        for (LlmPiiDetector.Span span : orderedSpans) {
            Pattern pattern = MaskEntityMap.boundaryAwarePattern(span.value());
            String replacement = Matcher.quoteReplacement("<" + span.type() + ">");

            result = pattern.matcher(result)
                .replaceAll(replacement);
        }

        return result;
    }

    private static List<LlmPiiDetector.Span> detectSpans(String guardrailName, String text, GuardrailContext context) {
        ChatClient chatClient = context.chatClient()
            .orElseThrow(() -> new MissingModelChildException(guardrailName));

        List<String> entities = context.inputParameters()
            .getList(ENTITIES, String.class, List.of());

        return LlmPiiDetector.detect(guardrailName, chatClient, text, entities);
    }
}
