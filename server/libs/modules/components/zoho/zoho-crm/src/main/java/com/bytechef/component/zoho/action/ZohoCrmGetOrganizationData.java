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

package com.bytechef.component.zoho.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.BASE_URL;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.GET_ORG_DATA;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.REGION;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;

public class ZohoCrmGetOrganizationData {

    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action(GET_ORG_DATA)
        .title("Get Organization data")
        .description("Get info about your organization")
        .outputSchema(
            object()
                .properties(
                    object("org")
                        .properties(
                            string("company_name"),
                            string("id"),
                            string("country"),
                            string("city"),
                            string("type"),
                            string("phone"),
                            string("website"),
                            string("country_code"))))
        .perform(ZohoCrmGetOrganizationData::perform);

    private ZohoCrmGetOrganizationData() {
    }

    private static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .http(http -> http.get(connectionParameters.getRequiredString(REGION) + BASE_URL + "/org"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
