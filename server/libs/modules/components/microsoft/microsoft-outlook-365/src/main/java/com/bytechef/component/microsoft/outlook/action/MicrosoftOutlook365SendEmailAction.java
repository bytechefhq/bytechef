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

package com.bytechef.component.microsoft.outlook.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BCC_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BODY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BODY_PREVIEW;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CC_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.COMPLETED_DATE_TIME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT_PROPERTY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT_TYPE_PROPERTY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONVERSATION_ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.DATE_TIME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.DATE_TIME_PROPERTY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.DUE_DATE_TIME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FLAG;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FLAG_STATUS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FROM;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.HAS_ATTACHMENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.IMPORTANCE;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.INFERENCE_CLASSIFICATION;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.INTERNET_MESSAGE_HEADERS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.INTERNET_MESSAGE_ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.IS_DELIVERY_RECEIPT_REQUESTED;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.IS_DRAFT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.IS_READ;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.IS_READ_RECEIPT_REQUESTED;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.MESSAGE;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.NAME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.PARENT_FOLDER_ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.RECEIVED_DATE_TIME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.RECIPIENT_PROPERTY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.REPLY_TO;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SAVE_TO_SENT_ITEMS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SENDER;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SEND_EMAIL;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SENT_DATE_TIME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.START_DATE_TIME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SUBJECT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TIME_ZONE_PROPERTY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TO_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.UNIQUE_BODY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.VALUE;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.WEB_LINK;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ZONE_ID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.UserSendMailParameterSet;
import com.microsoft.graph.requests.GraphServiceClient;

/**
 * @author Monika Domiter
 */
public class MicrosoftOutlook365SendEmailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(SEND_EMAIL)
        .title("Send Email")
        .description("Send the message.")
        .properties(
            object(MESSAGE)
                .label("Message")
                .description("The message to send.")
                .properties(
                    array(BCC_RECIPIENTS)
                        .label("Bcc recipients")
                        .description("The Bcc recipients for the message.")
                        .items(RECIPIENT_PROPERTY)
                        .required(false),
                    object(BODY)
                        .label("Body")
                        .description("The body of the message. It can be in HTML or text format. ")
                        .properties(
                            CONTENT_PROPERTY,
                            CONTENT_TYPE_PROPERTY)
                        .required(true),
                    string(BODY_PREVIEW)
                        .label("Body preview")
                        .description("The first 255 characters of the message body content.")
                        .required(false),
                    array(CC_RECIPIENTS)
                        .label("Cc recipients")
                        .description("The Cc recipients for the message.")
                        .items(RECIPIENT_PROPERTY)
                        .required(false),
                    string(CONVERSATION_ID)
                        .label("Conversation ID")
                        .description("The ID of the conversation the email belongs to.")
                        .required(false),
                    object(FLAG)
                        .label("Flag")
                        .description(
                            "The flag value that indicates the status, start date, due date, or completion date for " +
                                "the message.")
                        .properties(
                            object(COMPLETED_DATE_TIME)
                                .label("Completed Date Time")
                                .description("The date and time that the follow-up was finished.")
                                .properties(
                                    DATE_TIME_PROPERTY,
                                    TIME_ZONE_PROPERTY)
                                .required(false),
                            object(DUE_DATE_TIME)
                                .label("Due date time")
                                .description(
                                    "The date and time that the follow-up is to be finished. Note: To set the due " +
                                        "date, you must also specify the startDateTime; otherwise, you get a 400 Bad " +
                                        "Request response.")
                                .properties(
                                    DATE_TIME_PROPERTY,
                                    TIME_ZONE_PROPERTY)
                                .required(false),
                            string(FLAG_STATUS)
                                .label("Flag status")
                                .description("The status for follow-up for an item.")
                                .options(
                                    option("Not flagged", "NOT_FLAGGED"),
                                    option("Complete", "COMPLETE"),
                                    option("Flagged", "FLAGGED"))
                                .required(false),
                            object(START_DATE_TIME)
                                .label("Start date time")
                                .description("The date and time that the follow-up is to begin.")
                                .properties(
                                    DATE_TIME_PROPERTY,
                                    TIME_ZONE_PROPERTY)
                                .required(false))
                        .required(false),
                    object(FROM)
                        .label("From")
                        .description(
                            "The owner of the mailbox from which the message is sent. In most cases, this value is " +
                                "the same as the sender property, except for sharing or delegation scenarios. The " +
                                "value must correspond to the actual mailbox used.")
                        .properties(RECIPIENT_PROPERTY)
                        .required(true),
                    bool(HAS_ATTACHMENTS)
                        .label("Has attachments")
                        .description(
                            "Indicates whether the message has attachments. This property doesn't include inline " +
                                "attachments, so if a message contains only inline attachments, this property is false.")
                        .required(false),
                    string(IMPORTANCE)
                        .label("Importance")
                        .description("The importance of the message.")
                        .options(
                            option("Low", "LOW"),
                            option("Normal", "NORMAL"),
                            option("High", "HIGH"))
                        .required(false),
                    string(INFERENCE_CLASSIFICATION)
                        .label("Inference classification type")
                        .description(
                            "The classification of the message for the user, based on inferred relevance or " +
                                "importance, or on an explicit override.")
                        .options(
                            option("Focused", "FOCUSED"),
                            option("Other", "OTHER"))
                        .required(false),
                    array(INTERNET_MESSAGE_HEADERS)
                        .label("Internet message headers")
                        .description(
                            "A collection of message headers defined by RFC5322. The set includes message headers " +
                                "indicating the network path taken by a message from the sender to the recipient. " +
                                "It can also contain custom message headers that hold app data for the message.")
                        .items(
                            object()
                                .properties(
                                    string(NAME)
                                        .label("Name")
                                        .description("Represents the key in a key-value pair.")
                                        .required(false),
                                    string(VALUE)
                                        .label("Value")
                                        .description("The value in a key-value pair.")
                                        .required(false)))
                        .required(false),
                    string(INTERNET_MESSAGE_ID)
                        .label("Internet message ID")
                        .description("The message ID in the format specified by RFC2822.")
                        .required(false),
                    bool(IS_DELIVERY_RECEIPT_REQUESTED)
                        .label("Is delivery receipt requested")
                        .description("Indicates whether a read receipt is requested for the message.")
                        .required(false),
                    bool(IS_DRAFT)
                        .label("Is draft")
                        .description(
                            "Indicates whether the message is a draft. A message is a draft if it hasn't been sent yet.")
                        .required(false),
                    bool(IS_READ)
                        .label("Is read")
                        .description("Indicates whether the message has been read.")
                        .required(false),
                    bool(IS_READ_RECEIPT_REQUESTED)
                        .label("Is read receipt requested")
                        .description("Indicates whether a read receipt is requested for the message.")
                        .required(false),
                    string(PARENT_FOLDER_ID)
                        .label("Parent folder ID")
                        .description("The unique identifier for the message's parent mailFolder.")
                        .required(false),
                    object(RECEIVED_DATE_TIME)
                        .label("Received datetime")
                        .description("The date and time the message was received.")
                        .properties(
                            dateTime(DATE_TIME)
                                .label("Date time")
                                .required(false),
                            string(ZONE_ID)
                                .label("Zone id")
                                .required(false))
                        .required(false),
                    array(REPLY_TO)
                        .label("Reply to")
                        .description("The email addresses to use when replying.")
                        .items(RECIPIENT_PROPERTY)
                        .required(false),
                    object(SENDER)
                        .label("Sender")
                        .description(
                            "The account that is actually used to generate the message. In most cases, this " +
                                "value is the same as the from property. You can set this property to a " +
                                "different value when sending a message from a shared mailbox, for a shared " +
                                "calendar, or as a delegate. In any case, the value must correspond to the " +
                                "actual mailbox used.")
                        .properties(RECIPIENT_PROPERTY)
                        .required(false),
                    object(SENT_DATE_TIME)
                        .label("Sent date time")
                        .description("The date and time the message was sent.")
                        .properties(
                            dateTime(DATE_TIME)
                                .label("Date time")
                                .required(false),
                            string(ZONE_ID)
                                .label("Zone id")
                                .required(false))
                        .required(false),
                    string(SUBJECT)
                        .label("Subject")
                        .description("The subject of the message.")
                        .required(true),
                    array(TO_RECIPIENTS)
                        .label("To recipients")
                        .description("The To: recipients for the message.")
                        .items(RECIPIENT_PROPERTY)
                        .required(true),
                    object(UNIQUE_BODY)
                        .label("Unique body")
                        .description("")
                        .properties(
                            CONTENT_PROPERTY,
                            CONTENT_TYPE_PROPERTY)
                        .required(false),
                    string(WEB_LINK)
                        .label("Web link")
                        .description("The URL to open the message in Outlook on the web.")
                        .required(false))
                .required(true),
            bool(SAVE_TO_SENT_ITEMS)
                .label("Save to sent items")
                .description("Indicates whether to save the message in Sent Items.")
                .defaultValue(true)
                .required(false))
        .perform(MicrosoftOutlook365SendEmailAction::perform);

    private MicrosoftOutlook365SendEmailAction() {
    }

    @SuppressWarnings("rawtypes")
    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        GraphServiceClient graphClient = MicrosoftOutlook365Utils.getGraphServiceClient();

        Message message = MicrosoftOutlook365Utils.createMessage(inputParameters);

        graphClient.me()
            .sendMail(UserSendMailParameterSet
                .newBuilder()
                .withMessage(message)
                .withSaveToSentItems(inputParameters.getBoolean(SAVE_TO_SENT_ITEMS))
                .build())
            .buildRequest()
            .post();

        return null;
    }
}
