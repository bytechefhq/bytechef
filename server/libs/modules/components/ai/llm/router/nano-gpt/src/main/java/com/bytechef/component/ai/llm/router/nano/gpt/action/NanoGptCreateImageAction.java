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

package com.bytechef.component.ai.llm.router.nano.gpt.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.CREATE_IMAGE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.IMAGE_MESSAGE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SIZE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER_PROPERTY;
import static com.bytechef.component.ai.llm.router.nano.gpt.constant.NanoGptConstants.GUIDANCE_SCALE;
import static com.bytechef.component.ai.llm.router.nano.gpt.constant.NanoGptConstants.IMAGE_MODEL_PROPERTY;
import static com.bytechef.component.ai.llm.router.nano.gpt.constant.NanoGptConstants.N;
import static com.bytechef.component.ai.llm.router.nano.gpt.constant.NanoGptConstants.NUM_INFERENCE_STEPS;
import static com.bytechef.component.ai.llm.router.nano.gpt.constant.NanoGptConstants.STRENGTH;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.ImageModel;
import com.bytechef.component.ai.llm.router.nano.gpt.model.NanoGptImageModel;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;

/**
 * @author Marko Kriskovic
 */
public class NanoGptCreateImageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_IMAGE)
        .title("Create Image")
        .description("Create an image using text-to-image models.")
        .properties(
            IMAGE_MODEL_PROPERTY,
            IMAGE_MESSAGE_PROPERTY,
            string(SIZE)
                .label("Size")
                .description("The size of the generated image.")
                .options(
                    option("256×256", "256x256"),
                    option("512×512", "512x512"),
                    option("1024×1024", "1024x1024"))
                .required(false),
            string(RESPONSE_FORMAT)
                .label("Response Format")
                .description("Whether to return a signed URL or base64-encoded bytes.")
                .options(
                    option("URL", "url"),
                    option("Base64 JSON", "b64_json"))
                .defaultValue("url")
                .required(false),
            integer(N)
                .label("Number of Images")
                .description("Number of images to generate.")
                .defaultValue(1)
                .minValue(1)
                .required(false),
            integer(SEED)
                .label("Seed")
                .description("Random seed for reproducible generation.")
                .required(false),
            number(GUIDANCE_SCALE)
                .label("Guidance Scale")
                .description("How closely the model follows the text prompt (0–20).")
                .defaultValue(7.5)
                .minValue(0)
                .maxValue(20)
                .required(false),
            number(STRENGTH)
                .label("Strength")
                .description("How much the output differs from the input image in img2img mode (0–1).")
                .defaultValue(0.8)
                .minValue(0)
                .maxValue(1)
                .required(false),
            integer(NUM_INFERENCE_STEPS)
                .label("Inference Steps")
                .description("Number of denoising steps. More steps produce higher quality but take longer (1–100).")
                .defaultValue(30)
                .minValue(1)
                .maxValue(100)
                .required(false),
            USER_PROPERTY)
        .output(ModelUtils::output)
        .perform(NanoGptCreateImageAction::perform);

    public static final ImageModel IMAGE_MODEL =
        (inputParameters, connectionParameters) -> NanoGptImageModel.builder()
            .apiKey(connectionParameters.getString(TOKEN))
            .model(inputParameters.getRequiredString(MODEL))
            .size(inputParameters.getString(SIZE))
            .responseFormat(inputParameters.getString(RESPONSE_FORMAT, "url"))
            .n(inputParameters.getInteger(N))
            .seed(inputParameters.getInteger(SEED))
            .guidanceScale(inputParameters.getDouble(GUIDANCE_SCALE))
            .strength(inputParameters.getDouble(STRENGTH))
            .numInferenceSteps(inputParameters.getInteger(NUM_INFERENCE_STEPS))
            .user(inputParameters.getString(USER))
            .build();

    private NanoGptCreateImageAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return IMAGE_MODEL.getResponse(inputParameters, connectionParameters);
    }
}
