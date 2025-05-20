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

package com.bytechef.component.productboard.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
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
public class ProductboardCreateNoteAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createNote")
        .title("Create Note")
        .description("Creates a new note.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/notes", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("title").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("Title")
            .description("Title of note.")
            .required(true),
            string("content").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Content")
                .description("HTML-encoded rich text supported by certain tags; unsupported tags will be stripped out.")
                .required(true))
        .output(outputSchema(object()
            .properties(
                object("links")
                    .properties(
                        string("html").description("Note is accessible via this URL in the Productboard application.")
                            .required(false))
                    .required(false),
                object("data").properties(string("id").description("ID of the note.")
                    .required(false))
                    .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private ProductboardCreateNoteAction() {
    }
}
