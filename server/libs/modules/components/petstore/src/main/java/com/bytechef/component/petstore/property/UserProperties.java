
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

package com.bytechef.component.petstore.property;

import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.definition.Property;
import java.util.List;

/**
 * Provides schema definition.
 *
 * @generated
 */
public class UserProperties {
    public static final List<Property> PROPERTIES = List.of(integer("id").label("Id")
        .required(false)
        .exampleValue(10),
        string("username").label("Username")
            .required(false)
            .exampleValue("theUser"),
        string("firstName").label("FirstName")
            .required(false)
            .exampleValue("John"),
        string("lastName").label("LastName")
            .required(false)
            .exampleValue("James"),
        string("email").label("Email")
            .required(false)
            .exampleValue("john@email.com"),
        string("password").label("Password")
            .required(false)
            .exampleValue("12345"),
        string("phone").label("Phone")
            .required(false)
            .exampleValue("12345"),
        integer("userStatus").label("UserStatus")
            .description("User Status")
            .required(false)
            .exampleValue(1));
}
