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

package com.bytechef.component.ai.llm.stability.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.CREATE_IMAGE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.IMAGE_MESSAGE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.IMAGE_N_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.IMAGE_RESPONSE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.N;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STYLE;
import static com.bytechef.component.ai.llm.stability.constant.StabilityConstants.CFG_SCALE;
import static com.bytechef.component.ai.llm.stability.constant.StabilityConstants.CLIP_GUIDANCE_PRESET;
import static com.bytechef.component.ai.llm.stability.constant.StabilityConstants.HEIGHT;
import static com.bytechef.component.ai.llm.stability.constant.StabilityConstants.SAMPLER;
import static com.bytechef.component.ai.llm.stability.constant.StabilityConstants.STEPS;
import static com.bytechef.component.ai.llm.stability.constant.StabilityConstants.WIDTH;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static org.springframework.ai.stabilityai.api.StabilityAiApi.DEFAULT_IMAGE_MODEL;

import com.bytechef.component.ai.llm.ImageModel;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.ai.stabilityai.StabilityAiImageModel;
import org.springframework.ai.stabilityai.StyleEnum;
import org.springframework.ai.stabilityai.api.StabilityAiApi;
import org.springframework.ai.stabilityai.api.StabilityAiImageOptions;

/**
 * @author Monika Domiter
 * @author Marko Kriskovic
 */
public class StabilityCreateImageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_IMAGE)
        .title("Create Image")
        .description("Create an image using text-to-image models")
        .properties(
            string(MODEL)
                .label("Model")
                .description("The model to use for image generation.")
                .defaultValue(DEFAULT_IMAGE_MODEL)
                .required(false),
            IMAGE_MESSAGE_PROPERTY,
            integer(HEIGHT)
                .label("Height")
                .description(
                    "Height of the image to generate, in pixels, in an increment divisible by 64. Engine-specific dimension validation applies.")
                .defaultValue(512)
                .required(true),
            integer(WIDTH)
                .label("Width")
                .description(
                    "Width of the image to generate, in pixels, in an increment divisible by 64. Engine-specific dimension validation applies.")
                .defaultValue(512)
                .required(true),
            IMAGE_N_PROPERTY,
            IMAGE_RESPONSE_PROPERTY,
            string(STYLE)
                .label("Style")
                .description(
                    "Pass in a style preset to guide the image model towards a particular style. This list of style presets is subject to change.")
                .options(
                    ModelUtils.getEnumOptions(
                        Arrays.stream(StyleEnum.values())
                            .collect(Collectors.toMap(StyleEnum::toString, StyleEnum::name)))),
            integer(STEPS)
                .label("Steps")
                .description("Number of diffusion steps to run. Valid range: 10 to 50.")
                .minValue(10)
                .maxValue(50)
                .advancedOption(true),
            number(CFG_SCALE)
                .label("CFG scale")
                .description(
                    "The strictness level of the diffusion process adherence to the prompt text. Range: 0 to 35.")
                .minValue(0)
                .maxValue(35)
                .advancedOption(true),
            string(CLIP_GUIDANCE_PRESET)
                .label("Clip guidance preset")
                .description(
                    "Pass in a style preset to guide the image model towards a particular style. This list of style presets is subject to change.")
                .advancedOption(true),
            string(SAMPLER)
                .label("Sampler")
                .description(
                    "Which sampler to use for the diffusion process. If this value is omitted, an appropriate sampler will be automatically selected.")
                .advancedOption(true),
            number(SEED)
                .label("Seed")
                .description(
                    "Random noise seed (omit this option or use 0 for a random seed). Valid range: 0 to 4294967295.")
                .numberPrecision(0)
                .minValue(0)
                .maxValue(429496)
                .advancedOption(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("url")
                            .description("URL of the generated image."),
                        string("b64Json")
                            .description("Base64 encoded JSON of the generated image."))))
        .perform(StabilityCreateImageAction::perform);

    private StabilityCreateImageAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return IMAGE_MODEL.getResponse(inputParameters, connectionParameters);
    }

    public static final ImageModel IMAGE_MODEL = (inputParameters, connectionParameters) -> new StabilityAiImageModel(
        new StabilityAiApi(connectionParameters.getString(TOKEN)),
        StabilityAiImageOptions.builder()
            .cfgScale(inputParameters.getFloat(CFG_SCALE))
            .clipGuidancePreset(inputParameters.getString(CLIP_GUIDANCE_PRESET))
            .height(inputParameters.getInteger(HEIGHT))
            .model(inputParameters.getRequiredString(MODEL))
            .N(inputParameters.getInteger(N))
            .responseFormat(inputParameters.getString(RESPONSE_FORMAT))
            .sampler(inputParameters.getString(SAMPLER))
            .seed(inputParameters.getLong(SEED))
            .steps(inputParameters.getInteger(STEPS))
            .stylePreset(StyleEnum.valueOf(inputParameters.getString(STYLE)))
            .width(inputParameters.getInteger(WIDTH))
            .build());
}
