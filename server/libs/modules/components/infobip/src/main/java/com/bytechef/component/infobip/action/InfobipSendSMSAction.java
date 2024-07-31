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
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.infobip.constant.InfobipConstants.BULK_ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.CONTENT;
import static com.bytechef.component.infobip.constant.InfobipConstants.DESCRIPTION;
import static com.bytechef.component.infobip.constant.InfobipConstants.DESTINATIONS;
import static com.bytechef.component.infobip.constant.InfobipConstants.GROUP_ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.GROUP_NAME;
import static com.bytechef.component.infobip.constant.InfobipConstants.ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.MESSAGES;
import static com.bytechef.component.infobip.constant.InfobipConstants.MESSAGE_ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.NAME;
import static com.bytechef.component.infobip.constant.InfobipConstants.SENDER;
import static com.bytechef.component.infobip.constant.InfobipConstants.SEND_SMS;
import static com.bytechef.component.infobip.constant.InfobipConstants.SMS_COUNT;
import static com.bytechef.component.infobip.constant.InfobipConstants.STATUS;
import static com.bytechef.component.infobip.constant.InfobipConstants.TEXT;
import static com.bytechef.component.infobip.constant.InfobipConstants.TO;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class InfobipSendSMSAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(SEND_SMS)
        .title("Send SMS")
        .description("Send a new SMS message")
        .properties(
            string(SENDER)
                .label("From")
                .description("The sender ID. It can be alphanumeric or numeric (e.g., CompanyName).")
                .required(true),
            array(TO)
                .label("To")
                .description("Message recipient numbers.")
                .items(string())
                .required(true),
            string(TEXT)
                .label("Text")
                .description("Content of the message being sent.")
                .required(true))
        .outputSchema(
            object()
                .properties(
                    string(BULK_ID),
                    array(MESSAGES)
                        .items(
                            string(MESSAGE_ID),
                            object(STATUS)
                                .properties(
                                    integer(GROUP_ID),
                                    string(GROUP_NAME),
                                    integer(ID),
                                    string(NAME),
                                    string(DESCRIPTION)),
                            string(TO),
                            integer(SMS_COUNT))))
        .perform(InfobipSendSMSAction::perform);

    private InfobipSendSMSAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        List<Map<String, String>> destinations = inputParameters.getRequiredList(TO, String.class)
            .stream()
            .map(receiver -> Map.of(TO, receiver))
            .toList();

        return actionContext
            .http(http -> http.post("/sms/3/messages"))
            .body(
                Http.Body.of(
                    MESSAGES, List.of(
                        Map.of(
                            SENDER, inputParameters.getRequiredString(SENDER),
                            DESTINATIONS, destinations,
                            CONTENT, Map.of(TEXT, inputParameters.getRequiredString(TEXT))))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
