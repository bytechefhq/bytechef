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

package com.bytechef.component.dropbox.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.dropbox.constant.DropboxConstants.QUERY;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
public class DropboxSearchAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("search")
        .title("Search")
        .description(
            "Searches for files and folders. Can only be used to retrieve a maximum of 10,000 matches. Recent " +
                "changes may not immediately be reflected in search results due to a short delay in indexing. " +
                "Duplicate results may be returned across pages. Some results may not be returned.")
        .properties(
            string(QUERY)
                .label("Search String")
                .description(
                    "The string to search for. May match across multiple fields based on the request arguments.")
                .minLength(3)
                .maxLength(1000)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        array("matches")
                            .description("A list (possibly empty) of matches for the query.")
                            .items(
                                object()
                                    .properties(
                                        object("metadata")
                                            .description("The metadata for the matched file or folder.")
                                            .properties(
                                                string("name")
                                                    .description(
                                                        "The name of the file or folder, including its extension. " +
                                                            "This is the last component of the path."),
                                                string("path_lower")
                                                    .description(
                                                        "The full path to the file or folder in lowercase, as stored " +
                                                            "in the user's Dropbox."),
                                                string("path_display")
                                                    .description(
                                                        "The display-friendly version of the path to the file or " +
                                                            "folder, preserving original casing."),
                                                string("id")
                                                    .description("ID of the file or folder.")))))))
        .perform(DropboxSearchAction::perform);

    protected static final ContextFunction<Http, Http.Executor> POST_SEARCH_CONTEXT_FUNCTION =
        http -> http.post("https://api.dropboxapi.com/2/files/search_v2");

    private DropboxSearchAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(POST_SEARCH_CONTEXT_FUNCTION)
            .body(Http.Body.of(QUERY, inputParameters.getRequired(QUERY)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
