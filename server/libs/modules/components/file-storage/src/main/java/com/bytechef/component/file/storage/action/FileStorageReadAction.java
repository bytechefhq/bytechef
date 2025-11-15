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

package com.bytechef.component.file.storage.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.file.storage.constant.FileStorageConstants.FILE_ENTRY;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

/**
 * @author Ivica Cardic
 */
public class FileStorageReadAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("read")
        .title("Read from File as String")
        .description("Reads data from the file as string.")
        .properties(
            fileEntry(FILE_ENTRY)
                .label("File Entry")
                .description("The file object which contains content of the file to read from.")
                .required(true))
        .output(outputSchema(string().description("File content.")), sampleOutput("Sample content"))
        .perform(FileStorageReadAction::perform);

    protected static String perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.file(file -> file.readToString(inputParameters.getRequiredFileEntry(FILE_ENTRY)));
    }
}
