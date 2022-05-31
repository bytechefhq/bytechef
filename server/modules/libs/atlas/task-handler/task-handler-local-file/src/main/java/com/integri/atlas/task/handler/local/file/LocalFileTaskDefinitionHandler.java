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

package com.integri.atlas.task.handler.local.file;

import static com.integri.atlas.task.definition.model.DSL.FILE_ENTRY_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.INTEGER_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.OBJECT_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.OPERATION;
import static com.integri.atlas.task.definition.model.DSL.STRING_PROPERTY;
import static com.integri.atlas.task.handler.local.file.LocalFileTaskConstants.PROPERTY_FILE_ENTRY;
import static com.integri.atlas.task.handler.local.file.LocalFileTaskConstants.PROPERTY_FILE_NAME;
import static com.integri.atlas.task.handler.local.file.LocalFileTaskConstants.TASK_LOCAL_FILE;

import com.integri.atlas.task.definition.AbstractTaskDefinitionHandler;
import com.integri.atlas.task.definition.model.DSL;
import com.integri.atlas.task.definition.model.TaskDefinition;
import org.springframework.stereotype.Component;

@Component
public class LocalFileTaskDefinitionHandler extends AbstractTaskDefinitionHandler {

    private static final TaskDefinition TASK_DEFINITION = DSL
        .createTaskDefinition(TASK_LOCAL_FILE)
        .displayName("Local File")
        .description("Reads or writes a binary file from/to disk")
        .operations(
            OPERATION("read")
                .displayName("Read to file")
                .inputs(
                    STRING_PROPERTY(PROPERTY_FILE_NAME)
                        .displayName("File Name")
                        .description("The path of the file to read.")
                        .placeholder("/data/your_file.pdf")
                        .required(true)
                )
                .outputs(FILE_ENTRY_PROPERTY()),
            OPERATION("write")
                .displayName("Write from file")
                .inputs(
                    FILE_ENTRY_PROPERTY(PROPERTY_FILE_ENTRY)
                        .displayName("File")
                        .description("The object property which contains a reference to the file to be written.")
                        .required(true),
                    STRING_PROPERTY(PROPERTY_FILE_NAME)
                        .displayName("File Name")
                        .description("The path to which the file should be written.")
                        .placeholder("/data/your_file.pdf")
                        .required(true)
                )
                .outputs(OBJECT_PROPERTY().properties(INTEGER_PROPERTY("bytes")))
        );

    @Override
    public TaskDefinition getTaskDefinition() {
        return TASK_DEFINITION;
    }
}
