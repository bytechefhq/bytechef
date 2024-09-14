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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.ID;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.LIST_FOLDERS;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.NAME;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.PARENT_ID;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.VALUE;
import static com.bytechef.component.microsoft.one.drive.util.MicrosoftOneDriveUtils.getFolderId;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.one.drive.util.MicrosoftOneDriveUtils;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class MicrosoftOneDriveListFoldersAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(LIST_FOLDERS)
        .title("List Folders")
        .description("List folders in a OneDrive folder")
        .properties(
            string(PARENT_ID)
                .label("Parent folder")
                .description(
                    "Folder from which you want to list folders. If no folder is specified, the root folder will " +
                        "be used.")
                .options((ActionOptionsFunction<String>) MicrosoftOneDriveUtils::getFolderIdOptions)
                .required(false))
        .output(
            outputSchema(
                array()
                    .items(
                        object()
                            .properties(
                                string(ID),
                                string(NAME)))))
        .perform(MicrosoftOneDriveListFoldersAction::perform);

    private MicrosoftOneDriveListFoldersAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Map<String, ?> body = context
            .http(http -> http.get("/items/" + getFolderId(inputParameters.getString(PARENT_ID)) + "/children"))
            .queryParameters("$filter", "folder ne null")
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return body.get(VALUE);
    }
}
