/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.task.handler.binary.file;

import static com.integri.atlas.engine.core.task.TaskDescriptor.task;
import static com.integri.atlas.engine.core.task.description.TaskDescription.property;
import static com.integri.atlas.engine.core.task.description.TaskPropertyOption.option;
import static com.integri.atlas.engine.core.task.description.TaskPropertyType.SELECT;
import static com.integri.atlas.engine.core.task.description.TaskPropertyType.STRING;

import com.integri.atlas.engine.core.task.TaskDescriptor;
import com.integri.atlas.engine.core.task.description.TaskDescription;
import org.springframework.stereotype.Component;

@Component
public class BinaryFileTaskDescriptor implements TaskDescriptor {

    private static final TaskDescription TASK_DESCRIPTION = task("binaryFile")
        .displayName("Binary File")
        .description("Directs a stream based on true/false results of comparisons")
        .properties(
            property("operation")
                .displayName("Operation")
                .type(SELECT)
                .description("Operation to do with the file.")
                .options(option("Read", "READ"), option("Write", "WRITE"))
                .defaultValue("read")
                .required(true),
            property("filePath")
                .displayName("File Path")
                .type(STRING)
                .description("Path of the file to read or path to which the file should be written.")
                .defaultValue("")
                .placeholder("/data/your_file.pdf")
                .required(true),
            property("dataPropertyName")
                .displayName("Property Name")
                .type(STRING)
                .description(
                    "Name of the binary property to which to write the data of the read file or which contains the data for the file to be written."
                )
                .defaultValue("data")
                .required(true)
        );

    @Override
    public TaskDescription getDescription() {
        return TASK_DESCRIPTION;
    }
}
