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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.DISPLAY_NAME;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.NAME;
import static com.bytechef.component.microsoft.share.point.util.MicrosoftSharePointUtils.getSiteId;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.share.point.util.MicrosoftSharePointUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;

/**
 * @author Nikolina Spehar
 */
public class MicrosoftSharePointGetFileOrFolderByIdAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getFileOrFolderById")
        .title("Get File or Folder by ID")
        .description("Retrieves information about file or folder by its ID.")
        .properties(
            SITE_ID_PROPERTY,
            string(ID)
                .label("File or Folder ID")
                .description("The ID of the file or folder to retrieve.")
                .options((OptionsFunction<String>) MicrosoftSharePointUtils::getFolderAndFileIdOptions)
                .optionsLookupDependsOn(SITE_ID)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        dateTime("createdDateTime")
                            .description("The date and time when the folder was created."),
                        string("eTag"),
                        string(ID)
                            .description("ID of the folder."),
                        dateTime("lastModifiedDateTime")
                            .description("The date and time when the folder was last modified."),
                        string(NAME)
                            .description("Name of the folder."),
                        integer("size")
                            .description("Size of the folder in bytes."),
                        string("webUrl")
                            .description("URL to access the folder in a web browser."),
                        string("cTag"),
                        object("commentSettings")
                            .properties(
                                object("commentingDisabled")
                                    .description("Indicates whether commenting is disabled for the folder.")
                                    .properties(
                                        bool("isDisabled")
                                            .description("Value indicating whether commenting is disabled."))),
                        object("createdBy")
                            .properties(
                                object("user")
                                    .properties(
                                        string(ID)
                                            .description("ID of the user who created the folder."),
                                        string(DISPLAY_NAME)
                                            .description("Display name of the user who created the folder."))),
                        object("lastModifiedBy")
                            .properties(
                                object("user")
                                    .properties(
                                        string(ID)
                                            .description("ID of the user who last modified the folder."),
                                        string(DISPLAY_NAME)
                                            .description("Display name of the user who last modified the folder."))),
                        object("folder")
                            .properties(
                                integer("childCount")
                                    .description("Number of items contained in the folder.")),
                        object("shared")
                            .properties(
                                string("scope")
                                    .description("The scope of the shared item.")))))
        .perform(MicrosoftSharePointGetFileOrFolderByIdAction::perform)
        .help(
            "",
            "https://docs.bytechef.io/reference/components/microsoft-share-point_v1#get-file-or-folder-by-id")
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftSharePointGetFileOrFolderByIdAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return context.http(
                http -> http.get("/sites/%s/drive/items/%s".formatted(
                    inputParameters.getRequiredString(SITE_ID), inputParameters.getRequiredString(ID))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
