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

package com.bytechef.component.twilio.action;

import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType.FORM_URL_ENCODED;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.twilio.constant.TwilioConstants.BODY;
import static com.bytechef.component.twilio.constant.TwilioConstants.FROM;
import static com.bytechef.component.twilio.constant.TwilioConstants.MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.twilio.constant.TwilioConstants.TO;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Monika Kušter
 * @author Luka Ljubić
 */
public class TwilioSendSMSAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sendSMS")
        .title("Send SMS")
        .description("Send a new SMS message")
        .properties(
            string(TO)
                .label("To")
                .description("The recipient's phone number in E.164 format.")
                .controlType(ControlType.PHONE)
                .exampleValue("+15554449999")
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
                .controlType(ControlType.PHONE)
                .exampleValue("+15554449999")
                .required(true),
            string(BODY)
                .label("Body")
                .description(
                    "The text content of the outgoing message. SMS only: If the body contains more than 160 GSM-7 " +
                        "characters (or 70 UCS-2 characters), the message is segmented and charged accordingly. " +
                        "For long body text, consider using the send_as_mms parameter.")
                .maxLength(1600)
                .required(true))
        .output(outputSchema(MESSAGE_OUTPUT_PROPERTY))
        .perform(TwilioSendSMSAction::perform);

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.post("/Accounts/" + connectionParameters.getRequiredString(USERNAME) + "/Messages.json"))
            .body(
                Body.of(
                    Map.of(
                        BODY, inputParameters.getRequiredString(BODY),
                        FROM, inputParameters.getRequiredString(FROM),
                        TO, inputParameters.getRequiredString(TO)),
                    FORM_URL_ENCODED))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private TwilioSendSMSAction() {
    }
}
