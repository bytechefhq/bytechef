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

package com.bytechef.component.infobip.action;

import static com.bytechef.component.definition.Authorization.VALUE;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.infobip.constant.InfobipConstants.ACTION;
import static com.bytechef.component.infobip.constant.InfobipConstants.AMOUNT;
import static com.bytechef.component.infobip.constant.InfobipConstants.APPLICATION_ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.BASE_URL;
import static com.bytechef.component.infobip.constant.InfobipConstants.BRAND_CODE;
import static com.bytechef.component.infobip.constant.InfobipConstants.BULK_ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.CALLBACK_DATA;
import static com.bytechef.component.infobip.constant.InfobipConstants.CONTENT_TEMPLATE_ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.CUSTOM_DOMAIN;
import static com.bytechef.component.infobip.constant.InfobipConstants.DATE_TIME;
import static com.bytechef.component.infobip.constant.InfobipConstants.DAYS;
import static com.bytechef.component.infobip.constant.InfobipConstants.DELIVERY_TIME_WINDOW;
import static com.bytechef.component.infobip.constant.InfobipConstants.DESCRIPTION;
import static com.bytechef.component.infobip.constant.InfobipConstants.DESTINATIONS;
import static com.bytechef.component.infobip.constant.InfobipConstants.ENTITY_ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.FLASH;
import static com.bytechef.component.infobip.constant.InfobipConstants.FROM;
import static com.bytechef.component.infobip.constant.InfobipConstants.GROUP_ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.GROUP_NAME;
import static com.bytechef.component.infobip.constant.InfobipConstants.HOUR;
import static com.bytechef.component.infobip.constant.InfobipConstants.ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.INCLUDE_SMS_COUNT_IN_RESPONSE;
import static com.bytechef.component.infobip.constant.InfobipConstants.INDIA_DLT;
import static com.bytechef.component.infobip.constant.InfobipConstants.INTERMEDIATE_REPORT;
import static com.bytechef.component.infobip.constant.InfobipConstants.LANGUAGE;
import static com.bytechef.component.infobip.constant.InfobipConstants.LANGUAGE_CODE;
import static com.bytechef.component.infobip.constant.InfobipConstants.MESSAGES;
import static com.bytechef.component.infobip.constant.InfobipConstants.MESSAGE_ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.MINUTE;
import static com.bytechef.component.infobip.constant.InfobipConstants.MINUTE_LABEL;
import static com.bytechef.component.infobip.constant.InfobipConstants.NAME;
import static com.bytechef.component.infobip.constant.InfobipConstants.NOTIFY_CONTENT_TYPE;
import static com.bytechef.component.infobip.constant.InfobipConstants.NOTIFY_URL;
import static com.bytechef.component.infobip.constant.InfobipConstants.PRINCIPAL_ENTITY_ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.PROCESS_KEY;
import static com.bytechef.component.infobip.constant.InfobipConstants.RECIPIENT_TYPE;
import static com.bytechef.component.infobip.constant.InfobipConstants.REGIONAL;
import static com.bytechef.component.infobip.constant.InfobipConstants.REMOVE_PROTOCOL;
import static com.bytechef.component.infobip.constant.InfobipConstants.RESELLER_CODE;
import static com.bytechef.component.infobip.constant.InfobipConstants.SENDING_SPEED_LIMIT;
import static com.bytechef.component.infobip.constant.InfobipConstants.SEND_AT;
import static com.bytechef.component.infobip.constant.InfobipConstants.SEND_SMS;
import static com.bytechef.component.infobip.constant.InfobipConstants.SHORTEN_URL;
import static com.bytechef.component.infobip.constant.InfobipConstants.SMS_COUNT;
import static com.bytechef.component.infobip.constant.InfobipConstants.SOUTH_KOREA;
import static com.bytechef.component.infobip.constant.InfobipConstants.STATUS;
import static com.bytechef.component.infobip.constant.InfobipConstants.TEXT;
import static com.bytechef.component.infobip.constant.InfobipConstants.TIME_UNIT;
import static com.bytechef.component.infobip.constant.InfobipConstants.TO;
import static com.bytechef.component.infobip.constant.InfobipConstants.TRACK;
import static com.bytechef.component.infobip.constant.InfobipConstants.TRACKING;
import static com.bytechef.component.infobip.constant.InfobipConstants.TRACKING_URL;
import static com.bytechef.component.infobip.constant.InfobipConstants.TRACK_CLICKS;
import static com.bytechef.component.infobip.constant.InfobipConstants.TRANSLITERATION;
import static com.bytechef.component.infobip.constant.InfobipConstants.TURKEY_IYS;
import static com.bytechef.component.infobip.constant.InfobipConstants.TYPE;
import static com.bytechef.component.infobip.constant.InfobipConstants.URL_OPTIONS;
import static com.bytechef.component.infobip.constant.InfobipConstants.VALIDITY_PERIOD;
import static com.bytechef.component.infobip.constant.InfobipConstants.ZONE_ID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.infobip.util.InfobipUtils;
import com.infobip.ApiClient;
import com.infobip.ApiException;
import com.infobip.ApiKey;
import com.infobip.api.SmsApi;
import com.infobip.model.SmsAdvancedTextualRequest;
import com.infobip.model.SmsResponse;
import com.infobip.model.SmsSendingSpeedLimit;
import com.infobip.model.SmsSpeedLimitTimeUnit;
import com.infobip.model.SmsTextualMessage;
import com.infobip.model.SmsTracking;
import com.infobip.model.SmsUrlOptions;
import java.util.List;

/**
 * @author Monika Domiter
 */
public class InfobipSendSMSAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(SEND_SMS)
        .title("Send SMS")
        .description("Send a new SMS message")
        .properties(
            string(BULK_ID)
                .label("Bulk ID")
                .description(
                    "Unique ID assigned to the request if messaging multiple recipients or sending multiple messages " +
                        "via a single API request. If not provided, it will be auto-generated and returned in the " +
                        "API response. Typically, used to fetch delivery reports and message logs.")
                .required(false),
            array(MESSAGES)
                .label("Messages")
                .description(
                    "An array of message objects of a single message or multiple messages sent under one bulk ID.")
                .items(
                    string(CALLBACK_DATA)
                        .label("Callback data")
                        .description(
                            "Additional data that can be used for identifying, managing, or monitoring a message. " +
                                "Data included here will also be automatically included in the message Delivery " +
                                "Report.")
                        .maxLength(4000)
                        .required(false),
                    object(DELIVERY_TIME_WINDOW)
                        .label("Delivery time window")
                        .description(
                            "Sets specific SMS delivery window outside of which messages won't be delivered. Often, " +
                                "used when there are restrictions on when messages can be sent.")
                        .properties(
                            array(DAYS)
                                .label("Days")
                                .description("Days of the week which are included in the delivery time window.")
                                .items(
                                    string()
                                        .options(
                                            option("Monday", "MONDAY"),
                                            option("Tuesday", "TUESDAY"),
                                            option("Wednesday", "WEDNESDAY"),
                                            option("Thursday", "THURSDAY"),
                                            option("Friday", "FRIDAY"),
                                            option("Saturday", "SATURDAY"),
                                            option("Sunday", "SUNDAY")))
                                .minItems(1)
                                .required(true),
                            object(FROM)
                                .label("From")
                                .description(
                                    "The exact time of day to start sending messages. Time is expressed in the UTC " +
                                        "time zone. If set, use it together with the to property with minimum 1 hour " +
                                        "difference.")
                                .properties(
                                    integer(HOUR)
                                        .label("Hour")
                                        .description("Hour when the time window opens.")
                                        .maxValue(23)
                                        .required(true),
                                    integer(MINUTE)
                                        .label(MINUTE_LABEL)
                                        .description("Minute when the time window opens.")
                                        .maxValue(59)
                                        .required(true))
                                .required(false),
                            object(TO)
                                .label("To")
                                .description(
                                    "The exact time of day to end sending messages. Time is expressed in the UTC " +
                                        "time zone. If set, use it together with the from property with minimum 1 " +
                                        "hour difference.")
                                .properties(
                                    integer(HOUR)
                                        .label("Hour")
                                        .description("Hour when the time window closes.")
                                        .maxValue(23)
                                        .required(true),
                                    integer(MINUTE)
                                        .label(MINUTE_LABEL)
                                        .description("Minute when the time window closes.")
                                        .maxValue(59)
                                        .required(true))
                                .required(false))
                        .required(false),
                    array(DESTINATIONS)
                        .label("Destinations")
                        .description(
                            "An array of destination objects for where messages are being sent. A valid destination " +
                                "is required.")
                        .items(
                            string(MESSAGE_ID)
                                .label("Message id")
                                .description("The ID that uniquely identifies the message sent.")
                                .required(false),
                            string(TO)
                                .label("To")
                                .description("Message destination address. Addresses must be in international format")
                                .exampleValue("41793026727")
                                .maxLength(50)
                                .required(true))
                        .required(true),
                    bool(FLASH)
                        .label("Flash")
                        .description(
                            "Allows for sending a flash SMS to automatically appear on recipient devices without " +
                                "interaction. Set to true to enable flash SMS, or leave the default value, false to " +
                                "send a standard SMS.")
                        .defaultValue(false)
                        .required(false),
                    string(FROM)
                        .label("From")
                        .description(
                            "The sender ID which can be alphanumeric or numeric (e.g., CompanyName). Make sure you " +
                                "don't exceed character limit.")
                        .required(false),
                    bool(INTERMEDIATE_REPORT)
                        .label("Intermediate report")
                        .description(
                            "The real-time intermediate delivery report containing GSM error codes, messages status, " +
                                "pricing, network and country codes, etc., which will be sent on your callback server.")
                        .defaultValue(false)
                        .required(false),
                    object(LANGUAGE)
                        .label("Language")
                        .properties(
                            string(LANGUAGE_CODE)
                                .label("Language code")
                                .description("Language code for the correct character set.")
                                .options(
                                    option("Turkish", "TR"),
                                    option("Spanish", "ES"),
                                    option("Portuguese", "PT"),
                                    option("Autodetect", "AUTODETECT",
                                        "Platform select the character set based on message content."))
                                .required(false))
                        .required(false),
                    string(NOTIFY_CONTENT_TYPE)
                        .label("Notify content type")
                        .description("Preferred delivery report content type.")
                        .options(
                            option("application/json", "application/json"),
                            option("application/xml", "application/xml"))
                        .required(false),
                    string(NOTIFY_URL)
                        .label("Notify url")
                        .description(
                            "The URL on your call back server on to which a delivery report will be sent. The retry " +
                                "cycle for when your URL becomes unavailable uses the following formula: " +
                                "1min + (1min * retryNumber * retryNumber).")
                        .required(false),
                    object(REGIONAL)
                        .label("Regional")
                        .description("Region-specific parameters, often imposed by local laws. Use this, if country " +
                            "or region that you are sending an SMS to requires additional information.")
                        .properties(
                            object(INDIA_DLT)
                                .label("India DLT")
                                .description(
                                    "Distributed Ledger Technology (DLT) specific parameters required for sending " +
                                        "SMS to phone numbers registered in India.")
                                .properties(
                                    string(CONTENT_TEMPLATE_ID)
                                        .label("Content template ID")
                                        .description(
                                            "Registered DLT content template ID which matches message you are sending.")
                                        .required(false),
                                    string(PRINCIPAL_ENTITY_ID)
                                        .label("Principal entity ID")
                                        .description("Your assigned DLT principal entity ID.")
                                        .required(true)),
                            object(TURKEY_IYS)
                                .label("Turkey IYS")
                                .description(
                                    "IYS regulations specific parameters required for sending promotional SMS to " +
                                        "phone numbers registered in Turkey.")
                                .properties(
                                    integer(BRAND_CODE)
                                        .label("Brand code")
                                        .description(
                                            "Brand code is an ID of the company based on a company VAT number. If " +
                                                "not provided in request, default value is used from your Infobip " +
                                                "account.")
                                        .required(false),
                                    string(RECIPIENT_TYPE)
                                        .label("Recipient type")
                                        .description("Recipient Type")
                                        .options(
                                            option("Tacir", "TACIR"),
                                            option("Bireysel", "BIREYSEL"))
                                        .required(true))
                                .required(false),
                            object(SOUTH_KOREA)
                                .label("South Korea")
                                .description(
                                    "Use case dependent parameters for sending SMS to phone numbers registered in " +
                                        "South Korea.")
                                .properties(
                                    integer(RESELLER_CODE)
                                        .label("Reseller code")
                                        .description(
                                            "Reseller identification code: 9-digit registration number in the " +
                                                "business registration certificate for South Korea. Resellers should " +
                                                "submit this when sending.")
                                        .required(false))
                                .required(false))
                        .required(false),
                    object(SEND_AT)
                        .label("Send at")
                        .description(
                            "Date and time when the message is to be sent. Used for scheduled SMS. It can only be " +
                                "scheduled for no later than 180 days in advance.")
                        .properties(
                            dateTime(DATE_TIME)
                                .label("Date time")
                                .required(false),
                            string(ZONE_ID)
                                .label("Zone id")
                                .required(false))
                        .required(false),
                    string(TEXT)
                        .label("Text")
                        .description("Content of the message being sent.")
                        .required(false),
                    string(TRANSLITERATION)
                        .label("Transliteration")
                        .description(
                            "The transliteration of your sent message from one script to another. Transliteration is " +
                                "used to replace characters which are not recognized as part of your defaulted " +
                                "alphabet.")
                        .options(
                            option("Turkish", "TURKISH"),
                            option("Greek", "GREEK"),
                            option("Cyrillic", "CYRILLIC"),
                            option("Serbian cyrillic", "SERBIAN_CYRILLIC"),
                            option("Bulgarian cyrillic", "BULGARIAN_CYRILLIC"),
                            option("Central european", "CENTRAL_EUROPEAN"),
                            option("Baltic", "BALTIC"),
                            option("Portuguese", "PORTUGUESE"),
                            option("Colombian", "COLOMBIAN"),
                            option("Non unicode", "NON_UNICODE"))
                        .required(false),
                    number(VALIDITY_PERIOD)
                        .label("Validity period")
                        .description(
                            "The message validity period in minutes. When the period expires, it will not be allowed " +
                                "for the message to be sent. Validity period longer than 48h is not supported. Any " +
                                "bigger value will automatically default back to 2880.")
                        .defaultValue(2880)
                        .required(false),
                    string(ENTITY_ID)
                        .label("Entity id")
                        .description(
                            "Required for entity use in a send request for outbound traffic. Returned in " +
                                "notification events.")
                        .maxLength(50)
                        .required(false),
                    string(APPLICATION_ID)
                        .label("Application ID")
                        .description(
                            "Required for application use in a send request for outbound traffic. Returned in " +
                                "notification events.")
                        .maxLength(50)
                        .required(false))
                .required(true),
            object(SENDING_SPEED_LIMIT)
                .label("Sending speed limit")
                .description(
                    "Limits the send speed when sending messages in bulk to deliver messages over a longer period of " +
                        "time. You may wish to use this to allow your systems or agents to handle large amounts of " +
                        "incoming traffic, e.g., if you are expecting recipients to follow through with a " +
                        "call-to-action option from a message you sent. Not setting a send speed limit can overwhelm " +
                        "your resources with incoming traffic.")
                .properties(
                    integer(AMOUNT)
                        .label("Amount")
                        .description(
                            "The number of messages to be sent per timeUnit. By default, the system sends messages " +
                                "as fast as the infrastructure allows. Use this parameter to adapt sending capacity " +
                                "to your needs. The system is only able to work against its maximum capacity for " +
                                "ambitious message batches.")
                        .required(true),
                    string(TIME_UNIT)
                        .label("Time unit")
                        .description("The time unit to define when setting a messaging speed limit.")
                        .options(
                            option(MINUTE_LABEL, "MINUTE"),
                            option("Hour", "HOUR"),
                            option("Day", "DAY"))
                        .defaultValue("minute")
                        .required(false))
                .required(false),
            object(URL_OPTIONS)
                .label("Url options")
                .description("Sets up URL shortening and tracking feature. Not compatible with old tracking feature.")
                .properties(
                    bool(SHORTEN_URL)
                        .label("Shorten url")
                        .description(
                            "Enable shortening of the URLs within a message. Set this to true, if you want to set " +
                                "up other URL options.")
                        .defaultValue(true)
                        .required(false),
                    bool(TRACK_CLICKS)
                        .label("Track clicks")
                        .description(
                            "Enable tracking of short URL clicks within a message: which URL was clicked, how many " +
                                "times, and by whom.")
                        .defaultValue(true)
                        .required(false),
                    string(TRACKING_URL)
                        .label("Tracking url")
                        .description("The URL of your callback server on to which the Click report will be sent.")
                        .required(false),
                    bool(REMOVE_PROTOCOL)
                        .label("Remove protocol")
                        .description(
                            "Remove a protocol, such as https://, from links to shorten a message. Note that some " +
                                "mobiles may not recognize such links as a URL.")
                        .defaultValue(false)
                        .required(false),
                    string(CUSTOM_DOMAIN)
                        .label("Custom")
                        .description("Select a predefined custom domain to use when generating a short URL.")
                        .required(false))
                .required(false),
            object(TRACKING)
                .label("Tracking")
                .description("Sets up tracking parameters to track conversion metrics and type.")
                .properties(
                    string(BASE_URL)
                        .label("Base URL")
                        .description(
                            "Custom base URL for shortened links in messages when tracking URL conversions. " +
                                "Legacy - use urlOptions instead.")
                        .required(false),
                    string(PROCESS_KEY)
                        .label("Process key")
                        .description("The process key which uniquely identifies conversion tracking.")
                        .required(false),
                    string(TRACK)
                        .label("Track")
                        .description("")
                        .required(false),
                    string(TYPE)
                        .label("Type")
                        .description(
                            "Sets a custom conversion type naming convention, e.g. ONE_TIME_PIN or SOCIAL_INVITES")
                        .required(false))
                .required(false),
            bool(INCLUDE_SMS_COUNT_IN_RESPONSE)
                .label("Include SMS count in response")
                .description(
                    "Set to true to return smsCount in the response. smsCount is the total count of SMS submitted in " +
                        "the request. SMS messages have a character limit and messages longer than that limit will " +
                        "be split into multiple SMS and reflected in the total count of SMS submitted.")
                .defaultValue(false)
                .required(false))
        .outputSchema(
            object()
                .properties(
                    string(BULK_ID),
                    array(MESSAGES)
                        .items(
                            string(MESSAGE_ID),
                            object(STATUS)
                                .properties(
                                    string(GROUP_NAME),
                                    integer(ID),
                                    integer(GROUP_ID),
                                    string(NAME),
                                    string(ACTION),
                                    string(DESCRIPTION)),
                            string(TO),
                            integer(SMS_COUNT))))
        .perform(InfobipSendSMSAction::perform);

    private InfobipSendSMSAction() {
    }

    public static SmsResponse perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws ApiException {

        ApiClient apiClient = ApiClient.forApiKey(ApiKey.from(connectionParameters.getRequiredString(VALUE)))
            .build();

        SmsApi smsApi = new SmsApi(apiClient);

        List<SmsTextualMessage> smsTextualMessages = InfobipUtils.createSmsTextualMessageList(
            inputParameters.getRequiredList(MESSAGES, InfobipUtils.SmsTextualMessageCustom.class));

        SmsSendingSpeedLimit smsSendingSpeedLimit = new SmsSendingSpeedLimit()
            .amount(inputParameters.getInteger(AMOUNT))
            .timeUnit(SmsSpeedLimitTimeUnit.fromValue(inputParameters.getString(TIME_UNIT)));

        SmsAdvancedTextualRequest smsAdvancedTextualRequest = new SmsAdvancedTextualRequest()
            .bulkId(inputParameters.getString(BULK_ID))
            .sendingSpeedLimit(smsSendingSpeedLimit)
            .urlOptions(inputParameters.get(URL_OPTIONS, SmsUrlOptions.class))
            .tracking(inputParameters.get(TRACKING, SmsTracking.class))
            .includeSmsCountInResponse(inputParameters.getBoolean(INCLUDE_SMS_COUNT_IN_RESPONSE))
            .messages(smsTextualMessages);

        return smsApi
            .sendSmsMessage(smsAdvancedTextualRequest)
            .execute();

    }
}
