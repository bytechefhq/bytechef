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

import static com.bytechef.component.twilio.constant.TwilioConstants.ACCOUNT_SID;
import static com.bytechef.component.twilio.constant.TwilioConstants.ADDRESS_RETENTION;
import static com.bytechef.component.twilio.constant.TwilioConstants.APPLICATION_SID;
import static com.bytechef.component.twilio.constant.TwilioConstants.ATTEMPT;
import static com.bytechef.component.twilio.constant.TwilioConstants.BODY;
import static com.bytechef.component.twilio.constant.TwilioConstants.CONTENT;
import static com.bytechef.component.twilio.constant.TwilioConstants.CONTENT_RETENTION;
import static com.bytechef.component.twilio.constant.TwilioConstants.CONTENT_SID;
import static com.bytechef.component.twilio.constant.TwilioConstants.CONTENT_VARIABLES;
import static com.bytechef.component.twilio.constant.TwilioConstants.FORCE_DELIVERY;
import static com.bytechef.component.twilio.constant.TwilioConstants.FROM;
import static com.bytechef.component.twilio.constant.TwilioConstants.MAX_PRICE;
import static com.bytechef.component.twilio.constant.TwilioConstants.MEDIA_URL;
import static com.bytechef.component.twilio.constant.TwilioConstants.MESSAGING_SERVICE_SID;
import static com.bytechef.component.twilio.constant.TwilioConstants.PERSISTENT_ACTION;
import static com.bytechef.component.twilio.constant.TwilioConstants.PROVIDE_FEEDBACK;
import static com.bytechef.component.twilio.constant.TwilioConstants.RETAIN;
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
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.dateTime;
import static com.bytechef.hermes.component.definition.ComponentDSL.dynamicProperties;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.number;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.option;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.PASSWORD;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.USERNAME;

import com.bytechef.component.twilio.util.TwilioUtils;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.Parameters;
import com.bytechef.hermes.definition.Property;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import java.net.URI;
import java.util.List;

/**
 * @author Monika Domiter
 */
public class TwilioSendSMSAction {

    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action(SEND_SMS)
        .title("Send SMS")
        .description("Send a new SMS message")
        .properties(
            string(ACCOUNT_SID)
                .label("Account SID")
                .description("The SID of the Account creating the Message resource.")
                .required(false),
            string(TO)
                .label("to")
                .description(
                    "The recipient's phone number in E.164 format (for SMS/MMS) or channel address, e.g. " +
                        "whatsapp:+15552229999.")
                .controlType(Property.ControlType.PHONE)
                .required(true),
            string(STATUS_CALLBACK)
                .label("Status callback")
                .description(
                    "The URL of the endpoint to which Twilio sends Message status callback requests. URL must " +
                        "contain a valid hostname and underscores are not allowed. If you include this parameter " +
                        "with the messaging_service_sid, Twilio uses this URL instead of the Status Callback URL of " +
                        "the Messaging Service.")
                .controlType(Property.ControlType.URL)
                .required(false),
            string(APPLICATION_SID)
                .label("Application SID")
                .description(
                    "The SID of the associated TwiML Application. If this parameter is provided, the status_callback " +
                        "parameter of this request is ignored; Message status callback requests are sent to the " +
                        "TwiML App's message_status_callback URL.")
                .required(false),
            number(MAX_PRICE)
                .label("Maximum price")
                .description(
                    "The maximum price in US dollars that you are willing to pay for this Message's delivery. The " +
                        "value can have up to four decimal places. When the max_price parameter is provided, the " +
                        "cost of a message is checked before it is sent. If the cost exceeds max_price, the message " +
                        "is not sent and the Message status is failed.")
                .required(false),
            bool(PROVIDE_FEEDBACK)
                .label("Provide feedback")
                .description(
                    "Boolean indicating whether or not you intend to provide delivery confirmation feedback to Twilio" +
                        " (used in conjunction with the Message Feedback subresource). Boolean indicating whether or " +
                        "not you intend to provide delivery confirmation feedback to Twilio (used in conjunction " +
                        "with the Message Feedback subresource).")
                .defaultValue(false)
                .required(false),
            integer(ATTEMPT)
                .label("Attempt")
                .description(
                    "Total number of attempts made (including this request) to send the message regardless of the " +
                        "provider used.")
                .required(false),
            integer(VALIDITY_PERIOD)
                .label("Validity period")
                .description(
                    "The maximum length in seconds that the Message can remain in Twilio's outgoing message queue. " +
                        "If a queued Message exceeds the validity_period, the Message is not sent. A validity_period " +
                        "greater than 5 is recommended.")
                .defaultValue(14400)
                .minValue(1)
                .maxValue(14400)
                .required(false),
            bool(FORCE_DELIVERY)
                .label("Force delivery")
                .description("Reserved")
                .required(false),
            string(CONTENT_RETENTION)
                .label("Content retention")
                .description("Determines if the message content can be stored or redacted based on privacy settings.")
                .options(
                    option(RETAIN, RETAIN),
                    option("discard", "discard"))
                .required(false),
            string(ADDRESS_RETENTION)
                .label("Address retention")
                .description("Determines if the address can be stored or obfuscated based on privacy settings.")
                .options(
                    option(RETAIN, RETAIN),
                    option("obfuscate", "obfuscate"))
                .required(false),
            bool(SMART_ENCODED)
                .label("Smart encoded")
                .description(
                    "Whether to detect Unicode characters that have a similar GSM-7 character and replace them.")
                .required(false),
            string(PERSISTENT_ACTION)
                .label("Persistent action")
                .description("Rich actions for non-SMS/MMS channels. Used for sending location in WhatsApp messages.")
                .required(false),
            bool(SHORTEN_URLS)
                .label("Shorten URLs")
                .description(
                    "For Messaging Services with Link Shortening configured only: A Boolean indicating whether or " +
                        "not Twilio should shorten links in the body of the Message.")
                .defaultValue(false)
                .required(false),
            string(SCHEDULE_TYPE)
                .label("Schedule type")
                .description(
                    "For Messaging Services only: Include this parameter with a value of fixed in conjuction with " +
                        "the send_time parameter in order to schedule a Message.")
                .options(
                    option("fixed", "fixed"))
                .required(false),
            dateTime(SEND_AT)
                .label("Send at")
                .description("The time that Twilio will send the message. Must be in ISO 8601 format.")
                .required(false),
            bool(SEND_AS_MMS)
                .label("Send as MMS")
                .description(
                    "If set to true, Twilio delivers the message as a single MMS message, regardless of the presence " +
                        "of media.")
                .required(false),
            bool(CONTENT_VARIABLES)
                .label("Content variables")
                .description(
                    "For Content Editor/API only: Key-value pairs of Template variables and their substitution " +
                        "values. content_sid parameter must also be provided. If values are not defined in the " +
                        "content_variables parameter, the Template's default placeholder values are used.")
                .required(false),
            string(RISK_CHECK)
                .label("Risk check")
                .description(
                    "For SMS pumping protection feature only: Include this parameter with a value of disable to skip " +
                        "any kind of risk check on the respective message request.")
                .options(
                    option("enable", "enable"),
                    option("disable", "disable"))
                .required(false),
            string(SOURCE)
                .label("Source")
                .options(
                    option("From", FROM),
                    option("Messaging Service SID", MESSAGING_SERVICE_SID))
                .required(true),
            dynamicProperties(SOURCE)
                .loadPropertiesDependsOn(SOURCE)
                .properties(TwilioUtils::getSourceProperties)
                .required(true),
            string(CONTENT)
                .label("Content")
                .options(
                    option("Body", BODY),
                    option("Media URL", MEDIA_URL))
                .required(true),
            dynamicProperties(CONTENT)
                .loadPropertiesDependsOn(MEDIA_URL)
                .properties(TwilioUtils::getContentProperties)
                .required(false),
            string(CONTENT_SID)
                .label("Content SID")
                .description(
                    "For Content Editor/API only: The SID of the Content Template to be used with the Message," +
                        " e.g., HXXXXXXXXXXXXXXXXXXXXXXXXXXXXX. If this parameter is not provided, a Content " +
                        "Template is not used. Find the SID in the Console on the Content Editor page. For Content " +
                        "API users, the SID is found in Twilio's response when creating the Template or by fetching " +
                        "your Templates.")
                .required(false))
        .outputSchema(
            object()
                .properties(
                    string("body"),
                    string("numSegments"),
                    object("direction"),
                    object("from"),
                    string("to"),
                    dateTime("dateUpdated"),
                    string("price"),
                    string("errorMessage"),
                    string("uri"),
                    string("accountSid"),
                    string("numMedia"),
                    object("status"),
                    string("messagingServiceSid"),
                    string("sid"),
                    dateTime("dateSent"),
                    dateTime("dateCreated"),
                    integer("errorCode"),
                    object("currency"),
                    string("apiVersion"),
                    object("subresourceUris")))
        .perform(TwilioSendSMSAction::perform);

    private TwilioSendSMSAction() {

    }

    public static Message perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        String username = connectionParameters.getRequiredString(USERNAME);
        String password = connectionParameters.getRequiredString(PASSWORD);

        Twilio.init(username, password);

        String to = inputParameters.getString(TO);
        String from = inputParameters.getString(FROM);
        String body = inputParameters.getString(BODY);
        String pathAccountSid = inputParameters.getString(ACCOUNT_SID);
        List<URI> mediaURL = inputParameters.getList(MEDIA_URL, URI.class);
        String messagingServiceSID = inputParameters.getString(MESSAGING_SERVICE_SID);

        // first case: to, from, body
        // second case: pathAccountSid, to, from, body
        // third case: to, from, mediaUrl
        // fourth case: pathAccountSid, to, from, mediaUrl
        // fifth case: to, messagingServiceSID, body
        // sixth case: pathAccountSid, to, messagingServiceSid, body
        // seventh case: to, messagingServiceSid, mediaUrl
        // eighth case: pathAccountSid, to, messagingServiceSid, mediaUrl

        if (isFirstCase(from, body, pathAccountSid)) {
            return Message.creator(new PhoneNumber(to), messagingServiceSID, mediaURL)
                .create();
        }

        if (isSecondCase(from, body, pathAccountSid)) {
            return Message.creator(pathAccountSid, new PhoneNumber(to), messagingServiceSID, mediaURL)
                .create();
        }

        if (isThirdCase(from, body, pathAccountSid)) {
            return Message.creator(new PhoneNumber(to), messagingServiceSID, body)
                .create();
        }

        if (isFourthCase(from, body, pathAccountSid)) {
            return Message.creator(pathAccountSid, new PhoneNumber(to), messagingServiceSID, body)
                .create();
        }

        if (isFifthCase(from, body, pathAccountSid)) {
            return Message.creator(new PhoneNumber(to), new PhoneNumber(from), mediaURL)
                .create();
        }
        if (isSixthCase(from, body, pathAccountSid)) {
            return Message.creator(pathAccountSid, new PhoneNumber(to), new PhoneNumber(from), mediaURL)
                .create();
        }

        if (isSeventhCase(from, body, pathAccountSid)) {
            return Message.creator(new PhoneNumber(to), new PhoneNumber(from), body)
                .create();
        }

        return Message.creator(pathAccountSid, new PhoneNumber(to), new PhoneNumber(from), body)
            .create();

    }

    private static boolean isSeventhCase(String from, String body, String pathAccountSid) {
        return from != null && body != null && pathAccountSid == null;
    }

    private static boolean isSixthCase(String from, String body, String pathAccountSid) {
        return from != null && body == null && pathAccountSid != null;
    }

    private static boolean isFifthCase(String from, String body, String pathAccountSid) {
        return from != null && body == null && pathAccountSid == null;
    }

    private static boolean isFourthCase(String from, String body, String pathAccountSid) {
        return from == null && body != null && pathAccountSid != null;
    }

    private static boolean isThirdCase(String from, String body, String pathAccountSid) {
        return from == null && body != null && pathAccountSid == null;
    }

    private static boolean isSecondCase(String from, String body, String pathAccountSid) {
        return from == null && body == null && pathAccountSid != null;
    }

    private static boolean isFirstCase(String from, String body, String pathAccountSid) {
        return from == null && body == null && pathAccountSid == null;
    }
}
