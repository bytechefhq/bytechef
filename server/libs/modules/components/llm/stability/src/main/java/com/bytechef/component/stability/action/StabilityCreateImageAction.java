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

package com.bytechef.component.stability.action;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.llm.constant.LLMConstants.CREATE_IMAGE;
import static com.bytechef.component.llm.constant.LLMConstants.IMAGE_MESSAGE_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.llm.constant.LLMConstants.N;
import static com.bytechef.component.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.llm.constant.LLMConstants.SEED;
import static com.bytechef.component.llm.constant.LLMConstants.STYLE;
import static com.bytechef.component.stability.constant.StabilityConstants.CFG_SCALE;
import static com.bytechef.component.stability.constant.StabilityConstants.CLIP_GUIDANCE_PRESET;
import static com.bytechef.component.stability.constant.StabilityConstants.HEIGHT;
import static com.bytechef.component.stability.constant.StabilityConstants.SAMPLER;
import static com.bytechef.component.stability.constant.StabilityConstants.STEPS;
import static com.bytechef.component.stability.constant.StabilityConstants.WIDTH;
import static org.springframework.ai.stabilityai.api.StabilityAiApi.DEFAULT_IMAGE_MODEL;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.llm.Image;
import com.bytechef.component.llm.util.LLMUtils;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
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
            integer(N)
                .label("Number of responses")
                .description("The number of images to be generated. Must be between 1 and 10.")
                .defaultValue(1)
                .minValue(1)
                .maxValue(10)
                .advancedOption(true),
            string(RESPONSE_FORMAT)
                .label("Response format")
                .description("The format in which the generated images are returned.")
                .options(
                    option("url", "url"),
                    option("b64_json", "b64_json"))
                .defaultValue("url")
                .advancedOption(true),
            string(STYLE)
                .label("Style")
                .description(
                    "Pass in a style preset to guide the image model towards a particular style. This list of style presets is subject to change.")
                .options(
                    LLMUtils.getEnumOptions(
                        Arrays.stream(StyleEnum.values())
                            .collect(
                                Collectors.toMap(
                                    StyleEnum::toString, StyleEnum::toString, (f, s) -> f)))),
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
                        integer("created"),
                        array("data")
                            .items(
                                object()
                                    .properties(
                                        string("url")
                                            .controlType(Property.ControlType.URL),
                                        string("b64Json"),
                                        string("revisedPrompt"))))))
        .perform(StabilityCreateImageAction::perform);

    private StabilityCreateImageAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return Image.getResponse(IMAGE, inputParameters, connectionParameters);
    }

    private static final Image IMAGE = new Image() {

        @Override
        public ImageOptions createImageOptions(Parameters inputParameters) {
            return StabilityAiImageOptions.builder()
                .withModel(inputParameters.getRequiredString(MODEL))
                .withN(inputParameters.getInteger(N))
                .withHeight(inputParameters.getInteger(HEIGHT))
                .withWidth(inputParameters.getInteger(WIDTH))
                .withStylePreset(inputParameters.getString(STYLE))
                .withSteps(inputParameters.getInteger(STEPS))
                .withCfgScale(inputParameters.getFloat(CFG_SCALE))
                .withClipGuidancePreset(inputParameters.getString(CLIP_GUIDANCE_PRESET))
                .withSampler(inputParameters.getString(SAMPLER))
                .withSeed(inputParameters.getLong(SEED))
                .build();
        }

        @Override
        public ImageModel createImageModel(Parameters inputParameters, Parameters connectionParameters) {
            return new StabilityAiImageModel(
                new StabilityAiApi(connectionParameters.getString(TOKEN)),
                (StabilityAiImageOptions) createImageOptions(inputParameters));
        }
    };
}
