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

package com.bytechef.component.figma.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
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
public class FigmaGetCommentsAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getComments")
        .title("Get Comments")
        .description("Gets a list of comments left on the file.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/v1/files/{fileKey}/comments"

            ))
        .properties(string("fileKey").label("File Key")
            .description("File to get comments from. Figma file key copy from Figma file URL.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .output(
            outputSchema(
                object()
                    .properties(array("comments")
                        .items(object().properties(string("id").description("ID of the comment.")
                            .required(false),
                            string("file_key").description("File key of the file the comment is on.")
                                .required(false),
                            string("parent_id").description("ID of comment this comment is a reply to.")
                                .required(false),
                            object("user").properties(string("id").description("ID of the user.")
                                .required(false),
                                string("handle").description("Name of the user.")
                                    .required(false),
                                string("img_url").description("URL of the user's profile image.")
                                    .required(false),
                                string("email").description("Email of the user.")
                                    .required(false))
                                .description("User who posted the comment.")
                                .required(false))
                            .description("List of comments."))
                        .description("List of comments.")
                        .required(false))
                    .metadata(
                        Map.of(
                            "responseType", ResponseType.JSON))));

    private FigmaGetCommentsAction() {
    }
}
