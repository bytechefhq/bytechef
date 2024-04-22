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

package com.bytechef.component.microsoft.share.point.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.BASE_URL;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.CREATE_FOLDER;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.FOLDER;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.NAME;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.PARENT_FOLDER_PROPERTY;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.SITE_ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.SITE_ID_PROPERTY;
import static com.bytechef.component.microsoft.share.point.util.MicrosoftSharePointUtils.getFolderId;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class MicrosoftSharePointCreateFolderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_FOLDER)
        .title("Create folder")
        .description("Creates a new folder at path you specify.")
        .properties(
            SITE_ID_PROPERTY,
            PARENT_FOLDER_PROPERTY,
            string(NAME)
                .label("Folder name")
                .required(true))
        .outputSchema(
            object()
                .properties(
                    string(ID),
                    string(NAME)))
        .perform(MicrosoftSharePointCreateFolderAction::perform);

    private MicrosoftSharePointCreateFolderAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .http(http -> http.post(
                BASE_URL + "/" + inputParameters.getRequiredString(SITE_ID) + "/drive/items/" +
                    getFolderId(inputParameters) + "/children"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(
                Http.Body.of(
                    NAME, inputParameters.getRequiredString(NAME),
                    FOLDER, Map.of()))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
