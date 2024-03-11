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

package com.bytechef.component.freshsales.action;

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
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.ADDRESS;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.CITY;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.COUNTRY;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.CREATE_CONTACT;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.EMAIL;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.EMAIL_PROPERTY;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.FACEBOOK;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.FIRST_NAME;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.FIRST_NAME_PROPERTY;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.JOB_TITLE;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.LAST_NAME;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.LAST_NAME_PROPERTY;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.LINKEDIN;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.MEDIUM;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.MOBILE_NUMBER;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.STATE;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.TWITTER;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.WORK_NUMBER;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.ZIPCODE;
import static com.bytechef.component.freshsales.util.FreshsalesUtils.getHeaders;
import static com.bytechef.component.freshsales.util.FreshsalesUtils.getUrl;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;

/**
 * @author Monika Domiter
 */
public class FreshsalesCreateContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_CONTACT)
        .title("Create contact")
        .description("Add new contact in Freshsales CRM")
        .properties(
            FIRST_NAME_PROPERTY
                .description("First name of the contact"),
            LAST_NAME_PROPERTY
                .description("Last name of the contact"),
            string(JOB_TITLE)
                .label("Job title")
                .description("Designation of the contact in the account they belong to")
                .required(false),
            EMAIL_PROPERTY
                .description("Primary email address of the contact"),
            string(WORK_NUMBER)
                .label("Work number")
                .description("Work phone number of the contact")
                .controlType(Property.ControlType.PHONE)
                .required(false),
            string(MOBILE_NUMBER)
                .label("Mobile number")
                .description("Mobile phone number of the contact")
                .required(false),
            string(ADDRESS)
                .label("Address")
                .description("Address of the contact")
                .required(false),
            string(CITY)
                .label("City")
                .description("City that the contact belongs to")
                .required(false),
            string(STATE)
                .label("State")
                .description("State that the contact belongs to")
                .required(false),
            string(ZIPCODE)
                .label("Zip code")
                .description("Zipcode of the region that the contact belongs to")
                .required(false),
            string(COUNTRY)
                .label("Country")
                .description("Country that the contact belongs to")
                .required(false),
            string(MEDIUM)
                .label("Medium")
                .description("The medium that led your contact to your website/web app")
                .required(false),
            string(FACEBOOK)
                .label("Facebook")
                .description("Facebook username of the contact")
                .required(false),
            string(TWITTER)
                .label("Twitter")
                .description("Twitter username of the contact")
                .required(false),
            string(LINKEDIN)
                .label("LinkedIn")
                .description("LinkedIn account of the contact")
                .required(false))
        .outputSchema(
            object()
                .additionalProperties(
                    array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time()))
        .perform(FreshsalesCreateContactAction::perform);

    private FreshsalesCreateContactAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.post(getUrl(connectionParameters, "contacts")))
            .headers(getHeaders(connectionParameters))
            .body(Http.Body.of(false,
                FIRST_NAME, inputParameters.getString(FIRST_NAME),
                LAST_NAME, inputParameters.getString(LAST_NAME),
                JOB_TITLE, inputParameters.getString(JOB_TITLE),
                EMAIL, inputParameters.getRequiredString(EMAIL),
                WORK_NUMBER, inputParameters.getString(WORK_NUMBER),
                MOBILE_NUMBER, inputParameters.getString(MOBILE_NUMBER),
                ADDRESS, inputParameters.getString(ADDRESS),
                CITY, inputParameters.getString(CITY),
                STATE, inputParameters.getString(STATE),
                ZIPCODE, inputParameters.getString(ZIPCODE),
                COUNTRY, inputParameters.getString(COUNTRY),
                MEDIUM, inputParameters.getString(MEDIUM),
                FACEBOOK, inputParameters.getString(FACEBOOK),
                TWITTER, inputParameters.getString(TWITTER),
                LINKEDIN, inputParameters.getString(LINKEDIN)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

}
