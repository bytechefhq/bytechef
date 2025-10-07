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

package com.bytechef.component.ahrefs.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.ahrefs.util.AhrefsUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class AhrefsGetPageContentAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getPageContent")
        .title("Get Page Content")
        .description("Returns the content of a page.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/site-audit/page-content"

            ))
        .properties(string("target_url").label("Target URL")
            .description("The URL of the page to retrieve content for.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)),
            string("project_id").label("Project ID")
                .description(
                    "The unique identifier of the project. Only projects with verified ownership are supported.")
                .required(true)
                .options((ActionDefinition.OptionsFunction<String>) AhrefsUtils::getProjectIdOptions)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("select").label("Select")
                .description("A comma-separated list of columns to return.")
                .options(option("Crawl_datetime", "crawl_datetime"), option("Page_text", "page_text"),
                    option("Raw_html", "raw_html"), option("Rendered_html", "rendered_html"))
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .output(outputSchema(object()
            .properties(object("page-content")
                .properties(string("crawl_datetime").description("The timestamp when the page was crawled.")
                    .required(false),
                    string("page_text").description("The text extracted from the page content.")
                        .required(false),
                    string("raw_html").description("The raw HTML of the page.")
                        .required(false),
                    string("rendered_html").description("The rendered HTML of the page.")
                        .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private AhrefsGetPageContentAction() {
    }
}
