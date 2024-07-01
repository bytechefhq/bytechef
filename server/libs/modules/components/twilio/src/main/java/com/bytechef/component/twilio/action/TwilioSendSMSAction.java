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

package com.bytechef.component.twilio.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.twilio.constant.TwilioConstants.ACCOUNT_SID;
import static com.bytechef.component.twilio.constant.TwilioConstants.ADDRESS_RETENTION;
import static com.bytechef.component.twilio.constant.TwilioConstants.APPLICATION_SID;
import static com.bytechef.component.twilio.constant.TwilioConstants.ATTEMPT;
import static com.bytechef.component.twilio.constant.TwilioConstants.AUTH_TOKEN;
import static com.bytechef.component.twilio.constant.TwilioConstants.BASE_URL;
import static com.bytechef.component.twilio.constant.TwilioConstants.BODY;
import static com.bytechef.component.twilio.constant.TwilioConstants.CONTENT;
import static com.bytechef.component.twilio.constant.TwilioConstants.CONTENT_RETENTION;
import static com.bytechef.component.twilio.constant.TwilioConstants.CONTENT_SID;
import static com.bytechef.component.twilio.constant.TwilioConstants.CONTENT_VARIABLES;
import static com.bytechef.component.twilio.constant.TwilioConstants.DATE_TIME;
import static com.bytechef.component.twilio.constant.TwilioConstants.FORCE_DELIVERY;
import static com.bytechef.component.twilio.constant.TwilioConstants.FROM;
import static com.bytechef.component.twilio.constant.TwilioConstants.MAX_PRICE;
import static com.bytechef.component.twilio.constant.TwilioConstants.MEDIA_URL;
import static com.bytechef.component.twilio.constant.TwilioConstants.MESSAGING_SERVICE_SID;
import static com.bytechef.component.twilio.constant.TwilioConstants.PERSISTENT_ACTION;
import static com.bytechef.component.twilio.constant.TwilioConstants.PROVIDE_FEEDBACK;
import static com.bytechef.component.twilio.constant.TwilioConstants.RISK_CHECK;
import static com.bytechef.component.twilio.constant.TwilioConstants.SCHEDULE_TYPE;
import static com.bytechef.component.twilio.constant.TwilioConstants.SEND_AS_MMS;
import static com.bytechef.component.twilio.constant.TwilioConstants.SEND_AT;
import static com.bytechef.component.twilio.constant.TwilioConstants.SEND_SMS;
import static com.bytechef.component.twilio.constant.TwilioConstants.SHORTEN_URLS;
import static com.bytechef.component.twilio.constant.TwilioConstants.SMART_ENCODED;
import static com.bytechef.component.twilio.constant.TwilioConstants.SOURCE;
import static com.bytechef.component.twilio.constant.TwilioConstants.STATUS_CALLBACK;
import static com.bytechef.component.twilio.constant.TwilioConstants.TO;
import static com.bytechef.component.twilio.constant.TwilioConstants.VALIDITY_PERIOD;
import static com.bytechef.component.twilio.constant.TwilioConstants.ZONE_ID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.twilio.util.TwilioUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 * @author Luka Ljubić
 */
public class TwilioSendSMSAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(SEND_SMS)
        .title("Send SMS")
        .description("Send a new SMS message")
        .properties(
            string(ACCOUNT_SID)
                .label("Account SID")
                .description("The SID of the Account creating the Message resource.")
                .required(false)
                .advancedOption(true),
            string(TO)
                .label("To")
                .description(
                    "The recipient's phone number in E.164 format (for SMS/MMS) or channel address, e.g. " +
                        "whatsapp:+15552229999.")
                .controlType(ControlType.PHONE)
                .required(true),
            integer(SOURCE)
                .label("Source")
                .options(
                    option("From", 1),
                    option("Messaging Service SID", 2))
                .required(true),
            string(FROM)
                .label("From")
                .description(
                    "The sender's Twilio phone number (in E.164 format), alphanumeric sender ID, Wireless SIM, short " +
                        "code, or channel address (e.g., whatsapp:+15554449999). The value of the from parameter " +
                        "must be a sender that is hosted within Twilio and belongs to the Account creating the " +
                        "Message. If you are using messaging_service_sid, this parameter can be empty (Twilio " +
                        "assigns a from value from the Messaging Service's Sender Pool) or you can provide a " +
                        "specific sender from your Sender Pool.")
                .displayCondition("%s == %s".formatted(SOURCE, 1))
                .controlType(ControlType.PHONE)
                .required(true),
            string(MESSAGING_SERVICE_SID)
                .label("Messaging Service SID")
                .description(
                    "The SID of the Messaging Service you want to associate with the Message. When this parameter is " +
                        "provided and the from parameter is omitted, Twilio selects the optimal sender from the " +
                        "Messaging Service's Sender Pool. You may also provide a from parameter if you want to use a " +
                        "specific Sender from the Sender Pool.")
                .displayCondition("%s == %s".formatted(SOURCE, 2))
                .required(true),
            integer(CONTENT)
                .label("Content")
                .options(
                    option("Body", 1),
                    option("Media URL", 2))
                .required(false)
                .advancedOption(true),
            string(BODY)
                .label("Body")
                .description(
                    "The text content of the outgoing message. Can be up to 1,600 characters in length. SMS only: If " +
                        "the body contains more than 160 GSM-7 characters (or 70 UCS-2 characters), the message is " +
                        "segmented and charged accordingly. For long body text, consider using the send_as_mms " +
                        "parameter.")
                .maxLength(1600)
                .displayCondition("%s == %s".formatted(CONTENT, 1))
                .required(true),
            array(MEDIA_URL)
                .label("Media URL")
                .description(
                    "The URL of media to include in the Message content. jpeg, jpg, gif, and png file types are " +
                        "fully supported by Twilio and content is formatted for delivery on destination devices. The " +
                        "media size limit is 5 MB for supported file types (jpeg, jpg, png, gif) and 500 KB for " +
                        "other types of accepted media. To send more than one image in the message, provide multiple " +
                        "media_url parameters in the POST request. You can include up to ten media_url parameters " +
                        "per message. International and carrier limits apply.")
                .items(
                    string()
                        .controlType(ControlType.URL))
                .required(false)
                .advancedOption(true),
            string(STATUS_CALLBACK)
                .label("Status callback")
                .description(
                    "The URL of the endpoint to which Twilio sends Message status callback requests. URL must " +
                        "contain a valid hostname and underscores are not allowed. If you include this parameter " +
                        "with the messaging_service_sid, Twilio uses this URL instead of the Status Callback URL of " +
                        "the Messaging Service.")
                .controlType(ControlType.URL)
                .required(false)
                .advancedOption(true),
            string(APPLICATION_SID)
                .label("Application SID")
                .description(
                    "The SID of the associated TwiML Application. If this parameter is provided, the status_callback " +
                        "parameter of this request is ignored; Message status callback requests are sent to the " +
                        "TwiML App's message_status_callback URL.")
                .required(false)
                .advancedOption(true),
            number(MAX_PRICE)
                .label("Maximum price")
                .description(
                    "The maximum price in US dollars that you are willing to pay for this Message's delivery. When " +
                        "the max_price parameter is provided, the cost of a message is checked before it is sent. If " +
                        "the cost exceeds max_price, the message is not sent and the Message status is failed.")
                .maxNumberPrecision(4)
                .required(false)
                .advancedOption(true),
            bool(PROVIDE_FEEDBACK)
                .label("Provide feedback")
                .description(
                    "Boolean indicating whether or not you intend to provide delivery confirmation feedback to Twilio" +
                        " (used in conjunction with the Message Feedback subresource). Boolean indicating whether or " +
                        "not you intend to provide delivery confirmation feedback to Twilio (used in conjunction " +
                        "with the Message Feedback subresource).")
                .defaultValue(false)
                .required(false)
                .advancedOption(true),
            integer(ATTEMPT)
                .label("Attempt")
                .description(
                    "Total number of attempts made (including this request) to send the message regardless of the " +
                        "provider used.")
                .required(false)
                .advancedOption(true),
            integer(VALIDITY_PERIOD)
                .label("Validity period")
                .description(
                    "The maximum length in seconds that the Message can remain in Twilio's outgoing message queue. " +
                        "If a queued Message exceeds the validity_period, the Message is not sent. A validity_period " +
                        "greater than 5 is recommended.")
                .defaultValue(14400)
                .minValue(1)
                .maxValue(14400)
                .required(false)
                .advancedOption(true),
            bool(FORCE_DELIVERY)
                .label("Force delivery")
                .description("Reserved")
                .required(false)
                .advancedOption(true),
            string(CONTENT_RETENTION)
                .label("Content retention")
                .description("Determines if the message content can be stored or redacted based on privacy settings.")
                .options(
                    option("Retain", "retain"),
                    option("Discard", "discard"))
                .required(false)
                .advancedOption(true),
            string(ADDRESS_RETENTION)
                .label("Address retention")
                .description("Determines if the address can be stored or obfuscated based on privacy settings.")
                .options(
                    option("Retain", "retain"),
                    option("Obfuscate", "obfuscate"))
                .required(false)
                .advancedOption(true),
            bool(SMART_ENCODED)
                .label("Smart encoded")
                .description(
                    "Whether to detect Unicode characters that have a similar GSM-7 character and replace them.")
                .required(false)
                .advancedOption(true),
            array(PERSISTENT_ACTION)
                .label("Persistent action")
                .description("Rich actions for non-SMS/MMS channels. Used for sending location in WhatsApp messages.")
                .items(
                    string())
                .required(false)
                .advancedOption(true),
            bool(SHORTEN_URLS)
                .label("Shorten URLs")
                .description(
                    "For Messaging Services with Link Shortening configured only: A Boolean indicating whether or " +
                        "not Twilio should shorten links in the body of the Message.")
                .defaultValue(false)
                .required(false)
                .advancedOption(true),
            string(SCHEDULE_TYPE)
                .label("Schedule type")
                .description(
                    "For Messaging Services only: Include this parameter with a value of fixed in conjuction with " +
                        "the send_time parameter in order to schedule a Message.")
                .options(
                    option("fixed", "fixed"))
                .required(false)
                .advancedOption(true),
            object(SEND_AT)
                .label("Send at")
                .description("The time that Twilio will send the message. Must be in ISO 8601 format.")
                .properties(
                    dateTime(DATE_TIME)
                        .label("Date time")
                        .required(true),
                    string(ZONE_ID)
                        .label("Zone ID")
                        .options((ActionOptionsFunction<String>) TwilioUtils::getZoneIdOptions)
                        .required(true))
                .required(false)
                .advancedOption(true),
            bool(SEND_AS_MMS)
                .label("Send as MMS")
                .description(
                    "If set to true, Twilio delivers the message as a single MMS message, regardless of the presence " +
                        "of media.")
                .required(false)
                .advancedOption(true),
            string(CONTENT_VARIABLES)
                .label("Content variables")
                .description(
                    "For Content Editor/API only: Key-value pairs of Template variables and their substitution " +
                        "values. content_sid parameter must also be provided. If values are not defined in the " +
                        "content_variables parameter, the Template's default placeholder values are used.")
                .required(false)
                .advancedOption(true),
            string(RISK_CHECK)
                .label("Risk check")
                .description(
                    "For SMS pumping protection feature only: Include this parameter with a value of disable to skip " +
                        "any kind of risk check on the respective message request.")
                .options(
                    option("enable", "enable"),
                    option("disable", "disable"))
                .required(false)
                .advancedOption(true),
            string(CONTENT_SID)
                .label("Content SID")
                .description(
                    "For Content Editor/API only: The SID of the Content Template to be used with the Message," +
                        " e.g., HXXXXXXXXXXXXXXXXXXXXXXXXXXXXX. If this parameter is not provided, a Content " +
                        "Template is not used. Find the SID in the Console on the Content Editor page. For Content " +
                        "API users, the SID is found in Twilio's response when creating the Template or by fetching " +
                        "your Templates.")
                .displayCondition("%s == %s".formatted(CONTENT, 2))
                .required(false)
                .advancedOption(true))
        .outputSchema(
            object()
                .properties(
                    string("body"),
                    string("numSegments"),
                    string("direction"),
                    object("from")
                        .properties(
                            string("rawNumber")),
                    string("to"),
                    object("dateUpdated")
                        .properties(
                            dateTime(DATE_TIME),
                            string(ZONE_ID)),
                    string("price"),
                    string("errorMessage"),
                    string("uri"),
                    string("accountSid"),
                    string("numMedia"),
                    string("status"),
                    string("messagingServiceSid"),
                    string("sid"),
                    object("dateSent")
                        .properties(
                            dateTime(DATE_TIME),
                            string(ZONE_ID)),
                    object("dateCreated")
                        .properties(
                            dateTime(DATE_TIME),
                            string(ZONE_ID)),
                    integer("errorCode"),
                    object("currency")
                        .properties(
                            string("currencyCode"),
                            integer("defaultFractionDigits"),
                            integer("numericCode")),
                    string("apiVersion"),
                    object("subresourceUris")
                        .additionalProperties(string())))
        .perform(TwilioSendSMSAction::perform);

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .http(http -> http.post(BASE_URL + connectionParameters.getRequiredString(ACCOUNT_SID)
                + "/Messages.json"))
            .headers(
                Map.of("username", List.of(connectionParameters.getRequiredString(ACCOUNT_SID)),
                    "password", List.of(connectionParameters.getRequiredString(AUTH_TOKEN))))
            .body(
                Body.of(
                    BODY, inputParameters.getRequiredString(BODY),
                    FROM, inputParameters.getRequiredString(FROM),
                    TO, inputParameters.getRequiredString(TO)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private TwilioSendSMSAction() {
    }
}
