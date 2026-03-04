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

package com.bytechef.component.microsoft.teams.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.microsoft.commons.MicrosoftUtils;

/**
 * @author Monika Domiter
 */
public class MicrosoftTeamsConstants {

    public static final String ATTACHMENT = "attachment";
    public static final String ATTACHMENTS = "attachments";
    public static final String BODY = "body";
    public static final String CHANNEL_ID = "channelId";
    public static final String CHAT_ID = "chatId";
    public static final String CONTENT = "content";
    public static final String CONTENT_TYPE = "contentType";
    public static final String CONTENT_URL = "contentUrl";
    public static final String DESCRIPTION = "description";
    public static final String DISPLAY_NAME = "displayName";
    public static final String E_TAG = "eTag";
    public static final String ID = "id";
    public static final String LAST_TIME_CHECKED = "lastTimeChecked";
    public static final String NAME = "name";
    public static final String TEAM_ID = "teamId";
    public static final String VALUE = "value";
    public static final String WEB_DAV_URL = "webDavUrl";

    public static final ModifiableArrayProperty ATTACHMENTS_PROPERTY = array(ATTACHMENTS)
        .label("Attachments")
        .description(
            "The attachments to send with the message. The file to attach must already be in SharePoint.")
        .options((OptionsFunction<String>) MicrosoftUtils::getFileIdOptions)
        .required(false)
        .items(
            string(ATTACHMENT)
                .label("Attachment")
                .required(false))
        .displayCondition("%s == '%s'".formatted(CONTENT_TYPE, "html"));

    public static final ModifiableStringProperty CONTENT_TYPE_PROPERTY = string(CONTENT_TYPE)
        .label("Message Text Format")
        .options(
            option("Text", "text"),
            option("HTML", "html"))
        .defaultValue("text")
        .required(true);

    public static final ModifiableStringProperty CONTENT_PROPERTY = string(CONTENT)
        .label("Message Text")
        .controlType(ControlType.TEXT_AREA)
        .required(true);

    public static final ModifiableObjectProperty MESSAGE_OUTPUT_PROPERTY = object()
        .properties(
            string("id")
                .description("Unique identifier of the message."),
            string("replyToId")
                .description("ID of the parent message if this is a reply."),
            string("etag")
                .description("Entity tag for versioning."),
            string("messageType")
                .description("Type of the message."),
            string("createdDateTime")
                .description("Timestamp when the message was created."),
            string("lastModifiedDateTime")
                .description("Timestamp when the message was last modified."),
            string("lastEditedDateTime")
                .description("Timestamp when the message was last edited."),
            string("deletedDateTime")
                .description("Timestamp when the message was deleted."),
            string("subject")
                .description("Subject/title of the message."),
            string("summary")
                .description("Summary of the message."),
            string("chatId")
                .description("ID of the chat."),
            string("importance")
                .description("Importance level of the message."),
            string("locale")
                .description("Locale of the message."),
            string("webUrl")
                .description("Web URL to access the message."),
            string("policyViolation")
                .description("Policy violation details if applicable."),
            string("eventDetail")
                .description("Event details associated with the message."),
            object("from")
                .description("Information about the sender.")
                .properties(
                    object("application")
                        .description("Application identity of the sender."),
                    object("device")
                        .description("Device identity of the sender."),
                    object("user")
                        .description("User identity of the sender.")
                        .properties(
                            string("@odata.type")
                                .description("OData type of the user identity."),
                            string("id")
                                .description("User ID."),
                            string("displayName")
                                .description("Display name of the user."),
                            string("userIdentityType")
                                .description("Type of user identity."),
                            string("tenantId")
                                .description("Tenant ID of the user."))),
            object("body")
                .description("Plaintext/HTML representation of the content of the chat message.")
                .properties(
                    string("contentType")
                        .description("Type of the content."),
                    string("content")
                        .description("The content of the message.")),
            object("channelIdentity")
                .description("Channel identity where the message was posted.")
                .properties(
                    string("teamId")
                        .description("ID of the team."),
                    string("channelId")
                        .description("ID of the channel.")),
            array("attachments")
                .description("List of attachments included in the message."),
            array("mentions")
                .description("List of mentions in the message."),
            array("reactions")
                .description("List of reactions to the message."));

    private MicrosoftTeamsConstants() {
    }
}
