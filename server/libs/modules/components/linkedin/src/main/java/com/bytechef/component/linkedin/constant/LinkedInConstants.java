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

package com.bytechef.component.linkedin.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Monika Kušter
 */
public class LinkedInConstants {

    public static final String ARTICLE = "article";
    public static final String AUTHOR = "author";
    public static final String COMMENTARY = "commentary";
    public static final String CONTENT_TYPE = "contentType";
    public static final String COUNT = "count";
    public static final String CREATED = "created";
    public static final String DESCRIPTION = "description";
    public static final String DOCUMENT = "document";
    public static final String ELEMENTS = "elements";
    public static final String ID = "id";
    public static final String IMAGE = "image";
    public static final String IMAGES = "images";
    public static final String SOURCE = "source";
    public static final String START = "start";
    public static final String THUMBNAIL = "thumbnail";
    public static final String TITLE = "title";
    public static final String URN = "urn";
    public static final String VISIBILITY = "visibility";

    public static final ModifiableObjectProperty COMMENT_OUTPUT_PROPERTY = object()
        .properties(
            string("actor")
                .description("URN of the person or organization that created the comment."),
            string("commentUrn")
                .description("URN of the comment."),
            string("id")
                .description("ID of the comment."),
            object("message")
                .description("The message of the comment.")
                .properties(
                    string("text")
                        .description("The text of the comment.")),
            object(CREATED)
                .description("The time and the actor that created the comment.")
                .properties(
                    string("actor")
                        .description("URN of the actor that created the comment."),
                    integer("time")
                        .description("Time at which the comment was created in milliseconds since epoch.")),
            object("lastModified")
                .description("The time and the actor that last modified the comment.")
                .properties(
                    string("actor")
                        .description("URN of the actor that last modified the comment."),
                    integer("time")
                        .description("Time at which the comment was last modified in milliseconds since epoch.")),
            string("object")
                .description("URN of the activity, post or comment the comment was made on."),
            string("parentComment")
                .description("URN of the parent comment when the comment is a reply."),
            array("content")
                .description("Media attached to the comment.")
                .items(
                    object()
                        .properties(
                            string("type")
                                .description("Type of the media."),
                            string("url")
                                .description("URL of the media."))));

    private LinkedInConstants() {
    }
}
