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

package com.bytechef.component.azure.openai.action;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.llm.constants.LLMConstants.CREATE_IMAGE;
import static com.bytechef.component.llm.constants.LLMConstants.ENDPOINT;
import static com.bytechef.component.llm.constants.LLMConstants.IMAGE_MESSAGE_PROPERTY;
import static com.bytechef.component.llm.constants.LLMConstants.MODEL;
import static com.bytechef.component.llm.constants.LLMConstants.N;
import static com.bytechef.component.llm.constants.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.llm.constants.LLMConstants.SIZE;
import static com.bytechef.component.llm.constants.LLMConstants.STYLE;
import static com.bytechef.component.llm.constants.LLMConstants.USER;
import static com.bytechef.component.llm.constants.LLMConstants.USER_PROPERTY;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.KeyCredential;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.llm.util.LLMUtils;
import com.bytechef.component.llm.util.interfaces.Image;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.ai.azure.openai.AzureOpenAiImageModel;
import org.springframework.ai.azure.openai.AzureOpenAiImageOptions;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;

/**
 * @author Monika Domiter
 * @author Marko Kriskovic
 */
public class AzureOpenAICreateImageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_IMAGE)
        .title("Create image")
        .description("Create an image using text-to-image models")
        .properties(
            string(MODEL)
                .label("Model")
                .description("The model to use for image generation.")
                .options(LLMUtils.getEnumOptions(
                    Arrays.stream(AzureOpenAiImageOptions.ImageModel.values())
                        .collect(Collectors.toMap(
                            AzureOpenAiImageOptions.ImageModel::getValue, AzureOpenAiImageOptions.ImageModel::getValue,
                            (f, s) -> f))))
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
            integer(N)
                .label("Number of responses")
                .description(
                    "The number of images to generate. Must be between 1 and 10. For dall-e-3, only n=1 is supported..")
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
                    "The style of the generated images. Must be one of vivid or natural. Vivid causes the model to lean towards generating hyper-real and dramatic images. Natural causes the model to produce more natural, less hyper-real looking images. This parameter is only supported for dall-e-3.")
                .options(
                    option("vivid", "vivid"),
                    option("natural", "natural"))
                .advancedOption(true),
            USER_PROPERTY)
        .outputSchema(
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
                                    string("revisedPrompt")))))
        .perform(AzureOpenAICreateImageAction::perform);

    private AzureOpenAICreateImageAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return Image.getResponse(IMAGE, inputParameters, connectionParameters);
    }

    private static final Image IMAGE = new Image() {

        @Override
        public ImageOptions createImageOptions(Parameters inputParameters) {
            Integer[] size = inputParameters.getArray(SIZE, Integer.class);

            return AzureOpenAiImageOptions.builder()
                .withModel(inputParameters.getRequiredString(MODEL))
                .withN(inputParameters.getInteger(N))
                .withHeight(size[1])
                .withWidth(size[0])
                .withStyle(inputParameters.getString(STYLE))
                .withUser(inputParameters.getString(USER))
                .withResponseFormat(inputParameters.getString(RESPONSE_FORMAT))
                .build();
        }

        @Override
        public ImageModel createImageModel(Parameters inputParameters, Parameters connectionParameters) {
            OpenAIClient openAIClient = new OpenAIClientBuilder()
                .credential(new KeyCredential(connectionParameters.getString(TOKEN)))
                .endpoint(connectionParameters.getString(ENDPOINT))
                .buildClient();

            return new AzureOpenAiImageModel(openAIClient,
                (AzureOpenAiImageOptions) createImageOptions(inputParameters));
        }
    };
}
