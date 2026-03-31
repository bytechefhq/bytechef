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
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
public class DropboxMoveFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("moveFile")
        .title("Move File")
        .description("Move a file to a different location in the user's Dropbox.")
        .help("", "https://docs.bytechef.io/reference/components/dropbox_v1#move-file")
        .properties(
            string(FROM_PATH)
                .label("Source Path")
                .description("Path of the file in the user's Dropbox to be moved.")
                .exampleValue("/folder1/sourceFile.txt")
                .required(true),
            string(TO_PATH)
                .label("Destination Path")
                .description("Path in the user's Dropbox that is the destination.")
                .required(true),
            bool(AUTORENAME)
                .label("Auto Rename")
                .description(
                    "If there's a conflict, have the Dropbox server try to autorename the file to avoid the conflict.")
                .defaultValue(false)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("metadata")
                            .description("Metadata containing details about the moved file.")
                            .properties(
                                string("name")
                                    .description(
                                        "The name of the moved file, including its extension. This is the last " +
                                            "component of the path."),
                                string("path_lower")
                                    .description(
                                        "The full path to the moved file in lowercase, as stored in the user's Dropbox."),
                                string("path_display")
                                    .description(
                                        "The display-friendly version of the path to the moved file, preserving " +
                                            "original casing."),
                                string("id")
                                    .description("ID of the moved file.")))))
        .perform(DropboxMoveFileAction::perform);

    private DropboxMoveFileAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return DropboxUtils.move(inputParameters, context);
    }
}
