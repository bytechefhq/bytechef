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

package com.bytechef.component.coda.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.coda.property.CodaDocumentCreationResultProperties;
import com.bytechef.component.coda.util.CodaUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class CodaCopyDocAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("copyDoc")
        .title("Copy Doc")
        .description("Copies an existing doc.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/docs", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("title").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("Title")
            .description("Title of the new doc.")
            .required(true),
            string("sourceDoc").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Source Doc")
                .description("A doc ID from which to create a copy.")
                .required(true)
                .options((ActionDefinition.OptionsFunction<String>) CodaUtils::getSourceDocOptions))
        .output(outputSchema(object().properties(CodaDocumentCreationResultProperties.PROPERTIES)
            .description("The result of a doc creation.")
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private CodaCopyDocAction() {
    }
}
