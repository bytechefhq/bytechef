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

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.google.search.console.util.GoogleSearchConsoleUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class GoogleSearchConsoleDeleteSiteAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("deleteSite")
        .title("Delete Site")
        .description("Removes a site from the set of the user's Search Console sites.")
        .metadata(
            Map.of(
                "method", "DELETE",
                "path", "/sites/{siteUrl}"

            ))
        .properties(string("siteUrl").label("Site URL")
            .description(
                "The URI of the property as defined in Search Console. **Examples:** `http://www.example.com/` or `sc-domain:example.com`.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) GoogleSearchConsoleUtils::getSiteUrlOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)));

    private GoogleSearchConsoleDeleteSiteAction() {
    }
}
