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
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_INPUT_PROPERTY;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_OUTPUT_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction.CHECK_FOR_VIOLATIONS;
import static com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction.SANITIZE_TEXT;

import com.bytechef.component.ai.agent.guardrails.MissingModelChildException;
import com.bytechef.component.ai.agent.guardrails.util.LlmPiiDetectorUtils;
import com.bytechef.component.ai.agent.guardrails.util.MaskEntityMapUtils;
import com.bytechef.component.ai.agent.guardrails.util.PiiDetectorUtils;
import com.bytechef.component.ai.agent.guardrails.util.RegexParserUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;

/**
 * `LLM PII` is a classifier-based PII detector that complements the rule-based `PII` component. It runs in the **LLM
 * stage** of `CheckForViolations` (or `SanitizeText`) and asks an LLM to identify spans of personally-identifiable
 * information that don't fit clean regex shapes.
 *
 * @author Ivica Cardic
 */
public final class LlmPii {

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

                @Override
                public boolean requiresChatClient() {
                    return true;
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

                @Override
                public boolean requiresChatClient() {
                    return true;
                }
            });
    }

    private static Property[] sharedProperties() {
        return new Property[] {
            VALIDATE_INPUT_PROPERTY,
            VALIDATE_OUTPUT_PROPERTY,
            array(ENTITIES)
                .label("Entities")
                .description("PII entity types the LLM should look for.")
                .items(string())
                .options(PiiDetectorUtils.getPiiDetectionOptions())
        };
    }

    private static Optional<Violation> check(String guardrailName, String text, GuardrailContext context) {
        List<LlmPiiDetectorUtils.Span> spans = detectSpans(guardrailName, text, context);

        if (spans.isEmpty()) {
            return Optional.empty();
        }

        List<String> values = spans.stream()
            .map(LlmPiiDetectorUtils.Span::value)
            .toList();

        ArrayList<String> entityTypes = spans.stream()
            .map(LlmPiiDetectorUtils.Span::type)
            .distinct()
            .collect(Collectors.toCollection(ArrayList::new));

        return Optional.of(Violation.ofMatches(guardrailName, values, Map.of("entityTypes", entityTypes)));
    }

    private static String mask(String guardrailName, String text, GuardrailContext context) {
        List<LlmPiiDetectorUtils.Span> spans = detectSpans(guardrailName, text, context);

        if (spans.isEmpty() || text == null || text.isEmpty()) {
            return text;
        }

        List<LlmPiiDetectorUtils.Span> orderedSpans = spans.stream()
            .filter(span -> span.value() != null && !span.value()
                .isEmpty())
            .filter(LlmPii::containsWordCharacter)
            .sorted(Comparator.comparingInt((LlmPiiDetectorUtils.Span span) -> span.value()
                .length())
                .reversed())
            .toList();

        String result = text;

        for (LlmPiiDetectorUtils.Span span : orderedSpans) {
            Pattern pattern = MaskEntityMapUtils.boundaryAwarePattern(span.value());
            String replacement = Matcher.quoteReplacement("<" + span.type() + ">");

            result = pattern.matcher(RegexParserUtils.bounded(result))
                .replaceAll(replacement);
        }

        return result;
    }

    private static List<LlmPiiDetectorUtils.Span>
        detectSpans(String guardrailName, String text, GuardrailContext context) {
        ChatClient chatClient = context.requireChatClient(() -> new MissingModelChildException(guardrailName));

        Parameters parameters = context.inputParameters();

        List<String> entities = parameters.getList(ENTITIES, String.class, List.of());

        return LlmPiiDetectorUtils.detect(guardrailName, chatClient, text, entities, context.context());
    }

    /**
     * Reject hallucinated spans whose value has no word character — e.g. whitespace-only or pure punctuation. A
     * whitespace-only value produces a boundary-aware pattern that lacks {@code \b} anchors (the edges are non-word),
     * which causes the subsequent {@code replaceAll} to mass-replace every matching whitespace run in the model
     * response and corrupt the output.
     */
    private static boolean containsWordCharacter(LlmPiiDetectorUtils.Span span) {
        String value = span.value();

        for (int index = 0; index < value.length(); index++) {
            if (Character.isLetterOrDigit(value.charAt(index))) {
                return true;
            }
        }

        return false;
    }
}
