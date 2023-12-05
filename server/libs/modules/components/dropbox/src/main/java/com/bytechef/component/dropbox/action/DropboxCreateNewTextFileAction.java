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

import static com.bytechef.component.dropbox.constant.DropboxConstants.CREATENEWTEXTFILE;
import static com.bytechef.component.dropbox.constant.DropboxConstants.DESTINATION_FILENAME;
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
import com.dropbox.core.v2.files.ImportFormat;
import com.dropbox.core.v2.files.PaperCreateResult;
import com.dropbox.core.v2.files.PaperCreateUploader;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Mario Cvjetojevic
 */
public final class DropboxCreateNewTextFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATENEWTEXTFILE)
        .title("Create new text file")
        .description("Create a new text file.")
        .properties(
            string(DESTINATION_FILENAME)
                .label("Filename")
                .description("The path at which the new text file should be created.")
                .placeholder("/New_text_file.txt")
                .required(true))
        .perform(DropboxCreateNewTextFileAction::perform);

    private DropboxCreateNewTextFileAction() {
    }

    public static PaperCreateResult perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext actionContext)
        throws ComponentExecutionException {

        DbxUserFilesRequests dbxUserFilesRequests = getDbxUserFilesRequests(
            connectionParameters.getRequiredString(ACCESS_TOKEN));

        try (PaperCreateUploader paperCreateUploader = dbxUserFilesRequests.paperCreate(
            inputParameters.getRequiredString(DESTINATION_FILENAME), ImportFormat.PLAIN_TEXT)) {

            return paperCreateUploader.uploadAndFinish(InputStream.nullInputStream());
        } catch (IOException | DbxException exception) {
            throw new ComponentExecutionException("Unable to create new text file " + inputParameters, exception);
        }
    }
}
