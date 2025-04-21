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

package com.bytechef.component.acumbamail.action;

import static com.bytechef.component.acumbamail.constant.AcumbamailConstants.ADDRESS;
import static com.bytechef.component.acumbamail.constant.AcumbamailConstants.CITY;
import static com.bytechef.component.acumbamail.constant.AcumbamailConstants.COMPANY;
import static com.bytechef.component.acumbamail.constant.AcumbamailConstants.COUNTRY;
import static com.bytechef.component.acumbamail.constant.AcumbamailConstants.EMAIL;
import static com.bytechef.component.acumbamail.constant.AcumbamailConstants.NAME;
import static com.bytechef.component.acumbamail.constant.AcumbamailConstants.PHONE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Marija Horvat
 */
public class AcumbamailCreateSubscriberListAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createSubscriberList")
        .title("Create Subscriber List")
        .description("Creates a new subscribers list.")
        .properties(
            string(EMAIL)
                .label("Email")
                .description("Email address that will be used for list notifications.")
                .required(true),
            string(NAME)
                .label("Name")
                .description("List name")
                .required(true),
            string(COMPANY)
                .label("Company")
                .description("Company that the list belongs to")
                .required(false),
            string(COUNTRY)
                .label("Country")
                .description("Country where the list comes from")
                .required(false),
            string(CITY)
                .label("City")
                .description("City of the company")
                .required(false),
            string(ADDRESS)
                .label("Address")
                .description("Address of the company")
                .required(false),
            string(PHONE)
                .label("Phone")
                .description("Phone number of the company")
                .required(false))
        .output(outputSchema(integer().description("ID of the created list.")))
        .perform(AcumbamailCreateSubscriberListAction::perform);

    private AcumbamailCreateSubscriberListAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.post("/createList/"))
            .queryParameters(
                "sender_email", inputParameters.getRequiredString(EMAIL),
                NAME, inputParameters.getRequiredString(NAME),
                COMPANY, inputParameters.getString(COMPANY),
                COUNTRY, inputParameters.getString(COUNTRY),
                CITY, inputParameters.getString(CITY),
                ADDRESS, inputParameters.getString(ADDRESS),
                PHONE, inputParameters.getString(PHONE))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
