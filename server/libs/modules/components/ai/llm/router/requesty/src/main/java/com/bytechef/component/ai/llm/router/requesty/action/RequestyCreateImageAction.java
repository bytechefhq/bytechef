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

package com.bytechef.component.ai.llm.router.requesty.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.CREATE_IMAGE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.IMAGE_MESSAGE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SIZE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER_PROPERTY;
import static com.bytechef.component.ai.llm.router.requesty.constant.RequestyConstants.IMAGE_MODEL_PROPERTY;
import static com.bytechef.component.ai.llm.router.requesty.constant.RequestyConstants.QUALITY;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.ImageModel;
import com.bytechef.component.ai.llm.router.requesty.model.RequestyImageModel;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;

/**
 * @author Thibault Jaigu
 */
public class RequestyCreateImageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_IMAGE)
        .title("Create Image")
        .description("Create an image using text-to-image models.")
        .help("", "https://docs.bytechef.io/reference/components/requesty_v1#create-image")
        .properties(
            IMAGE_MODEL_PROPERTY,
            IMAGE_MESSAGE_PROPERTY,
            string(SIZE)
                .label("Size")
                .description("The size of the generated image. Not every model supports every size.")
                .options(
                    option("1024×1024", "1024x1024"),
                    option("1536×1024", "1536x1024"),
                    option("1024×1536", "1024x1536"))
                .defaultValue("1024x1024")
                .required(false),
            string(QUALITY)
                .label("Quality")
                .description("The quality of the generated image.")
                .options(
                    option("Auto", "auto"),
                    option("High", "high"),
                    option("Medium", "medium"),
                    option("Low", "low"))
                .defaultValue("auto")
                .required(false),
            USER_PROPERTY)
        .output(ModelUtils::output)
        .perform(RequestyCreateImageAction::perform);

    public static final ImageModel IMAGE_MODEL =
        (inputParameters, connectionParameters) -> RequestyImageModel.builder()
            .apiKey(connectionParameters.getString(TOKEN))
            .model(inputParameters.getRequiredString(MODEL))
            .quality(inputParameters.getString(QUALITY))
            .size(inputParameters.getString(SIZE))
            .user(inputParameters.getString(USER))
            .build();

    private RequestyCreateImageAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return IMAGE_MODEL.getResponse(inputParameters, connectionParameters);
    }
}
