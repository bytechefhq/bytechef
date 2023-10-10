
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

import com.bytechef.hermes.component.definition.ActionDefinition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.ParameterMap;
import com.bytechef.hermes.component.exception.ComponentExecutionException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.bytechef.component.filesystem.constant.FilesystemConstants.CREATE_TEMP_DIR;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;

/**
 * @author Ivica Cardic
 */
public class FilesystemCreateTempDirAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_TEMP_DIR)
        .title("Create Temp Directory")
        .description("Creates a temporary directory on the filesystem.")
        .perform(FilesystemCreateTempDirAction::perform);

    protected static Object perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext context) {

        try {
            Path path = Files.createTempDirectory("createTempDir_");

            File file = path.toFile();

            return file.getAbsolutePath();
        } catch (IOException ioException) {
            throw new ComponentExecutionException(
                "Unable to create temporary directory " + inputParameters, ioException);
        }
    }
}
