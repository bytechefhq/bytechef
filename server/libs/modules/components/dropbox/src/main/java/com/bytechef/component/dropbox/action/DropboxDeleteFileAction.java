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
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.dropbox.constant.DropboxConstants.PATH;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.dropbox.util.DropboxUtils;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
public class DropboxDeleteFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteFile")
        .title("Delete File")
        .description("Delete the file at a given path.")
        .help("", "https://docs.bytechef.io/reference/components/dropbox_v1#delete-file")
        .properties(
            string(PATH)
                .label("Path")
                .description("Path of the file to be deleted.")
                .exampleValue("/folder1/sourceFile.txt")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("metadata")
                            .description("Metadata containing details about the deleted file.")
                            .properties(
                                string("name")
                                    .description(
                                        "The name of the deleted file, including its extension. This is the last " +
                                            "component of the path."),
                                string("path_lower")
                                    .description(
                                        "The full path to the deleted file in lowercase, as stored in the user's " +
                                            "Dropbox."),
                                string("path_display")
                                    .description(
                                        "The display-friendly version of the path to the deleted file, preserving " +
                                            "original casing."),
                                string("id")
                                    .description("ID of the deleted file.")))))
        .perform(DropboxDeleteFileAction::perform);

    private DropboxDeleteFileAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return DropboxUtils.delete(inputParameters, context);
    }
}
