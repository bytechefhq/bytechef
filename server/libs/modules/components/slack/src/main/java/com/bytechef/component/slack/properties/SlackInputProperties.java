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

package com.bytechef.component.slack.properties;

import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.dynamicProperties;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import java.util.List;

/**
 * @author Mario Cvjetojevic
 */
public final class SlackInputProperties {
    public static final String CONTENT_TYPE = "contentType";
    public static final String ATTACHMENTS = "attachments";
    public static final String BLOCKS = "blocks";
    public static final String TEXT = "text";
    public static final String CONTENT = "content";
    public static final String AS_USER = "asUser";
    public static final String ICON_EMOJI = "iconEmoji";
    public static final String ICON_URL = "iconUrl";
    public static final String LINK_NAMES = "linkNames";
    public static final String METADATA = "metadata";
    public static final String MRKDWN = "mrkdwn";
    public static final String PARSE = "parse";
    public static final String REPLY_BROADCAST = "replyBroadcast";
    public static final String THREAD_TS = "threadTs";
    public static final String UNFURL_LINKS = "unfurlLinks";
    public static final String UNFURL_MEDIA = "unfurlMedia";
    public static final String USERNAME = "username";

    public static final Property CONTENT_TYPE_PROPERTY = string(CONTENT_TYPE)
        .label("Content type")
        .description("" +
            "One of these arguments is required to describe the content of the message. If attachments or blocks " +
            "are included, text will be used as fallback text for notifications only.")
        .options(
            option("Attachments", ATTACHMENTS,
                "A JSON-based array of structured attachments, presented as a URL-encoded string."),
            option("Blocks", BLOCKS,
                "A JSON-based array of structured blocks, presented as a URL-encoded string."),
            option("Text", TEXT,
                "How this field works and whether it is required depends on other fields you use in your API call."))
        .required(true),
        CONTENT_PROPERTY = dynamicProperties(CONTENT)
            .loadPropertiesDependsOn(CONTENT_TYPE)
            .properties(SlackInputProperties::getContentTypeProperties)
            .required(true),
        AS_USER_PROPERTY = bool(AS_USER)
            .label("As user")
            .description(
                "(Legacy) Pass true to post the message as the authed user instead of as a bot. Defaults to false. " +
                    "Can only be used by classic Slack apps. See authorship below."),
        ICON_EMOJI_PROPERTY = string(ICON_EMOJI)
            .label("Icon emoji")
            .description(
                "Emoji to use as the icon for this message. Overrides icon_url."),
        ICON_URL_PROPERTY = string(ICON_URL)
            .label("Icon URL")
            .description(
                "URL to an image to use as the icon for this message."),
        LINK_NAMES_PROPERTY = bool(LINK_NAMES)
            .label("Link names")
            .description(
                "Find and link user groups. No longer supports linking individual users; use syntax shown in " +
                    "Mentioning Users instead."),
        METADATA_PROPERTY = string(METADATA)
            .label("Metadata")
            .description(
                "JSON object with event_type and event_payload fields, presented as a URL-encoded string. Metadata " +
                    "you post to Slack is accessible to any app or user who is a member of that workspace."),
        MRKDWN_PROPERTY = bool(MRKDWN)
            .label("Mrkdwn")
            .description(
                "Disable Slack markup parsing by setting to false. Enabled by default."),
        PARSE_PROPERTY = string(PARSE)
            .label("Parse")
            .description(
                "Change how messages are treated. See below."),
        REPLY_BROADCAST_PROPERTY = bool(REPLY_BROADCAST)
            .label("Reply broadcast")
            .description(
                "Used in conjunction with thread_ts and indicates whether reply should be made visible to everyone " +
                    "in the channel or conversation. Defaults to false."),
        THREAD_TS_PROPERTY = string(THREAD_TS)
            .label("Thread ts")
            .description(
                "Provide another message's ts value to make this message a reply. Avoid using a reply's ts value; " +
                    "use its parent instead."),
        UNFURL_LINKS_PROPERTY = bool(UNFURL_LINKS)
            .label("Unfurl links")
            .description(
                "Pass true to enable unfurling of primarily text-based content."),
        UNFURL_MEDIA_PROPERTY = bool(UNFURL_MEDIA)
            .label("Unfurl media")
            .description(
                "Pass false to disable unfurling of media content."),
        USERNAME_PROPERTY = string(USERNAME)
            .label("Username")
            .description(
                "Set your bot's user name.");

    static List<? extends Property.ValueProperty<?>> getContentTypeProperties(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return switch (inputParameters.getRequiredString(CONTENT_TYPE)) {

            case ATTACHMENTS -> List.of(
                string(ATTACHMENTS)
                    .label("Attachments")
                    .description(
                        "A JSON-based array of structured attachments, presented as a URL-encoded string.")
                    .required(true));

            case BLOCKS -> List.of(
                string(BLOCKS)
                    .label("Blocks")
                    .description(
                        "A JSON-based array of structured blocks, presented as a URL-encoded string.")
                    .required(true));

            case TEXT -> List.of(
                string(TEXT)
                    .label("Text")
                    .description(
                        "How this field works and whether it is required depends on other fields you use in your API call.")
                    .required(true));

            default -> throw new IllegalArgumentException();
        };
    }
}
