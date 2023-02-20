
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
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.exception.ActionExecutionException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.bytechef.component.filesystem.constant.FilesystemConstants.CREATE_TEMP_DIR;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.definition.DefinitionDSL.display;

/**
 * @author Ivica Cardic
 */
public class FilesystemCreateTempDirAction {

    public static final ActionDefinition CREATE_TEMP_DIR_ACTION = action(CREATE_TEMP_DIR)
        .display(display("Create Temp Directory")
            .description("Creates a temporary directory oon the filesystem."))
        .perform(FilesystemCreateTempDirAction::performCreateTempDir);

    public static String performCreateTempDir(Context context, ExecutionParameters executionParameters) {
        try {
            Path path = Files.createTempDirectory("createTempDir_");

            File file = path.toFile();

            return file.getAbsolutePath();
        } catch (IOException ioException) {
            throw new ActionExecutionException(
                "Unable to create temporary directory " + executionParameters, ioException);
        }
    }
}
