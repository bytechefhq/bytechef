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

package com.bytechef.component.copper.action;

import static com.bytechef.component.copper.constant.CopperConstants.ADDRESS;
import static com.bytechef.component.copper.constant.CopperConstants.ASSIGNEE_ID;
import static com.bytechef.component.copper.constant.CopperConstants.CATEGORY;
import static com.bytechef.component.copper.constant.CopperConstants.CATEGORY_LABEL;
import static com.bytechef.component.copper.constant.CopperConstants.CITY;
import static com.bytechef.component.copper.constant.CopperConstants.CONTACT_TYPE_ID;
import static com.bytechef.component.copper.constant.CopperConstants.COUNTRY;
import static com.bytechef.component.copper.constant.CopperConstants.DETAILS;
import static com.bytechef.component.copper.constant.CopperConstants.EMAIL_DOMAIN;
import static com.bytechef.component.copper.constant.CopperConstants.ID;
import static com.bytechef.component.copper.constant.CopperConstants.NAME;
import static com.bytechef.component.copper.constant.CopperConstants.NUMBER;
import static com.bytechef.component.copper.constant.CopperConstants.OTHER;
import static com.bytechef.component.copper.constant.CopperConstants.PHONE_NUMBERS;
import static com.bytechef.component.copper.constant.CopperConstants.POSTAL_CODE;
import static com.bytechef.component.copper.constant.CopperConstants.SOCIALS;
import static com.bytechef.component.copper.constant.CopperConstants.STATE;
import static com.bytechef.component.copper.constant.CopperConstants.STREET;
import static com.bytechef.component.copper.constant.CopperConstants.TAGS;
import static com.bytechef.component.copper.constant.CopperConstants.URL;
import static com.bytechef.component.copper.constant.CopperConstants.WEBSITES;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.copper.util.CopperOptionUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Monika Domiter
 */
public class CopperCreateCompanyAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createCompany")
        .title("Create Company")
        .description("Creates a new company.")
        .properties(
            string(NAME)
                .label("Name")
                .description("The name of the company.")
                .required(true),
            string(ASSIGNEE_ID)
                .label("Assignee ID")
                .description("ID of the user that will be the owner of the company.")
                .options((OptionsFunction<String>) CopperOptionUtils::getUserOptions)
                .required(false),
            string(EMAIL_DOMAIN)
                .label("Email Domain")
                .description("The domain to which email addresses for the company belong.")
                .required(false),
            string(CONTACT_TYPE_ID)
                .label("Contact Type ID")
                .description("ID of the Contact type for the company.")
                .options((OptionsFunction<String>) CopperOptionUtils::getContactTypesOptions)
                .required(false),
            string(DETAILS)
                .label("Details")
                .description("Description of the company.")
                .required(false),
            array(PHONE_NUMBERS)
                .label("Phone Numbers")
                .description("Phone numbers belonging to the company.")
                .items(
                    object()
                        .properties(
                            string(NUMBER)
                                .label("Number")
                                .description("A phone number.")
                                .required(false),
                            string(CATEGORY)
                                .label(CATEGORY_LABEL)
                                .description("The category of the phone number.")
                                .options(
                                    option("Work", "work"),
                                    option("Mobile", "mobile"),
                                    option("Home", "home"),
                                    option("Other", OTHER))
                                .required(false)))
                .required(false),
            array(SOCIALS)
                .label("Socials")
                .description("Social profiles belonging to the company.")
                .items(
                    object()
                        .properties(
                            string(URL)
                                .label("URL")
                                .description("The URL of a social profile.")
                                .required(false),
                            string(CATEGORY)
                                .label(CATEGORY_LABEL)
                                .description("The category of the social profile.")
                                .options(
                                    option("LinkedIn", "linkedin"),
                                    option("Twitter", "twitter"),
                                    option("Facebook", "facebook"),
                                    option("Youtube", "youtube"),
                                    option("Quora", "quora"),
                                    option("Instagram", "instagram"),
                                    option("Pinterest", "pinterest"),
                                    option("Other", OTHER))
                                .required(false)))
                .required(false),
            array(WEBSITES)
                .label("Websites")
                .description("Websites belonging to the company.")
                .items(
                    object()
                        .properties(
                            string(URL)
                                .label("URL")
                                .description("The URL of a website.")
                                .required(false),
                            string(CATEGORY)
                                .label(CATEGORY_LABEL)
                                .description("The category of the website.")
                                .options(
                                    option("work", "work"),
                                    option("personal", "personal"),
                                    option(OTHER, OTHER))
                                .required(false)))
                .required(false),
            object(ADDRESS)
                .label("Address")
                .description("Company's street, city, state, postal code, and country.")
                .properties(
                    string(STREET)
                        .label("Street")
                        .required(false),
                    string(CITY)
                        .label("City")
                        .required(false),
                    string(STATE)
                        .label("State")
                        .required(false),
                    string(POSTAL_CODE)
                        .label("Postal Code")
                        .required(false),
                    string(COUNTRY)
                        .label("Country")
                        .required(false))
                .required(false),
            array(TAGS)
                .description("Tags associated with the company")
                .label("Tags")
                .items(string())
                .options((OptionsFunction<String>) CopperOptionUtils::getTagsOptions)
                .required(false))
        .output(outputSchema(
            object()
                .properties(
                    string(ID)
                        .description("ID of the new company."),
                    string(NAME)
                        .description("Name of the new company."),
                    object(ADDRESS)
                        .description("Address of the new company.")
                        .properties(
                            string(STREET)
                                .description("Street of the new company."),
                            string(CITY)
                                .description("City of the new company."),
                            string(STATE)
                                .description("State of the new company."),
                            string(POSTAL_CODE)
                                .description("Postal code of the new company."),
                            string(COUNTRY)
                                .description("Country of the new company.")),
                    string(ASSIGNEE_ID)
                        .description("ID of the user that is owner of the new company."),
                    string(CONTACT_TYPE_ID)
                        .description("ID of the contact type of the new company."),
                    string(DETAILS)
                        .description("Description of the new company."),
                    string(EMAIL_DOMAIN)
                        .description("Domain to which email addresses of the new company belong."),
                    array(PHONE_NUMBERS)
                        .description("Phone numbers belonging to the new company.")
                        .items(
                            object()
                                .properties(
                                    string(NUMBER)
                                        .description("Phone number for the new company."),
                                    string(CATEGORY)
                                        .description("Category of the phone number."))),
                    array(SOCIALS)
                        .description("Social profiles belonging to the company.")
                        .items(
                            object()
                                .properties(
                                    string(URL)
                                        .description("URL of the social profile."),
                                    string(CATEGORY)
                                        .description("Category of the social profile."))),
                    array(TAGS)
                        .description("Tags associated with the company.")
                        .items(string()),
                    array(WEBSITES)
                        .description("Websites belonging to the company.")
                        .items(
                            object()
                                .properties(
                                    string(URL)
                                        .description("URL of the website."),
                                    string(CATEGORY)
                                        .description("Category of the website."))))))
        .perform(CopperCreateCompanyAction::perform);

    protected static final ContextFunction<Http, Http.Executor> POST_COMPANIES_CONTEXT_FUNCTION =
        http -> http.post("/companies");

    private CopperCreateCompanyAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(POST_COMPANIES_CONTEXT_FUNCTION)
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
                    TAGS, inputParameters.getList(TAGS, String.class)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

    }
}
