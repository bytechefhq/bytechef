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
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.CHUNK_SIZE;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MODEL_NO_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MODEL_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MODEL_URL_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.NUM_RESULTS;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.PROVIDER_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.QUERY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.TEXT;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.universal.text.action.definition.AiTextActionDefinition;
import com.bytechef.component.ai.universal.text.constant.AiTextConstants;
import com.bytechef.component.definition.Parameters;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.definition.BaseOutputDefinition;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marko Kriskovic
 */
public class SimilaritySearchAction implements AiTextAction {

    public final AiTextActionDefinition actionDefinition;

    private static final String RESPONSE_SCHEMA = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "title": "ResultObject",
          "type": "object",
          "properties": {
            "result": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "chunk": {
                    "type": "string",
                    "description": "Text content of the chunk"
                  },
                  "match_score": {
                    "type": "number",
                    "description": "Similarity or relevance score of the chunk"
                  }
                },
                "required": ["chunk", "match_score"],
                "additionalProperties": false
              }
            }
          },
          "required": ["result"],
          "additionalProperties": false
        }
        """;

    public SimilaritySearchAction(
        ApplicationProperties.Ai.Provider provider, PropertyService propertyService) {

        this.actionDefinition = new AiTextActionDefinition(
            action(AiTextConstants.SIMILARITY_SEARCH)
                .title("Similarity Search")
                .description(
                    "Search through a large text and find the parts that are the most relevant. Returns a JSON list.")
                .properties(
                    PROVIDER_PROPERTY.apply(provider, propertyService),
                    MODEL_OPTIONS_PROPERTY,
                    MODEL_NO_OPTIONS_PROPERTY,
                    MODEL_URL_PROPERTY,
                    string(TEXT)
                        .label("Text")
                        .description("The text that is to be queried.")
                        .minLength(100)
                        .required(true),
                    string(QUERY)
                        .label("Query")
                        .description("The term you are looking for in the text.")
                        .required(true),
                    integer(NUM_RESULTS)
                        .label("Number of results")
                        .description("Number of relevant text sections that you want returned.")
                        .minValue(1)
                        .required(true),
                    integer(CHUNK_SIZE)
                        .label("Chunk Size")
                        .description("Number of words around each relevant part of the result.")
                        .minValue(1)
                        .defaultValue(5),
                    MAX_TOKENS_PROPERTY,
                    TEMPERATURE_PROPERTY)
                .output(
                    (inputParameters, connectionParameters, context) -> BaseOutputDefinition.OutputResponse.of(
                        context.outputSchema(outputSchema -> outputSchema.getOutputSchema(RESPONSE_SCHEMA)))),
            provider, this, propertyService);
    }

    public Parameters createParameters(Parameters inputParameters) {
        Map<String, Object> modelInputParametersMap = new HashMap<>();

        String systemPrompt =
            "You are similarity search. You return a JSON list of results that are most similar to the given query. A result contains the attributes 'chunk' and 'match_score'. You will return N results that match the query the best with a chunk size of C which tells the maximum amount of words the result will contain.";

        String userBuilder = "Text: " + inputParameters.getString(TEXT) + "\n"
            + "Query: " + inputParameters.getString(QUERY) + "\n"
            + "N: " + inputParameters.getInteger(NUM_RESULTS) + "\n"
            + "C: " + inputParameters.getInteger(CHUNK_SIZE);

        modelInputParametersMap.put(
            "messages",
            List.of(
                Map.of("content", systemPrompt, ROLE, SYSTEM.name()),
                Map.of("content", userBuilder, ROLE, USER.name())));
        modelInputParametersMap.put("model", inputParameters.getString(MODEL));
        modelInputParametersMap.put(
            "response",
            Map.of(
                "responseFormat", ChatModel.ResponseFormat.JSON,
                "responseSchema", RESPONSE_SCHEMA));

        return ParametersFactory.createParameters(modelInputParametersMap);
    }
}
