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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.productboard.property.ProductboardOwnerProperties;
import com.bytechef.component.productboard.util.ProductboardUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ProductboardUpdateNoteAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("updateNote")
        .title("Update Note")
        .description("Updates a note.")
        .metadata(
            Map.of(
                "method", "PATCH",
                "path", "/notes/{noteId}", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(integer("X-Version").label("X - Version")
            .defaultValue(1)
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.HEADER)),
            string("noteId").label("Note ID")
                .description("ID of the note")
                .required(true)
                .options((ActionDefinition.OptionsFunction<String>) ProductboardUtils::getNoteIdOptions)
                .metadata(
                    Map.of(
                        "type", PropertyType.PATH)),
            object("data").properties(string("content").label("Content")
                .description("The content of a note. This can only be updated on notes without existing snippets.")
                .required(false),
                object("owner").properties(ProductboardOwnerProperties.PROPERTIES)
                    .label("Owner")
                    .required(false),
                array("tags").items(string().description("A list of tags for categorizing the note."))
                    .placeholder("Add to Tags")
                    .label("Tags")
                    .description("A list of tags for categorizing the note.")
                    .required(false),
                string("title").label("Title")
                    .description("Title of note.")
                    .required(false))
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Data")
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

    private ProductboardUpdateNoteAction() {
    }
}
