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

package com.bytechef.component.ai.image.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.image.constant.AiImageConstants.MODEL_NO_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.image.constant.AiImageConstants.MODEL_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.image.constant.AiImageConstants.MODEL_PROVIDER_PROPERTY;
import static com.bytechef.component.ai.image.constant.AiImageConstants.PROMPT;
import static com.bytechef.component.ai.image.constant.AiImageConstants.TEXT;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.image.action.definition.AiImageActionDefinition;
import com.bytechef.component.ai.image.constant.AiImageConstants;
import com.bytechef.component.definition.Parameters;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.ParametersFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marko Kriskovic
 */
public class GenerateImageAction implements AiImageAction {

    public final AiImageActionDefinition actionDefinition;

    public GenerateImageAction(ApplicationProperties.Ai.Component component) {
        this.actionDefinition = new AiImageActionDefinition(
            action(AiImageConstants.GENERATE_IMAGE)
                .title("Generate Image")
                .description("AI generate an image that you prompt.")
                .properties(
                    MODEL_PROVIDER_PROPERTY,
                    MODEL_OPTIONS_PROPERTY,
                    MODEL_NO_OPTIONS_PROPERTY,
                    string(PROMPT)
                        .label("Prompt")
                        .description("Write your prompt for generating an image.")
                        .required(true),
                    MAX_TOKENS_PROPERTY,
                    TEMPERATURE_PROPERTY)
                .output(),
            component, this);
    }

    public Parameters createParameters(Parameters inputParameters) {
        Map<String, Object> modelInputParametersMap = new HashMap<>();

        modelInputParametersMap.put("messages",
            List.of(
                Map.of("content", inputParameters.getString(TEXT), "role", "user")));
        modelInputParametersMap.put("model", inputParameters.getString(MODEL));

        return ParametersFactory.createParameters(modelInputParametersMap);
    }
}
