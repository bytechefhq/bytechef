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

package com.bytechef.component.google.search.console.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.google.search.console.property.GoogleSearchConsoleSitesListResponseProperties;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class GoogleSearchConsoleListSitesAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("listSites")
        .title("List Sites")
        .description("Lists the user's Search Console sites.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/sites"

            ))
        .properties()
        .output(outputSchema(object().properties(GoogleSearchConsoleSitesListResponseProperties.PROPERTIES)
            .description("List of sites with access level information.")
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private GoogleSearchConsoleListSitesAction() {
    }
}
