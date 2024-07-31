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

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.infobip.constant.InfobipConstants.CONTENT;
import static com.bytechef.component.infobip.constant.InfobipConstants.DESCRIPTION;
import static com.bytechef.component.infobip.constant.InfobipConstants.FROM;
import static com.bytechef.component.infobip.constant.InfobipConstants.GROUP_ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.GROUP_NAME;
import static com.bytechef.component.infobip.constant.InfobipConstants.ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.MESSAGE_COUNT;
import static com.bytechef.component.infobip.constant.InfobipConstants.MESSAGE_ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.NAME;
import static com.bytechef.component.infobip.constant.InfobipConstants.SEND_WHATSAPP_TEXT_MESSAGE;
import static com.bytechef.component.infobip.constant.InfobipConstants.STATUS;
import static com.bytechef.component.infobip.constant.InfobipConstants.TEXT;
import static com.bytechef.component.infobip.constant.InfobipConstants.TO;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * @author Monika Kušter
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
            string(TEXT)
                .label("Text")
                .description("Content of the message being sent.")
                .maxLength(4096)
                .required(true))
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
                            string(DESCRIPTION))))
        .perform(InfobipSendWhatsappTextMesageAction::perform);

    private InfobipSendWhatsappTextMesageAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.post("/whatsapp/1/message/text"))
            .body(
                Http.Body.of(
                    FROM, inputParameters.getRequiredString(FROM),
                    TO, inputParameters.getRequiredString(TO),
                    CONTENT, Map.of(TEXT, inputParameters.getRequiredString(TEXT))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
