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
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.CONTACT_OUTPUT_PROPERTY;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.EMAIL;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.FAMILY_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.GIVEN_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.MIDDLE_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.PHONE_NUMBER;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.RESOURCE_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.TITLE;
import static com.bytechef.component.google.contacts.util.GoogleContactsUtils.createName;
import static com.bytechef.component.google.contacts.util.GoogleContactsUtils.createOrganization;
import static com.bytechef.google.commons.GoogleUtils.translateGoogleIOException;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PhoneNumber;
import java.io.IOException;
import java.util.List;

/**
 * @author Martin Tarasovič
 */
public class GoogleContactsUpdateContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateContact")
        .title("Update Contact")
        .description("Modifies an existing contact.")
        .properties(
            string(RESOURCE_NAME)
                .label("Resource Name")
                .description("Resource name of the contact to be updated.")
                .required(true),
            string(GIVEN_NAME)
                .label("First Name")
                .description("New first name of the contact.")
                .required(true),
            string(MIDDLE_NAME)
                .label("Middle Name")
                .description("New middle name of the contact.")
                .required(false),
            string(FAMILY_NAME)
                .label("Last Name")
                .description("Updated last name of the contact.")
                .required(true),
            string(TITLE)
                .label("Job Title")
                .description("Updated job title of the contact.")
                .required(false),
            string(NAME)
                .label("Company")
                .description("Updated name of the company where the contact is employed.")
                .required(false),
            string(EMAIL)
                .label("Email Address")
                .description("Updated email address of the contact.")
                .controlType(ControlType.EMAIL)
                .required(false),
            string(PHONE_NUMBER)
                .label("Phone Number")
                .description("Updated phone number of the contact.")
                .controlType(ControlType.PHONE)
                .required(false))
        .output(outputSchema(CONTACT_OUTPUT_PROPERTY))
        .perform(GoogleContactsUpdateContactAction::perform);

    public static Person perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        PeopleService peopleService = GoogleServices.getPeopleService(connectionParameters);

        String resourceName = inputParameters.getRequiredString(RESOURCE_NAME);
        String personFields = "names,emailAddresses,phoneNumbers,organizations";

        Person existingPerson;
        try {
            existingPerson = peopleService
                .people()
                .get(resourceName)
                .setPersonFields(personFields)
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }

        existingPerson
            .setNames(List.of(createName(inputParameters)))
            .setEmailAddresses(List.of(new EmailAddress().setValue(inputParameters.getString(EMAIL))))
            .setPhoneNumbers(List.of(new PhoneNumber().setValue(inputParameters.getString(PHONE_NUMBER))))
            .setOrganizations(List.of(createOrganization(inputParameters)));

        try {
            return peopleService
                .people()
                .updateContact(resourceName, existingPerson)
                .setUpdatePersonFields(personFields)
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }
    }
}
