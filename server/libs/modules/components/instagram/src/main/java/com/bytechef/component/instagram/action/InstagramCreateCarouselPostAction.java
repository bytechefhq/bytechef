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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.instagram.constant.InstagramConstants.CAPTION;
import static com.bytechef.component.instagram.constant.InstagramConstants.IMAGE_URLS;
import static com.bytechef.component.instagram.constant.InstagramConstants.MEDIA_OUTPUT_PROPERTY;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.instagram.util.InstagramUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class InstagramCreateCarouselPostAction {

    protected static final int MAX_IMAGES = 10;
    protected static final int MIN_IMAGES = 2;

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createCarouselPost")
        .title("Create Carousel Post")
        .description("Publishes a carousel post with 2 to 10 images to the connected Instagram professional account.")
        .help("", "https://docs.bytechef.io/reference/components/instagram_v1#create-carousel-post")
        .properties(
            array(IMAGE_URLS)
                .label("Image URLs")
                .description(
                    "Public URLs of the JPEG images to publish, in the order they appear in the carousel. " +
                        "Instagram downloads each image from its URL.")
                .items(string())
                .minItems(MIN_IMAGES)
                .maxItems(MAX_IMAGES)
                .required(true),
            string(CAPTION)
                .label("Caption")
                .description("Caption of the post. It can include hashtags and mentions.")
                .maxLength(2200)
                .controlType(ControlType.TEXT_AREA)
                .required(false))
        .output(outputSchema(MEDIA_OUTPUT_PROPERTY))
        .perform(InstagramCreateCarouselPostAction::perform);

    private InstagramCreateCarouselPostAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        List<String> imageUrls = inputParameters.getRequiredList(IMAGE_URLS, String.class);

        if (imageUrls.size() < MIN_IMAGES || imageUrls.size() > MAX_IMAGES) {
            throw new IllegalArgumentException(
                "A carousel post needs between " + MIN_IMAGES + " and " + MAX_IMAGES + " images, got " +
                    imageUrls.size());
        }

        String userId = InstagramUtils.getUserId(context);

        List<String> childContainerIds = new ArrayList<>();

        for (String imageUrl : imageUrls) {
            Map<String, Object> childBody = Map.of("image_url", imageUrl, "is_carousel_item", true);

            childContainerIds.add(InstagramUtils.createMediaContainer(context, userId, childBody));
        }

        Map<String, Object> carouselBody = new HashMap<>();

        carouselBody.put("children", String.join(",", childContainerIds));
        carouselBody.put("media_type", "CAROUSEL");

        String caption = inputParameters.getString(CAPTION);

        if (caption != null && !caption.isBlank()) {
            carouselBody.put("caption", caption);
        }

        String carouselContainerId = InstagramUtils.createMediaContainer(context, userId, carouselBody);

        return InstagramUtils.publishMediaContainer(context, userId, carouselContainerId);
    }
}
