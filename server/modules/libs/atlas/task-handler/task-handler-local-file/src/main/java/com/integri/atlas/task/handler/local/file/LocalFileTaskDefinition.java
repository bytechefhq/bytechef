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

import static com.integri.atlas.task.definition.dsl.TaskProperty.JSON_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.SELECT_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.STRING_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.show;
import static com.integri.atlas.task.definition.dsl.TaskPropertyOption.option;
import static com.integri.atlas.task.handler.local.file.LocalFileTaskConstants.*;
import static com.integri.atlas.task.handler.local.file.LocalFileTaskConstants.PROPERTY_FILE_ENTRY;
import static com.integri.atlas.task.handler.local.file.LocalFileTaskConstants.PROPERTY_FILE_NAME;
import static com.integri.atlas.task.handler.local.file.LocalFileTaskConstants.PROPERTY_OPERATION;

import com.integri.atlas.task.definition.TaskDefinition;
import com.integri.atlas.task.definition.dsl.TaskSpecification;
import com.integri.atlas.task.handler.local.file.LocalFileTaskConstants.Operation;
import org.springframework.stereotype.Component;

@Component
public class LocalFileTaskDefinition implements TaskDefinition {

    public static final TaskSpecification TASK_SPECIFICATION = TaskSpecification
        .create(TASK_LOCAL_FILE)
        .displayName("Local File")
        .description("Reads or writes a binary file from/to disk")
        .properties(
            SELECT_PROPERTY(PROPERTY_OPERATION)
                .displayName("Operation")
                .description("The operation to perform.")
                .options(
                    option("Read to file", Operation.READ.name()),
                    option("Write from file", Operation.WRITE.name())
                )
                .defaultValue(Operation.READ.name())
                .required(true),
            JSON_PROPERTY(PROPERTY_FILE_ENTRY)
                .displayName("File")
                .description("The object property which contains a reference to the file to be written.")
                .displayOption(show(PROPERTY_OPERATION, Operation.WRITE.name()))
                .required(true),
            STRING_PROPERTY(PROPERTY_FILE_NAME)
                .displayName("File Name")
                .description("The path of the file to read.")
                .displayOption(show(PROPERTY_OPERATION, "READ"))
                .defaultValue("")
                .placeholder("/data/your_file.pdf")
                .required(true),
            STRING_PROPERTY(PROPERTY_FILE_NAME)
                .displayName("File Name")
                .description("The path to which the file should be written.")
                .displayOption(show(PROPERTY_OPERATION, Operation.WRITE.name()))
                .defaultValue("")
                .placeholder("/data/your_file.pdf")
                .required(true)
        );

    @Override
    public TaskSpecification getSpecification() {
        return TASK_SPECIFICATION;
    }
}
