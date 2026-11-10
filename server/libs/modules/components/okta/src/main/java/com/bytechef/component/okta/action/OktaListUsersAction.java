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
import static com.bytechef.component.okta.constant.OktaConstants.FILTER;
import static com.bytechef.component.okta.constant.OktaConstants.LIMIT;
import static com.bytechef.component.okta.constant.OktaConstants.LIMIT_PROPERTY;
import static com.bytechef.component.okta.constant.OktaConstants.Q;
import static com.bytechef.component.okta.constant.OktaConstants.SEARCH;
import static com.bytechef.component.okta.constant.OktaConstants.USER_OUTPUT_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.okta.util.OktaUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class OktaListUsersAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listUsers")
        .title("List Users")
        .description(
            "List users in the Okta organization, optionally narrowed by a search expression, a filter expression " +
                "or a simple query.")
        .properties(
            string(SEARCH)
                .label("Search")
                .description(
                    "Search expression that matches most user properties, for example " +
                        "profile.department eq \"Engineering\" or status eq \"ACTIVE\". Recommended over Filter and " +
                        "Query.")
                .required(false),
            string(FILTER)
                .label("Filter")
                .description(
                    "Filter expression on a limited set of properties (status, lastUpdated, id, profile.login, " +
                        "profile.email, profile.firstName, profile.lastName), for example status eq \"ACTIVE\".")
                .required(false),
            string(Q)
                .label("Query")
                .description(
                    "Matches the beginning of the first name, last name or email of users. Doesn't return " +
                        "deprovisioned users and doesn't support pagination.")
                .required(false),
            LIMIT_PROPERTY)
        .output(outputSchema(array().items(USER_OUTPUT_PROPERTY)))
        .help("", "https://docs.bytechef.io/reference/components/okta_v1#list-users")
        .perform(OktaListUsersAction::perform);

    private OktaListUsersAction() {
    }

    public static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return OktaUtils.getItems(
            context, "/users", OktaUtils.getQueryParameters(inputParameters, SEARCH, FILTER, Q),
            inputParameters.getInteger(LIMIT));
    }
}
