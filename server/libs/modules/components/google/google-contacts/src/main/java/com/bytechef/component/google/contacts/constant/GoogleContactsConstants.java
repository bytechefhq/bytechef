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

package com.bytechef.component.google.contacts.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Monika Kušter
 */
public class GoogleContactsConstants {

    public static final String EMAIL = "email";
    public static final String FAMILY_NAME = "familyName";
    public static final String GIVEN_NAME = "givenName";
    public static final String MIDDLE_NAME = "middleName";
    public static final String NAME = "name";
    public static final String PAGE_SIZE = "pageSize";
    public static final String PHONE_NUMBER = "phoneNumber";
    public static final String QUERY = "query";
    public static final String READ_MASK = "readMask";
    public static final String RESOURCE_NAME = "resourceName";
    public static final String TITLE = "title";

    public static final ModifiableObjectProperty CONTACT_OUTPUT_PROPERTY =
        object()
            .properties(
                array("names")
                    .description("The person's names.")
                    .items(
                        object()
                            .properties(
                                string(FAMILY_NAME)
                                    .description("The family name."),
                                string(GIVEN_NAME)
                                    .description("The given name."),
                                string(MIDDLE_NAME)
                                    .description("The middle name(s)."))),
                array("organizations")
                    .description("The person's past or current organizations.")
                    .items(
                        object()
                            .properties(
                                string(NAME)
                                    .description("The name of the organization."),
                                string(TITLE)
                                    .description("The person's job title at the organization."))),
                array("emailAddresses")
                    .description("The person's email addresses.")
                    .items(
                        object()
                            .properties(
                                string("value")
                                    .description("The email address."))),
                array("phoneNumbers")
                    .description("The person's phone numbers.")
                    .items(
                        object()
                            .properties(
                                string("value")
                                    .description("The phone number."))));

    private GoogleContactsConstants() {
    }
}
