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

package com.bytechef.component.google.search.console;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.google.search.console.action.GoogleSearchConsoleAddSiteAction;
import com.bytechef.component.google.search.console.action.GoogleSearchConsoleDeleteSiteAction;
import com.bytechef.component.google.search.console.action.GoogleSearchConsoleGetSiteAction;
import com.bytechef.component.google.search.console.action.GoogleSearchConsoleListSitesAction;
import com.bytechef.component.google.search.console.action.GoogleSearchConsoleSearchAnalyticsAction;
import com.bytechef.component.google.search.console.connection.GoogleSearchConsoleConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractGoogleSearchConsoleComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("googleSearchConsole")
            .title("Google Search Console")
            .description(
                "The Search Console API provides access to both Search Console data (verified users only) and to public information on an URL basis (anyone)."))
                    .actions(modifyActions(GoogleSearchConsoleListSitesAction.ACTION_DEFINITION,
                        GoogleSearchConsoleDeleteSiteAction.ACTION_DEFINITION,
                        GoogleSearchConsoleGetSiteAction.ACTION_DEFINITION,
                        GoogleSearchConsoleAddSiteAction.ACTION_DEFINITION,
                        GoogleSearchConsoleSearchAnalyticsAction.ACTION_DEFINITION))
                    .connection(modifyConnection(GoogleSearchConsoleConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
