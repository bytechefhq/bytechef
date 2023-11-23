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

import static com.bytechef.component.dropbox.constant.DropboxConstants.SEARCH;
import static com.bytechef.component.dropbox.constant.DropboxConstants.SEARCH_STRING;
import static com.bytechef.component.dropbox.util.DropboxUtils.getDropboxRequestObject;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.ACCESS_TOKEN;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

import com.bytechef.hermes.component.definition.ActionDefinition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.ParameterMap;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.dropbox.core.DbxException;
import java.util.List;

/**
 * @author Mario Cvjetojevic
 */
public final class DropboxSearchAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(SEARCH)
        .title("Search")
        .description("""
            Searches for files and folders.

            Can only be used to retrieve a maximum of 10,000 matches.

            Recent changes may not immediately be reflected in search results due to a short delay in indexing.
            Duplicate results may be returned across pages. Some results may not be returned.""")
        .properties(
            string(SEARCH_STRING)
                .label("Search string")
                .description(
                    "The string to search for. May match across multiple fields based on the request arguments."
                        + "Must have length of at most 1000 and not be null.")
                .required(true))
        .outputSchema(object())
        .perform(DropboxSearchAction::perform);

    protected static SearchV2Result perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext actionContext)
        throws ComponentExecutionException {
        try {
            return new SearchV2Result(
                getDropboxRequestObject(connectionParameters.getRequiredString(ACCESS_TOKEN))
                    .searchV2(inputParameters.getRequiredString(SEARCH_STRING)));
        } catch (DbxException dbxException) {
            throw new ComponentExecutionException("Unable to search " + inputParameters, dbxException);
        }
    }

    record SearchV2Result(List<SearchMatchV2> entries, String cursor, boolean hasMore) {
        SearchV2Result(com.dropbox.core.v2.files.SearchV2Result searchV2Result) {
            this(
                searchV2Result.getMatches()
                    .stream()
                    .map(SearchMatchV2::new)
                    .toList(),
                searchV2Result.getCursor(),
                searchV2Result.getHasMore());
        }
    }

    record SearchMatchV2(List<HighlightSpan> highlightSpans) {
        SearchMatchV2(com.dropbox.core.v2.files.SearchMatchV2 searchMatchV2) {
            this(searchMatchV2.getHighlightSpans()
                .stream()
                .map(HighlightSpan::new)
                .toList());
        }
    }

    record HighlightSpan(String highlightStr, boolean isHighlighted) {
        HighlightSpan(com.dropbox.core.v2.files.HighlightSpan highlightSpan) {
            this(highlightSpan.getHighlightStr(), highlightSpan.getIsHighlighted());
        }
    }

    private DropboxSearchAction() {
    }
}
