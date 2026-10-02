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

package com.bytechef.component.ai.universal.text.action;

import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.CUSTOM_PATTERNS;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MASK_MAP;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.PII_DETECTION;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.SENSITIVE_KEYWORDS;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.TEXT;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.universal.text.constant.AiTextConstants;
import com.bytechef.component.ai.universal.text.util.MaskSpans;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.ai.sensitivedata.MatchDeadline;
import com.bytechef.platform.ai.sensitivedata.PiiPatternCatalog;
import com.bytechef.platform.ai.sensitivedata.PiiPatternLabels;
import com.bytechef.platform.ai.sensitivedata.RegexPiiDetector;
import com.bytechef.platform.ai.sensitivedata.RegexSecretDetector;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataRedactor;
import com.bytechef.platform.ai.sensitivedata.SensitiveKind;
import com.bytechef.platform.ai.sensitivedata.SensitiveSpan;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiTokenSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Marko Kriskovic
 * @author Ivica Cardic
 */
public final class MaskAction {

    private static final Map<String, String> LEGACY_TYPE_ALIASES = Map.of(
        "EMAIL", "EMAIL_ADDRESS",
        "PHONE", "PHONE_NUMBER",
        "SSN", "US_SSN");

    private MaskAction() {
    }

    public static ModifiableActionDefinition of() {
        return action(AiTextConstants.MASK)
            .title("Mask")
            .description(
                "Replaces sensitive content with reversible tokens and returns the map to restore them. Secrets are " +
                    "redacted irreversibly and are never in the map.")
            .properties(
                string(TEXT)
                    .label("Text")
                    .description("The text to process.")
                    .required(true),
                array(SENSITIVE_KEYWORDS)
                    .label("Sensitive Keywords")
                    .description("Words or phrases to mask, matched case-insensitively.")
                    .items(string()),
                array(PII_DETECTION)
                    .label("PII Detection")
                    .description("PII types to mask. Leave empty to mask every type in the curated default.")
                    .items(string())
                    .options(getPiiDetectionOptions()),
                array(CUSTOM_PATTERNS)
                    .label("Custom Patterns")
                    .description("Java regular expressions to mask.")
                    .items(string()))
            .output(
                outputSchema(
                    object()
                        .properties(
                            string(TEXT)
                                .description("The text with sensitive content replaced by tokens."),
                            object(MASK_MAP)
                                .description("Mapping of each token to the value it replaced.")
                                .additionalProperties(string()))),
                sampleOutput(
                    Map.of(
                        TEXT, "Hello, my name is [PII_KEYWORD_1_k3n9] and my email is [PII_EMAIL_ADDRESS_2_k3n9].",
                        MASK_MAP, Map.of(
                            "[PII_KEYWORD_1_k3n9]", "John Doe",
                            "[PII_EMAIL_ADDRESS_2_k3n9]", "john@example.com"))))
            .help("", "https://docs.bytechef.io/reference/components/ai-text_v1#mask")
            .perform(MaskAction::perform);
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        String text = inputParameters.getRequiredString(TEXT);
        List<String> keywords = inputParameters.getList(SENSITIVE_KEYWORDS, String.class, List.of());
        List<String> customPatterns = inputParameters.getList(CUSTOM_PATTERNS, String.class, List.of());
        MatchDeadline deadline = MatchDeadline.in(SensitiveDataRedactor.DetectionBounds.DEFAULTS.timeout());

        List<SensitiveSpan> extraCandidates = new ArrayList<>(MaskSpans.keywordSpans(text, keywords));

        extraCandidates.addAll(MaskSpans.customPatternSpans(text, customPatterns, deadline));

        SensitiveDataRedactor redactor = new SensitiveDataRedactor(
            List.of(new RegexPiiDetector(selectedPatterns(inputParameters)), new RegexSecretDetector()));
        PiiTokenSession session = PiiTokenSession.create();

        try {
            SensitiveDataRedactor.RedactionResult result = redactor.tokenizeWithSpans(
                text, Set.of(SensitiveKind.PII, SensitiveKind.SECRET), session,
                SensitiveDataRedactor.DEFAULT_MIN_CONFIDENCE, null, extraCandidates);

            return Map.of(TEXT, result.text(), MASK_MAP, session.tokens());
        } finally {
            session.close();
        }
    }

    public static List<Option<String>> getPiiDetectionOptions() {
        List<Option<String>> options = new ArrayList<>();

        for (PiiPatternCatalog.PiiPattern pattern : PiiPatternCatalog.ALL) {
            options.add(option(PiiPatternLabels.labelOf(pattern.type()), pattern.type()));
        }

        return options;
    }

    private static List<PiiPatternCatalog.PiiPattern> selectedPatterns(Parameters inputParameters) {
        List<String> selected = inputParameters.getList(PII_DETECTION, String.class, List.of());

        if (selected.isEmpty()) {
            return PiiPatternCatalog.curatedDefault();
        }

        List<String> resolved = selected.stream()
            .map(type -> LEGACY_TYPE_ALIASES.getOrDefault(type, type))
            .toList();

        List<PiiPatternCatalog.PiiPattern> patterns = PiiPatternCatalog.filterByTypes(resolved);

        if (patterns.isEmpty()) {
            throw new IllegalArgumentException(
                "AI Text Mask 'PII Detection' selected no known type: " + resolved);
        }

        return patterns;
    }
}
