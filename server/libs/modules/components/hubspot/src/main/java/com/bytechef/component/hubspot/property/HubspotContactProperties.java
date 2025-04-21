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

package com.bytechef.component.hubspot.property;

import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class HubspotContactProperties {
    public static final List<ComponentDsl.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(string("id").label("Id")
        .description("ID of the newly created contact.")
        .required(false),
        object("properties").properties(string("firstname").label("Firstname")
            .description("First name of the contact.")
            .required(false),
            string("lastname").label("Lastname")
                .description("Last name of the contact.")
                .required(false),
            string("email").label("Email")
                .description("Email address of the contact.")
                .required(false),
            string("phone").label("Phone")
                .description("Phone number of the contact.")
                .required(false),
            string("company").label("Company")
                .description("Company contact belongs to.")
                .required(false),
            string("website").label("Website")
                .description("Website of the contact.")
                .required(false))
            .label("Properties")
            .required(false));

    private HubspotContactProperties() {
    }
}
