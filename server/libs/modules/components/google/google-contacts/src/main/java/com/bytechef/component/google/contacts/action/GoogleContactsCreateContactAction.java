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

package com.bytechef.component.google.contacts.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.CONTACT_OUTPUT_PROPERTY;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.EMAIL;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.EMAIL_ADDRESSES;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.FAMILY_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.GIVEN_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.MIDDLE_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.NAMES;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.ORGANIZATIONS;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.PERSON_FIELDS;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.PHONE_NUMBER;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.PHONE_NUMBERS;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.TITLE;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.TYPE;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.VALUE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 * @author Nikolina Spehar
 */
public class GoogleContactsCreateContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createContact")
        .title("Create Contact")
        .description("Creates a new contact.")
        .properties(
            string(GIVEN_NAME)
                .label("First Name")
                .description("The first name of the contact.")
                .required(true),
            string(MIDDLE_NAME)
                .label("Middle Name")
                .description("The middle name of the contact.")
                .required(false),
            string(FAMILY_NAME)
                .label("Last Name")
                .description("The last name of the contact.")
                .required(true),
            string(TITLE)
                .label("Job Title")
                .description("The job title of the contact.")
                .required(false),
            string(NAME)
                .label("Company")
                .description("The company of the contact.")
                .required(false),
            string(EMAIL)
                .label("Email")
                .description("The email addresses of the contact.")
                .controlType(ControlType.EMAIL)
                .required(false),
            string(PHONE_NUMBER)
                .label("Phone Number")
                .description("The phone numbers of the contact.")
                .controlType(ControlType.PHONE)
                .required(false))
        .output(outputSchema(CONTACT_OUTPUT_PROPERTY))
        .perform(GoogleContactsCreateContactAction::perform);

    private GoogleContactsCreateContactAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.post("/people:createContact"))
            .configuration(responseType(Http.ResponseType.JSON))
            .queryParameter(PERSON_FIELDS, "emailAddresses,names,phoneNumbers,organizations")
            .body(
                Http.Body.of(
                    NAMES, List.of(
                        Map.of(
                            GIVEN_NAME, inputParameters.getRequiredString(GIVEN_NAME),
                            MIDDLE_NAME, inputParameters.getString(MIDDLE_NAME, ""),
                            FAMILY_NAME, inputParameters.getRequiredString(FAMILY_NAME))),
                    ORGANIZATIONS, List.of(
                        Map.of(
                            NAME, inputParameters.getString(NAME, ""),
                            TITLE, inputParameters.getString(TITLE, ""),
                            TYPE, "work")),
                    EMAIL_ADDRESSES, List.of(
                        Map.of(
                            VALUE, inputParameters.getString(EMAIL, ""),
                            TYPE, "work")),
                    PHONE_NUMBERS, List.of(
                        Map.of(
                            VALUE, inputParameters.getString(PHONE_NUMBER, ""),
                            TYPE, "mobile"))))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
