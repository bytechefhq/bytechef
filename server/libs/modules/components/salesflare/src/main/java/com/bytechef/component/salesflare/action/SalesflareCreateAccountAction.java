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

package com.bytechef.component.salesflare.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;

import com.bytechef.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class SalesflareCreateAccountAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createAccount")
        .title("Create account")
        .description("Creates new account")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/accounts", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("__item").properties(string("name").label("Name")
            .description("Account name")
            .required(true),
            string("website").label("Website")
                .description("Account website")
                .required(false),
            string("description").label("Description")
                .description("Account description")
                .required(false),
            string("email").label("Email")
                .required(false),
            string("phone_number").label("Phone Number")
                .required(false),
            array("social_profiles").items(string().description("Social profile URL"))
                .placeholder("Add to Social Profiles")
                .label("Social Profiles")
                .description("Social profile URL")
                .required(false))
            .label("Account")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)));

    private SalesflareCreateAccountAction() {
    }
}
