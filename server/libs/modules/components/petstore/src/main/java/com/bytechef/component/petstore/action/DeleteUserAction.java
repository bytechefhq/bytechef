
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

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class DeleteUserAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("deleteUser")
        .display(
            display("Delete user")
                .description("This can only be done by the logged in user."))
        .metadata(
            Map.of(
                "method", "DELETE",
                "path", "/user/{username}"

            ))
        .properties(string("username").label("Username")
            .description("The name that needs to be deleted")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)));
}
