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
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MASK_MAP;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MODEL_NO_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MODEL_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MODEL_URL_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.PROVIDER_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.TEXT;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.universal.text.action.definition.AiTextActionDefinition;
import com.bytechef.component.ai.universal.text.constant.AiTextConstants;
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
public class UnmaskAction implements AiTextAction {

    private static final String SYSTEM_PROMPT =
        "Replace the redacted content with values in the map. Return only the unredacted text with no additional " +
            "commentary.";

    public static AiTextActionDefinition of(
        ApplicationProperties.Ai.Provider provider, PropertyService propertyService) {

        return new AiTextActionDefinition(
            action(AiTextConstants.UNMASK)
                .title("Unmask")
                .description("Uses AI and a map of masking entities to unmask the text.")
                .properties(
                    PROVIDER_PROPERTY.apply(provider, propertyService),
                    MODEL_OPTIONS_PROPERTY,
                    MODEL_NO_OPTIONS_PROPERTY,
                    MODEL_URL_PROPERTY,
                    string(TEXT)
                        .label("Text")
                        .description("The text to process.")
                        .required(true),
                    object(MASK_MAP)
                        .label("Masked map")
                        .description("Map of masked entities to replace with values.")
                        .additionalProperties(string()),
                    MAX_TOKENS_PROPERTY,
                    TEMPERATURE_PROPERTY)
                .output(
                    outputSchema(string().description("The text with sensitive content redacted.")),
                    sampleOutput("Hello, my name is [REDACTED] and my email is [EMAIL].")),
            provider, new UnmaskAction(), propertyService);
    }

    private UnmaskAction() {
    }

    @Override
    public Parameters createParameters(Parameters inputParameters) {
        Map<String, Object> modelInputParametersMap = new HashMap<>();

        StringBuilder userPrompt = new StringBuilder();

        userPrompt.append("Text: ")
            .append(inputParameters.getString(TEXT))
            .append("\n\nInstructions:\n");

        Map<String, String> maskMap = inputParameters.getMap(MASK_MAP, String.class, Map.of());

        if (!maskMap.isEmpty()) {
            userPrompt.append("Mask Map: ")
                .append("\n");

            maskMap.forEach((key, value) -> userPrompt.append("- ")
                .append(key)
                .append(": ")
                .append(value)
                .append("\n"));
        }

        modelInputParametersMap.put(
            "messages",
            List.of(
                Map.of("content", SYSTEM_PROMPT, ROLE, SYSTEM.name()),
                Map.of("content", userPrompt.toString(), ROLE, USER.name())));
        modelInputParametersMap.put("model", inputParameters.getString(MODEL));

        return ParametersFactory.create(modelInputParametersMap);
    }
}
