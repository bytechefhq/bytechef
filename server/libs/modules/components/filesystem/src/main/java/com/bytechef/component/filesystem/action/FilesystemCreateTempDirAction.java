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

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Ivica Cardic
 */
public class FilesystemCreateTempDirAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createTempDir")
        .title("Create Temp Directory")
        .description(
            "Creates a file in the temporary directory on the filesystem. Returns the created directory's full path.")
        .output(
            outputSchema(string().description("The full path of the created directory.")),
            sampleOutput("/sample_tmp_dir"))
        .perform(FilesystemCreateTempDirAction::perform);

    private FilesystemCreateTempDirAction() {
    }

    protected static String perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) throws IOException {

        Path path = Files.createTempDirectory("createTempDir_");

        File file = path.toFile();

        return file.getAbsolutePath();

    }
}
