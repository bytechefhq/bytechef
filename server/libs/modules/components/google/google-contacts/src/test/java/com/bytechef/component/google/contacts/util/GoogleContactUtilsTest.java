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

package com.bytechef.component.google.contacts.util;

import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.COMPANY;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.FIRST_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.JOB_TITLE;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.LAST_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.MIDDLE_NAME;
import static com.bytechef.component.google.contacts.util.GoogleContactsUtils.createName;
import static com.bytechef.component.google.contacts.util.GoogleContactsUtils.createOrganization;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Organization;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class GoogleContactUtilsTest {

    private Parameters mockedParameters;

    @Test
    void testCreateName() {
        mockedParameters = MockParametersFactory.create(
            Map.of(FIRST_NAME, "John", MIDDLE_NAME, "A.", LAST_NAME, "Doe"));

        Name name = createName(mockedParameters);

        assertEquals("John", name.getGivenName());
        assertEquals("A.", name.getMiddleName());
        assertEquals("Doe", name.getFamilyName());
    }

    @Test
    void testCreateOrganization() {
        mockedParameters = MockParametersFactory.create(Map.of(COMPANY, "Tech Corp", JOB_TITLE, "Engineer"));

        Organization organization = createOrganization(mockedParameters);

        assertEquals("Tech Corp", organization.getName());
        assertEquals("Engineer", organization.getTitle());
    }
}
