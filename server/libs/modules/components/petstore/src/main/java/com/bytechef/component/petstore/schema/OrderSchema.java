
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.component.petstore.schema;

import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.dateTime;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.option;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.definition.Property;
import java.util.List;

/**
 * Provides schema definition.
 *
 * @generated
 */
public class OrderSchema {
    public static final List<Property> COMPONENT_SCHEMA = List.of(integer("id").label("Id")
        .required(false)
        .exampleValue(10),
        integer("petId").label("PetId")
            .required(false)
            .exampleValue(198772),
        integer("quantity").label("Quantity")
            .required(false)
            .exampleValue(7),
        dateTime("shipDate").label("ShipDate")
            .required(false),
        string("status").label("Status")
            .description("Order Status")
            .options(option("Placed", "placed"), option("Approved", "approved"), option("Delivered", "delivered"))
            .required(false)
            .exampleValue("approved"),
        bool("complete").label("Complete")
            .required(false));
}
