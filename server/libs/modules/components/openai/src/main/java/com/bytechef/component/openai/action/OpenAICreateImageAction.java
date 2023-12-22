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

import static com.bytechef.component.openai.constant.OpenAIConstants.CREATE_IMAGE;
import static com.bytechef.component.openai.constant.OpenAIConstants.DALL_E_2;
import static com.bytechef.component.openai.constant.OpenAIConstants.DALL_E_3;
import static com.bytechef.component.openai.constant.OpenAIConstants.DEFAULT_SIZE;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static com.bytechef.component.openai.constant.OpenAIConstants.N;
import static com.bytechef.component.openai.constant.OpenAIConstants.PROMPT;
import static com.bytechef.component.openai.constant.OpenAIConstants.QUALITY;
import static com.bytechef.component.openai.constant.OpenAIConstants.RESPONSE_FORMAT;
import static com.bytechef.component.openai.constant.OpenAIConstants.SIZE;
import static com.bytechef.component.openai.constant.OpenAIConstants.STYLE;
import static com.bytechef.component.openai.constant.OpenAIConstants.USER;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.dynamicProperties;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.option;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.TOKEN;

import com.bytechef.component.openai.util.OpenAIUtils;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.hermes.component.definition.Parameters;
import com.bytechef.hermes.definition.Property;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.service.OpenAiService;

/**
 * @author Monika Domiter
 */
public class OpenAICreateImageAction {
    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_IMAGE)
        .title("Create image")
        .description("Create an image using text-to-image models")
        .properties(
            dynamicProperties(PROMPT)
                .label("Prompt")
                .description("A text description of the desired image(s).")
                .loadPropertiesDependsOn(MODEL)
                .properties(OpenAIUtils::getModelProperties)
                .required(true),
            string(MODEL)
                .label("Model")
                .description("The model to use for image generation.")
                .options(
                    option(DALL_E_3, DALL_E_3),
                    option(DALL_E_2, DALL_E_2))
                .defaultValue(DALL_E_2)
                .required(false),
            dynamicProperties(N)
                .label("n")
                .description("The number of images to generate.")
                .loadPropertiesDependsOn(MODEL)
                .properties(OpenAIUtils::getNumberOfImagesProperties)
                .required(false),
            string(QUALITY)
                .label("Quality")
                .description("The quality of the image that will be generated.")
                .options(
                    option("standard", "standard"),
                    option("hd", "hd"))
                .required(false),
            string(RESPONSE_FORMAT)
                .label("Response format")
                .description("The format in which the generated images are returned.")
                .options(
                    option("url", "url"),
                    option("b64_json", "b64_json"))
                .defaultValue("url")
                .required(false),
            string(SIZE)
                .label("Size")
                .description("The size of the generated images.")
                .options((ActionOptionsFunction) OpenAIUtils::getSizeOptions)
                .defaultValue(DEFAULT_SIZE)
                .required(false),
            string(STYLE)
                .label("Style")
                .options()
                .description("The style of the generated images.")
                .options(
                    option("vivid", "vivid"),
                    option("natural", "natural"))
                .displayCondition("%s === '%s'".formatted(MODEL, "dall-e-3"))
                .required(false),
            string(USER)
                .label("User")
                .description("A unique identifier representing your end-user, " +
                    "which can help OpenAI to monitor and detect abuse.")
                .required(false))
        .outputSchema(
            object().properties(
                integer("created"),
                array("data")
                    .items(
                        object()
                            .properties(
                                string("url")
                                    .controlType(Property.ControlType.URL),
                                string("b64Json"),
                                string("revisedPrompt"))))

        )
        .perform(OpenAICreateImageAction::perform);

    private OpenAICreateImageAction() {
    }

    public static ImageResult perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        String token = (String) connectionParameters.get(TOKEN);

        OpenAiService openAiService = new OpenAiService(token);

        CreateImageRequest createImageRequest = new CreateImageRequest();

        createImageRequest.setPrompt(inputParameters.getRequiredString(PROMPT));
        createImageRequest.setModel(inputParameters.getString(MODEL));
        createImageRequest.setN(inputParameters.getInteger(N));
        createImageRequest.setQuality(inputParameters.getString(QUALITY));
        createImageRequest.setResponseFormat(inputParameters.getString(RESPONSE_FORMAT));
        createImageRequest.setSize(inputParameters.getString(SIZE));
        createImageRequest.setStyle(inputParameters.getString(STYLE));
        createImageRequest.setUser(inputParameters.getString(USER));

        return openAiService.createImage(createImageRequest);
    }

}
