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
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.FILE;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.NAME;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.PARENT_FOLDER;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.SITE_ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.SITE_ID_PROPERTY;
import static com.bytechef.component.microsoft.share.point.util.MicrosoftSharePointUtils.getFolderId;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.share.point.util.MicrosoftSharePointUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;

/**
 * @author Monika Domiter
 */
public class MicrosoftSharePointUploadFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("uploadFile")
        .title("Upload File")
        .description("Upload file to Microsoft SharePoint folder.")
        .properties(
            SITE_ID_PROPERTY,
            string(PARENT_FOLDER)
                .label("Parent Folder ID")
                .description("If no folder is selected, file will be uploaded to root folder")
                .optionsLookupDependsOn(SITE_ID)
                .options((ActionOptionsFunction<String>) MicrosoftSharePointUtils::getFolderIdOptions)
                .required(false),
            fileEntry(FILE)
                .label("File Entry")
                .description("File to upload.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        dateTime("createdDateTime")
                            .description("The date and time when the file was created."),
                        string("eTag")
                            .description("eTag for the entire item (metadata + content)."),
                        string(ID)
                            .description("ID of the file."),
                        dateTime("lastModifiedDateTime")
                            .description("The date and time when the file was last modified."),
                        string(NAME)
                            .description("Name of the file."),
                        integer("size")
                            .description("Size of the file in bytes."),
                        string("webUrl")
                            .description("URL to access the file in a web browser."),
                        object("createdBy")
                            .properties(
                                object("user")
                                    .properties(
                                        string("email")
                                            .description("Email of the user who created the file."),
                                        string(ID)
                                            .description("ID of the user who created the file."),
                                        string("displayName")
                                            .description("Display name of the user who created the file."))),
                        object("lastModifiedBy")
                            .properties(
                                object("user")
                                    .properties(
                                        string("email")
                                            .description("Email of the user who last modified the file."),
                                        string(ID)
                                            .description("ID of the user who last modified file."),
                                        string("displayName")
                                            .description("Display name of the user who last modified the file."))),
                        object(FILE)
                            .properties(
                                object("hashes")
                                    .description("Hashes of the file's binary content")
                                    .properties(
                                        string("quickXorHash")
                                            .description(
                                                "A proprietary hash of the file that can be used to determine if the " +
                                                    "contents of the file change.")),
                                string("mimeType")
                                    .description("The MIME type for the file..")))))
        .perform(MicrosoftSharePointUploadFileAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftSharePointUploadFileAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        FileEntry fileEntry = inputParameters.getRequiredFileEntry(FILE);

        return context
            .http(http -> http.put(
                "/sites/%s/drive/items/%s:/%s:/content".formatted(
                    inputParameters.getRequiredString(SITE_ID), getFolderId(inputParameters), fileEntry.getName())))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(fileEntry))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
