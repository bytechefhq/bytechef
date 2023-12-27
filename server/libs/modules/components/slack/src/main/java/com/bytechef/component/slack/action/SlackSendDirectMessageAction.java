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

package com.bytechef.component.slack.action;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.slack.constant.SlackConstants.SEND_DIRECT_MESSAGE;
import static com.bytechef.component.slack.constant.SlackConstants.TEXT;
import static com.bytechef.component.slack.constant.SlackConstants.USER_ID;
import static com.bytechef.component.slack.properties.SlackInputProperties.AS_USER;
import static com.bytechef.component.slack.properties.SlackInputProperties.AS_USER_PROPERTY;
import static com.bytechef.component.slack.properties.SlackInputProperties.ATTACHMENTS;
import static com.bytechef.component.slack.properties.SlackInputProperties.BLOCKS;
import static com.bytechef.component.slack.properties.SlackInputProperties.CONTENT_PROPERTY;
import static com.bytechef.component.slack.properties.SlackInputProperties.CONTENT_TYPE_PROPERTY;
import static com.bytechef.component.slack.properties.SlackInputProperties.ICON_EMOJI;
import static com.bytechef.component.slack.properties.SlackInputProperties.ICON_EMOJI_PROPERTY;
import static com.bytechef.component.slack.properties.SlackInputProperties.ICON_URL;
import static com.bytechef.component.slack.properties.SlackInputProperties.ICON_URL_PROPERTY;
import static com.bytechef.component.slack.properties.SlackInputProperties.LINK_NAMES;
import static com.bytechef.component.slack.properties.SlackInputProperties.LINK_NAMES_PROPERTY;
import static com.bytechef.component.slack.properties.SlackInputProperties.METADATA;
import static com.bytechef.component.slack.properties.SlackInputProperties.METADATA_PROPERTY;
import static com.bytechef.component.slack.properties.SlackInputProperties.MRKDWN;
import static com.bytechef.component.slack.properties.SlackInputProperties.MRKDWN_PROPERTY;
import static com.bytechef.component.slack.properties.SlackInputProperties.PARSE;
import static com.bytechef.component.slack.properties.SlackInputProperties.PARSE_PROPERTY;
import static com.bytechef.component.slack.properties.SlackInputProperties.REPLY_BROADCAST;
import static com.bytechef.component.slack.properties.SlackInputProperties.REPLY_BROADCAST_PROPERTY;
import static com.bytechef.component.slack.properties.SlackInputProperties.THREAD_TS;
import static com.bytechef.component.slack.properties.SlackInputProperties.THREAD_TS_PROPERTY;
import static com.bytechef.component.slack.properties.SlackInputProperties.UNFURL_LINKS;
import static com.bytechef.component.slack.properties.SlackInputProperties.UNFURL_LINKS_PROPERTY;
import static com.bytechef.component.slack.properties.SlackInputProperties.UNFURL_MEDIA;
import static com.bytechef.component.slack.properties.SlackInputProperties.UNFURL_MEDIA_PROPERTY;
import static com.bytechef.component.slack.properties.SlackInputProperties.USERNAME;
import static com.bytechef.component.slack.properties.SlackInputProperties.USERNAME_PROPERTY;
import static com.bytechef.component.slack.properties.SlackOutputProperties.CHAT_POST_MESSAGE_RESPONSE_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.Parameters;
import com.slack.api.bolt.App;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.users.UsersListRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.users.UsersListResponse;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Mario Cvjetojevic
 */
public final class SlackSendDirectMessageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(SEND_DIRECT_MESSAGE)
        .title("Send direct message")
        .description(
            "Sends a direct message to another user in a workspace. If it hasn't already, a direct message" +
                " conversation will be created.")
        .properties(
            string(USER_ID)
                .label("User")
                .description(
                    "The id of a user to send the direct message to.")
                .options((OptionsDataSource.ActionOptionsFunction) SlackSendDirectMessageAction::getUserOptions)
                .required(true),
            CONTENT_TYPE_PROPERTY,
            CONTENT_PROPERTY,
            AS_USER_PROPERTY,
            ICON_EMOJI_PROPERTY,
            ICON_URL_PROPERTY,
            LINK_NAMES_PROPERTY,
            METADATA_PROPERTY,
            MRKDWN_PROPERTY,
            PARSE_PROPERTY,
            REPLY_BROADCAST_PROPERTY,
            THREAD_TS_PROPERTY,
            UNFURL_LINKS_PROPERTY,
            UNFURL_MEDIA_PROPERTY,
            USERNAME_PROPERTY)
        .outputSchema(CHAT_POST_MESSAGE_RESPONSE_PROPERTY)
        .perform(SlackSendDirectMessageAction::perform);

    private SlackSendDirectMessageAction() {
    }

    public static ChatPostMessageResponse perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext)
        throws SlackApiException, IOException {

        return new App()
            .client()
            .chatPostMessage(ChatPostMessageRequest
                .builder()
                .token(connectionParameters.getRequiredString(ACCESS_TOKEN))
                .channel(inputParameters.getRequiredString(USER_ID))
                .attachmentsAsString(inputParameters.getString(ATTACHMENTS))
                .blocksAsString(inputParameters.getString(BLOCKS))
                .text(inputParameters.getString(TEXT))
                .asUser(inputParameters.getBoolean(AS_USER))
                .iconEmoji(inputParameters.getString(ICON_EMOJI))
                .iconUrl(inputParameters.getString(ICON_URL))
                .linkNames(inputParameters.getBoolean(LINK_NAMES))
                .metadataAsString(inputParameters.getString(METADATA))
                .mrkdwn(inputParameters.getBoolean(MRKDWN))
                .parse(inputParameters.getString(PARSE))
                .replyBroadcast(inputParameters.getBoolean(REPLY_BROADCAST))
                .threadTs(inputParameters.getString(THREAD_TS))
                .unfurlLinks(inputParameters.getBoolean(UNFURL_LINKS))
                .unfurlMedia(inputParameters.getBoolean(UNFURL_MEDIA))
                .username(inputParameters.getString(USERNAME))
                .build());
    }

    public static List<Option<String>> getUserOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context)
        throws IOException, SlackApiException {

        UsersListResponse response = new App()
            .client()
            .usersList(UsersListRequest
                .builder()
                .token(connectionParameters.getRequiredString(ACCESS_TOKEN))
                .build());

        List<Option<String>> options = response.getMembers()
            .stream()
            .filter(user -> StringUtils.isNotEmpty(searchText) &&
                StringUtils.startsWith(user.getName(), searchText))
            .map(user -> (Option<String>) option(user.getName(), user.getId()))
            .toList();

        return options;
    }
}
