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

package com.bytechef.component.dropbox.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.dropbox.constant.DropboxConstants.AUTORENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.FILENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.FILE_ENTRY;
import static com.bytechef.component.dropbox.constant.DropboxConstants.MUTE;
import static com.bytechef.component.dropbox.constant.DropboxConstants.PATH;
import static com.bytechef.component.dropbox.constant.DropboxConstants.STRICT_CONFLICT;
import static com.bytechef.component.dropbox.util.DropboxUtils.uploadFile;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
public class DropboxUploadFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("uploadFile")
        .title("Upload File")
        .description("Create a new file up to a size of 150MB with the contents provided in the request.")
        .properties(
            fileEntry(FILE_ENTRY)
                .label("File")
                .description("The object property which contains a reference to the file to be written.")
                .required(true),
            string(PATH)
                .label("Destination Path")
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
                .label("Strict Conflict")
                .description(
                    "Be more strict about how each WriteMode detects conflict. For example, always return a " +
                        "conflict error when mode = WriteMode.update and the given \"rev\" doesn't match the " +
                        "existing file's \"rev\", even if the existing file has been deleted.")
                .defaultValue(false)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("name")
                            .description("Name of the file. The last component of the path (including extension)."),
                        string("path_lower")
                            .description("The lowercased full path in the user's Dropbox."),
                        string("path_display")
                            .description("The cased path to be used for display purposes only."),
                        string("id")
                            .description("ID of the folder."),
                        integer("size")
                            .description("The file size in bytes."),
                        bool("is_downloadable")
                            .description("If file can be downloaded directly."),
                        string("content_hash")
                            .description("A hash of the file content."))))
        .perform(DropboxUploadFileAction::perform);

    private DropboxUploadFileAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return uploadFile(inputParameters, context, inputParameters.getRequiredFileEntry(FILE_ENTRY));
    }
}
