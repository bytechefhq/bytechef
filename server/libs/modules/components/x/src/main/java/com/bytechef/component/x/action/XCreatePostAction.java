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

package com.bytechef.component.x.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.x.constant.XConstants.DATA;
import static com.bytechef.component.x.constant.XConstants.ID;
import static com.bytechef.component.x.constant.XConstants.MEDIA;
import static com.bytechef.component.x.constant.XConstants.TEXT;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.x.util.XUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class XCreatePostAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createPost")
        .title("Create Post")
        .description("Creates a new post for the authenticated user,")
        .properties(
            string(TEXT)
                .label("Text")
                .description("The text of the post to create.")
                .controlType(ControlType.TEXT_AREA)
                .required(false),
            array(MEDIA)
                .label("Images")
                .description("The images to attach to the post.")
                .items(fileEntry())
                .maxItems(4)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        object(DATA)
                            .properties(
                                array("edit_history_tweet_ids")
                                    .items(string()),
                                string(ID)
                                    .description("Unique identifier of created Tweet."),
                                string(TEXT)
                                    .description("The content of the Tweet.")))))
        .perform(XCreatePostAction::perform);

    private XCreatePostAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String text = inputParameters.getString(TEXT);
        List<FileEntry> images = inputParameters.getList(MEDIA, FileEntry.class);

        boolean hasText = text != null && !text.isBlank();
        boolean hasImages = images != null && !images.isEmpty();

        if (!hasText && !hasImages) {
            throw new IllegalArgumentException("At least one of 'Text' or 'Images' must be provided to create a post.");
        }

        Map<String, Object> bodyMap = new HashMap<>();

        if (hasText) {
            bodyMap.put(TEXT, text);
        }

        if (hasImages) {
            List<String> mediaIds = uploadImages(images, context);

            if (!mediaIds.isEmpty()) {
                bodyMap.put(MEDIA, Map.of("media_ids", mediaIds));
            }
        }

        return context.http(http -> http.post("/tweets"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(bodyMap))
            .execute()
            .getBody();
    }

    private static List<String> uploadImages(List<FileEntry> images, Context context) {
        List<String> mediaIds = new ArrayList<>(images.size());

        for (FileEntry fileEntry : images) {
            String uploadedMediaId = XUtils.uploadMedia(context, fileEntry, "tweet_image");

            mediaIds.add(uploadedMediaId);
        }

        return mediaIds;
    }
}
