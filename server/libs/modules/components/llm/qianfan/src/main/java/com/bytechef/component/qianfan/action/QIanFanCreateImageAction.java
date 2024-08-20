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

package com.bytechef.component.qianfan.action;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.qianfan.constant.QIanFanConstants;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.qianfan.QianFanImageModel;
import org.springframework.ai.qianfan.QianFanImageOptions;
import org.springframework.ai.qianfan.api.QianFanApi;
import org.springframework.ai.qianfan.api.QianFanImageApi;
import org.springframework.retry.support.RetryTemplate;
import util.LLMUtils;
import util.interfaces.Image;

import java.util.Arrays;
import java.util.stream.Collectors;

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
import static constants.LLMConstants.HEIGHT;
import static constants.LLMConstants.MESSAGES;
import static constants.LLMConstants.MODEL;
import static constants.LLMConstants.N;
import static constants.LLMConstants.QUALITY;
import static constants.LLMConstants.RESPONSE_FORMAT;
import static constants.LLMConstants.STYLE;
import static constants.LLMConstants.USER;
import static constants.LLMConstants.USER_PROPERTY;
import static constants.LLMConstants.WEIGHT;
import static constants.LLMConstants.WIDTH;

/**
 * @author Monika Domiter
 */
public class QIanFanCreateImageAction {

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
                .description("The model to use for image generation.")
                .options(LLMUtils.getEnumOptions(
                    Arrays.stream(QianFanImageApi.ImageModel.values())
                        .collect(Collectors.toMap(
                            QianFanImageApi.ImageModel::getValue, QianFanImageApi.ImageModel::getValue, (f,s)->f))))
                .required(true),
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
        .perform(QIanFanCreateImageAction::perform);

    private QIanFanCreateImageAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return Image.getResponse(IMAGE, inputParameters, connectionParameters);
    }

    public static final Image IMAGE = new Image() {
        @Override
        public ImageOptions createImageOptions(Parameters inputParameters) {
            return QianFanImageOptions.builder()
                .withModel(inputParameters.getRequiredString(MODEL))
                .withN(inputParameters.getInteger(N))
                .withStyle(inputParameters.getString(STYLE))
                .withUser(inputParameters.getString(USER))
                .withHeight(inputParameters.getInteger(HEIGHT))
                .withWidth(inputParameters.getInteger(WIDTH))
                .build();
        }

        @Override
        public ImageModel createImageModel(Parameters inputParameters, Parameters connectionParameters) {
            return new QianFanImageModel(new QianFanImageApi(connectionParameters.getString(TOKEN), connectionParameters.getString(QIanFanConstants.SECRET_KEY)),
                (QianFanImageOptions) createImageOptions(inputParameters), new RetryTemplate());
        }
    };
}
