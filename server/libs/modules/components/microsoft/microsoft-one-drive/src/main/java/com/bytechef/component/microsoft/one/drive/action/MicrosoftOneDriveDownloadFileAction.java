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
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.BASE_URL;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.DOWNLOAD_FILE;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.ID;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.PARENT_ID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.one.drive.util.MicrosoftOneDriveUtils;

/**
 * @author Monika Domiter
 */
public class MicrosoftOneDriveDownloadFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(DOWNLOAD_FILE)
        .title("Download file")
        .description("Download a file from your Microsoft OneDrive")
        .properties(
            string(PARENT_ID)
                .label("Parent folder")
                .description("Folder from which you want to download the file.")
                .options((ActionOptionsFunction<String>) MicrosoftOneDriveUtils::getFolderIdOptions)
                .required(false),
            string(ID)
                .label("File")
                .description("File to download")
                .optionsLookupDependsOn(PARENT_ID)
                .options((ActionOptionsFunction<String>) MicrosoftOneDriveUtils::getFileIdOptions)
                .required(true))
        .outputSchema(fileEntry())
        .perform(MicrosoftOneDriveDownloadFileAction::perform);

    private MicrosoftOneDriveDownloadFileAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Http.Response response = context
            .http(http -> http.get(BASE_URL + "/items/" + inputParameters.getRequiredString(ID) + "/content"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        Http.Response fileResponse = context
            .http(http -> http.get(response.getFirstHeader("location")))
            .configuration(Http.responseType(Http.ResponseType.BINARY))
            .execute();

        return fileResponse.getBody();
    }
}
