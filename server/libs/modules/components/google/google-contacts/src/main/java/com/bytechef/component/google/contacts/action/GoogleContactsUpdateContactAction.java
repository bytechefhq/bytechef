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
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.E_TAG;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.FAMILY_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.GIVEN_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.MIDDLE_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.NAMES;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.ORGANIZATIONS;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.PHONE_NUMBER;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.PHONE_NUMBERS;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.RESOURCE_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.TITLE;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.VALUE;
import static com.bytechef.component.google.contacts.util.GoogleContactsUtils.getContactToUpdate;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.google.contacts.util.GoogleContactsUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Martin Tarasovič
 * @author Nikolina Spehar
 */
public class GoogleContactsUpdateContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateContact")
        .title("Update Contact")
        .description("Modifies an existing contact.")
        .properties(
            string(RESOURCE_NAME)
                .label("Resource Name")
                .description("Resource name of the contact to be updated.")
                .options((OptionsFunction<String>) GoogleContactsUtils::getContactResourceNameOptions)
                .required(true),
            string(GIVEN_NAME)
                .label("First Name")
                .description("New first name of the contact.")
                .required(false),
            string(MIDDLE_NAME)
                .label("Middle Name")
                .description("New middle name of the contact.")
                .required(false),
            string(FAMILY_NAME)
                .label("Last Name")
                .description("Updated last name of the contact.")
                .required(false),
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
        .help("", "https://docs.bytechef.io/reference/components/google-contacts_v1#update-contact")
        .perform(GoogleContactsUpdateContactAction::perform);

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, Object> contactToUpdate = getContactToUpdate(
            inputParameters.getRequiredString(RESOURCE_NAME), context);

        Map<String, Object> body = updateContact(inputParameters, contactToUpdate);

        return context.http(http -> http.patch(
            "/%s:updateContact".formatted(inputParameters.getRequiredString(RESOURCE_NAME))))
            .configuration(responseType(Http.ResponseType.JSON))
            .queryParameters("updatePersonFields", "emailAddresses,names,phoneNumbers,organizations")
            .body(Body.of(body))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static Map<String, Object> updateContact(
        Parameters parameters, Map<String, Object> contactToUpdate) {

        List<String> updateParametersList = List.of(
            GIVEN_NAME, MIDDLE_NAME, FAMILY_NAME, TITLE, NAME, EMAIL, PHONE_NUMBER);

        Map<String, String> names = (Map<String, String>) contactToUpdate.get(NAMES);
        Map<String, String> organizations = (Map<String, String>) contactToUpdate.get(ORGANIZATIONS);
        Map<String, String> emailAddresses = (Map<String, String>) contactToUpdate.get(EMAIL_ADDRESSES);
        Map<String, String> phoneNumbers = (Map<String, String>) contactToUpdate.get(PHONE_NUMBERS);

        for (String parameter : updateParametersList) {
            switch (parameter) {
                case GIVEN_NAME -> {
                    String givenName = parameters.getString(GIVEN_NAME);

                    if (givenName != null) {
                        names.put(GIVEN_NAME, givenName);
                    }
                }
                case MIDDLE_NAME -> {
                    String middleName = parameters.getString(MIDDLE_NAME);

                    if (middleName != null) {
                        names.put(MIDDLE_NAME, middleName);
                    }
                }
                case FAMILY_NAME -> {
                    String familyName = parameters.getString(FAMILY_NAME);

                    if (familyName != null) {
                        names.put(FAMILY_NAME, familyName);
                    }
                }
                case TITLE -> {
                    String title = parameters.getString(TITLE);

                    if (title != null) {
                        organizations.put(TITLE, title);
                    }
                }
                case NAME -> {
                    String name = parameters.getString(NAME);

                    if (name != null) {
                        organizations.put(NAME, name);
                    }
                }
                case EMAIL -> {
                    String email = parameters.getString(EMAIL);

                    if (email != null) {
                        emailAddresses.put(VALUE, email);
                    }
                }
                case PHONE_NUMBER -> {
                    String phoneNumber = parameters.getString(PHONE_NUMBER);

                    if (phoneNumber != null) {
                        phoneNumbers.put(VALUE, phoneNumber);
                    }
                }
                default -> {
                }
            }
        }

        return Map.of(
            E_TAG, contactToUpdate.get(E_TAG),
            NAMES, List.of(names),
            ORGANIZATIONS, List.of(organizations),
            EMAIL_ADDRESSES, List.of(emailAddresses),
            PHONE_NUMBERS, List.of(phoneNumbers));
    }
}
