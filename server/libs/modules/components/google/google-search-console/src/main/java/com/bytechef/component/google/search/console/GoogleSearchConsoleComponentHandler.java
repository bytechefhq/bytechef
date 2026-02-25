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

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.google.commons.GoogleConnection;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class GoogleSearchConsoleComponentHandler extends AbstractGoogleSearchConsoleComponentHandler {

    @Override
    public List<ModifiableActionDefinition> modifyActions(ModifiableActionDefinition... actionDefinitions) {
        for (ModifiableActionDefinition actionDefinition : actionDefinitions) {
            String name = actionDefinition.getName();

            switch (name) {
                case "addSite" ->
                    actionDefinition.help(
                        "",
                        "https://docs.bytechef.io/reference/components/google-search-console_v1#add-site");
                case "deleteSite" ->
                    actionDefinition.help(
                        "",
                        "https://docs.bytechef.io/reference/components/google-search-console_v1#delete-site");
                case "getSite" ->
                    actionDefinition.help(
                        "",
                        "https://docs.bytechef.io/reference/components/google-search-console_v1#get-site");
                case "listSites" ->
                    actionDefinition.help(
                        "",
                        "https://docs.bytechef.io/reference/components/google-search-console_v1#list-sites");
                case "searchAnalytics" ->
                    actionDefinition.help(
                        "",
                        "https://docs.bytechef.io/reference/components/google-search-console_v1" +
                            "#search-analytics");
                default -> {

                }
            }
        }
        return super.modifyActions(actionDefinitions);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return GoogleConnection.createConnection(
            "https://searchconsole.googleapis.com/webmasters/v3",
            1,
            "https://docs.bytechef.io/reference/components/google-search-console_v1#connection-setup",
            (connection, context) -> Map.of(
                "https://www.googleapis.com/auth/webmasters", true,
                "https://www.googleapis.com/auth/webmasters.readonly", false));
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/google-search-console.svg")
            .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION)
            .customAction(true)
            .customActionHelp(
                "Google Search Console API documentation",
                "https://developers.google.com/webmaster-tools/")
            .version(1);
    }
}
