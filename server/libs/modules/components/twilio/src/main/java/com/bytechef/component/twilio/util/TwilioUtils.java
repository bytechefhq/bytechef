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

package com.bytechef.component.twilio.util;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.twilio.constant.TwilioConstants.BODY;
import static com.bytechef.component.twilio.constant.TwilioConstants.CONTENT;
import static com.bytechef.component.twilio.constant.TwilioConstants.FROM;
import static com.bytechef.component.twilio.constant.TwilioConstants.MEDIA_URL;
import static com.bytechef.component.twilio.constant.TwilioConstants.MESSAGING_SERVICE_SID;
import static com.bytechef.component.twilio.constant.TwilioConstants.SOURCE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Monika Domiter
 */
public class TwilioUtils {

    private TwilioUtils() {
    }

    public static List<? extends Property.ValueProperty<?>> getContentProperties(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        String content = inputParameters.getString(CONTENT);

        if (content.equals(BODY)) {
            ComponentDSL.ModifiableStringProperty body = string(BODY)
                .label("Body")
                .description(
                    "The text content of the outgoing message. Can be up to 1,600 characters in length. SMS only: If " +
                        "the body contains more than 160 GSM-7 characters (or 70 UCS-2 characters), the message is " +
                        "segmented and charged accordingly. For long body text, consider using the send_as_mms " +
                        "parameter.")
                .maxLength(1600)
                .required(true);

            return List.of(body);
        } else {
            ComponentDSL.ModifiableArrayProperty arrayProperty = array(MEDIA_URL)
                .label("Media URL")
                .description(
                    "The URL of media to include in the Message content. jpeg, jpg, gif, and png file types are " +
                        "fully supported by Twilio and content is formatted for delivery on destination devices. The " +
                        "media size limit is 5 MB for supported file types (jpeg, jpg, png, gif) and 500 KB for " +
                        "other types of accepted media. To send more than one image in the message, provide multiple " +
                        "media_url parameters in the POST request. You can include up to ten media_url parameters " +
                        "per message. International and carrier limits apply.")
                .required(true);

            return List.of(arrayProperty);
        }
    }

    public static List<? extends Property.ValueProperty<?>> getSourceProperties(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        String source = inputParameters.getString(SOURCE);

        ComponentDSL.ModifiableStringProperty stringProperty;

        if (source.equals(FROM)) {
            stringProperty = string(FROM)
                .label("From")
                .description(
                    "The sender's Twilio phone number (in E.164 format), alphanumeric sender ID, Wireless SIM, short " +
                        "code, or channel address (e.g., whatsapp:+15554449999). The value of the from parameter " +
                        "must be a sender that is hosted within Twilio and belongs to the Account creating the " +
                        "Message. If you are using messaging_service_sid, this parameter can be empty (Twilio " +
                        "assigns a from value from the Messaging Service's Sender Pool) or you can provide a " +
                        "specific sender from your Sender Pool.")
                .controlType(Property.ControlType.PHONE)
                .required(true);
        } else {
            stringProperty = string(MESSAGING_SERVICE_SID)
                .label("Messaging Service SID")
                .description(
                    "The SID of the Messaging Service you want to associate with the Message. When this parameter is " +
                        "provided and the from parameter is omitted, Twilio selects the optimal sender from the " +
                        "Messaging Service's Sender Pool. You may also provide a from parameter if you want to use a " +
                        "specific Sender from the Sender Pool.")
                .required(true);
        }

        return List.of(stringProperty);
    }

    public static List<Option<String>> getZoneIdOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context) {
        Set<String> availableZoneIds = ZoneId.getAvailableZoneIds();

        List<Option<String>> options = new ArrayList<>();
        for (String zoneId : availableZoneIds) {
            options.add(
                option(zoneId, zoneId));
        }

        return options;
    }
}
