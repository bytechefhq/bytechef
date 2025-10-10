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
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.CRITERIA;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.CRITERION;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.HIGHEST_SCORE;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.IS_DECIMAL;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.LOWEST_SCORE;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MODEL_NO_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MODEL_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MODEL_URL_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.PROVIDER_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.TEXT;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.universal.text.action.definition.AiTextActionDefinition;
import com.bytechef.component.ai.universal.text.constant.AiTextConstants;
import com.bytechef.component.ai.universal.text.util.AiTextUtils;
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
public class ScoreAction implements AiTextAction {

    public final AiTextActionDefinition actionDefinition;

    private static final String RESPONSE_SCHEMA = """
        {
           "$schema": "https://json-schema.org/draft/2020-12/schema",
           "title": "CriteriaList",
           "type": "object",
           "properties": {
             "criteria": {
               "type": "array",
               "items": {
                 "type": "object",
                 "properties": {
                   "criteriaName": {
                     "type": "string",
                     "description": "Name of the evaluation criteria"
                   },
                   "criteriaValue": {
                     "type": "number",
                     "description": "Numeric value or score for the criteria"
                   }
                 },
                 "required": ["criteriaName", "criteriaValue"],
                 "additionalProperties": false
               }
             }
           },
           "required": ["criteria"],
           "additionalProperties": false
         }
        """;

    public ScoreAction(ApplicationProperties.Ai.Provider provider, PropertyService propertyService) {
        this.actionDefinition = getActionDefinition(provider, propertyService);
    }

    private AiTextActionDefinition getActionDefinition(
        ApplicationProperties.Ai.Provider provider, PropertyService propertyService) {

        return new AiTextActionDefinition(
            action(AiTextConstants.SCORE)
                .title("Score")
                .description("Scores the text based on several criteria")
                .properties(
                    PROVIDER_PROPERTY.apply(provider, propertyService),
                    MODEL_OPTIONS_PROPERTY,
                    MODEL_NO_OPTIONS_PROPERTY,
                    MODEL_URL_PROPERTY,
                    string(TEXT)
                        .label("Text")
                        .description("The text that is to be scored.")
                        .required(true),
                    array(CRITERIA)
                        .label("Criteria")
                        .items(
                            object()
                                .properties(
                                    string(CRITERION)
                                        .label("Criterion")
                                        .description("What is the criterion of the text that is to be scored.")
                                        .exampleValue("Grammar")
                                        .required(true),
                                    number(LOWEST_SCORE)
                                        .label("Lowest Score")
                                        .description("The lowest possible score the text can achieve in this category.")
                                        .defaultValue(1),
                                    number(HIGHEST_SCORE)
                                        .label("Highest Score")
                                        .description(
                                            "The highest possible score the text can achieve in this category.")
                                        .defaultValue(10),
                                    bool(IS_DECIMAL)
                                        .label("Decimal Numbers")
                                        .description("Whether the score should use decimal numbers or not.")
                                        .defaultValue(false)))
                        .required(true),
                    MAX_TOKENS_PROPERTY,
                    TEMPERATURE_PROPERTY)
                .output(
                    (inputParameters, connectionParameters, context) -> OutputResponse.of(
                        context.outputSchema(outputSchema -> outputSchema.getOutputSchema(RESPONSE_SCHEMA)))),
            provider, this, propertyService);
    }

    public Parameters createParameters(Parameters inputParameters) {
        Map<String, Object> modelInputParametersMap = new HashMap<>();

        String systemPrompt =
            "You are an objective text scoring judge. You will receive a text and list of criteria that you will score the text on. Within the list of criteria you will also receive `Lowest Score` which indicates the lowest possible score you can give, `Highest Score` which indicates the highest possible score you can give and `Decimal` which tells you that you will be using decimal numbers if true or only integers if false. Your response will be a JSON array of objects for each criteria containing your score and a short explanation.";

        StringBuilder userBuilder = new StringBuilder();

        userBuilder.append("Text: ")
            .append(inputParameters.getString(TEXT))
            .append("\n");

        List<AiTextUtils.Criteria> criteria = inputParameters.getList(
            CRITERIA, AiTextUtils.Criteria.class, List.of());

        userBuilder.append("Criteria: {")
            .append("\n");

        criteria.forEach(criterion -> userBuilder.append("{")
            .append("\n")
            .append("Criterion: ")
            .append(criterion.criterion())
            .append("\n")
            .append("Lowest Score: ")
            .append(criterion.lowestScore())
            .append("\n")
            .append("Highest Score: ")
            .append(criterion.highestScore())
            .append("\n")
            .append("Decimal: ")
            .append(criterion.isDecimal())
            .append("\n")
            .append("},")
            .append("\n"));

        userBuilder.append("}\n");

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
                "responseSchema", RESPONSE_SCHEMA));

        return ParametersFactory.createParameters(modelInputParametersMap);
    }

}
