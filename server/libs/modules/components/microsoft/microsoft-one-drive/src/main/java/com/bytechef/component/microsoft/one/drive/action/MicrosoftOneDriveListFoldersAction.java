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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.FOLDER_OUTPUT_PROPERTY;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.PARENT_ID;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.VALUE;
import static com.bytechef.component.microsoft.one.drive.util.MicrosoftOneDriveUtils.getFolderId;
import static com.bytechef.microsoft.commons.MicrosoftUtils.ODATA_NEXT_LINK;
import static com.bytechef.microsoft.commons.MicrosoftUtils.getItemsFromNextPage;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.one.drive.util.MicrosoftOneDriveUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class MicrosoftOneDriveListFoldersAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listFolders")
        .title("List Folders")
        .description("List folders in a OneDrive folder.")
        .properties(
            string(PARENT_ID)
                .label("Parent Folder ID")
                .description(
                    "ID of the Folder from which you want to list folders. If no folder is specified, the root " +
                        "folder will be used.")
                .options((OptionsFunction<String>) MicrosoftOneDriveUtils::getFolderIdOptions)
                .required(false))
        .output(
            outputSchema(
                array()
                    .items(FOLDER_OUTPUT_PROPERTY)))
        .perform(MicrosoftOneDriveListFoldersAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftOneDriveListFoldersAction() {
    }

    public static List<Map<?, ?>>
        perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, ?> body = context
            .http(http -> http.get(
                "/me/drive/items/%s/children".formatted(getFolderId(inputParameters.getString(PARENT_ID)))))
            .queryParameter("$filter", "folder ne null")
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Map<?, ?>> folders = new ArrayList<>();

        if (body.get(VALUE) instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    folders.add(map);
                }
            }
        }

        folders.addAll(getItemsFromNextPage((String) body.get(ODATA_NEXT_LINK), context));

        return folders;
    }
}
