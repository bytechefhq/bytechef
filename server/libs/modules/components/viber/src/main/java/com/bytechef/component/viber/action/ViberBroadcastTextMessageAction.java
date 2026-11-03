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

package com.bytechef.component.viber.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.viber.constant.ViberConstants.RECEIVERS;
import static com.bytechef.component.viber.constant.ViberConstants.SENDER_AVATAR;
import static com.bytechef.component.viber.constant.ViberConstants.SENDER_NAME;
import static com.bytechef.component.viber.constant.ViberConstants.TEXT;
import static com.bytechef.component.viber.constant.ViberConstants.TRACKING_DATA;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.viber.util.ViberUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ViberBroadcastTextMessageAction {

    private static final int MAX_RECEIVERS = 300;

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("broadcastTextMessage")
        .title("Broadcast Text Message")
        .description("Sends a text message to up to 300 Viber subscribers at once.")
        .properties(
            array(RECEIVERS)
                .label("Receiver IDs")
                .description("Viber user IDs of the receivers. Use the user.id values from a Viber trigger.")
                .items(string())
                .minItems(1)
                .maxItems(MAX_RECEIVERS)
                .required(true),
            string(TEXT)
                .label("Text")
                .description(
                    "Text of the message to be sent. The placeholders replace_me_with_receiver_id, " +
                        "replace_me_with_url_encoded_receiver_id and replace_me_with_user_name are replaced for " +
                        "each receiver.")
                .maxLength(7000)
                .controlType(ControlType.TEXT_AREA)
                .required(true),
            string(SENDER_NAME)
                .label("Sender Name")
                .description("Name shown as the sender. Defaults to the bot name.")
                .maxLength(28)
                .required(false),
            string(SENDER_AVATAR)
                .label("Sender Avatar URL")
                .description("URL of the avatar image shown for the sender.")
                .required(false),
            string(TRACKING_DATA)
                .label("Tracking Data")
                .description("Data returned with the next message from the receiver.")
                .maxLength(4096)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        integer("status"),
                        string("status_message"),
                        array("failed_list")
                            .items(
                                object()
                                    .properties(
                                        string("receiver"),
                                        integer("status"),
                                        string("status_message"))))))
        .perform(ViberBroadcastTextMessageAction::perform);

    private ViberBroadcastTextMessageAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        List<String> receivers = inputParameters.getRequiredList(RECEIVERS, String.class);

        if (receivers.isEmpty() || receivers.size() > MAX_RECEIVERS) {
            throw new IllegalArgumentException(
                "Receiver IDs must contain between 1 and %d entries.".formatted(MAX_RECEIVERS));
        }

        Map<String, Object> body = new HashMap<>();

        body.put("broadcast_list", receivers);
        body.put("type", "text");
        body.put("sender", ViberUtils.buildSender(inputParameters, SENDER_NAME, context));
        body.put("text", inputParameters.getRequiredString(TEXT));

        String trackingData = inputParameters.getString(TRACKING_DATA);

        if (trackingData != null && !trackingData.isBlank()) {
            body.put("tracking_data", trackingData);
        }

        Map<String, Object> response = context.http(http -> http.post("/broadcast_message"))
            .body(Http.Body.of(body))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return ViberUtils.checkStatus(response);
    }
}
