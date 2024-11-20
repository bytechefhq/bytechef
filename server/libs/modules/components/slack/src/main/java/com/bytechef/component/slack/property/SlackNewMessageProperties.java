/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.slack.property;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import java.util.List;

/**
 * @author Vihar Shah
 */
public class SlackNewMessageProperties {
    public static final List<ModifiableValueProperty<?, ?>> PROPERTIES =
        List.of(string("token").label("Verification Token"),
            string("team_id").label("Team ID"),
            string("api_app_id").label("API App ID"),
            object("event")
                .properties(List.of(
                    string("user").label("User "),
                    string("type").label("Type"),
                    string("ts").label("Timestamp"),
                    string("bot_id").label("Bot ID"),
                    string("app_id").label("App ID"),
                    string("text").label("Text"),
                    string("team").label("Team"),
                    object("bot_profile")
                        .properties(List.of(
                            string("id").label("ID"))),
                    bool("deleted").label("Deleted"),
                    string("name").label("Name"),
                    number("updated").label("Updated"),
                    string("app_id").label("App ID"),
                    object("icons")
                        .properties(List.of(
                            string("image_36").label("Image 36"),
                            string("image_48").label("Image 48"),
                            string("image_72").label("Image 72"))),
                    string("team_id").label("Team ID"))),
            array("blocks").label("Blocks")
                .items(
                    object("block")
                        .properties(List.of(
                            string("type").label("Type"),
                            string("block_id").label("Block ID"),
                            object("text")
                                .properties(List.of(
                                    string("type").label("Type"),
                                    string("text").label("Text"),
                                    bool("verbatim").label("Verbatim"))),
                            array("elements").label("Elements")
                                .items(
                                    object("element")
                                        .properties(List.of(
                                            string("type").label("Type"),
                                            object("text")
                                                .properties(List.of(
                                                    string("type").label("Type"),
                                                    string("text").label("Text"))),
                                            bool("verbatim").label("Verbatim"),
                                            string("action_id").label("Action ID"),
                                            string("value").label("Value"),
                                            object("placeholder")
                                                .properties(List.of(
                                                    string("type").label("Type"),
                                                    string("text").label("Text"),
                                                    bool("emoji").label("Emoji"))),
                                            array("elements").label("Elements")
                                                .items(
                                                    object("element")
                                                        .properties(List.of(
                                                            string("type").label("Type"),
                                                            string("user_id").label("User  ID"),
                                                            string("text").label("Text")))))))))),
            array("files")
                .label("Files")
                .items(
                    object("file")
                        .properties(List.of(
                            string("id").label("ID"),
                            number("created").label("Created"),
                            number("timestamp").label("Timestamp"),
                            string("name").label("Name"),
                            string("title").label("Title"),
                            string("mimetype").label("Mime type"),
                            string("filetype").label("File type"),
                            string("pretty_type").label("Pretty type"),
                            string("user").label("User "),
                            string("user_team").label("User  team"),
                            bool("editable").label("Editable"),
                            number("size").label("Size"),
                            string("mode").label("Mode"),
                            bool("is_external").label("Is external"),
                            string("external_type").label("External type"),
                            bool("is_public").label("Is public"),
                            bool("public_url_shared").label("Public URL shared"),
                            bool("display_as_bot").label("Display as bot"),
                            string("username").label("Username"),
                            string("url_private").label("Private URL"),
                            string("url_private_download").label("Private download URL"),
                            string("permalink").label("Permalink"),
                            string("permalink_public").label("Public permalink"),
                            string("subject").label("Subject"),
                            array("to").label("To")
                                .items(object("to")
                                    .properties(List.of(
                                        string("address").label("Address"),
                                        string("name").label("Name"),
                                        string("original").label("Original")))),
                            array("from").label("From")
                                .items(object("from")
                                    .properties(List.of(
                                        string("address").label("Address"),
                                        string("name").label("Name"),
                                        string("original").label("Original")))),
                            array("cc")
                                .items(string("cc")),
                            array("attachments")
                                .label("Attachments")
                                .items(object("attachment")),
                            number("original_attachment_count").label("Original attachment count"),
                            string("plain_text").label("Plain text"),
                            string("preview").label("Preview"),
                            string("preview_plain_text").label("Preview plain text"),
                            object("headers")
                                .properties(List.of(
                                    string("date").label("Date"),
                                    string("message_id").label(" Message ID"))),
                            bool("has_more").label("Has more"),
                            bool("sent_to_self").label("Sent to self"),
                            string("bot_id").label("Bot ID"),
                            bool("has_rich_preview").label("Has rich preview"),
                            string("file_access").label("File access")))),
            string("channel").label("Channel"),
            string("event_ts").label("Event timestamp"),
            string("channel_type").label("Channel type"),
            bool("upload").label("Upload"),
            bool("display_as_bot").label("Display as bot"),
            string("subtype").label("Subtype"),
            array("attachments")
                .items(object("attachment")
                    .properties(List.of(
                        number("id"),
                        string("color"),
                        string("fallback"),
                        string("text"),
                        string("pretext"),
                        array("fields")
                            .items(object("field")
                                .properties(List.of(
                                    string("value"),
                                    string("title"),
                                    bool("short")))),
                        array("mrkdwn_in")
                            .items(string("mrkdwn_in"))))),
            string("type").label("Type"),
            array("authed_teams").label("Authed Teams")
                .items(
                    string("authed_team")),
            string("event_id").label("Event ID"),
            number("event_time").label("Event Time"),
            string("context_team_id").label("Context team ID"),
            array("authorizations").label("Authorizations")
                .items(
                    object("authorization")
                        .properties(List.of(
                            string("team_id").label("Team ID"),
                            string("user_id").label("User  ID"),
                            bool("is_bot").label("Is bot"),
                            bool("is_enterprise_install").label("Is enterprise install")))),
            string("event_context").label("Event context"));
}
