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

package com.integri.atlas.task.handler.file;

import static com.integri.atlas.task.definition.dsl.DSL.FILE_ENTRY_PROPERTY;
import static com.integri.atlas.task.definition.dsl.DSL.OPERATION;
import static com.integri.atlas.task.definition.dsl.DSL.STRING_PROPERTY;
import static com.integri.atlas.task.handler.file.FileTaskConstants.Operation;
import static com.integri.atlas.task.handler.file.FileTaskConstants.PROPERTY_CONTENT;
import static com.integri.atlas.task.handler.file.FileTaskConstants.PROPERTY_FILE_ENTRY;
import static com.integri.atlas.task.handler.file.FileTaskConstants.PROPERTY_FILE_NAME;
import static com.integri.atlas.task.handler.file.FileTaskConstants.TASK_FILE;

import com.integri.atlas.task.definition.TaskDefinitionHandler;
import com.integri.atlas.task.definition.dsl.DSL;
import com.integri.atlas.task.definition.dsl.TaskDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class FileTaskDefinitionHandler implements TaskDefinitionHandler {

    private static final TaskDefinition TASK_DEFINITION = DSL
        .create(TASK_FILE)
        .displayName("File")
        .description("Reads and writes data from a file")
        .operations(
            OPERATION(Operation.READ.name())
                .displayName("Read from file")
                .description("Reads data from a csv file.")
                .inputs(
                    FILE_ENTRY_PROPERTY(PROPERTY_FILE_ENTRY)
                        .displayName("File")
                        .description("The object property which contains a reference to the file to read from.")
                        .required(true)
                )
                .outputs(STRING_PROPERTY()),
            OPERATION(Operation.WRITE.name())
                .displayName("Write to file")
                .description("Writes the data to a csv file.")
                .inputs(
                    STRING_PROPERTY(PROPERTY_CONTENT)
                        .displayName("Content")
                        .description("String to write to the file.")
                        .required(true),
                    STRING_PROPERTY(PROPERTY_FILE_NAME)
                        .displayName("File Name")
                        .description("File name to set for binary data. By default, \"file.txt\" will be used.")
                        .defaultValue("file.txt")
                )
                .outputs(FILE_ENTRY_PROPERTY())
        );

    @Override
    public TaskDefinition getTaskDefinition() {
        return TASK_DEFINITION;
    }
}
