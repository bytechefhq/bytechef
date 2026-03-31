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
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.dropbox.constant.DropboxConstants.AUTORENAME;
import static com.bytechef.component.dropbox.constant.DropboxConstants.FROM_PATH;
import static com.bytechef.component.dropbox.constant.DropboxConstants.TO_PATH;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.dropbox.util.DropboxUtils;

/**
 * @author Monika Kušter
 */
public class DropboxCopyFolderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("copyFolder")
        .title("Copy Folder")
        .description(
            "Copy folder to a different location in the user's Dropbox. All content of the folder will be copied.")
        .help("", "https://docs.bytechef.io/reference/components/dropbox_v1#copy-folder")
        .properties(
            string(FROM_PATH)
                .label("From Path")
                .description("The source path of the folder.")
                .exampleValue("/folder1/sourceFolder")
                .required(true),
            string(TO_PATH)
                .label("Destination Path")
                .description("The destination path for the copied folder.")
                .exampleValue("/folder2/destinationFolder")
                .required(true),
            bool(AUTORENAME)
                .label("Auto Rename")
                .description(
                    "If there's a conflict, have the Dropbox server try to autorename the folder to avoid the conflict.")
                .defaultValue(false)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("metadata")
                            .description("Metadata containing details about the copied folder.")
                            .properties(
                                string("name")
                                    .description("The name of the copied folder."),
                                string("path_lower")
                                    .description(
                                        "The full path to the copied folder in lowercase, as stored in the user's " +
                                            "Dropbox."),
                                string("path_display")
                                    .description(
                                        "The display-friendly version of the path to the folder, preserving " +
                                            "original casing."),
                                string("id")
                                    .description("ID of the copied folder.")))))
        .perform(DropboxCopyFolderAction::perform);

    private DropboxCopyFolderAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return DropboxUtils.copy(inputParameters, context);
    }
}
