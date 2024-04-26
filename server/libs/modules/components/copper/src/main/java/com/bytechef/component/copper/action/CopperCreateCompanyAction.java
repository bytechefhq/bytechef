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

package com.bytechef.component.copper.action;

import static com.bytechef.component.copper.constant.CopperConstants.ADDRESS;
import static com.bytechef.component.copper.constant.CopperConstants.ADDRESS_PROPERTY;
import static com.bytechef.component.copper.constant.CopperConstants.ASSIGNEE_ID;
import static com.bytechef.component.copper.constant.CopperConstants.ASSIGNEE_PROPERTY;
import static com.bytechef.component.copper.constant.CopperConstants.BASE_URL;
import static com.bytechef.component.copper.constant.CopperConstants.CATEGORY;
import static com.bytechef.component.copper.constant.CopperConstants.CITY;
import static com.bytechef.component.copper.constant.CopperConstants.CONTACT_TYPE_ID;
import static com.bytechef.component.copper.constant.CopperConstants.CONTACT_TYPE_PROPERTY;
import static com.bytechef.component.copper.constant.CopperConstants.COUNTRY;
import static com.bytechef.component.copper.constant.CopperConstants.CREATE_COMPANY;
import static com.bytechef.component.copper.constant.CopperConstants.DETAILS;
import static com.bytechef.component.copper.constant.CopperConstants.DETAILS_PROPERTY;
import static com.bytechef.component.copper.constant.CopperConstants.EMAIL_DOMAIN;
import static com.bytechef.component.copper.constant.CopperConstants.ID;
import static com.bytechef.component.copper.constant.CopperConstants.NAME;
import static com.bytechef.component.copper.constant.CopperConstants.NAME_PROPERTY;
import static com.bytechef.component.copper.constant.CopperConstants.NUMBER;
import static com.bytechef.component.copper.constant.CopperConstants.PHONE_NUMBERS;
import static com.bytechef.component.copper.constant.CopperConstants.PHONE_NUMBERS_PROPERTY;
import static com.bytechef.component.copper.constant.CopperConstants.POSTAL_CODE;
import static com.bytechef.component.copper.constant.CopperConstants.SOCIALS;
import static com.bytechef.component.copper.constant.CopperConstants.SOCIALS_PROPERTY;
import static com.bytechef.component.copper.constant.CopperConstants.STATE;
import static com.bytechef.component.copper.constant.CopperConstants.STREET;
import static com.bytechef.component.copper.constant.CopperConstants.TAGS;
import static com.bytechef.component.copper.constant.CopperConstants.TAGS_PROPERTY;
import static com.bytechef.component.copper.constant.CopperConstants.URL;
import static com.bytechef.component.copper.constant.CopperConstants.WEBSITES;
import static com.bytechef.component.copper.constant.CopperConstants.WEBSITES_PROPERTY;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Domiter
 */
public class CopperCreateCompanyAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_COMPANY)
        .title("Create company")
        .description("Creates a new Company")
        .properties(
            NAME_PROPERTY
                .description("The name of the Company."),
            ASSIGNEE_PROPERTY
                .description("User that will be the owner of the Company."),
            string(EMAIL_DOMAIN)
                .label("Email domain")
                .description("The domain to which email addresses for the Company belong.")
                .required(false),
            CONTACT_TYPE_PROPERTY
                .description("Contact Type of the Company."),
            DETAILS_PROPERTY
                .description("Description of the Company."),
            PHONE_NUMBERS_PROPERTY
                .description("Phone numbers belonging to the Company."),
            SOCIALS_PROPERTY
                .description("Social profiles belonging to the Company."),
            WEBSITES_PROPERTY
                .description("Websites belonging to the Company."),
            ADDRESS_PROPERTY
                .description("Company's street, city, state, postal code, and country."),
            TAGS_PROPERTY
                .description("Tags associated with the Company"))
        .outputSchema(
            object()
                .properties(
                    string(ID),
                    string(NAME),
                    object(ADDRESS)
                        .properties(
                            string(STREET),
                            string(CITY),
                            string(STATE),
                            string(POSTAL_CODE),
                            string(COUNTRY)),
                    string(ASSIGNEE_ID),
                    string(CONTACT_TYPE_ID),
                    string(DETAILS),
                    string(EMAIL_DOMAIN),
                    array(PHONE_NUMBERS)
                        .items(
                            object()
                                .properties(
                                    string(NUMBER),
                                    string(CATEGORY))),
                    array(SOCIALS)
                        .items(
                            object()
                                .properties(
                                    string(URL),
                                    string(CATEGORY))),
                    array(TAGS)
                        .items(string()),
                    array(WEBSITES)
                        .items(
                            object()
                                .properties(
                                    string(URL),
                                    string(CATEGORY)))))
        .perform(CopperCreateCompanyAction::perform);

    private CopperCreateCompanyAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(http -> http.post(BASE_URL + "/companies"))
            .body(
                Http.Body.of(
                    NAME, inputParameters.getString(NAME),
                    ASSIGNEE_ID, inputParameters.getString(ASSIGNEE_ID),
                    EMAIL_DOMAIN, inputParameters.getString(EMAIL_DOMAIN),
                    CONTACT_TYPE_ID, inputParameters.getString(CONTACT_TYPE_ID),
                    DETAILS, inputParameters.getString(DETAILS),
                    PHONE_NUMBERS, inputParameters.getList(PHONE_NUMBERS),
                    SOCIALS, inputParameters.getList(SOCIALS),
                    WEBSITES, inputParameters.getList(WEBSITES),
                    ADDRESS, inputParameters.get(ADDRESS),
                    TAGS, inputParameters.getList(TAGS)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new Context.TypeReference<>() {});

    }
}
