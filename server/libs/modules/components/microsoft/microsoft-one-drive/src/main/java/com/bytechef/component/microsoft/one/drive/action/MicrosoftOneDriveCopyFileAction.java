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
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.microsoft.one.drive.util.MicrosoftOneDriveUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftOneDriveCopyFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("copyFile")
        .title("Copy File")
        .description("Copy a selected file to a different location within Microsoft OneDrive.")
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
        .perform(MicrosoftOneDriveCopyFileAction::perform);

    private MicrosoftOneDriveCopyFileAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Http.Response response = context
            .http(http -> http.post(
                "/me/drive/items/%s/copy".formatted(inputParameters.getRequiredString(ID))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(
                NAME, inputParameters.getString(NAME),
                "parentReference", Map.of(ID, getFolderId(inputParameters.getString(PARENT_ID)))))
            .execute();

        int statusCode = response.getStatusCode();

        if (statusCode == 202) {
            String location = response.getFirstHeader("location");

            Http.Response response1 = context
                .http(http -> http.get(location))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute();

            Map<String, Object> body = response1.getBody(new TypeReference<>() {});

            String status = (String) body.get("status");

            if (status.equals("completed")) {
                return null;
            } else {
                if (body.get("error") instanceof Map<?, ?> map) {
                    throw new ProviderException(response1.getStatusCode(), (String) map.get("message"));
                } else {
                    throw new ProviderException("Failed to copy file. Status code: " + response1.getStatusCode());
                }
            }
        } else {
            Map<String, Map<String, Object>> body = response.getBody(new TypeReference<>() {});

            Map<String, Object> error = body.get("error");

            throw new ProviderException(statusCode, (String) error.get("message"));
        }
    }
}
