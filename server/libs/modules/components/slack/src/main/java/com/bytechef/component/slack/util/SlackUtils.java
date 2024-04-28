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

package com.bytechef.component.slack.util;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.slack.constant.SlackConstants.AS_USER;
import static com.bytechef.component.slack.constant.SlackConstants.ATTACHMENTS;
import static com.bytechef.component.slack.constant.SlackConstants.BLOCKS;
import static com.bytechef.component.slack.constant.SlackConstants.ICON_EMOJI;
import static com.bytechef.component.slack.constant.SlackConstants.ICON_URL;
import static com.bytechef.component.slack.constant.SlackConstants.LINK_NAMES;
import static com.bytechef.component.slack.constant.SlackConstants.METADATA;
import static com.bytechef.component.slack.constant.SlackConstants.MRKDWN;
import static com.bytechef.component.slack.constant.SlackConstants.PARSE;
import static com.bytechef.component.slack.constant.SlackConstants.REPLY_BROADCAST;
import static com.bytechef.component.slack.constant.SlackConstants.TEXT;
import static com.bytechef.component.slack.constant.SlackConstants.THREAD_TS;
import static com.bytechef.component.slack.constant.SlackConstants.UNFURL_LINKS;
import static com.bytechef.component.slack.constant.SlackConstants.UNFURL_MEDIA;
import static com.bytechef.component.slack.constant.SlackConstants.USERNAME;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.slack.api.bolt.App;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.conversations.ConversationsListRequest;
import com.slack.api.methods.request.users.UsersListRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.methods.response.users.UsersListResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class SlackUtils {

    public static ChatPostMessageResponse chatPostMessage(
        Parameters inputParameters, Parameters connectionParameters, String channelParameter)
        throws SlackApiException, IOException {

        return new App()
            .client()
            .chatPostMessage(ChatPostMessageRequest
                .builder()
                .token(connectionParameters.getRequiredString(ACCESS_TOKEN))
                .channel(inputParameters.getRequiredString(channelParameter))
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

    public static List<Option<String>> getChannelOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context)
        throws IOException, SlackApiException {

        ConversationsListResponse response = new App()
            .client()
            .conversationsList(
                ConversationsListRequest
                    .builder()
                    .token(connectionParameters.getRequiredString(ACCESS_TOKEN))
                    .build());

        return response.getChannels()
            .stream()
            .map(channel -> (Option<String>) option(channel.getName(), channel.getId()))
            .toList();
    }

    public static List<Option<String>> getUserOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) throws IOException, SlackApiException {

        UsersListResponse response = new App()
            .client()
            .usersList(
                UsersListRequest.builder()
                    .token(connectionParameters.getRequiredString(ACCESS_TOKEN))
                    .build());

        return response.getMembers()
            .stream()
            .map(user -> (Option<String>) option(user.getName(), user.getId()))
            .toList();
    }
}
