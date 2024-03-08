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

package com.bytechef.component.copper.constant;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.copper.util.CopperOptionUtils;
import com.bytechef.component.definition.ComponentDSL.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;
import com.bytechef.component.definition.OptionsDataSource;

/**
 * @author Monika Domiter
 */
public class CopperConstants {

    public static final String ADDRESS = "address";
    public static final String ASSIGNEE_ID = "assignee_id";
    public static final String ACTIVITY_TYPE = "activity_type";
    public static final String BASE_URL = "https://api.copper.com/developer_api/v1";
    public static final String CATEGORY = "category";
    public static final String CATEGORY_LABEL = "Category";
    public static final String CITY = "city";
    public static final String COMPANY_ID = "company_id";
    public static final String CONTACT_TYPE_ID = "contact_type_id";
    public static final String COPPER = "copper";
    public static final String COUNTRY = "country";
    public static final String CREATE_COMPANY = "createCompany";
    public static final String CREATE_PERSON = "createPerson";
    public static final String CREATE_ACTIVITY = "createActivity";
    public static final String DETAILS = "details";
    public static final String EMAIL = "email";
    public static final String EMAILS = "emails";
    public static final String EMAIL_DOMAIN = "email_domain";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String NUMBER = "number";
    public static final String OTHER = "other";
    public static final String PARENT = "parent";
    public static final String PHONE_NUMBERS = "phone_numbers";
    public static final String POSTAL_CODE = "postal_code";
    public static final String SOCIALS = "socials";
    public static final String STATE = "state";
    public static final String STREET = "street";
    public static final String TAGS = "tags";
    public static final String TITLE = "title";
    public static final String TYPE = "type";
    public static final String URL = "url";
    public static final String WEBSITES = "websites";

    public static final ModifiableObjectProperty ADDRESS_PROPERTY = object(ADDRESS)
        .label("Address")
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
                .label("Postal code")
                .required(false),
            string(COUNTRY)
                .label("Country")
                .required(false))
        .required(false);

    public static final ModifiableStringProperty ASSIGNEE_PROPERTY = string(ASSIGNEE_ID)
        .label("Assignee")
        .options((OptionsDataSource.ActionOptionsFunction<String>) CopperOptionUtils::getUserOptions)
        .required(false);

    public static final ModifiableStringProperty CONTACT_TYPE_PROPERTY = string(CONTACT_TYPE_ID)
        .label("Contact type")
        .options((OptionsDataSource.ActionOptionsFunction<String>) CopperOptionUtils::getContactTypesOptions)
        .required(false);

    public static final ModifiableStringProperty DETAILS_PROPERTY = string(DETAILS)
        .label("Details")
        .required(false);

    public static final ModifiableStringProperty NAME_PROPERTY = string(NAME)
        .label("Name")
        .required(true);
    public static final ModifiableArrayProperty PHONE_NUMBERS_PROPERTY = array(PHONE_NUMBERS)
        .label("Phone numbers")
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
        .required(false);

    public static final ModifiableArrayProperty SOCIALS_PROPERTY = array(SOCIALS)
        .label("Socials")
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
        .required(false);

    public static final ModifiableArrayProperty TAGS_PROPERTY = array(TAGS)
        .label("Tags")
        .items(
            string()
                .options((OptionsDataSource.ActionOptionsFunction<String>) CopperOptionUtils::getTagsOptions))
        .required(false);

    public static final ModifiableArrayProperty WEBSITES_PROPERTY = array(WEBSITES)
        .label("Websites")
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
        .required(false);

    private CopperConstants() {
    }
}
