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

package com.bytechef.component.microsoft.teams.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.ATTACHMENTS;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.ATTACHMENTS_PROPERTY;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.BODY;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CHANNEL_ID;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CONTENT;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CONTENT_PROPERTY;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CONTENT_TYPE;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CONTENT_TYPE_PROPERTY;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.MESSAGE_ID;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.TEAM_ID;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.teams.util.MicrosoftTeamsUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class MicrosoftTeamsReplyToChannelMessageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("replyToChannelMessage")
        .title("Reply to Channel Message")
        .description(
            "Sends a reply to a channel message. Sending attachments is supported with Message Text Format is set to " +
                "\"html\".")
        .properties(
            string(TEAM_ID)
                .label("Team ID")
                .description("ID of the team where the channel is located.")
                .options((OptionsFunction<String>) MicrosoftTeamsUtils::getTeamIdOptions)
                .required(true),
            string(CHANNEL_ID)
                .label("Channel ID")
                .description("Channel of the message that will get a reply.")
                .optionsLookupDependsOn(TEAM_ID)
                .options((OptionsFunction<String>) MicrosoftTeamsUtils::getChannelIdOptions)
                .required(true),
            string(MESSAGE_ID)
                .label("Message ID")
                .description("ID of the message that will get a reply.")
                .required(true),
            CONTENT_TYPE_PROPERTY,
            CONTENT_PROPERTY,
            ATTACHMENTS_PROPERTY)
        .output(outputSchema(MESSAGE_OUTPUT_PROPERTY))
        .perform(MicrosoftTeamsReplyToChannelMessageAction::perform)
        .help(
            "",
            "https://docs.bytechef.io/reference/components/microsoft-teams_v1#reply-to-message")
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftTeamsReplyToChannelMessageAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<String> fileIds = inputParameters.getList(ATTACHMENTS, String.class, List.of());
        List<Map<String, String>> attachments = MicrosoftTeamsUtils.getAttachmentsList(fileIds, context);

        String htmlAttachmentsTag = MicrosoftTeamsUtils.getHtmlAttachmentsTag(attachments);

        return context
            .http(http -> http.post(
                "/teams/%s/channels/%s/messages/%s/replies".formatted(
                    inputParameters.getRequiredString(TEAM_ID),
                    inputParameters.getRequiredString(CHANNEL_ID),
                    inputParameters.getRequiredString(MESSAGE_ID))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(
                Http.Body.of(
                    BODY, Map.of(
                        CONTENT, inputParameters.getRequiredString(CONTENT) + htmlAttachmentsTag,
                        CONTENT_TYPE, inputParameters.getRequiredString(CONTENT_TYPE)),
                    ATTACHMENTS, attachments))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
