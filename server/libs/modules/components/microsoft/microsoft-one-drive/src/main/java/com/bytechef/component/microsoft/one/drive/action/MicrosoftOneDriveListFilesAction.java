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
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.BASE_URL;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.ID;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.LIST_FILES;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.PARENT_ID_PROPERTY;
import static com.bytechef.component.microsoft.one.drive.util.MicrosoftOneDriveUtils.getFolderId;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class MicrosoftOneDriveListFilesAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(LIST_FILES)
        .title("List Files")
        .description("List files in a OneDrive folder")
        .properties(PARENT_ID_PROPERTY)
        .outputSchema(
            array()
                .items(
                    object()
                        .properties(
                            string(ID),
                            string("name"))))
        .perform(MicrosoftOneDriveListFilesAction::perform);

    private MicrosoftOneDriveListFilesAction() {
    }

    public static List<Map<?, ?>> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Map<String, ?> body = context
            .http(http -> http.get(BASE_URL + "/items/" + getFolderId(inputParameters) + "/children"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Map<?, ?>> files = new ArrayList<>();

        if (body.get("value") instanceof List<?> list) {
            for (Object item : list) {
                if ((item instanceof Map<?, ?> map) && (map.containsKey("file"))) {
                    files.add(map);
                }
            }
        }

        return files;
    }
}
