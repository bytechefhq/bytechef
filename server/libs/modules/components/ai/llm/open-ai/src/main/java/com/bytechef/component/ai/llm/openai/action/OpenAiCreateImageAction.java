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

package com.bytechef.component.ai.llm.openai.action;

import static com.bytechef.component.ai.llm.ImageModel.Quality.HD;
import static com.bytechef.component.ai.llm.ImageModel.Quality.STANDARD;
import static com.bytechef.component.ai.llm.ImageModel.ResponseFormat.URL;
import static com.bytechef.component.ai.llm.ImageModel.Style.NATURAL;
import static com.bytechef.component.ai.llm.ImageModel.Style.VIVID;
import static com.bytechef.component.ai.llm.constant.LLMConstants.CREATE_IMAGE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.IMAGE_MESSAGE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.IMAGE_N_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.IMAGE_RESPONSE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.N;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SIZE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STYLE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER_PROPERTY;
import static com.bytechef.component.ai.llm.openai.constant.OpenAiConstants.QUALITY;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.ImageModel;
import com.bytechef.component.ai.llm.ImageModel.Quality;
import com.bytechef.component.ai.llm.ImageModel.ResponseFormat;
import com.bytechef.component.ai.llm.ImageModel.Style;
import com.bytechef.component.ai.llm.openai.constant.OpenAiConstants;
import com.bytechef.component.ai.llm.openai.definition.Size;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.core.retry.RetryTemplate;

/**
 * @author Monika Domiter
 * @author Marko Kriskovic
 */
public class OpenAiCreateImageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_IMAGE)
        .title("Create Image")
        .description("Create an image using text-to-image models")
        .properties(
            string(MODEL)
                .label("Model")
                .description("The model to use for image generation.")
                .options(OpenAiConstants.IMAGE_MODELS)
                .required(true),
            IMAGE_MESSAGE_PROPERTY,
            string(SIZE)
                .label("Size")
                .description("The size of the generated images.")
                .options(
                    option("Dall-e-2 256x256", Size.DALL_E_2_256x256.name()),
                    option("Dall-e-2 512x512", Size.DALL_E_2_512x512.name()),
                    option("1024x1024", Size._1024x1024.name()),
                    option("Dall-e-3 1792x1024", Size.DALL_E_3_1792x1024.name()),
                    option("Dall-e-3 1024x1792", Size.DALL_E_3_1024x1792.name()))
                .required(true),
            IMAGE_N_PROPERTY,
            IMAGE_RESPONSE_PROPERTY,
            string(QUALITY)
                .label("Quality")
                .description("The quality of the image that will be generated.")
                .options(
                    option("Standard", STANDARD.name()),
                    option("HD", HD.name()))
                .defaultValue(STANDARD.name())
                .advancedOption(true),
            string(STYLE)
                .label("Style")
                .description(
                    "The style of the generated images. Must be one of vivid or natural. Vivid causes the model to lean towards generating hyper-real and dramatic images. Natural causes the model to produce more natural, less hyper-real looking images. This parameter is only supported for dall-e-3.")
                .options(
                    option("vivid", VIVID.name()),
                    option("natural", NATURAL.name()))
                .defaultValue(NATURAL.name())
                .displayCondition("model == 'dall-e-3'")
                .advancedOption(true),
            USER_PROPERTY)
        .output(
            outputSchema(
                object()
                    .properties(
                        string("url")
                            .description("URL of the generated image."),
                        string("b64Json")
                            .description("Base64 encoded JSON of the generated image."))))
        .perform(OpenAiCreateImageAction::perform);

    public static final ImageModel IMAGE_MODEL = (inputParameters, connectionParameters) -> {
        ResponseFormat responseFormat = inputParameters.get(RESPONSE_FORMAT, ResponseFormat.class, URL);
        Size size = inputParameters.getRequired(SIZE, Size.class);
        Style style = inputParameters.get(STYLE, Style.class, NATURAL);
        Quality quality = inputParameters.get(QUALITY, Quality.class, STANDARD);

        return new OpenAiImageModel(
            OpenAiImageApi.builder()
                .apiKey(connectionParameters.getString(TOKEN))
                .restClientBuilder(ModelUtils.getRestClientBuilder())
                .build(),
            OpenAiImageOptions.builder()
                .height(size.getDimensions()[1])
                .model(inputParameters.getRequiredString(MODEL))
                .N(inputParameters.getInteger(N))
                .responseFormat(responseFormat.getValue())
                .quality(quality.getValue())
                .style(style.getValue())
                .user(inputParameters.getString(USER))
                .width(size.getDimensions()[0])
                .build(),
            new RetryTemplate());
    };

    private OpenAiCreateImageAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return IMAGE_MODEL.getResponse(inputParameters, connectionParameters);
    }
}
