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

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.vtiger.constant.VTigerConstants.GET_ME;
import static com.bytechef.component.vtiger.constant.VTigerConstants.INSTANCE_URL;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;

/**
 * @author Luka Ljubić
 */
public class VTigerGetMeAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(GET_ME)
        .title("Get Me")
        .description("Get more information about yourself")
        .outputSchema(
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
                            string("status"))))
        .perform(VTigerGetMeAction::perform);

    private VTigerGetMeAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return context
            .http(http -> http.get(connectionParameters.getRequiredString(INSTANCE_URL)
                + "/restapi/v1/vtiger/default/me"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
