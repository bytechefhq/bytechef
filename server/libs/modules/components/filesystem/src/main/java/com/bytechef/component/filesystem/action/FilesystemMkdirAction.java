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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.filesystem.constant.FilesystemConstants.PATH;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Ivica Cardic
 */
public class FilesystemMkdirAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("mkdir")
        .title("Create")
        .description("Creates a directory.")
        .properties(
            string(PATH)
                .label("Path")
                .description("The path of a directory.")
                .required(true))
        .output(
            outputSchema(string().description("The full path of the created directory.")),
            sampleOutput("/sample_data"))
        .perform(FilesystemMkdirAction::perform);

    private FilesystemMkdirAction() {
    }

    /**
     * Creates a directory by creating all nonexistent parent directories first.
     *
     * <p>
     * An exception is not thrown if the directory could not be created because it already exists.
     */
    protected static String perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) throws IOException {

        return String.valueOf(Files.createDirectories(Paths.get(inputParameters.getRequiredString(PATH))));
    }
}
