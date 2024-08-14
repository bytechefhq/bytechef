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

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static constants.LLMConstants.CONTENT;
import static constants.LLMConstants.CREATE_IMAGE;
import static constants.LLMConstants.DEFAULT_SIZE;
import static constants.LLMConstants.ENDPOINT;
import static constants.LLMConstants.HEIGHT;
import static constants.LLMConstants.MESSAGES;
import static constants.LLMConstants.MODEL;
import static constants.LLMConstants.N;
import static constants.LLMConstants.RESPONSE_FORMAT;
import static constants.LLMConstants.STYLE;
import static constants.LLMConstants.USER;
import static constants.LLMConstants.WEIGHT;
import static constants.LLMConstants.WIDTH;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.KeyCredential;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.azure.openai.AzureOpenAiImageModel;
import org.springframework.ai.azure.openai.AzureOpenAiImageOptions;
import org.springframework.ai.image.ImageMessage;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import util.LLMUtils;
import util.records.ImageMessageRecord;

/**
 * @author Monika Domiter
 */
public class AzureOpenAICreateImageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_IMAGE)
        .title("Create image")
        .description("Create an image using text-to-image models")
        .properties(
            array(MESSAGES)
                .label("Messages")
                .description("A list of messages comprising the conversation so far.")
                .items(
                    object().properties(
                        string(CONTENT)
                            .label("Content")
                            .description("The contents of the message.")
                            .required(true),
                        number(WEIGHT)
                            .label("Weight")
                            .description("Weight of the prompt")
                            .required(false)))
                .required(true),
            integer(N)
                .label("n")
                .description(
                    "The number of images to generate. Must be between 1 and 10. For dall-e-3, only n=1 is supported.")
                .defaultValue(1)
                .required(false),
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .description("The model to use for image generation.")
                .options(LLMUtils.getEnumOptions(
                    Arrays.stream(AzureOpenAiImageOptions.ImageModel.values())
                        .collect(Collectors.toMap(
                            AzureOpenAiImageOptions.ImageModel::getValue, AzureOpenAiImageOptions.ImageModel::getValue))))
                .required(true),
//            string(QUALITY)
//                .label("Quality")
//                .description("The quality of the image that will be generated.")
//                .options(
//                    option("standard", "standard"),
//                    option("hd", "hd"))
//                .required(false),
            string(RESPONSE_FORMAT)
                .label("Response format")
                .description("The format in which the generated images are returned.")
                .options(
                    option("url", "url"),
                    option("b64_json", "b64_json"))
                .defaultValue("url")
                .required(false),
            integer(HEIGHT)
                .label("Height")
                .description("The height of the generated images.")
                .defaultValue(DEFAULT_SIZE)
                .required(true),
            integer(WIDTH)
                .label("Width")
                .description("The width of the generated images.")
                .defaultValue(DEFAULT_SIZE)
                .required(true),
            string(STYLE)
                .label("Style")
                .description("The style of the generated images.")
                .options(
                    option("vivid", "vivid"),
                    option("natural", "natural"))
                .required(false),
            string(USER)
                .label("User")
                .description(
                    "A unique identifier representing your end-user, which can help OpenAI to monitor and detect " +
                        "abuse.")
                .required(false))
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

        OpenAIClient openAIClient = new OpenAIClientBuilder()
            .credential(new KeyCredential(connectionParameters.getString(TOKEN)))
            .endpoint(connectionParameters.getString(ENDPOINT))
            .buildClient();

        ImageOptions openAiImageOptions = AzureOpenAiImageOptions.builder()
            .withModel(inputParameters.getRequiredString(MODEL))
            .withN(inputParameters.getInteger(N))
            .withResponseFormat(inputParameters.getString(RESPONSE_FORMAT))
            .withStyle(inputParameters.getString(STYLE))
            .withUser(inputParameters.getString(USER))
            .withHeight(inputParameters.getInteger(HEIGHT))
            .withWidth(inputParameters.getInteger(WIDTH))
            .build();
        ImageModel imageModel = new AzureOpenAiImageModel(openAIClient, (AzureOpenAiImageOptions) openAiImageOptions);

        List<ImageMessageRecord> imageMessageList = inputParameters.getList(MESSAGES, new Context.TypeReference<>() {});
        List<ImageMessage> imageMessage = imageMessageList.stream()
            .map(messageRecord -> new ImageMessage(messageRecord.getContent(), messageRecord.getWeight()))
            .toList();

        ImageResponse response = imageModel.call(new ImagePrompt(imageMessage));
        return response.getResult()
            .getOutput();
    }
}
