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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.filesystem.constant.FilesystemConstants.FILENAME;
import static com.bytechef.component.filesystem.constant.FilesystemConstants.FILE_ENTRY;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class FilesystemWriteFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("writeFile")
        .title("Write to File")
        .properties(
            fileEntry(FILE_ENTRY)
                .label("File Entry")
                .description("File entry object to be written.")
                .required(true),
            string(FILENAME)
                .label("File path")
                .description("The path to which the file should be written.")
                .placeholder("/data/your_file.pdf")
                .required(true))
        .output(
            outputSchema(object().properties(integer("bytes").description("Number of bytes written."))),
            sampleOutput(Map.of("bytes", 1024)))
        .perform(FilesystemWriteFileAction::perform);

    private FilesystemWriteFileAction() {
    }

    protected static Map<String, ?> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) throws IOException {

        String fileName = inputParameters.getRequiredString(FILENAME);

        try (InputStream inputStream = context.file(
            file -> file.getInputStream(inputParameters.getRequiredFileEntry(FILE_ENTRY)))) {

            return Map.of("bytes", Files.copy(inputStream, Path.of(fileName), StandardCopyOption.REPLACE_EXISTING));
        }
    }
}
