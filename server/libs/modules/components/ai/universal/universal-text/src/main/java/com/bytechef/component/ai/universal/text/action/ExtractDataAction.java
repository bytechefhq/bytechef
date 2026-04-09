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
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_SCHEMA;
import static com.bytechef.component.ai.llm.constant.LLMConstants.ROLE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.ADDITIONAL_CONTEXT;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MODEL_NO_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MODEL_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MODEL_URL_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.PROVIDER_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.TEXT;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.JSON_SCHEMA_BUILDER;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.universal.text.action.definition.AiTextActionDefinition;
import com.bytechef.component.ai.universal.text.constant.AiTextConstants;
import com.bytechef.component.definition.Parameters;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marko Kriskovic
 */
public class ExtractDataAction implements AiTextAction {

    public static AiTextActionDefinition of(
        ApplicationProperties.Ai.Provider provider, PropertyService propertyService) {

        return new AiTextActionDefinition(
            action(AiTextConstants.EXTRACT_DATA)
                .title("Extract Data")
                .description("Uses AI to pull specific structured information from unstructured text content.")
                .properties(
                    PROVIDER_PROPERTY.apply(provider, propertyService),
                    MODEL_OPTIONS_PROPERTY,
                    MODEL_NO_OPTIONS_PROPERTY,
                    MODEL_URL_PROPERTY,
                    string(TEXT)
                        .label("Text")
                        .description("The text content to extract data from.")
                        .required(true),
                    string(RESPONSE_SCHEMA)
                        .label("Response Schema")
                        .description("Define desired structure for the structured data response.")
                        .controlType(JSON_SCHEMA_BUILDER)
                        .required(true),
                    string(ADDITIONAL_CONTEXT)
                        .label("Additional Context")
                        .description("Extra information to guide the extraction process."),
                    MAX_TOKENS_PROPERTY,
                    TEMPERATURE_PROPERTY)
                .output(
                    (inputParameters, connectionParameters, context) -> {
                        String responseSchema = inputParameters.getString(RESPONSE_SCHEMA);

                        return OutputResponse.of(
                            context.outputSchema(outputSchema -> outputSchema.getOutputSchema(responseSchema)));
                    }),
            provider, new ExtractDataAction(), propertyService);
    }

    private ExtractDataAction() {
    }

    @Override
    public Parameters createParameters(Parameters inputParameters) {
        Map<String, Object> modelInputParametersMap = new HashMap<>();

        String additionalContext = inputParameters.getString(ADDITIONAL_CONTEXT);

        String systemPrompt =
            "You are a data extraction specialist. Extract the requested data from the given text and return it as a JSON object matching the specified schema. If a field cannot be found in the text, use null for its value.";

        StringBuilder userBuilder = new StringBuilder();

        userBuilder.append("Text: ")
            .append(inputParameters.getString(TEXT));

        if (additionalContext != null && !additionalContext.isEmpty()) {
            userBuilder.append("\n\nAdditional context: ")
                .append(additionalContext);
        }

        String responseSchema = inputParameters.getRequiredString(RESPONSE_SCHEMA);

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
