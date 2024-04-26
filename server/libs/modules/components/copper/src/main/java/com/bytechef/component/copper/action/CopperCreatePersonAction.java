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
import static com.bytechef.component.copper.constant.CopperConstants.COMPANY_ID;
import static com.bytechef.component.copper.constant.CopperConstants.CONTACT_TYPE_ID;
import static com.bytechef.component.copper.constant.CopperConstants.CONTACT_TYPE_PROPERTY;
import static com.bytechef.component.copper.constant.CopperConstants.COUNTRY;
import static com.bytechef.component.copper.constant.CopperConstants.CREATE_PERSON;
import static com.bytechef.component.copper.constant.CopperConstants.DETAILS;
import static com.bytechef.component.copper.constant.CopperConstants.DETAILS_PROPERTY;
import static com.bytechef.component.copper.constant.CopperConstants.EMAIL;
import static com.bytechef.component.copper.constant.CopperConstants.EMAILS;
import static com.bytechef.component.copper.constant.CopperConstants.ID;
import static com.bytechef.component.copper.constant.CopperConstants.NAME;
import static com.bytechef.component.copper.constant.CopperConstants.NAME_PROPERTY;
import static com.bytechef.component.copper.constant.CopperConstants.NUMBER;
import static com.bytechef.component.copper.constant.CopperConstants.OTHER;
import static com.bytechef.component.copper.constant.CopperConstants.PHONE_NUMBERS;
import static com.bytechef.component.copper.constant.CopperConstants.PHONE_NUMBERS_PROPERTY;
import static com.bytechef.component.copper.constant.CopperConstants.POSTAL_CODE;
import static com.bytechef.component.copper.constant.CopperConstants.SOCIALS;
import static com.bytechef.component.copper.constant.CopperConstants.SOCIALS_PROPERTY;
import static com.bytechef.component.copper.constant.CopperConstants.STATE;
import static com.bytechef.component.copper.constant.CopperConstants.STREET;
import static com.bytechef.component.copper.constant.CopperConstants.TAGS;
import static com.bytechef.component.copper.constant.CopperConstants.TAGS_PROPERTY;
import static com.bytechef.component.copper.constant.CopperConstants.TITLE;
import static com.bytechef.component.copper.constant.CopperConstants.URL;
import static com.bytechef.component.copper.constant.CopperConstants.WEBSITES;
import static com.bytechef.component.copper.constant.CopperConstants.WEBSITES_PROPERTY;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.copper.util.CopperOptionUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;

/**
 * @author Monika Domiter
 */
public class CopperCreatePersonAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_PERSON)
        .title("Create person")
        .description("Creates a new Person")
        .properties(
            NAME_PROPERTY
                .description("The first and last name of the Person."),
            array(EMAILS)
                .label("Emails")
                .description("Email addresses belonging to the Person.")
                .items(
                    object("Email")
                        .properties(
                            string(EMAIL)
                                .label("Email")
                                .description("An email address.")
                                .controlType(Property.ControlType.EMAIL)
                                .required(false),
                            string(CATEGORY)
                                .label("Category")
                                .description("The category of the email address.")
                                .options(
                                    option("Work", "work"),
                                    option("Personal", "personal"),
                                    option(OTHER, OTHER))
                                .required(false)))
                .required(false),
            ASSIGNEE_PROPERTY
                .description("User that will be the owner of the Person."),
            string(TITLE)
                .label("Title")
                .description("The professional title of the Person.")
                .required(false),
            string(COMPANY_ID)
                .label("Company")
                .description("Primary Company with which the Person is associated.")
                .options((OptionsDataSource.ActionOptionsFunction<String>) CopperOptionUtils::getCompanyIdOptions)
                .required(false),
            CONTACT_TYPE_PROPERTY
                .description("The unique identifier of the Contact Type of the Person."),
            DETAILS_PROPERTY
                .description("Description of the person."),
            PHONE_NUMBERS_PROPERTY
                .description("Phone numbers belonging to the person."),
            SOCIALS_PROPERTY
                .description("Social profiles belonging to the Person."),
            WEBSITES_PROPERTY
                .description("Websites belonging to the Person."),
            ADDRESS_PROPERTY
                .description("Person's street, city, state, postal code, and country."),
            TAGS_PROPERTY
                .description("Tags associated with the Person."))
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
                    string(COMPANY_ID),
                    string(CONTACT_TYPE_ID),
                    string(DETAILS),
                    array(EMAILS)
                        .items(
                            object()
                                .properties(
                                    string(EMAIL),
                                    string(CATEGORY))),
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
                    string(TITLE),
                    array(WEBSITES)
                        .items(
                            object()
                                .properties(
                                    string(URL),
                                    string(CATEGORY)))))
        .perform(CopperCreatePersonAction::perform);

    private CopperCreatePersonAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(http -> http.post(BASE_URL + "/people"))
            .body(
                Http.Body.of(
                    NAME, inputParameters.getString(NAME),
                    EMAILS, inputParameters.getList(EMAILS),
                    ASSIGNEE_ID, inputParameters.getString(ASSIGNEE_ID),
                    TITLE, inputParameters.getString(TITLE),
                    COMPANY_ID, inputParameters.getString(COMPANY_ID),
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
