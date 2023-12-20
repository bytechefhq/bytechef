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

package com.bytechef.component.filesystem.action;

import static com.bytechef.component.filesystem.constant.FilesystemConstants.FILENAME;
import static com.bytechef.component.filesystem.constant.FilesystemConstants.READ_FILE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.Parameters;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Ivica Cardic
 */
public class FilesystemReadFileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(READ_FILE)
        .title("Read from file")
        .properties(string(FILENAME)
            .label("Filename")
            .description("The path of the file to read.")
            .placeholder("/data/your_file.pdf")
            .required(true))
        .outputSchema(ComponentDSL.fileEntry())
        .perform(FilesystemReadFileAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context)
        throws IOException {

        String filename = inputParameters.getRequiredString(FILENAME);

        try (InputStream inputStream = new FileInputStream(filename)) {
            return context.file(file -> file.storeContent(filename, inputStream));
        }
    }
}
