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

package com.bytechef.component.ai.llm.nano.gpt.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.ATTACHMENTS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.CREATE_IMAGE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.IMAGE_MESSAGE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SIZE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER_PROPERTY;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.ASPECT_RATIO;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.IMAGE_MODEL_PROPERTY;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.ImageModel;
import com.bytechef.component.ai.llm.nano.gpt.model.NanoGptImageModel;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;

/**
 * @author Marko Kriskovic
 */
public class NanoGptCreateImageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_IMAGE)
        .title("Create Image")
        .description("Create an image using text-to-image models")
        .properties(
            IMAGE_MODEL_PROPERTY,
            IMAGE_MESSAGE_PROPERTY,
            string(ASPECT_RATIO)
                .label("Aspect Ratio")
                .description("Specific aspect ratios for generated images")
                .options(
                    option("1:1", "1:1", "1024×1024 (default)"),
                    option("2:3", "2:3", "832×1248"),
                    option("3:2", "3:2", "1248×832"),
                    option("3:4", "3:4", "864×1184"),
                    option("4:3", "4:3", "1184×864"),
                    option("4:5", "4:5", "896×1152"),
                    option("5:4", "5:4", "1152×896"),
                    option("9:16", "9:16", "768×1344"),
                    option("16:9", "16:9", "1344×768"),
                    option("21:9", "21:9", "1536×672"),
                    option("1:4", "1:4", "supported by google/gemini-3.1-flash-image-preview only"),
                    option("4:1", "4:1", "supported by google/gemini-3.1-flash-image-preview only"),
                    option("1:8", "1:8", "supported by google/gemini-3.1-flash-image-preview only"),
                    option("8:1", "8:1", "supported by google/gemini-3.1-flash-image-preview only"))
                .defaultValue("1:1")
                .required(false),
            string(SIZE)
                .label("Size")
                .description("The size of the generated images.")
                .options(
                    option("1K", "1K", "Standard resolution (default)"),
                    option("2K", "2K", "Higher resolution"),
                    option("4K", "4K", "Highest resolution"),
                    option("0.5K", "0.5K",
                        "Lower resolution, optimized for efficiency (supported by google/gemini-3.1-flash-image-preview only)"))
                .defaultValue("1K")
                .required(false),
            ATTACHMENTS_PROPERTY,
            USER_PROPERTY)
        .output(ModelUtils::output)
        .perform(NanoGptCreateImageAction::perform);

    public static final ImageModel IMAGE_MODEL =
        (inputParameters, connectionParameters) -> NanoGptImageModel.builder()
            .apiKey(connectionParameters.getString(TOKEN))
            .model(inputParameters.getRequiredString(MODEL))
            .size(inputParameters.getString(SIZE))
            .aspectRatio(inputParameters.getString(ASPECT_RATIO))
            .user(inputParameters.getString(USER))
            .build();

    private NanoGptCreateImageAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return IMAGE_MODEL.getResponse(inputParameters, connectionParameters);
    }
}
