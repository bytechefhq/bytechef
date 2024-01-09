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

import static com.bytechef.component.infobip.constant.InfobipConstants.ACTION;
import static com.bytechef.component.infobip.constant.InfobipConstants.APPLICATION_ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.CALLBACK_DATA;
import static com.bytechef.component.infobip.constant.InfobipConstants.CONTENT;
import static com.bytechef.component.infobip.constant.InfobipConstants.CUSTOM_DOMAIN;
import static com.bytechef.component.infobip.constant.InfobipConstants.DESCRIPTION;
import static com.bytechef.component.infobip.constant.InfobipConstants.ENTITY_ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.FROM;
import static com.bytechef.component.infobip.constant.InfobipConstants.GROUP_ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.GROUP_NAME;
import static com.bytechef.component.infobip.constant.InfobipConstants.ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.MESSAGE_COUNT;
import static com.bytechef.component.infobip.constant.InfobipConstants.MESSAGE_ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.NAME;
import static com.bytechef.component.infobip.constant.InfobipConstants.NOTIFY_URL;
import static com.bytechef.component.infobip.constant.InfobipConstants.PREVIEW_URL;
import static com.bytechef.component.infobip.constant.InfobipConstants.REMOVE_PROTOCOL;
import static com.bytechef.component.infobip.constant.InfobipConstants.SEND_WHATSAPP_TEXT_MESSAGE;
import static com.bytechef.component.infobip.constant.InfobipConstants.SHORTEN_URL;
import static com.bytechef.component.infobip.constant.InfobipConstants.STATUS;
import static com.bytechef.component.infobip.constant.InfobipConstants.TEXT;
import static com.bytechef.component.infobip.constant.InfobipConstants.TO;
import static com.bytechef.component.infobip.constant.InfobipConstants.TRACKING_URL;
import static com.bytechef.component.infobip.constant.InfobipConstants.TRACK_CLICKS;
import static com.bytechef.component.infobip.constant.InfobipConstants.URL_OPTIONS;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.VALUE;

import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.Parameters;
import com.infobip.ApiClient;
import com.infobip.ApiException;
import com.infobip.ApiKey;
import com.infobip.api.WhatsAppApi;
import com.infobip.model.WhatsAppSingleMessageInfo;
import com.infobip.model.WhatsAppTextContent;
import com.infobip.model.WhatsAppTextMessage;
import com.infobip.model.WhatsAppUrlOptions;

/**
 * @author Monika Domiter
 */
public class InfobipSendWhatsappTextMesageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(SEND_WHATSAPP_TEXT_MESSAGE)
        .title("Send Whatsapp Text Message")
        .description("Send a new SMS message")
        .properties(
            string(FROM)
                .label("From")
                .description(
                    "Registered WhatsApp sender number. Must be in international format and comply with " +
                        "WhatsApp's requirements.")
                .maxLength(24)
                .required(true),
            string(TO)
                .label("To")
                .description("Message recipient number. Must be in international format.")
                .maxLength(24)
                .required(true),
            string(MESSAGE_ID)
                .label("Message ID")
                .description("The ID that uniquely identifies the message sent.")
                .required(false),
            object(CONTENT)
                .label("Content")
                .description("The content object to build a message that will be sent.")
                .properties(
                    string(TEXT)
                        .label("Text")
                        .description("Content of the message being sent.")
                        .maxLength(4096)
                        .required(true),
                    bool(PREVIEW_URL)
                        .label("Preview URL")
                        .description(
                            "Allows for URL preview from within the message. If set to true, the message content " +
                                "must contain a URL starting with https:// or http://.")
                        .defaultValue(false)
                        .required(false))
                .required(true),
            string(CALLBACK_DATA)
                .label("Callback data")
                .description("Custom client data that will be included in a Delivery Report.")
                .maxLength(4000)
                .required(false),
            string(NOTIFY_URL)
                .label("Notify URL")
                .description("The URL on your callback server to which delivery and seen reports will be sent.")
                .maxLength(2048)
                .required(false),
            object(URL_OPTIONS)
                .label("URL options")
                .description(
                    "Sets up URL shortening and tracking feature. Not compatible with old tracking feature.")
                .properties(
                    bool(SHORTEN_URL)
                        .label("Shorten URL")
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
                        .label("Tracking URL")
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
                        .label("Custom domain")
                        .description("Select a predefined custom domain to use when generating a short URL.")
                        .required(false))
                .required(false),
            string(ENTITY_ID)
                .label("Entity ID")
                .description(
                    "Required for entity use in a send request for outbound traffic. Returned in " +
                        "notification events.")
                .maxLength(255)
                .required(false),
            string(APPLICATION_ID)
                .label("Application ID")
                .description(
                    "Required for application use in a send request for outbound traffic. Returned in " +
                        "notification events.")
                .maxLength(255)
                .required(false))
        .outputSchema(
            object()
                .properties(
                    string(TO),
                    integer(MESSAGE_COUNT),
                    string(MESSAGE_ID),
                    object(STATUS)
                        .properties(
                            integer(GROUP_ID),
                            string(GROUP_NAME),
                            integer(ID),
                            string(NAME),
                            string(DESCRIPTION),
                            string(ACTION))))
        .perform(InfobipSendWhatsappTextMesageAction::perform);

    private InfobipSendWhatsappTextMesageAction() {
    }

    public static WhatsAppSingleMessageInfo perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws ApiException {

        ApiClient apiClient = ApiClient.forApiKey(ApiKey.from(connectionParameters.getRequiredString(VALUE)))
            .build();

        WhatsAppApi whatsAppApi = new WhatsAppApi(apiClient);

        WhatsAppTextMessage whatsAppTextMessage = new WhatsAppTextMessage()
            .from(inputParameters.getRequiredString(FROM))
            .to(inputParameters.getRequiredString(TO))
            .messageId(inputParameters.getString(MESSAGE_ID))
            .content(inputParameters.getRequired(CONTENT, WhatsAppTextContent.class))
            .callbackData(inputParameters.getString(CALLBACK_DATA))
            .notifyUrl(inputParameters.getString(NOTIFY_URL))
            .urlOptions(inputParameters.get(URL_OPTIONS, WhatsAppUrlOptions.class))
            .entityId(inputParameters.getString(ENTITY_ID))
            .applicationId(inputParameters.getString(APPLICATION_ID));

        return whatsAppApi.sendWhatsAppTextMessage(whatsAppTextMessage)
            .execute();
    }
}
