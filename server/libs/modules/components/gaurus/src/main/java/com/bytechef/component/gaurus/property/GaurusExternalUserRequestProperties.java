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

package com.bytechef.component.gaurus.property;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class GaurusExternalUserRequestProperties {
    public static final List<ComponentDsl.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(
        string("name").label("Name")
            .description("External user company name.")
            .required(false)
            .exampleValue("Pero Perić d.o.o."),
        string("oib").label("Oib")
            .description("External user OIB (Croatian personal identification number).")
            .required(false)
            .exampleValue("12345678901"),
        string("mail").label("Mail")
            .description("External user email address.")
            .required(false)
            .exampleValue("pero@example.com"),
        array("bankEntries").items(object().properties(GaurusBankEntryRequestProperties.PROPERTIES))
            .placeholder("Add to Bank Entries")
            .label("Bank Entries")
            .description("List of bank entries to create consent jobs for.")
            .required(false));

    private GaurusExternalUserRequestProperties() {
    }
}
