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

import static com.bytechef.component.dropbox.constant.DropboxConstants.COPY;
import static com.bytechef.component.dropbox.constant.DropboxConstants.DESTINATION_FILENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.SOURCE_FILENAME;
import static com.bytechef.component.dropbox.util.DropboxUtils.getDbxUserFilesRequests;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.ACCESS_TOKEN;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

import com.bytechef.hermes.component.definition.ActionDefinition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.ParameterMap;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.RelocationResult;

/**
 * @author Mario Cvjetojevic
 */
public final class DropboxCopyAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(COPY)
        .title("Copy")
        .description(
            "Copy a file or folder to a different location in the user's Dropbox. If the source path is a folder " +
                "all its contents will be copied.")
        .properties(
            string(SOURCE_FILENAME)
                .label("Source path")
                .description("The path which the file or folder should be copyed from.")
                .required(true),
            string(DESTINATION_FILENAME)
                .label("Destination path")
                .description("The path which the file or folder should be copyed to.")
                .required(true))
        .perform(DropboxCopyAction::perform);

    private DropboxCopyAction() {
    }

    public static RelocationResult perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext actionContext)
        throws ComponentExecutionException {

        try {
            DbxUserFilesRequests dbxUserFilesRequests = getDbxUserFilesRequests(
                connectionParameters.getRequiredString(ACCESS_TOKEN));

            return dbxUserFilesRequests.copyV2(
                inputParameters.getRequiredString(SOURCE_FILENAME),
                inputParameters.getRequiredString(DESTINATION_FILENAME));
        } catch (DbxException dbxException) {
            throw new ComponentExecutionException("Unable to copy " + inputParameters, dbxException);
        }
    }
}
