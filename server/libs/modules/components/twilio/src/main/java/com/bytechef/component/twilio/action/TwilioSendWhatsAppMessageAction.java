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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType.FORM_URL_ENCODED;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.twilio.constant.TwilioConstants.BODY;
import static com.bytechef.component.twilio.constant.TwilioConstants.CONTENT_SID;
import static com.bytechef.component.twilio.constant.TwilioConstants.CONTENT_VARIABLES;
import static com.bytechef.component.twilio.constant.TwilioConstants.FROM;
import static com.bytechef.component.twilio.constant.TwilioConstants.MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.twilio.constant.TwilioConstants.TO;
import static com.bytechef.component.twilio.constant.TwilioConstants.USE_TEMPLATE;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.twilio.util.TwilioUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Monika Kušter
 */
public class TwilioSendWhatsAppMessageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sendWhatsAppMessage")
        .title("Send WhatsApp Message")
        .description("Send a new WhatsApp message.")
        .properties(
            string(TO)
                .label("To")
                .description("The recipient channel address.")
                .controlType(ControlType.PHONE)
                .exampleValue("whatsapp:+15554449999")
                .required(true),
            string(FROM)
                .label("From")
                .description("The sender's Twilio channel address.")
                .exampleValue("whatsapp:+15554449999")
                .controlType(ControlType.PHONE)
                .required(true),
            bool(USE_TEMPLATE)
                .label("Use Template")
                .description("Use a template for the message body.")
                .defaultValue(true)
                .required(true),
            string(CONTENT_SID)
                .label("Content Sid")
                .description("The SID of the content template to be used for the message body.")
                .options((OptionsFunction<String>) TwilioUtils::getContentSidOptions)
                .maxLength(34)
                .minLength(34)
                .displayCondition("%s == true".formatted(USE_TEMPLATE))
                .required(true),
            object(CONTENT_VARIABLES)
                .description("Key-value pairs of template variables and their substitution values.")
                .additionalProperties(string())
                .displayCondition("%s == true".formatted(USE_TEMPLATE))
                .required(false),
            string(BODY)
                .label("Body")
                .description("The text content of the outgoing message.")
                .maxLength(1600)
                .displayCondition("%s == false".formatted(USE_TEMPLATE))
                .required(true))
        .output(outputSchema(MESSAGE_OUTPUT_PROPERTY))
        .perform(TwilioSendWhatsAppMessageAction::perform);

    private TwilioSendWhatsAppMessageAction() {
    }

    protected static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, String> bodyMap = createBodyMap(inputParameters, context);

        return context
            .http(http -> http.post("/Accounts/" + connectionParameters.getRequiredString(USERNAME) + "/Messages.json"))
            .body(Http.Body.of(bodyMap, FORM_URL_ENCODED))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static Map<String, String> createBodyMap(Parameters inputParameters, Context context) {
        String from = inputParameters.getRequiredString(FROM);
        String to = inputParameters.getRequiredString(TO);

        validatePhoneNumber(from);
        validatePhoneNumber(to);

        Map<String, String> bodyMap = new HashMap<>();

        bodyMap.put(FROM, from);
        bodyMap.put(TO, to);

        if (inputParameters.getRequiredBoolean(USE_TEMPLATE)) {
            bodyMap.put(CONTENT_SID, inputParameters.getRequiredString(CONTENT_SID));

            Map<String, String> map = inputParameters.getMap(CONTENT_VARIABLES, String.class);

            if (map != null) {
                bodyMap.put(CONTENT_VARIABLES, context.json(json -> json.write(map)));
            }
        } else {
            bodyMap.put(BODY, inputParameters.getRequiredString(BODY));
        }

        return bodyMap;
    }

    private static void validatePhoneNumber(String phoneNumber) {
        Pattern pattern = Pattern.compile("whatsapp:\\+\\d{11,15}");

        Matcher matcher = pattern.matcher(phoneNumber);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid phone number format.");
        }
    }
}
