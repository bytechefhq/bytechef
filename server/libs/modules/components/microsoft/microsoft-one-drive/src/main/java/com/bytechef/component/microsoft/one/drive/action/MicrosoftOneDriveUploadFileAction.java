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

package com.bytechef.component.microsoft.one.drive.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.BASE_URL;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.FILE;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.ID;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.PARENT_ID_PROPERTY;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.UPLOAD_FILE;
import static com.bytechef.component.microsoft.one.drive.util.MicrosoftOneDriveUtils.getFolderId;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Domiter
 */
public class MicrosoftOneDriveUploadFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(UPLOAD_FILE)
        .title("Upload file")
        .description("Upload a file to your Microsoft OneDrive")
        .properties(
            PARENT_ID_PROPERTY
                .description("Folder where to upload file"),
            fileEntry(FILE)
                .label("File")
                .description("File to upload")
                .required(true))
        .outputSchema(
            object()
                .properties(
                    string(ID),
                    string("name")))
        .perform(MicrosoftOneDriveUploadFileAction::perform);

    private MicrosoftOneDriveUploadFileAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        FileEntry fileEntry = inputParameters.getRequiredFileEntry(FILE);

        return context
            .http(http -> http.put(
                BASE_URL + "/items/" + getFolderId(inputParameters) + ":/" + fileEntry.getName() + ":/content"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(fileEntry))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
