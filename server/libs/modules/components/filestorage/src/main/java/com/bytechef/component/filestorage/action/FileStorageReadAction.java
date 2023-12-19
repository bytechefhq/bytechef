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

package com.bytechef.component.filestorage.action;

import static com.bytechef.component.filestorage.constant.FileStorageConstants.FILE_ENTRY;
import static com.bytechef.component.filestorage.constant.FileStorageConstants.READ;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.ParameterMap;

/**
 * @author Ivica Cardic
 */
public class FileStorageReadAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(READ)
        .title("Read from file")
        .description("Reads data from the file.")
        .properties(fileEntry(FILE_ENTRY)
            .label("File")
            .description(
                "The object property which contains a reference to the file to read from.")
            .required(true))
        .outputSchema(string())
        .perform(FileStorageReadAction::perform);

    protected static Object perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext context) {

        return context.file(file -> file.readToString(inputParameters.getRequiredFileEntry(FILE_ENTRY)));
    }
}
