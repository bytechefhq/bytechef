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
import static com.bytechef.component.instagram.constant.InstagramConstants.MEDIA_OUTPUT_PROPERTY;
import static com.bytechef.component.instagram.constant.InstagramConstants.VIDEO_URL;

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
public class InstagramCreateReelAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createReel")
        .title("Create Reel")
        .description(
            "Publishes a reel to the connected Instagram professional account. Waits until Instagram has finished " +
                "processing the video, for up to about five minutes.")
        .help("", "https://docs.bytechef.io/reference/components/instagram_v1#create-reel")
        .properties(
            string(VIDEO_URL)
                .label("Video URL")
                .description("Public URL of the video to publish. Instagram downloads the video from this URL.")
                .required(true),
            string(CAPTION)
                .label("Caption")
                .description("Caption of the reel. It can include hashtags and mentions.")
                .maxLength(2200)
                .controlType(ControlType.TEXT_AREA)
                .required(false))
        .output(outputSchema(MEDIA_OUTPUT_PROPERTY))
        .perform(InstagramCreateReelAction::perform);

    private InstagramCreateReelAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        String userId = InstagramUtils.getUserId(context);

        Map<String, Object> body = new HashMap<>();

        body.put("media_type", "REELS");
        body.put("video_url", inputParameters.getRequiredString(VIDEO_URL));

        String caption = inputParameters.getString(CAPTION);

        if (caption != null && !caption.isBlank()) {
            body.put("caption", caption);
        }

        String containerId = InstagramUtils.createMediaContainer(context, userId, body);

        InstagramUtils.waitUntilContainerFinished(context, containerId);

        return InstagramUtils.publishMediaContainer(context, userId, containerId);
    }
}
