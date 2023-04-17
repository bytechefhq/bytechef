
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

package com.bytechef.component.petstore.action;

import static com.bytechef.hermes.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.util.HttpClientUtils.ResponseFormat;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class LoginUserAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("loginUser")
        .display(
            display("Logs user into the system")
                .description(""))
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/user/login"

            ))
        .properties(string("username").label("Username")
            .description("The user name for login")
            .required(false)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)),
            string("password").label("Password")
                .description("The password for login in clear text")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .outputSchema(string().metadata(
            Map.of(
                "responseFormat", ResponseFormat.TEXT)));
}
