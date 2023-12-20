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

import static com.bytechef.component.filesystem.constant.FilesystemConstants.MKDIR;
import static com.bytechef.component.filesystem.constant.FilesystemConstants.PATH;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.ParameterMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Ivica Cardic
 */
public class FilesystemMkdirAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(MKDIR)
        .title("Create")
        .description("Creates a directory.")
        .properties(
            string(PATH)
                .label("Path")
                .description("The path of a directory.")
                .required(true))
        .perform(FilesystemMkdirAction::perform);

    /**
     * Creates a directory by creating all nonexistent parent directories first.
     *
     * <p>
     * An exception is not thrown if the directory could not be created because it already exists.
     */
    protected static Object perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext context) throws IOException {

        return Files.createDirectories(Paths.get(inputParameters.getRequiredString(PATH)));
    }
}
