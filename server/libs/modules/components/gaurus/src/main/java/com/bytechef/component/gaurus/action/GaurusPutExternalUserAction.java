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

package com.bytechef.component.gaurus.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class GaurusPutExternalUserAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("putExternalUser")
        .title("Updates an external user.")
        .description(null)
        .metadata(
            Map.of(
                "method", "PUT",
                "path", "/external-users/{externalUserId}"

            ))
        .properties(integer("externalUserId").label("External User Id")
            .description("External user identifier.")
            .required(true)
            .exampleValue(42)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)));

    private GaurusPutExternalUserAction() {
    }
}
