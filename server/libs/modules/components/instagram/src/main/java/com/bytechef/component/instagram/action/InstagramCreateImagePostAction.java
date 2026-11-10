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

package com.bytechef.component.instagram.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.instagram.constant.InstagramConstants.CAPTION;
import static com.bytechef.component.instagram.constant.InstagramConstants.IMAGE_URL;
import static com.bytechef.component.instagram.constant.InstagramConstants.MEDIA_OUTPUT_PROPERTY;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.instagram.util.InstagramUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class InstagramCreateImagePostAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createImagePost")
        .title("Create Image Post")
        .description("Publishes a single image post to the connected Instagram professional account.")
        .help("", "https://docs.bytechef.io/reference/components/instagram_v1#create-image-post")
        .properties(
            string(IMAGE_URL)
                .label("Image URL")
                .description("Public URL of the JPEG image to publish. Instagram downloads the image from this URL.")
                .required(true),
            string(CAPTION)
                .label("Caption")
                .description("Caption of the post. It can include hashtags and mentions.")
                .maxLength(2200)
                .controlType(ControlType.TEXT_AREA)
                .required(false))
        .output(outputSchema(MEDIA_OUTPUT_PROPERTY))
        .perform(InstagramCreateImagePostAction::perform);

    private InstagramCreateImagePostAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        String userId = InstagramUtils.getUserId(context);

        Map<String, Object> body = new HashMap<>();

        body.put("image_url", inputParameters.getRequiredString(IMAGE_URL));

        String caption = inputParameters.getString(CAPTION);

        if (caption != null && !caption.isBlank()) {
            body.put("caption", caption);
        }

        String containerId = InstagramUtils.createMediaContainer(context, userId, body);

        return InstagramUtils.publishMediaContainer(context, userId, containerId);
    }
}
