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
                    .items(
                        object()
                            .properties(
                                string(FAMILY_NAME),
                                string(GIVEN_NAME),
                                string(MIDDLE_NAME))),
                array("organizations")
                    .items(
                        object()
                            .properties(
                                string(NAME),
                                string(TITLE))),
                array("emailAddresses")
                    .items(
                        object()
                            .properties(
                                string("value"))),
                array("phoneNumbers")
                    .items(
                        object()
                            .properties(
                                string("value"))));

    private GoogleContactsConstants() {
    }
}
