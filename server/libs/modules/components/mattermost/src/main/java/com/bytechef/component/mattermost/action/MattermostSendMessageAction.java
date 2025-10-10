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

package com.bytechef.component.mattermost.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.mattermost.util.MattermostUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class MattermostSendMessageAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("sendMessage")
        .title("Send message")
        .description("Send message to a channel.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/posts", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("channel_id").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("Channel Id")
            .description("The channel ID to send message to.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) MattermostUtils::getChannelIdOptions),
            string("message").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Message")
                .description("The message contents.")
                .required(true))
        .output(outputSchema(object().properties(string("id").description("ID of the message.")
            .required(false),
            integer("create_at").description("The time in milliseconds a post was created.")
                .required(false),
            integer("update_at").description("The time in milliseconds a post was last updated.")
                .required(false),
            integer("edit_at").description("The time in milliseconds a post was last edited.")
                .required(false),
            integer("delete_at").description("The time in milliseconds a post was deleted.")
                .required(false),
            bool("is_pinned").description("True if post is pinned to the channel it is in.")
                .required(false),
            string("user_id").description("ID of the user.")
                .required(false),
            string("channel_id").description("ID of the channel.")
                .required(false),
            string("root_id").description("Post ID if post is created as a comment on another post.")
                .required(false),
            string("parent_id").required(false), string("original_id").required(false),
            string("message").description("The actual content of the message.")
                .required(false),
            string("type").required(false), object("props").required(false),
            string("hashtags").description("Any hashtags included in the message content.")
                .required(false),
            string("pending_post_id").required(false),
            integer("reply_count").description("Number of replies to this message.")
                .required(false),
            integer("last_reply_at").description("The time in milliseconds of the most recent reply.")
                .required(false),
            object("participants").required(false), bool("is_following").required(false),
            object("metadata")
                .properties(
                    array("embeds")
                        .items(
                            object()
                                .properties(
                                    string("type").description("The type of content that is embedded in this post.")
                                        .required(false),
                                    string("url").description("The URL of the embedded content, if one exists.")
                                        .required(false),
                                    object("data").description("Any additional information about the embedded content.")
                                        .required(false))
                                .description("Information about content embedded in the post."))
                        .description("Information about content embedded in the post.")
                        .required(false),
                    array("emojis").items(object().properties(string("id").description("The ID of the emoji.")
                        .required(false),
                        string("creator_id").description("The ID of the user that made the emoji.")
                            .required(false),
                        string("name").description("The name of the emoji.")
                            .required(false),
                        integer("create_at").description("The time in milliseconds the emoji was made.")
                            .required(false),
                        integer("update_at").description("The time in milliseconds the emoji was last updated.")
                            .required(false),
                        integer("delete_at").description("The time in milliseconds the emoji was deleted.")
                            .required(false))
                        .description("The custom emojis that appear in this post."))
                        .description("The custom emojis that appear in this post.")
                        .required(false),
                    array("files").items(object().properties(string("id").description("The ID of the file.")
                        .required(false),
                        string("user_id").description("The ID of the user that uploaded this file.")
                            .required(false),
                        string("post_id").description("If this file is attached to a post, the ID of that post.")
                            .required(false),
                        integer("create_at").description("The time in milliseconds a file was created.")
                            .required(false),
                        integer("update_at").description("The time in milliseconds a file was last updated.")
                            .required(false),
                        integer("delete_at").description("The time in milliseconds a file was deleted.")
                            .required(false),
                        string("name").description("The name of the file.")
                            .required(false),
                        string("extension").description("The extension at the end of the file name.")
                            .required(false),
                        integer("size").description("The size of the file in bytes.")
                            .required(false),
                        string("mime_type").description("The MIME type of the file.")
                            .required(false),
                        integer("width").description("If this file is an image, the width of the file.")
                            .required(false),
                        integer("height").description("If this file is an image, the height of the file.")
                            .required(false),
                        bool("has_preview_image")
                            .description("If this file is an image, whether or not it has a preview-sized version.")
                            .required(false))
                        .description("The file info objects for any files attached to the post."))
                        .description("The file info objects for any files attached to the post.")
                        .required(false),
                    array("images").items(object().properties(integer("height").description("Image height.")
                        .required(false),
                        integer("width").description("Image width.")
                            .required(false))
                        .description(
                            "An object mapping the URL of an external image to an object containing the dimensions of that image."))
                        .description(
                            "An object mapping the URL of an external image to an object containing the dimensions of that image.")
                        .required(false),
                    array("reactions")
                        .items(object().properties(
                            string("user_id").description("The ID of the user that made this reaction.")
                                .required(false),
                            string("post_id").description("The ID of the post to which this reaction was made.")
                                .required(false),
                            string("emoji_name").description("The name of the emoji that was used for this reaction.")
                                .required(false),
                            integer("create_at").description("The time in milliseconds this reaction was made.")
                                .required(false))
                            .description("Any reactions made to this post."))
                        .description("Any reactions made to this post.")
                        .required(false))
                .description("Additional information used to display the post.")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private MattermostSendMessageAction() {
    }
}
