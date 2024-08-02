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

package com.bytechef.component.dropbox.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.dropbox.constant.DropboxConstants.AUTORENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.FILENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.FILE_ENTRY;
import static com.bytechef.component.dropbox.constant.DropboxConstants.MUTE;
import static com.bytechef.component.dropbox.constant.DropboxConstants.PATH;
import static com.bytechef.component.dropbox.constant.DropboxConstants.STRICT_CONFLICT;
import static com.bytechef.component.dropbox.constant.DropboxConstants.UPLOAD_FILE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
public class DropboxUploadFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(UPLOAD_FILE)
        .title("Upload file")
        .description("Create a new file up to a size of 150MB with the contents provided in the request.")
        .properties(
            fileEntry(FILE_ENTRY)
                .label("File")
                .description("The object property which contains a reference to the file to be written.")
                .required(true),
            string(PATH)
                .label("Destination path")
                .description("The path to which the file should be written.")
                .placeholder("/directory/")
                .required(true),
            string(FILENAME)
                .label("Filename")
                .description("Name of the file. Needs to have the appropriate extension.")
                .placeholder("your_file.pdf")
                .required(true),
            bool(AUTORENAME)
                .label("Auto Rename")
                .description(
                    "If there's a conflict, as determined by mode, have the Dropbox server try to autorename the " +
                        "file to avoid conflict.")
                .defaultValue(false)
                .required(false),
            bool(MUTE)
                .label("Mute")
                .description(
                    "Normally, users are made aware of any file modifications in their Dropbox account via " +
                        "notifications in the client software. If true, this tells the clients that this " +
                        "modification shouldn't result in a user notification.")
                .defaultValue(false)
                .required(false),
            bool(STRICT_CONFLICT)
                .label("Strict conflict")
                .description(
                    "Be more strict about how each WriteMode detects conflict. For example, always return a " +
                        "conflict error when mode = WriteMode.update and the given \"rev\" doesn't match the " +
                        "existing file's \"rev\", even if the existing file has been deleted.")
                .defaultValue(false)
                .required(false))
        .outputSchema(
            object()
                .properties(
                    string("id"),
                    date("clientModified"),
                    date("serverModified"),
                    string("rev"),
                    integer("size"),
                    object("symlinkInfo")
                        .properties(
                            string("target")),
                    object("sharingInfo")
                        .properties(
                            string("parentSharedFolderId"),
                            string("modifiedBy")),
                    bool("isDownloadable"),
                    object("exportInfo")
                        .properties(
                            string("exportAs"),
                            array("exportOptions")
                                .items(string())),
                    array("propertyGroups")
                        .items(
                            object()
                                .properties(
                                    string("templateId"),
                                    array("fields")
                                        .items(
                                            object()
                                                .properties(
                                                    string("name"),
                                                    string("value"))))),
                    bool("hasExplicitSharedMembers"),
                    string("contentHash"),
                    object("fileLockInfo")
                        .properties(
                            bool("isLockholder"),
                            string("lockholderName"),
                            string("lockholderAccountId"),
                            date("created"))))
        .perform(DropboxUploadFileAction::perform);

    private DropboxUploadFileAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        // TODO

        String destination = inputParameters.getRequiredString(PATH);

        String headerJson = actionContext.json(json -> {
            Map<String, Object> ime = Map.of(
                AUTORENAME, inputParameters.getBoolean(AUTORENAME),
                "mode", "add",
                MUTE, inputParameters.getBoolean(MUTE),
                PATH,
                (destination.endsWith("/") ? destination : destination + "/")
                    + inputParameters.getRequiredString(FILENAME),
                STRICT_CONFLICT, inputParameters.getBoolean(STRICT_CONFLICT));
            return json.write(ime);
        });

        String fileContent = actionContext.file(file -> Base64.getEncoder()
            .encodeToString(file.readAllBytes(inputParameters.getRequiredFileEntry(FILE_ENTRY))));

        return actionContext.http(http -> http.post("https://content.dropboxapi.com/2/files/upload"))
            .headers(
                Map.of(
                    "Dropbox-API-Arg", List.of(headerJson),
                    "Content-Type", List.of("application/octet-stream")))
            .body(Http.Body.of(fileContent))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
