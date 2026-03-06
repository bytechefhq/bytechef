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

package com.bytechef.component.microsoft.one.drive.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.ID;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.NAME;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.PARENT_ID;
import static com.bytechef.component.microsoft.one.drive.util.MicrosoftOneDriveUtils.getFolderId;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.one.drive.util.MicrosoftOneDriveUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftOneDriveCopyFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("copyFile")
        .title("Copy File")
        .description("Copy a selected file to a different location within Microsoft OneDrive.")
        .help("", "https://docs.bytechef.io/reference/components/microsoft-one-drive_v1#copy-file")
        .properties(
            string(ID)
                .label("File ID")
                .description("ID of the file to copy.")
                .options((OptionsFunction<String>) MicrosoftOneDriveUtils::getFileIdOptions)
                .required(true),
            string(NAME)
                .label("New File Name")
                .description(
                    "The new name for the copy. If this isn't provided, the same name will be used as the original.")
                .required(false),
            string(PARENT_ID)
                .label("Destination Folder ID")
                .description(
                    "The ID of the folder where the copied file will be stored. If not specified, the root folder " +
                        "will be used.")
                .options((OptionsFunction<String>) MicrosoftOneDriveUtils::getFolderIdOptions)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("@odata.context"),
                        string(ID)
                            .description("ID of the copied file."),
                        string("createdDateTime")
                            .description("The date and time when the copied file was created."),
                        string("lastActionDateTime"),
                        integer("percentageComplete")
                            .description("Percentage completion of the copy operation."),
                        string("resourceId"),
                        string("resourceLocation"),
                        string("status")
                            .description("Status of the copy operation."))))
        .perform(MicrosoftOneDriveCopyFileAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftOneDriveCopyFileAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Http.Response response = context
            .http(http -> http.post("/me/drive/items/%s/copy".formatted(inputParameters.getRequiredString(ID))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(
                Http.Body.of(
                    NAME, inputParameters.getString(NAME),
                    "parentReference", Map.of(ID, getFolderId(inputParameters.getString(PARENT_ID)))))
            .execute();

        if (response.getStatusCode() == 202) {
            String location = response.getFirstHeader("location");
            String status;

            do {
                Http.Response statusResponse = context
                    .http(http -> http.get(location))
                    .configuration(Http.responseType(Http.ResponseType.JSON))
                    .execute();

                Map<String, Object> body = statusResponse.getBody(new TypeReference<>() {});

                status = (String) body.get("status");

                if (status.equals("completed")) {
                    return body;
                } else if (status.equals("failed")) {
                    throw MicrosoftUtils.processErrorResponse(
                        statusResponse.getStatusCode(), body, statusResponse.getHeaders(), context);
                }
            } while (status.equals("inProgress"));
        }

        return null;
    }
}
