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

import static com.bytechef.component.dropbox.constant.DropboxConstants.GETFILELINK;
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
import com.dropbox.core.v2.files.GetTemporaryLinkResult;

/**
 * @author Mario Cvjetojevic
 */
public final class DropboxGetFileLinkAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(GETFILELINK)
        .title("Get file link")
        .description(
            "Get a temporary link to stream content of a file. This link will expire in four hours and afterwards " +
                "you will get 410 Gone. This URL should not be used to display content directly in the browser. " +
                "The Content-Type of the link is determined automatically by the file's mime type.")
        .properties(
            string(SOURCE_FILENAME)
                .label("Path")
                .description("The path to the file you want a temporary link to. Must match pattern " +
                    "\" (/(.|[\\\\r\\\\n])*|id:.*)|(rev:[0-9a-f]{9,})|(ns:[0-9]+(/.*)?)\" and not be null.")
                .required(true))
        .outputSchema(string())
        .perform(DropboxGetFileLinkAction::perform);

    private DropboxGetFileLinkAction() {
    }

    public static String perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext actionContext)
        throws ComponentExecutionException {

        try {
            DbxUserFilesRequests dbxUserFilesRequests = getDbxUserFilesRequests(
                connectionParameters.getRequiredString(ACCESS_TOKEN));

            GetTemporaryLinkResult getTemporaryLinkResult = dbxUserFilesRequests.getTemporaryLink(
                inputParameters.getRequiredString(SOURCE_FILENAME));

            return getTemporaryLinkResult.getLink();
        } catch (DbxException dbxException) {
            throw new ComponentExecutionException("Unable to get file link " + inputParameters, dbxException);
        }
    }
}
