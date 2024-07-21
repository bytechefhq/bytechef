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

package com.bytechef.component.hubspot.property;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL.ModifiableValueProperty;

import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class HubspotSimplePublicObjectInputForCreateProperties {
    public static final List<ModifiableValueProperty<?, ?>> PROPERTIES = List.of(
        object("properties").properties(string("company").label("Company")
            .required(false),
            string("email").label("Email")
                .required(false),
            string("firstname").label("Firstname")
                .required(false),
            string("lastname").label("Lastname")
                .required(false),
            string("phone").label("Phone")
                .required(false),
            string("website_url").label("Website")
                .required(false))
            .additionalProperties(string())
            .placeholder("Add to Properties")
            .label("Properties")
            .required(true),
        array("associations").items(object().properties(HubspotPublicAssociationsForObjectProperties.PROPERTIES))
            .placeholder("Add to Associations")
            .label("Associations")
            .required(true));

    private HubspotSimplePublicObjectInputForCreateProperties() {
    }
}
