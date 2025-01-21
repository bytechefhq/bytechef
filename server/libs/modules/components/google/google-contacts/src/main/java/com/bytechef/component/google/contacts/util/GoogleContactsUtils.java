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

import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.FAMILY_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.GIVEN_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.MIDDLE_NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.NAME;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.TITLE;

import com.bytechef.component.definition.Parameters;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Organization;

/**
 * @author Monika Kušter
 */
public class GoogleContactsUtils {

    private GoogleContactsUtils() {
    }

    public static Name createName(Parameters inputParameters) {
        return new Name()
            .setGivenName(inputParameters.getRequiredString(GIVEN_NAME))
            .setMiddleName(inputParameters.getString(MIDDLE_NAME))
            .setFamilyName(inputParameters.getRequiredString(FAMILY_NAME));
    }

    public static Organization createOrganization(Parameters inputParameters) {
        return new Organization()
            .setName(inputParameters.getString(NAME))
            .setTitle(inputParameters.getString(TITLE));
    }
}
