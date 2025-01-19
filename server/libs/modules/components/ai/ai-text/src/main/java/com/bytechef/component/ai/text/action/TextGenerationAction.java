/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.ai.text.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.text.constant.AiTextConstants.MODEL_NO_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.text.constant.AiTextConstants.MODEL_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.text.constant.AiTextConstants.MODEL_URL_PROPERTY;
import static com.bytechef.component.ai.text.constant.AiTextConstants.PROMPT;
import static com.bytechef.component.ai.text.constant.AiTextConstants.PROVIDER_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.text.action.definition.AiTextActionDefinition;
import com.bytechef.component.ai.text.constant.AiTextConstants;
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
public class TextGenerationAction implements AiTextAction {

    public final AiTextActionDefinition actionDefinition;

    public TextGenerationAction(ApplicationProperties.Ai.Provider provider, PropertyService propertyService) {
        this.actionDefinition = new AiTextActionDefinition(
            action(AiTextConstants.TEXT_GENERATION)
                .title("Text Generation")
                .description("AI generates text based on the given prompt.")
                .properties(
                    PROVIDER_PROPERTY.apply(provider, propertyService),
                    MODEL_OPTIONS_PROPERTY,
                    MODEL_NO_OPTIONS_PROPERTY,
                    MODEL_URL_PROPERTY,
                    string(PROMPT)
                        .label("Prompt")
                        .description("Write your prompt for generating text.")
                        .required(true),
                    MAX_TOKENS_PROPERTY,
                    TEMPERATURE_PROPERTY)
                .output(),
            provider, this, propertyService);
    }

    public Parameters createParameters(Parameters inputParameters) {
        Map<String, Object> modelInputParametersMap = new HashMap<>();

        modelInputParametersMap.put(
            "messages", List.of(Map.of("content", inputParameters.getString(PROMPT), "role", "user")));
        modelInputParametersMap.put("model", inputParameters.getString(MODEL));

        return ParametersFactory.createParameters(modelInputParametersMap);
    }
}
