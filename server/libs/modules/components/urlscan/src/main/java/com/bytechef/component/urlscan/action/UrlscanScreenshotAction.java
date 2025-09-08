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
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class UrlscanScreenshotAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("screenshot")
        .title("Screenshot")
        .description("Use the scan UUID to retrieve the screenshot for a scan once the scan has finished.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/screenshots/{scanId}.png"

            ))
        .properties(string("scanId").label("Scan Id")
            .description("UUID of scan result.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .output(outputSchema(fileEntry().metadata(
            Map.of(
                "responseType", ResponseType.binary("image/png")))));

    private UrlscanScreenshotAction() {
    }
}
