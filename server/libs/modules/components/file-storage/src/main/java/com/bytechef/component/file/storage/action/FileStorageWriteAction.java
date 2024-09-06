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

package com.bytechef.component.file.storage.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.outputSchema;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.file.storage.constant.FileStorageConstants.CONTENT;
import static com.bytechef.component.file.storage.constant.FileStorageConstants.FILENAME;
import static com.bytechef.component.file.storage.constant.FileStorageConstants.WRITE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;

/**
 * @author Ivica Cardic
 */
public class FileStorageWriteAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(WRITE)
        .title("Write to file")
        .description("Writes the data to the file.")
        .properties(
            string(CONTENT)
                .label("Content")
                .description("String to write to the file.")
                .required(true),
            string(FILENAME)
                .label("Filename")
                .description(
                    "Filename to set for data. By default, \"file.txt\" will be used.")
                .defaultValue("file.txt"))
        .output(outputSchema(fileEntry()))
        .perform(FileStorageWriteAction::perform);

    protected static FileEntry perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Object content = inputParameters.getRequired(CONTENT);
        String fileName = inputParameters.getString(FILENAME, "file.txt");

        return context.file(file -> file.storeContent(
            fileName, content instanceof String string ? string : content.toString()));
    }
}
