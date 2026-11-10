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

package com.bytechef.component.okta.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.okta.constant.OktaConstants.GROUP_OUTPUT_PROPERTY;
import static com.bytechef.component.okta.constant.OktaConstants.USER_ID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.okta.util.OktaUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class OktaListUserGroupsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listUserGroups")
        .title("List User Groups")
        .description("List the groups the user is a member of.")
        .properties(
            string(USER_ID)
                .label("User ID")
                .description("The ID, login or login shortname of the user.")
                .required(true))
        .output(outputSchema(array().items(GROUP_OUTPUT_PROPERTY)))
        .help("", "https://docs.bytechef.io/reference/components/okta_v1#list-user-groups")
        .perform(OktaListUserGroupsAction::perform);

    private OktaListUserGroupsAction() {
    }

    public static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String userId = OktaUtils.encodePathSegment(inputParameters.getRequiredString(USER_ID));

        return context.http(http -> http.get("/users/" + userId + "/groups"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
