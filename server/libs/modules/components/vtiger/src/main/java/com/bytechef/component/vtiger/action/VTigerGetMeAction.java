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

import static com.bytechef.component.definition.ComponentDsl.action;
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
                                string("id"),
                                string("user_name"),
                                string("user_type"),
                                string("email"),
                                string("phone_home"),
                                string("phone_work"),
                                string("phone_mobile"),
                                string("userlable"),
                                string("address_street"),
                                string("address_city"),
                                string("address_state"),
                                string("address_country"),
                                string("roleid"),
                                string("language"),
                                string("is_admin"),
                                string("is_owner"),
                                string("status")))))
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
