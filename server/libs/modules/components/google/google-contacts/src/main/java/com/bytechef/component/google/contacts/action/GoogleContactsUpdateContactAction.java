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
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.PERSON_FIELDS;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.PHONE_NUMBER;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.PHONE_NUMBERS;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.RESOURCE_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.TITLE;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.VALUE;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.google.contacts.util.GoogleContactsUtils;
import java.util.HashMap;
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

    private GoogleContactsUpdateContactAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String resourceName = inputParameters.getRequiredString(RESOURCE_NAME);

        Map<String, Object> person = getPerson(resourceName, context);

        return context.http(http -> http.patch("/%s:updateContact".formatted(resourceName)))
            .configuration(responseType(Http.ResponseType.JSON))
            .queryParameter("updatePersonFields", "emailAddresses,names,phoneNumbers,organizations")
            .body(Body.of(updateContact(inputParameters, person)))
            .execute()
            .getBody();
    }

    private static Map<String, Object> getPerson(String resourceName, Context context) {
        return context.http(http -> http.get("/%s".formatted(resourceName)))
            .configuration(responseType(Http.ResponseType.JSON))
            .queryParameter(PERSON_FIELDS, "emailAddresses,names,phoneNumbers,organizations")
            .execute()
            .getBody(new TypeReference<>() {});
    }

    public static Map<String, Object> updateContact(Parameters parameters, Map<String, Object> person) {
        Map<String, String> names = new HashMap<>(getContactNames(person.get(NAMES)));

        updateMap(parameters, names, GIVEN_NAME, GIVEN_NAME);
        updateMap(parameters, names, MIDDLE_NAME, MIDDLE_NAME);
        updateMap(parameters, names, FAMILY_NAME, FAMILY_NAME);

        Map<String, String> organizations = new HashMap<>(getContactOrganizations(person.get(ORGANIZATIONS)));

        updateMap(parameters, organizations, TITLE, TITLE);
        updateMap(parameters, organizations, NAME, NAME);

        Map<String, String> emailAddresses =
            new HashMap<>(getContactEmailAddressesOrPhoneNumbers(person.get(EMAIL_ADDRESSES)));

        updateMap(parameters, emailAddresses, EMAIL, VALUE);

        Map<String, String> phoneNumbers =
            new HashMap<>(getContactEmailAddressesOrPhoneNumbers(person.get(PHONE_NUMBERS)));

        updateMap(parameters, phoneNumbers, PHONE_NUMBER, VALUE);

        return Map.of(
            E_TAG, person.get(E_TAG),
            NAMES, List.of(names),
            ORGANIZATIONS, List.of(organizations),
            EMAIL_ADDRESSES, List.of(emailAddresses),
            PHONE_NUMBERS, List.of(phoneNumbers));
    }

    private static Map<String, String> getContactNames(Object names) {
        Map<String, String> namesMap = new HashMap<>();

        if (names instanceof List<?> namesList && namesList.getFirst() instanceof Map<?, ?> namesObject) {
            namesMap.put(GIVEN_NAME, getIfMapContains(namesObject, GIVEN_NAME));
            namesMap.put(MIDDLE_NAME, getIfMapContains(namesObject, MIDDLE_NAME));
            namesMap.put(FAMILY_NAME, getIfMapContains(namesObject, FAMILY_NAME));
        }

        return namesMap;
    }

    private static void updateMap(Parameters parameters, Map<String, String> map, String parameterName, String mapKey) {
        if (parameters.getString(parameterName) != null) {
            map.put(mapKey, parameters.getString(parameterName));
        }
    }

    private static Map<String, String> getContactOrganizations(Object organizations) {
        Map<String, String> organizationsMap = new HashMap<>();

        if (organizations instanceof List<?> organizationsList &&
            organizationsList.getFirst() instanceof Map<?, ?> organizationObject) {

            organizationsMap.put(TITLE, getIfMapContains(organizationObject, TITLE));
            organizationsMap.put(NAME, getIfMapContains(organizationObject, NAME));
        }

        return organizationsMap;
    }

    private static Map<String, String> getContactEmailAddressesOrPhoneNumbers(Object object) {
        Map<String, String> map = new HashMap<>();

        if (object instanceof List<?> list &&
            list.getFirst() instanceof Map<?, ?> firstElement) {

            map.put(VALUE, getIfMapContains(firstElement, VALUE));
        }

        return map;
    }

    private static String getIfMapContains(Map<?, ?> map, String key) {
        return map.containsKey(key) ? (String) map.get(key) : "";
    }
}
