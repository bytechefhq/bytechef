/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.box.action;

import static com.bytechef.component.box.constant.BoxConstants.FILE_ID;
import static com.bytechef.component.box.constant.BoxConstants.ID;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.box.util.BoxUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Domiter
 */
public class BoxDownloadFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("downloadFile")
        .title("Download File")
        .description("Download a selected file.")
        .properties(
            string(ID)
                .label("Parent Folder ID")
                .description("ID of the folder from which you want to download the file.")
                .options((OptionsFunction<String>) BoxUtils::getRootFolderOptions)
                .defaultValue("0")
                .required(true),
            string(FILE_ID)
                .label("File ID")
                .description("ID of the file to download.")
                .optionsLookupDependsOn(ID)
                .options((OptionsFunction<String>) BoxUtils::getFileIdOptions)
                .required(true))
        .output(outputSchema(fileEntry()))
        .perform(BoxDownloadFileAction::perform);

    private BoxDownloadFileAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Http.Response response = context
            .http(http -> http.get("/files/" + inputParameters.getRequiredString(FILE_ID) + "/content"))
            .configuration(Http.responseType(ResponseType.JSON))
            .execute();

        Http.Response fileResponse = context
            .http(http -> http.get(response.getFirstHeader("location")))
            .configuration(Http.responseType(Http.ResponseType.BINARY))
            .execute();

        return fileResponse.getBody();
    }
}
