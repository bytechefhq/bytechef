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

import static com.bytechef.component.dropbox.constant.DropboxConstants.LISTAFOLDER;
import static com.bytechef.component.dropbox.constant.DropboxConstants.SOURCE_FILENAME;
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
public final class DropboxListFolderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(LISTAFOLDER)
        .title("List folder")
        .description("Lists content of a folder.")
        .properties(
            string(SOURCE_FILENAME)
                .label("Path")
                .description("A unique identifier for the file. Must match pattern " +
                    "\" (/(.|[\\\\r\\\\n])*)?|id:.*|(ns:[0-9]+(/.*)?)\" and not be null.")
                .required(true))
        .outputSchema(object())
        .perform(DropboxListFolderAction::perform);

    protected static ListFolderResult perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext actionContext)
        throws ComponentExecutionException {
        try {
            return new ListFolderResult(
                getDropboxRequestObject(connectionParameters.getRequiredString(ACCESS_TOKEN))
                    .listFolder(inputParameters.getRequiredString(SOURCE_FILENAME)));
        } catch (DbxException dbxException) {
            throw new ComponentExecutionException("Unable to list folder " + inputParameters, dbxException);
        }
    }

    record ListFolderResult(List<Metadata> entries, String cursor, boolean hasMore) {
        ListFolderResult(com.dropbox.core.v2.files.ListFolderResult listFolderResult) {
            this(
                listFolderResult.getEntries()
                    .stream()
                    .map(Metadata::new)
                    .toList(),
                listFolderResult.getCursor(),
                listFolderResult.getHasMore());
        }
    }

    record Metadata(String name, String pathLower, String pathDisplay, String parentSharedFolderId,
        String previewUrl) {
        Metadata(com.dropbox.core.v2.files.Metadata metadata) {
            this(
                metadata.getName(),
                metadata.getPathLower(),
                metadata.getPathDisplay(),
                metadata.getParentSharedFolderId(),
                metadata.getPreviewUrl());
        }
    }

    private DropboxListFolderAction() {
    }
}
