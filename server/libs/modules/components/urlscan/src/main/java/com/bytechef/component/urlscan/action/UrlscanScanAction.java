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

package com.bytechef.component.urlscan.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class UrlscanScanAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("scan")
        .title("Scan")
        .description("Submit a URL to be scanned and control options for how the scan should be performed.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/api/v1/scan", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("url").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("URL")
            .description("The URL to be scanned.")
            .required(true),
            string("visibility").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Visibility")
                .description("Intended visibility of the final scan result.")
                .options(option("Public", "public"), option("Unlisted", "unlisted"), option("Private", "private"))
                .required(false),
            array("tags").items(string().metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .description("User-defined tags to annotate this scan."))
                .placeholder("Add to Tags")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Tags")
                .description("User-defined tags to annotate this scan.")
                .required(false))
        .output(outputSchema(object()
            .properties(string("uuid").description("UUID for scan result, also called $scanId.")
                .required(false),
                string("country").description("Country for scanning.")
                    .required(false),
                string("visibility").description("Determined visibility for scan.")
                    .required(false),
                string("url").description("Determined URL being scanned.")
                    .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private UrlscanScanAction() {
    }
}
