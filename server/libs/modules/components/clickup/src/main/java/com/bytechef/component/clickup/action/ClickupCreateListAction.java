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

package com.bytechef.component.clickup.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.clickup.util.ClickupUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ClickupCreateListAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createList")
        .title("Create List")
        .description("Creates a new List in specified Folder.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/folder/{folderId}/list", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(string("folderId").label("Folder ID")
            .description("ID of the folder where new list will be created.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) ClickupUtils::getFolderIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("name").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Name")
                .description("The name of the list.")
                .required(true))
        .output(outputSchema(object().properties(string("id").description("The ID of the list.")
            .required(false),
            string("name").description("The name of the list.")
                .required(false),
            object("folder").properties(string("id").description("The ID of the folder.")
                .required(false),
                string("name").description("The name of the folder.")
                    .required(false))
                .description("The folder where the list is located.")
                .required(false),
            object("space").properties(string("id").description("The ID of the space.")
                .required(false),
                string("name").description("The name of the space.")
                    .required(false))
                .description("The space where the folder is located.")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private ClickupCreateListAction() {
    }
}
