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

package com.bytechef.component.instagram.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.Property.ValueProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class InstagramConstants {

    public static final String API_VERSION = "v27.0";
    public static final String BASE_URL = "https://graph.instagram.com/" + API_VERSION;

    public static final String CAPTION = "caption";
    public static final String COMMENT_ID = "commentId";
    public static final String IMAGE_URL = "imageUrl";
    public static final String IMAGE_URLS = "imageUrls";
    public static final String MEDIA_ID = "mediaId";
    public static final String MESSAGE = "message";
    public static final String VIDEO_URL = "videoUrl";

    public static final String COMMENT_FIELDS = "id,text,username,timestamp,like_count,replies";

    public static final ModifiableObjectProperty COMMENT_OUTPUT_PROPERTY = createCommentOutputProperty();

    public static final ModifiableObjectProperty MEDIA_OUTPUT_PROPERTY = object()
        .properties(
            string("id")
                .description("ID of the published Instagram media."));

    private InstagramConstants() {
    }

    public static ModifiableObjectProperty createCommentOutputProperty(ValueProperty<?>... additionalProperties) {
        List<ValueProperty<?>> properties = new ArrayList<>(List.of(additionalProperties));

        properties.addAll(
            List.of(
                string("id")
                    .description("ID of the comment."),
                string("text")
                    .description("Text of the comment."),
                string("username")
                    .description("Username of the Instagram user who wrote the comment."),
                string("timestamp")
                    .description("ISO 8601 date and time when the comment was created."),
                integer("like_count")
                    .description("Number of likes on the comment."),
                object("replies")
                    .description("Replies to the comment.")
                    .properties(
                        array("data")
                            .items(
                                object()
                                    .properties(
                                        string("id")
                                            .description("ID of the reply."),
                                        string("text")
                                            .description("Text of the reply."),
                                        string("username")
                                            .description("Username of the Instagram user who wrote the reply."),
                                        string("timestamp")
                                            .description("ISO 8601 date and time when the reply was created."))))));

        return object().properties(properties);
    }
}
