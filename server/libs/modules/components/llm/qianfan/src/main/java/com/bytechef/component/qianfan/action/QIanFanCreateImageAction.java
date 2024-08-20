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
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static constants.LLMConstants.CREATE_IMAGE;
import static constants.LLMConstants.IMAGE_MESSAGE_PROPERTY;
import static constants.LLMConstants.MODEL;
import static constants.LLMConstants.N;
import static constants.LLMConstants.SIZE;
import static constants.LLMConstants.STYLE;
import static constants.LLMConstants.USER;
import static constants.LLMConstants.USER_PROPERTY;

/**
 * @author Monika Domiter
 */
public class QIanFanCreateImageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_IMAGE)
        .title("Create image")
        .description("Create an image using text-to-image models")
        .properties(
            string(MODEL)
                .label("Model")
                .description("The model to use for image generation.")
                .options(LLMUtils.getEnumOptions(
                    Arrays.stream(QianFanImageApi.ImageModel.values())
                        .collect(Collectors.toMap(
                            QianFanImageApi.ImageModel::getValue, QianFanImageApi.ImageModel::getValue, (f,s)->f))))
                .required(true),
            IMAGE_MESSAGE_PROPERTY,
            object(SIZE)
                .label("Size")
                .description("The size of the generated images.")
                .options(
                    option("Avatars: 768x768", new Integer[]{768, 768}),
                    option("Avatars: 1024x1024", new Integer[]{1024, 1024}),
                    option("Avatars: 1536x1536", new Integer[]{1536, 1536}),
                    option("Avatars: 2048x2048", new Integer[]{2048, 2048}),
                    option("Article illustrations: 1024x768", new Integer[]{1024, 768}),
                    option("Article illustrations: 2048x1536", new Integer[]{2048, 1536}),
                    option("Posters and flyers: 576x1024", new Integer[]{576, 1024}),
                    option("Posters and flyers: 1152x2048", new Integer[]{1152, 2048}),
                    option("Posters and flyers: 768x1024", new Integer[]{768, 1024}),
                    option("Posters and flyers: 1536x2048", new Integer[]{1536, 2048}),
                    option("Computer wallpapers: 1024x576", new Integer[]{1024, 576}),
                    option("Computer wallpapers: 2048x1152", new Integer[]{2048, 1152}))
                .required(true),
            integer(N)
                .label("Number of responses")
                .description("The number of images to generate. Must be between 1 and 4.")
                .defaultValue(1)
                .minValue(1)
                .maxValue(4)
                .advancedOption(true),
            string(STYLE)
                .label("Style")
                .description("The style of the generated images. The default style is Base.")
                .options(
                    option("3D Model", "3D Model"),
                    option("Abstract", "Abstract"),
                    option("Analog Film", "Analog Film"),
                    option("Anime", "Anime"),
                    option("Base", "Base"),
                    option("Cinematic", "Cinematic"),
                    option("Comic Book", "Comic Book"),
                    option("Craft Clay", "Craft Clay"),
                    option("Digital Art", "Digital Art"),
                    option("Enhance", "Enhance"),
                    option("Fantasy Art", "Fantasy Art"),
                    option("Isometric", "Isometric"),
                    option("Line Art", "Line Art"),
                    option("Lowpoly", "Lowpoly"),
                    option("Neonpunk", "Neonpunk"),
                    option("Origami", "Origami"),
                    option("Photographic", "Photographic"),
                    option("Pixel Art", "Pixel Art"),
                    option("Texture", "Texture"))
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
        .perform(QIanFanCreateImageAction::perform);

    private QIanFanCreateImageAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return Image.getResponse(IMAGE, inputParameters, connectionParameters);
    }

    private static final Image IMAGE = new Image() {
        @Override
        public ImageOptions createImageOptions(Parameters inputParameters) {
            Integer[] size = inputParameters.getArray(SIZE, Integer.class);

            return QianFanImageOptions.builder()
                .withModel(inputParameters.getRequiredString(MODEL))
                .withN(inputParameters.getInteger(N))
                .withHeight(size[1])
                .withWidth(size[0])
                .withStyle(inputParameters.getString(STYLE))
                .withUser(inputParameters.getString(USER))
                .build();
        }

        @Override
        public ImageModel createImageModel(Parameters inputParameters, Parameters connectionParameters) {
            return new QianFanImageModel(new QianFanImageApi(connectionParameters.getString(TOKEN), connectionParameters.getString(QIanFanConstants.SECRET_KEY)),
                (QianFanImageOptions) createImageOptions(inputParameters), new RetryTemplate());
        }
    };
}
