/*
 * Copyright 2023-present ByteChef Inc.
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

import static com.bytechef.component.OpenAPIComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ClickupCreateFolderAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createFolder")
        .title("Create Folder")
        .description("Creates a new folder in a ClickUp workspace.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/space/{spaceId}/folder", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(number("spaceId").label("Space")
            .description("Space where new folder will be created.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            object("__item").properties(string("name").label("Name")
                .description("The name of the folder.")
                .required(true))
                .label("Folder")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY)))
        .outputSchema(object()
            .properties(object("body")
                .properties(string("id").required(false), string("name").required(false),
                    object("space").properties(string("id").required(false), string("name").required(false))
                        .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));

    private ClickupCreateFolderAction() {
    }
}
