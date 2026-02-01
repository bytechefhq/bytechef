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

package com.bytechef.component.ai.universal.image.action;

import static com.bytechef.component.ai.llm.Provider.AZURE_OPEN_AI;
import static com.bytechef.component.ai.llm.Provider.OPEN_AI;
import static com.bytechef.component.ai.llm.Provider.STABILITY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.CONTENT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.IMAGE_MESSAGES;
import static com.bytechef.component.ai.llm.constant.LLMConstants.IMAGE_N_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.IMAGE_RESPONSE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PROMPT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PROVIDER;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SIZE;
import static com.bytechef.component.ai.llm.stability.constant.StabilityConstants.HEIGHT;
import static com.bytechef.component.ai.llm.stability.constant.StabilityConstants.WIDTH;
import static com.bytechef.component.ai.universal.image.constant.AiImageConstants.MODEL_NO_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.universal.image.constant.AiImageConstants.MODEL_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.universal.image.constant.AiImageConstants.PROVIDER_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.TEXT_AREA;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.component.ai.llm.openai.definition.Size;
import com.bytechef.component.ai.universal.image.action.definition.AiImageActionDefinition;
import com.bytechef.component.ai.universal.image.constant.AiImageConstants;
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
public class GenerateImageAction implements AiImageAction {

    public static AiImageActionDefinition of(
        ApplicationProperties.Ai.Provider provider, PropertyService propertyService) {

        return new AiImageActionDefinition(
            action(AiImageConstants.GENERATE_IMAGE)
                .title("Generate Image")
                .description("AI generate an image that you prompt.")
                .properties(
                    PROVIDER_PROPERTY.apply(provider, propertyService),
                    MODEL_OPTIONS_PROPERTY,
                    MODEL_NO_OPTIONS_PROPERTY,
                    string(PROMPT)
                        .label("Prompt")
                        .description("Write your prompt for generating an image.")
                        .controlType(TEXT_AREA)
                        .required(true),
                    IMAGE_N_PROPERTY,
                    string(SIZE)
                        .label("Size")
                        .description("The size of the generated images.")
                        .options(
                            option("Dall-e-2 256x256", Size.DALL_E_2_256x256.name()),
                            option("Dall-e-2 512x512", Size.DALL_E_2_512x512.name()),
                            option("1024x1024", Size._1024x1024.name()),
                            option("Dall-e-3 1792x1024", Size.DALL_E_3_1792x1024.name()),
                            option("Dall-e-3 1024x1792", Size.DALL_E_3_1024x1792.name()))
                        .displayCondition("contains({'%s','%s'}, provider)".formatted(AZURE_OPEN_AI, OPEN_AI))
                        .required(true),
                    integer(HEIGHT)
                        .label("Height")
                        .description(
                            "Height of the image to generate, in pixels, in an increment divisible by 64. Engine-specific dimension validation applies.")
                        .defaultValue(512)
                        .displayCondition("provider == '%s'".formatted(STABILITY))
                        .required(true),
                    integer(WIDTH)
                        .label("Width")
                        .description(
                            "Width of the image to generate, in pixels, in an increment divisible by 64. Engine-specific dimension validation applies.")
                        .defaultValue(512)
                        .displayCondition("provider == '%s'".formatted(STABILITY))
                        .required(true),
                    IMAGE_RESPONSE_PROPERTY)
                .output(
                    outputSchema(
                        object()
                            .properties(
                                string("url")
                                    .description("URL of the generated image."),
                                string("b64Json")
                                    .description("Base64 encoded JSON of the generated image.")))),
            provider, new GenerateImageAction(), propertyService);
    }

    private GenerateImageAction() {
    }

    @Override
    public Parameters createParameters(Parameters inputParameters) {
        Map<String, Object> modelInputParametersMap = new HashMap<>();

        Provider provider = Provider.valueOf(inputParameters.getString(PROVIDER));

        if (provider == STABILITY) {
            modelInputParametersMap.put(HEIGHT, inputParameters.getRequired(HEIGHT, Size.class));
            modelInputParametersMap.put(WIDTH, inputParameters.getRequired(WIDTH, Size.class));
        } else {
            modelInputParametersMap.put(SIZE, inputParameters.getRequired(SIZE, Size.class));
        }

        modelInputParametersMap.put(IMAGE_MESSAGES, List.of(Map.of(CONTENT, inputParameters.getString(PROMPT))));
        modelInputParametersMap.put(MODEL, inputParameters.getString(MODEL));

        return ParametersFactory.createParameters(modelInputParametersMap);
    }
}
