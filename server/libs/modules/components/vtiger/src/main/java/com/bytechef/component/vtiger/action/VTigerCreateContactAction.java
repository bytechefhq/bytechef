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

package com.bytechef.component.vtiger.action;

import static com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.vtiger.constant.VTigerConstants.CREATE_CONTACT;
import static com.bytechef.component.vtiger.constant.VTigerConstants.EMAIL;
import static com.bytechef.component.vtiger.constant.VTigerConstants.FIRSTNAME;
import static com.bytechef.component.vtiger.constant.VTigerConstants.INSTANCE_URL;
import static com.bytechef.component.vtiger.constant.VTigerConstants.LASTNAME;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Luka Ljubić
 */
public class VTigerCreateContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_CONTACT)
        .title("Create a contact")
        .description("Create a new contact")
        .properties(
            string(FIRSTNAME)
                .label("First Name")
                .description("First name of the contact")
                .required(true),
            string(LASTNAME)
                .label("Last Name")
                .description("Last name of the contact")
                .required(true),
            string(EMAIL)
                .label("Contact email")
                .description("email for your new contact")
                .required(true))
        .outputSchema(
            object()
                .properties(
                    object("result")
                        .properties(
                            string(FIRSTNAME),
                            string(LASTNAME),
                            string(EMAIL),
                            string("phone"))))
        .perform(VTigerCreateContactAction::perform);

    private VTigerCreateContactAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        Map<String, String> paramMap = new LinkedHashMap<>();

        paramMap.put(FIRSTNAME, inputParameters.getRequiredString(FIRSTNAME));
        paramMap.put(LASTNAME, inputParameters.getRequiredString(LASTNAME));
        paramMap.put(EMAIL, inputParameters.getRequiredString(EMAIL));

        return context
            .http(http -> http.post(
                connectionParameters.getRequiredString(INSTANCE_URL) + "/restapi/v1/vtiger/default/create"))
            .body(
                Body.of(
                    "elementType", "Contacts",
                    "element", paramMap))
            .configuration(responseType(Context.Http.ResponseType.JSON))
            .execute()
            .getBody(new Context.TypeReference<>() {});
    }
}
