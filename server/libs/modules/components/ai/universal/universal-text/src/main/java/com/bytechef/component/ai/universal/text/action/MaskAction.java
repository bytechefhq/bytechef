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

import static com.bytechef.component.ai.llm.ChatModel.Role.SYSTEM;
import static com.bytechef.component.ai.llm.ChatModel.Role.USER;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.ROLE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.CUSTOM_PATTERNS;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MASK_MAP;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MODEL_NO_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MODEL_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MODEL_URL_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.PII_DETECTION;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.PROVIDER_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.SENSITIVE_KEYWORDS;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.TEXT;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.universal.text.action.definition.AiTextActionDefinition;
import com.bytechef.component.ai.universal.text.constant.AiTextConstants;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marko Kriskovic
 */
public class MaskAction implements AiTextAction {

    public static AiTextActionDefinition of(
        ApplicationProperties.Ai.Provider provider, PropertyService propertyService) {

        return new AiTextActionDefinition(
            action(AiTextConstants.MASK)
                .title("Mask")
                .description("Uses AI to detect and redact sensitive content from text.")
                .properties(
                    PROVIDER_PROPERTY.apply(provider, propertyService),
                    MODEL_OPTIONS_PROPERTY,
                    MODEL_NO_OPTIONS_PROPERTY,
                    MODEL_URL_PROPERTY,
                    string(TEXT)
                        .label("Text")
                        .description("The text to process.")
                        .required(true),
                    array(SENSITIVE_KEYWORDS)
                        .label("Sensitive Keywords")
                        .description("Words or phrases to detect and redact.")
                        .items(string()),
                    array(PII_DETECTION)
                        .label("PII Detection")
                        .description(
                            "Detect personally identifiable information (email, phone, SSN, credit card, IP address).")
                        .items(string())
                        .options(getPiiDetectionOptions()),
                    array(CUSTOM_PATTERNS)
                        .label("Custom Patterns")
                        .description("Custom patterns to detect and redact.")
                        .items(string()),
                    MAX_TOKENS_PROPERTY,
                    TEMPERATURE_PROPERTY)
                .output(
                    outputSchema(
                        object()
                            .properties(
                                string(TEXT)
                                    .description("The text with sensitive content redacted."),
                                object(MASK_MAP)
                                    .description("Mapping of mask tokens to their original values.")
                                    .additionalProperties(string()))),
                    sampleOutput(
                        Map.of(
                            TEXT, "Hello, my name is [REDACTED_1] and my email is [EMAIL_1].",
                            MASK_MAP, Map.of("[REDACTED_1]", "John Doe", "[EMAIL_1]", "john@example.com")))),
            provider, new MaskAction(), propertyService);
    }

    private MaskAction() {
    }

    public static List<Option<String>> getPiiDetectionOptions() {
        return List.of(
            ComponentDsl.option("Email address", "EMAIL"),
            ComponentDsl.option("Phone number", "PHONE"),
            ComponentDsl.option("Credit card number", "CREDIT_CARD"),
            ComponentDsl.option("IP address", "IP_ADDRESS"),
            ComponentDsl.option("US Social Security Number", "SSN"));
    }

    @Override
    public Parameters createParameters(Parameters inputParameters) {
        Map<String, Object> modelInputParametersMap = new HashMap<>();

        String systemPrompt =
            "You are a content redaction specialist. Detect and replace sensitive information in the given text with mask tokens. "
                + "Increment the number suffix (_1, _2, ...) for each unique occurrence of the same type so every masking is unique. "
                + "Respond with a JSON object with two fields: \"text\" (the redacted text) and \"maskMap\" (an object mapping each mask token to the original value it replaced).";

        StringBuilder userBuilder = new StringBuilder();

        userBuilder.append("Text: ")
            .append(inputParameters.getString(TEXT))
            .append("\n\nInstructions:\n");

        List<String> keywords = inputParameters.getList(SENSITIVE_KEYWORDS, String.class, List.of());

        if (!keywords.isEmpty()) {
            userBuilder.append("- Replace each of the following sensitive keywords with [REDACTED_N]: ")
                .append(String.join(", ", keywords))
                .append("\n");
        }

        List<String> selectedPiiTypes = inputParameters.getList(PII_DETECTION, String.class, List.of());

        for (String piiType : selectedPiiTypes) {
            userBuilder.append("- Replace all ")
                .append(piiType.toLowerCase()
                    .replace("_", " "))
                .append(" values with [")
                .append(piiType)
                .append("_N]\n");
        }

        List<String> customPatterns = inputParameters.getList(CUSTOM_PATTERNS, String.class, List.of());

        if (!customPatterns.isEmpty()) {
            userBuilder.append("- Replace all matches of the following patterns with [CUSTOM_N]: ")
                .append(String.join(", ", customPatterns))
                .append("\n");
        }

        String responseSchema = """
            {
              "type": "object",
              "properties": {
                "text": {
                  "type": "string"
                },
                "maskMap": {
                  "type": "object",
                  "additionalProperties": {
                    "type": "string"
                  }
                }
              },
              "required": ["text", "maskMap"]
            }
            """;

        modelInputParametersMap.put(
            "messages",
            List.of(
                Map.of("content", systemPrompt, ROLE, SYSTEM.name()),
                Map.of("content", userBuilder.toString(), ROLE, USER.name())));
        modelInputParametersMap.put("model", inputParameters.getString(MODEL));
        modelInputParametersMap.put(
            "response",
            Map.of(
                "responseFormat", ChatModel.ResponseFormat.JSON,
                "responseSchema", responseSchema));

        return ParametersFactory.create(modelInputParametersMap);
    }
}
