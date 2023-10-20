
/*
 * Copyright 2021 <your company/name>.
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

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.exception.ComponentExecutionException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import static com.bytechef.component.filesystem.constant.FilesystemConstants.FILENAME;
import static com.bytechef.component.filesystem.constant.FilesystemConstants.FILE_ENTRY;
import static com.bytechef.component.filesystem.constant.FilesystemConstants.WRITE_FILE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.definition.DefinitionDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class FilesystemWriteFileAction {

    public static final ActionDefinition ACTION_DEFINITION = action(WRITE_FILE)
        .display(display("Write to file"))
        .properties(
            fileEntry(FILE_ENTRY)
                .label("File")
                .description(
                    "The object property which contains a reference to the file to be written.")
                .required(true),
            string(FILENAME)
                .label("Filename")
                .description("The path to which the file should be written.")
                .placeholder("/data/your_file.pdf")
                .required(true))
        .outputSchema(object().properties(integer("bytes")))
        .execute(FilesystemWriteFileAction::executeWriteFile);

    public static Map<String, Long> executeWriteFile(Context context, InputParameters inputParameters) {
        String fileName = inputParameters.getRequiredString(FILENAME);

        try (
            InputStream inputStream = context
                .getFileStream(inputParameters.get(FILE_ENTRY, Context.FileEntry.class))) {
            return Map.of("bytes", Files.copy(inputStream, Path.of(fileName), StandardCopyOption.REPLACE_EXISTING));
        } catch (IOException ioException) {
            throw new ComponentExecutionException("Unable to create file " + inputParameters, ioException);
        }
    }
}
