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
import static com.bytechef.component.definition.ComponentDSL.dynamicProperties;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.openai.constant.OpenAIConstants.CREATE_IMAGE;
import static com.bytechef.component.openai.constant.OpenAIConstants.DALL_E_2;
import static com.bytechef.component.openai.constant.OpenAIConstants.DALL_E_3;
import static com.bytechef.component.openai.constant.OpenAIConstants.DEFAULT_SIZE;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL_PROPERTIES;
import static com.bytechef.component.openai.constant.OpenAIConstants.N;
import static com.bytechef.component.openai.constant.OpenAIConstants.PROMPT;
import static com.bytechef.component.openai.constant.OpenAIConstants.QUALITY;
import static com.bytechef.component.openai.constant.OpenAIConstants.RESPONSE_FORMAT;
import static com.bytechef.component.openai.constant.OpenAIConstants.RESPONSE_READ_TIMEOUT;
import static com.bytechef.component.openai.constant.OpenAIConstants.SIZE;
import static com.bytechef.component.openai.constant.OpenAIConstants.STYLE;
import static com.bytechef.component.openai.constant.OpenAIConstants.USER;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.openai.util.OpenAIUtils;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.service.OpenAiService;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;

/**
 * @author Monika Domiter
 */
public class OpenAICreateImageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_IMAGE)
        .title("Create image")
        .description("Create an image using text-to-image models")
        .properties(
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .description("The model to use for image generation.")
                .options(
                    option(DALL_E_3, DALL_E_3),
                    option(DALL_E_2, DALL_E_2))
                .defaultValue(DALL_E_2)
                .required(true),
            dynamicProperties(MODEL_PROPERTIES)
                .propertiesLookupDependsOn(MODEL)
                .properties(OpenAIUtils::getModelProperties)
                .required(true),
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
                .optionsLookupDependsOn(MODEL)
                .label("Size")
                .description("The size of the generated images.")
                .options((ActionOptionsFunction<String>) OpenAIUtils::getSizeOptions)
                .defaultValue(DEFAULT_SIZE)
                .required(false),
            string(STYLE)
                .label("Style")
                .description("The style of the generated images.")
                .options(
                    option("vivid", "vivid"),
                    option("natural", "natural"))
                .displayCondition("%s == '%s'".formatted(MODEL, DALL_E_3))
                .required(false),
            string(USER)
                .label("User")
                .description(
                    "A unique identifier representing your end-user, which can help OpenAI to monitor and detect " +
                        "abuse.")
                .required(false),
            integer(RESPONSE_READ_TIMEOUT)
                .advancedOption(true)
                .label("Response read timeout")
                .description("Maximum wait time in seconds to wait on AI response")
                .defaultValue(10L)
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
        .perform(OpenAICreateImageAction::perform);

    private OpenAICreateImageAction() {
    }

    @Nonnull
    private static Long getPropertyDefaultValue(String key) {
        List<? extends Property> properties = ACTION_DEFINITION.getProperties()
            .orElseThrow();

        for (Property property : properties) {
            if (Objects.equals(key, property.getName())) {
                Optional<Long> defaultValue = ((ComponentDSL.ModifiableIntegerProperty) property).getDefaultValue();

                return defaultValue.orElseThrow();
            }
        }

        throw new NoSuchElementException("Property " + key + " not found");
    }

    public static ImageResult perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String token = (String) connectionParameters.get(TOKEN);

        Long responseReadTimeout = inputParameters.getLong(RESPONSE_READ_TIMEOUT);

        if (responseReadTimeout == null) {
            responseReadTimeout = getPropertyDefaultValue(RESPONSE_READ_TIMEOUT);
        }

        OpenAiService openAiService = new OpenAiService(token, Duration.ofSeconds(responseReadTimeout));

        CreateImageRequest createImageRequest = new CreateImageRequest();

        Map<String, ?> modelProperties = inputParameters.getRequiredMap(MODEL_PROPERTIES);

        createImageRequest.setPrompt((String) modelProperties.get(PROMPT));
        createImageRequest.setModel(inputParameters.getRequiredString(MODEL));
        createImageRequest.setN((Integer) modelProperties.get(N));
        createImageRequest.setQuality(inputParameters.getString(QUALITY));
        createImageRequest.setResponseFormat(inputParameters.getString(RESPONSE_FORMAT));
        createImageRequest.setSize(inputParameters.getString(SIZE));
        createImageRequest.setStyle(inputParameters.getString(STYLE));
        createImageRequest.setUser(inputParameters.getString(USER));

        return openAiService.createImage(createImageRequest);
    }
}
