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

package com.bytechef.component.openai.action;

import static com.bytechef.component.ai.llm.ImageModel.Quality.HD;
import static com.bytechef.component.ai.llm.ImageModel.Quality.STANDARD;
import static com.bytechef.component.ai.llm.ImageModel.ResponseFormat.B64_JSON;
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
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.openai.constant.OpenAiConstants.QUALITY;

import com.bytechef.component.ai.llm.ImageModel;
import com.bytechef.component.ai.llm.ImageModel.Quality;
import com.bytechef.component.ai.llm.ImageModel.ResponseFormat;
import com.bytechef.component.ai.llm.ImageModel.Style;
import com.bytechef.component.ai.llm.util.LLMUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.openai.constant.OpenAiConstants;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.retry.support.RetryTemplate;

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
            object(SIZE)
                .label("Size")
                .description("The size of the generated images.")
                .options(
                    option("Dall-e-2 256x256", new Integer[] {
                        256, 256
                    }),
                    option("Dall-e-2 512x512", new Integer[] {
                        512, 512
                    }),
                    option("1024x1024", new Integer[] {
                        1024, 1024
                    }),
                    option("Dall-e-3 1792x1024", new Integer[] {
                        1792, 1024
                    }),
                    option("Dall-e-3 1024x1792", new Integer[] {
                        1024, 1792
                    }))
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
                        integer("created"),
                        array("data")
                            .items(
                                object()
                                    .properties(
                                        string("url")
                                            .controlType(Property.ControlType.URL),
                                        string("b64Json"),
                                        string("revisedPrompt"))))))
        .perform(OpenAiCreateImageAction::perform);

    public static final ImageModel IMAGE_MODEL = (inputParameters, connectionParameters) -> {
        ResponseFormat responseFormat = inputParameters.get(RESPONSE_FORMAT, ResponseFormat.class, URL);
        Integer[] size = inputParameters.getArray(SIZE, Integer.class);
        Style style = inputParameters.get(STYLE, Style.class, NATURAL);
        Quality quality = inputParameters.get(QUALITY, Quality.class, STANDARD);

        return new OpenAiImageModel(
            new OpenAiImageApi(connectionParameters.getString(TOKEN)),
            OpenAiImageOptions.builder()
                .height(size[1])
                .model(inputParameters.getRequiredString(MODEL))
                .N(inputParameters.getInteger(N))
                .responseFormat(responseFormat.getValue())
                .quality(quality.getValue())
                .style(style.getValue())
                .user(inputParameters.getString(USER))
                .width(size[0])
                .build(),
            new RetryTemplate());
    };

    private OpenAiCreateImageAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return IMAGE_MODEL.getResponse(inputParameters, connectionParameters);
    }
}
