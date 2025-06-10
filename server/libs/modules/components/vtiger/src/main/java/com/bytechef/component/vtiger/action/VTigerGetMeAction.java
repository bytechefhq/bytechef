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

package com.bytechef.component.vtiger.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
public class VTigerGetMeAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getMe")
        .title("Get Me")
        .description("Get more information about yourself.")
        .output(
            outputSchema(
                object()
                    .properties(
                        object("result")
                            .properties(
                                string("id")
                                    .description("ID of the user."),
                                string("user_name")
                                    .description("Username of the user."),
                                string("user_type")
                                    .description("Type of the user."),
                                string("email")
                                    .description("Email address of the user."),
                                string("phone_home")
                                    .description("Home phone number of the user."),
                                string("phone_work")
                                    .description("Work phone number of the user."),
                                string("phone_mobile")
                                    .description("Mobile phone number of the user."),
                                string("userlable")
                                    .description("Label of the user."),
                                string("address_street")
                                    .description("Street address of the user."),
                                string("address_city")
                                    .description("City of the user."),
                                string("address_state")
                                    .description("State of the user."),
                                string("address_country")
                                    .description("Country of the user."),
                                string("roleid")
                                    .description("Role ID of the user."),
                                string("language")
                                    .description("Language of the user."),
                                bool("is_admin")
                                    .description("Indicates if the user is an admin."),
                                bool("is_owner")
                                    .description("Indicates if the user is an owner."),
                                string("status")
                                    .description("Status of the user.")))))
        .perform(VTigerGetMeAction::perform);

    private VTigerGetMeAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.get("/me"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
