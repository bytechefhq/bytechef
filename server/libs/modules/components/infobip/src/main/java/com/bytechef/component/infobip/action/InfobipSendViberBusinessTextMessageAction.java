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

package com.bytechef.component.infobip.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.infobip.constant.InfobipConstants.SENDER;
import static com.bytechef.component.infobip.constant.InfobipConstants.TEXT;
import static com.bytechef.component.infobip.constant.InfobipConstants.TO;
import static com.bytechef.component.infobip.constant.InfobipConstants.VIBER_MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.infobip.util.InfobipUtils.sendViberTextMessage;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class InfobipSendViberBusinessTextMessageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sendViberBusinessTextMessage")
        .title("Send Viber Business Text Message")
        .description("Send a Viber Business Messages text message to a single recipient.")
        .properties(
            string(SENDER)
                .label("Viber Sender")
                .description("Viber Business Messages sender that Infobip activated for you.")
                .required(true),
            string(TO)
                .label("To")
                .description(
                    "Phone number of the recipient in international format. To reply, use the sender of the " +
                        "message received by the New Viber Business Message trigger.")
                .required(true),
            string(TEXT)
                .label("Text")
                .description("Content of the message being sent.")
                .controlType(ControlType.TEXT_AREA)
                .required(true))
        .output(outputSchema(VIBER_MESSAGE_OUTPUT_PROPERTY))
        .help("", "https://docs.bytechef.io/reference/components/infobip_v1#send-viber-business-text-message")
        .perform(InfobipSendViberBusinessTextMessageAction::perform);

    private InfobipSendViberBusinessTextMessageAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return sendViberTextMessage("/viber/2/messages", inputParameters, actionContext);
    }
}
