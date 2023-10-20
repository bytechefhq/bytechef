
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
import com.bytechef.hermes.component.Parameters;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.exception.ActionExecutionException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.bytechef.component.filesystem.constant.FilesystemConstants.FILENAME;
import static com.bytechef.component.filesystem.constant.FilesystemConstants.READ_FILE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.definition.DefinitionDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class FilesystemReadFileAction {

    public static final ActionDefinition ACTION_DEFINITION = action(READ_FILE)
        .display(display("Read from file"))
        .properties(string(FILENAME)
            .label("Filename")
            .description("The path of the file to read.")
            .placeholder("/data/your_file.pdf")
            .required(true))
        .outputSchema(ComponentDSL.fileEntry())
        .perform(FilesystemReadFileAction::performReadFile);

    public static FileEntry performReadFile(Context context, Parameters parameters) {
        String filename = parameters.getRequiredString(FILENAME);

        try (InputStream inputStream = new FileInputStream(filename)) {
            return context.storeFileContent(filename, inputStream);
        } catch (IOException ioException) {
            throw new ActionExecutionException("Unable to open file " + parameters, ioException);
        }
    }
}
