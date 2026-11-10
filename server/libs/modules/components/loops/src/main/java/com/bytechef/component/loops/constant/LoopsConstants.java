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

package com.bytechef.component.loops.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import java.util.List;

/**
 * @author Nikolina Spehar
 */
public class LoopsConstants {

    public static final String AUDIENCE_SEGMENT_ID = "audienceSegmentId";
    public static final String CAMPAIGN_GROUP_ID = "campaignGroupId";
    public static final String CAMPAIGN_ID = "campaignId";
    public static final String EMAIL = "email";
    public static final String EMAIL_MESSAGE_ID = "emailMessageId";
    public static final String EXPECTED_REVISION_ID = "expectedRevisionId";
    public static final String FIRST_NAME = "firstName";
    public static final String FROM_EMAIL = "fromEmail";
    public static final String FROM_NAME = "fromName";
    public static final String ID = "id";
    public static final String LAST_NAME = "lastName";
    public static final String LMX = "lmx";
    public static final String MAILING_LIST_ID = "mailingListId";
    public static final String MAILING_LISTS = "mailingLists";
    public static final String NAME = "name";
    public static final String PREVIEW_TEXT = "previewText";
    public static final String REPLY_TO_EMAIL = "replyToEmail";
    public static final String SUBJECT = "subject";
    public static final String USER_GROUP = "userGroup";
    public static final String USER_ID = "userId";

    public static final List<ModifiableStringProperty> CAMPAIGN_PROPERTIES = List.of(
        string(ID)
            .description("The ID of the campaign."),
        string("url")
            .description("The URL of the campaign in the Loops app."),
        string(NAME)
            .description("The name of the campaign."),
        string("status")
            .description("The status of the campaign, for example Draft or Sent."),
        string("createdAt")
            .description("ISO 8601 timestamp for when the campaign was created."),
        string("updatedAt")
            .description("ISO 8601 timestamp for when the campaign was last updated."),
        string(EMAIL_MESSAGE_ID)
            .description("The ID of the campaign's email message."),
        string(CAMPAIGN_GROUP_ID)
            .description("The ID of the campaign group this campaign belongs to, if set."),
        string(MAILING_LIST_ID)
            .description("The ID of the mailing list this campaign sends to, if set."),
        string(AUDIENCE_SEGMENT_ID)
            .description("The ID of the audience segment this campaign targets, if set."));

    public static final ModifiableObjectProperty CAMPAIGN_OUTPUT_PROPERTY = object()
        .properties(CAMPAIGN_PROPERTIES);

    public static final ModifiableObjectProperty EMAIL_MESSAGE_OUTPUT_PROPERTY = object()
        .properties(
            string(ID)
                .description("The ID of the email message."),
            string(CAMPAIGN_ID)
                .description("The campaign this email message belongs to, if any."),
            string("transactionalId")
                .description("The transactional email this email message belongs to, if any."),
            string(SUBJECT)
                .description("The email subject."),
            string(PREVIEW_TEXT)
                .description("The email preview text."),
            string(FROM_NAME)
                .description("The email sender name."),
            string(FROM_EMAIL)
                .description("The sender email address, without the team's sending domain."),
            string(REPLY_TO_EMAIL)
                .description("The email reply-to address."),
            string("emailFormat")
                .description("The rendering format of the email: styled or plain."),
            string(LMX)
                .description("The email body serialized as LMX."),
            string("contentRevisionId")
                .description("The current content revision. Pass it as Expected Revision ID on the next update."),
            string("updatedAt")
                .description("ISO 8601 timestamp for when the email message was last updated."),
            array("warnings")
                .description("Non-fatal issues raised while compiling the submitted LMX.")
                .items(
                    object()
                        .properties(
                            string("rule"),
                            string("severity"),
                            string("message"),
                            string("path"))));

    private LoopsConstants() {
    }
}
