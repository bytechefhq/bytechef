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
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.CATEGORIES;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.EXAMPLES;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MODEL_NO_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MODEL_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MODEL_URL_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.PROVIDER_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.TEXT;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.universal.text.action.definition.AiTextActionDefinition;
import com.bytechef.component.ai.universal.text.constant.AiTextConstants;
import com.bytechef.component.definition.Parameters;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Marko Kriskovic
 */
public class ClassifyTextAction implements AiTextAction {

    public final AiTextActionDefinition actionDefinition;

    public ClassifyTextAction(ApplicationProperties.Ai.Provider provider, PropertyService propertyService) {
        this.actionDefinition = getActionDefinition(provider, propertyService);
    }

    private AiTextActionDefinition getActionDefinition(
        ApplicationProperties.Ai.Provider provider, PropertyService propertyService) {

        return new AiTextActionDefinition(
            action(AiTextConstants.CLASSIFY_TEXT)
                .title("Classify Text")
                .description("AI reads, analyzes and classifies your text into one of defined categories.")
                .properties(
                    PROVIDER_PROPERTY.apply(provider, propertyService),
                    MODEL_OPTIONS_PROPERTY,
                    MODEL_NO_OPTIONS_PROPERTY,
                    MODEL_URL_PROPERTY,
                    string(TEXT)
                        .label("Text")
                        .description("The text that is to be classified.")
                        .required(true),
                    array(CATEGORIES)
                        .label("Categories")
                        .description("A list of categories that the model can choose from.")
                        .items(string())
                        .required(true),
                    object(EXAMPLES)
                        .label("Examples")
                        .description(
                            "You can classify a few samples, to guide your model on how to classify the real data.")
                        .additionalProperties(string()),
                    MAX_TOKENS_PROPERTY,
                    TEMPERATURE_PROPERTY)
                .output(),
            provider, this, propertyService);
    }

    public Parameters createParameters(Parameters inputParameters) {
        Map<String, Object> modelInputParametersMap = new HashMap<>();

        String systemPrompt =
            "You will receive a list of categories, text and examples. You will choose which of the given categories fits the given text the most. Your response will only be the chosen category.";

        String userBuilder = "List of categories: " + inputParameters.getList(CATEGORIES) + "\nText: " +
            inputParameters.getString(TEXT);

        Map<String, ?> exampleMap = inputParameters.getMap(EXAMPLES, String.class, Map.of());

        if (!exampleMap.isEmpty()) {
            userBuilder = userBuilder + "\nExamples: " +
                exampleMap.entrySet()
                    .stream()
                    .map(Map.Entry::toString)
                    .collect(Collectors.joining(";"));
        }

        modelInputParametersMap.put(
            "messages",
            List.of(
                Map.of("content", systemPrompt, ROLE, SYSTEM.name()),
                Map.of("content", userBuilder, ROLE, USER.name())));
        modelInputParametersMap.put("model", inputParameters.getString(MODEL));
        modelInputParametersMap.put("response", Map.of(
            "responseFormat", ChatModel.ResponseFormat.TEXT));

        return ParametersFactory.createParameters(modelInputParametersMap);
    }
}
