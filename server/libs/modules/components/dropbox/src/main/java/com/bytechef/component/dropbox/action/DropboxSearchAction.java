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

package com.bytechef.component.dropbox.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.dropbox.constant.DropboxConstants.QUERY;
import static com.bytechef.component.dropbox.constant.DropboxConstants.SEARCH;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
public class DropboxSearchAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(SEARCH)
        .title("Search")
        .description(
            "Searches for files and folders. Can only be used to retrieve a maximum of 10,000 matches. Recent " +
                "changes may not immediately be reflected in search results due to a short delay in indexing. " +
                "Duplicate results may be returned across pages. Some results may not be returned.")
        .properties(
            string(QUERY)
                .label("Search string")
                .description(
                    "The string to search for. May match across multiple fields based on the request arguments.")
                .minLength(3)
                .maxLength(1000)
                .required(true))
        .outputSchema(
            object()
                .properties(
                    array("matches")
                        .items(
                            object()
                                .properties(
                                    object("match_type")
                                        .properties(
                                            string(".tag")),
                                    object("metadata")
                                        .properties(
                                            string(".tag"),
                                            string("id"),
                                            string("name"),
                                            string("path_display"),
                                            string("path_lower"))))))
        .perform(DropboxSearchAction::perform);

    protected static final ContextFunction<Http, Http.Executor> POST_SEARCH_CONTEXT_FUNCTION =
        http -> http.post("https://api.dropboxapi.com/2/files/search_v2");

    private DropboxSearchAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(POST_SEARCH_CONTEXT_FUNCTION)
            .body(Http.Body.of(QUERY, inputParameters.getRequired(QUERY)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
