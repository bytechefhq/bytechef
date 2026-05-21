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

package com.bytechef.component.microsoft.share.point.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.SITE_ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.SITE_ID_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.share.point.util.MicrosoftSharePointUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;

/**
 * @author Nikolina Spehar
 */
public class MicrosoftSharePointDeleteFileOrFolderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteFileOrFolder")
        .title("Delete File or Folder")
        .description("Deletes specified file or folder ID")
        .properties(
            SITE_ID_PROPERTY,
            string(ID)
                .label("File or Folder ID")
                .description("The ID of the file or folder to retrieve.")
                .options((OptionsFunction<String>) MicrosoftSharePointUtils::getFolderAndFileIdOptions)
                .optionsLookupDependsOn(SITE_ID)
                .required(true))
        .perform(MicrosoftSharePointDeleteFileOrFolderAction::perform)
        .help(
            "",
            "https://docs.bytechef.io/reference/components/microsoft-share-point_v1#delete-file-or-folder")
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftSharePointDeleteFileOrFolderAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        context.http(http -> http.delete(
            "/sites/%s/drive/items/%s".formatted(
                inputParameters.getRequiredString(SITE_ID), inputParameters.getRequiredString(ID))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        return null;
    }
}
