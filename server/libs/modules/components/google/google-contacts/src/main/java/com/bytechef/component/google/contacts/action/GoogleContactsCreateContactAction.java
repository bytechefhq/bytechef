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

package com.bytechef.component.google.contacts.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.COMPANY;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.CREATE_CONTACT;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.EMAIL;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.FIRST_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.JOB_TITLE;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.LAST_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.MIDDLE_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.PHONE_NUMBER;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Organization;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PhoneNumber;
import java.io.IOException;
import java.util.List;

/**
 * @author Monika Domiter
 */
public class GoogleContactsCreateContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_CONTACT)
        .title("Create contact")
        .description("Creates a new contact")
        .properties(
            string(FIRST_NAME)
                .label("First name")
                .description("The first name of the contact")
                .required(true),
            string(MIDDLE_NAME)
                .label("Middle name")
                .description("The middle name of the contact")
                .required(false),
            string(LAST_NAME)
                .label("Last name")
                .description("The last name of the contact")
                .required(true),
            string(JOB_TITLE)
                .label("Job title")
                .description("The job title of the contact")
                .required(false),
            string(COMPANY)
                .label("Company")
                .description("The company of the contact")
                .required(false),
            string(EMAIL)
                .label("Email")
                .description("The email addresses of the contact")
                .controlType(Property.ControlType.EMAIL)
                .required(false),
            string(PHONE_NUMBER)
                .label("Phone number")
                .description("The phone numbers of the contact")
                .controlType(Property.ControlType.PHONE)
                .required(false))
        .outputSchema(
            object()
                .properties(
                    array("names")
                        .items(
                            object()
                                .properties(
                                    string(FIRST_NAME),
                                    string(MIDDLE_NAME),
                                    string(LAST_NAME))),
                    array("organizations")
                        .items(
                            object()
                                .properties(
                                    string(COMPANY),
                                    string(JOB_TITLE))),
                    array("emailAddresses")
                        .items(
                            object()
                                .properties(
                                    string("value"))),
                    array("phoneNumbers")
                        .items(
                            object()
                                .properties(
                                    string("value")))))
        .perform(GoogleContactsCreateContactAction::perform);

    private GoogleContactsCreateContactAction() {
    }

    public static Person perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        PeopleService peopleService = GoogleServices.getPeopleService(connectionParameters);

        Person person = new Person()
            .setNames(List.of(createName(inputParameters)))
            .setEmailAddresses(List.of(new EmailAddress().setValue(inputParameters.getString(EMAIL))))
            .setPhoneNumbers(List.of(new PhoneNumber().setValue(inputParameters.getString(PHONE_NUMBER))))
            .setOrganizations(List.of(createOrganization(inputParameters)));

        return peopleService
            .people()
            .createContact(person)
            .execute();
    }

    private static Organization createOrganization(Parameters inputParameters) {

        return new Organization()
            .setName(inputParameters.getString(COMPANY))
            .setTitle(inputParameters.getString(JOB_TITLE));
    }

    private static Name createName(Parameters inputParameters) {

        return new Name()
            .setGivenName(inputParameters.getRequiredString(FIRST_NAME))
            .setMiddleName(inputParameters.getString(MIDDLE_NAME))
            .setFamilyName(inputParameters.getRequiredString(LAST_NAME));
    }

}
