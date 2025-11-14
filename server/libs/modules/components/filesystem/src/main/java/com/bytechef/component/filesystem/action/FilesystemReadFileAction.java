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

package com.bytechef.component.filesystem.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.filesystem.constant.FilesystemConstants.FILENAME;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Ivica Cardic
 */
public class FilesystemReadFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("readFile")
        .title("Read File")
        .description("Reads all data from a specified file path and outputs it in file entry format.")
        .properties(
            string(FILENAME)
                .label("File path")
                .description("The path of the file to read.")
                .placeholder("/data/your_file.pdf")
                .required(true))
        .output(outputSchema(fileEntry()))
        .perform(FilesystemReadFileAction::perform);

    private FilesystemReadFileAction() {
    }

    protected static FileEntry perform(
        Parameters inputParameters, Parameters connectionParameters, Context context)
        throws IOException {

        String filename = inputParameters.getRequiredString(FILENAME);

        try (InputStream inputStream = new FileInputStream(filename)) {
            return context.file(file -> file.storeContent(filename, inputStream));
        }
    }
}
