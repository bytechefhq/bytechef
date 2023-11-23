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

import static com.bytechef.component.dropbox.constant.DropboxConstants.CREATENEWFOLDER;
import static com.bytechef.component.dropbox.constant.DropboxConstants.DESTINATION_FILENAME;
import static com.bytechef.component.dropbox.util.DropboxUtils.getDropboxRequestObject;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.ACCESS_TOKEN;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

import com.bytechef.hermes.component.definition.ActionDefinition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.ParameterMap;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.CreateFolderResult;

/**
 * @author Mario Cvjetojevic
 */
public final class DropboxCreateNewFolderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATENEWFOLDER)
        .title("Create new folder")
        .description("Create a folder at a given path.")
        .properties(string(DESTINATION_FILENAME)
            .label("Destination path")
            .description("Path to create a folder on.")
            .required(true))
        .perform(DropboxCreateNewFolderAction::perform);

    protected static CreateFolderResult perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext actionContext)
        throws ComponentExecutionException {
        try {
            return getDropboxRequestObject(connectionParameters.getRequiredString(ACCESS_TOKEN))
                .createFolderV2(inputParameters.getRequiredString(DESTINATION_FILENAME));
        } catch (DbxException dbxException) {
            throw new ComponentExecutionException("Unable to create new folder " + inputParameters, dbxException);
        }
    }

    private DropboxCreateNewFolderAction() {
    }
}
