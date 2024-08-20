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

package com.bytechef.component.zhipu.action;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.zhipuai.ZhiPuAiImageModel;
import org.springframework.ai.zhipuai.ZhiPuAiImageOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiImageApi;
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
import static com.bytechef.component.definition.ComponentDSL.string;
import static constants.LLMConstants.CONTENT;
import static constants.LLMConstants.CREATE_IMAGE;
import static constants.LLMConstants.MESSAGES;
import static constants.LLMConstants.MODEL;
import static constants.LLMConstants.USER;
import static constants.LLMConstants.USER_PROPERTY;
import static constants.LLMConstants.WEIGHT;

/**
 * @author Monika Domiter
 */
public class ZhiPuCreateImageAction {

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
            string(MODEL)
                .label("Model")
                .description("The model to use for image generation.")
                .options(LLMUtils.getEnumOptions(
                    Arrays.stream(ZhiPuAiImageApi.ImageModel.values())
                        .collect(Collectors.toMap(
                            ZhiPuAiImageApi.ImageModel::getValue, ZhiPuAiImageApi.ImageModel::getValue, (f,s)->f))))
                .required(true),
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
        .perform(ZhiPuCreateImageAction::perform);

    private ZhiPuCreateImageAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return Image.getResponse(IMAGE, inputParameters, connectionParameters);
    }

    public static final Image IMAGE = new Image() {
        @Override
        public ImageOptions createImageOptions(Parameters inputParameters) {
            return ZhiPuAiImageOptions.builder()
                .withModel(inputParameters.getRequiredString(MODEL))
                .withUser(inputParameters.getString(USER))
                .build();
        }

        @Override
        public ImageModel createImageModel(Parameters inputParameters, Parameters connectionParameters) {
            return new ZhiPuAiImageModel(new ZhiPuAiImageApi(connectionParameters.getString(TOKEN)),
                (ZhiPuAiImageOptions) createImageOptions(inputParameters), new RetryTemplate());
        }
    };
}
