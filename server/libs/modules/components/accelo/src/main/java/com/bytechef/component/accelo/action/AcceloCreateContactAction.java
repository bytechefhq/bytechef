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

import static com.bytechef.component.accelo.constant.AcceloConstants.COMPANY;
import static com.bytechef.component.accelo.constant.AcceloConstants.CREATE_CONTACT;
import static com.bytechef.component.accelo.constant.AcceloConstants.EMAIL;
import static com.bytechef.component.accelo.constant.AcceloConstants.FIRST_NAME;
import static com.bytechef.component.accelo.constant.AcceloConstants.LAST_NAME;
import static com.bytechef.component.accelo.constant.AcceloConstants.PHONE;
import static com.bytechef.component.accelo.constant.AcceloConstants.POSITION;
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

import com.bytechef.component.accelo.util.AcceloUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;

/**
 * @author Monika Domiter
 */
public class AcceloCreateContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_CONTACT)
        .title("Create contact")
        .description("Creates a new contact")
        .properties(
            string(FIRST_NAME)
                .label("First name")
                .description("The firstname of the contact.")
                .required(false),
            string(LAST_NAME)
                .label("Last name")
                .description("The lastname of the contact.")
                .required(false),
            string(COMPANY)
                .label("Company")
                .description("This is the company the new affiliated contact will be associated with.")
                .options((OptionsDataSource.ActionOptionsFunction<String>) AcceloUtils::getCompanyIdOptions)
                .required(true),
            string(PHONE)
                .label("Phone")
                .description("The contact's phone number in their role in the associated company.")
                .required(false),
            string(EMAIL)
                .label("Email")
                .description("The contact's email address.")
                .controlType(Property.ControlType.EMAIL)
                .required(false),
            string(POSITION)
                .label("Position")
                .description("The contact's position in the associated company.")
                .required(false))
        .outputSchema(
            object()
                .additionalProperties(
                    array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time()))
        .perform(AcceloCreateContactAction::perform);

    private AcceloCreateContactAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(http -> http.post(createUrl(connectionParameters, "contacts")))
            .body(
                Http.Body.of(
                    FIRST_NAME, inputParameters.getString(FIRST_NAME),
                    LAST_NAME, inputParameters.getString(LAST_NAME),
                    COMPANY, inputParameters.getString(COMPANY),
                    PHONE, inputParameters.getString(PHONE),
                    EMAIL, inputParameters.getString(EMAIL),
                    POSITION, inputParameters.getString(POSITION)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
