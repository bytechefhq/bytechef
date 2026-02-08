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

package com.bytechef.component.image.helper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.IMAGE;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.IMAGE_PROPERTY;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Kušter
 */
public class ImageHelperImageToBase64Action {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("imageToBase64")
        .title("Image to Base64")
        .description("Converts image to Base64 string.")
        .help("", "https://docs.bytechef.io/reference/components/image-helper_v1#image-to-base64")
        .properties(IMAGE_PROPERTY)
        .output(outputSchema(string().description("Base64 encoded image.")))
        .perform(ImageHelperImageToBase64Action::perform);

    private ImageHelperImageToBase64Action() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        byte[] fileContent = context.file(file -> file.readAllBytes(inputParameters.getRequiredFileEntry(IMAGE)));

        return context.encoder(encoder -> encoder.base64EncodeToString(fileContent));
    }
}
