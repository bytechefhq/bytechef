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

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.dropbox.constant.DropboxConstants.SEARCH;
import static com.bytechef.component.dropbox.constant.DropboxConstants.SEARCH_STRING;
import static com.bytechef.component.dropbox.util.DropboxUtils.getDbxUserFilesRequests;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.SearchV2Result;

/**
 * @author Mario Cvjetojevic
 */
public final class DropboxSearchAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(SEARCH)
        .title("Search")
        .description(
            "Searches for files and folders. Can only be used to retrieve a maximum of 10,000 matches. Recent " +
                "changes may not immediately be reflected in search results due to a short delay in indexing. " +
                "Duplicate results may be returned across pages. Some results may not be returned.")
        .properties(
            string(SEARCH_STRING)
                .label("Search string")
                .description(
                    "The string to search for. May match across multiple fields based on the request arguments."
                        + "Must have length of at most 1000 and not be null.")
                .required(true))
        .outputSchema(
            object()
                .properties(
                    array("matches")
                        .items(
                            object()
                                .properties(
                                    array("highlightSpans")
                                        .items(
                                            object()
                                                .properties(
                                                    string("highlightStr")
                                                        .required(true),
                                                    bool("isHighlighted")
                                                        .required(true)))
                                        .label("Highlight spans")))
                        .label("Matches"),
                    bool("hasMore")
                        .required(true),
                    string("cursor")
                        .required(true)))
        .perform(DropboxSearchAction::perform);

    private DropboxSearchAction() {
    }

    public static SearchV2Result perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext)
        throws DbxException {

        DbxUserFilesRequests dbxUserFilesRequests = getDbxUserFilesRequests(
            connectionParameters.getRequiredString(ACCESS_TOKEN));

        return dbxUserFilesRequests.searchV2(inputParameters.getRequiredString(SEARCH_STRING));
    }
}
