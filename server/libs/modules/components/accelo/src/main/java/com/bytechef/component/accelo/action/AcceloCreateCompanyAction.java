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

package com.bytechef.component.accelo.action;

import static com.bytechef.component.accelo.constant.AcceloConstants.COMMENTS;
import static com.bytechef.component.accelo.constant.AcceloConstants.CREATE_COMPANY;
import static com.bytechef.component.accelo.constant.AcceloConstants.NAME;
import static com.bytechef.component.accelo.constant.AcceloConstants.PHONE;
import static com.bytechef.component.accelo.constant.AcceloConstants.WEBSITE;
import static com.bytechef.component.accelo.util.AcceloUtils.createUrl;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.nullable;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.time;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Domiter
 */
public class AcceloCreateCompanyAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_COMPANY)
        .title("Create company")
        .description("Creates a new company")
        .properties(
            string(NAME)
                .label("Name")
                .description("The name of the company")
                .required(true),
            string(WEBSITE)
                .label("Website")
                .description("The company's website.")
                .required(false),
            string(PHONE)
                .label("Phone")
                .description("A contact phone number for the company.")
                .required(false),
            string(COMMENTS)
                .label("Comments")
                .description("Any comments or notes made against the company.")
                .required(false))
        .outputSchema(
            object()
                .additionalProperties(
                    array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time()))
        .perform(AcceloCreateCompanyAction::perform);

    private AcceloCreateCompanyAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.post(createUrl(connectionParameters, "companies")))
            .body(
                Http.Body.of(
                    NAME, inputParameters.getRequiredString(NAME),
                    WEBSITE, inputParameters.getString(WEBSITE),
                    PHONE, inputParameters.getString(PHONE),
                    COMMENTS, inputParameters.getString(COMMENTS)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
