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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.dropbox.constant.DropboxConstants.AUTORENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.FILENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.MUTE;
import static com.bytechef.component.dropbox.constant.DropboxConstants.PATH;
import static com.bytechef.component.dropbox.constant.DropboxConstants.STRICT_CONFLICT;
import static com.bytechef.component.dropbox.constant.DropboxConstants.TEXT;
import static com.bytechef.component.dropbox.util.DropboxUtils.uploadFile;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
public class DropboxCreateNewTextFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createTextFile")
        .title("Create a new paper file")
        .description("Create a new .paper file on which you can write at a given path")
        .properties(
            string(PATH)
                .label("Path")
                .description("The path of the new paper file. Root is /.")
                .required(true),
            string(FILENAME)
                .label("Filename")
                .description("Name of the paper file")
                .required(true),
            string(TEXT)
                .label("Text")
                .description("The text to write into the file.")
                .controlType(ControlType.TEXT_AREA)
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
        .output(
            outputSchema(
                object()
                    .properties(
                        string("url"),
                        string("resultPath"),
                        string("fileId"),
                        integer("paperRevision"))))
        .perform(DropboxCreateNewTextFileAction::perform);

    private DropboxCreateNewTextFileAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        FileEntry fileEntry = actionContext.file(
            file -> file.storeContent(
                inputParameters.getRequiredString(FILENAME) + ".paper",
                inputParameters.getRequiredString(TEXT)));

        return uploadFile(inputParameters, actionContext, fileEntry);
    }
}
