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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.OptionsDataSource;
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
            .options((OptionsDataSource.ActionOptionsFunction<String>) MattermostUtils::getChannelIdOptions),
            string("message").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Message")
                .description("The message contents.")
                .required(true))
        .output(outputSchema(object()
            .properties(string("id").required(false), integer("create_at").required(false),
                integer("update_at").required(false), integer("edit_at").required(false),
                integer("delete_at").required(false), bool("is_pinned").required(false),
                string("user_id").required(false), string("channel_id").required(false),
                string("root_id").required(false), string("parent_id").required(false),
                string("original_id").required(false), string("message").required(false),
                string("type").required(false), object("props").required(false), string("hashtags").required(false),
                string("pending_post_id").required(false), integer("reply_count").required(false),
                integer("last_reply_at").required(false), object("participants").required(false),
                bool("is_following").required(false), object("metadata").required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private MattermostSendMessageAction() {
    }
}
