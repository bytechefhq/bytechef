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

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.vtiger.constant.VTigerConstants.EMAIL;
import static com.bytechef.component.vtiger.constant.VTigerConstants.FIRSTNAME;
import static com.bytechef.component.vtiger.constant.VTigerConstants.LASTNAME;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
public class VTigerCreateContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createContact")
        .title("Create Contact")
        .description("Creates a new contact.")
        .properties(
            string(FIRSTNAME)
                .label("First Name")
                .description("First name of the contact.")
                .required(true),
            string(LASTNAME)
                .label("Last Name")
                .description("Last name of the contact.")
                .required(true),
            string(EMAIL)
                .label("Email")
                .description("Email address of the contact.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("result")
                            .properties(
                                string(FIRSTNAME),
                                string(LASTNAME),
                                string(EMAIL),
                                string("phone"),
                                string("assigned_user_id"),
                                string("id")))))
        .perform(VTigerCreateContactAction::perform);

    private VTigerCreateContactAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.post("/create"))
            .body(
                Body.of(
                    "elementType", "Contacts",
                    "element",
                    Map.of(FIRSTNAME, inputParameters.getRequiredString(FIRSTNAME),
                        LASTNAME, inputParameters.getRequiredString(LASTNAME),
                        EMAIL, inputParameters.getRequiredString(EMAIL))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
