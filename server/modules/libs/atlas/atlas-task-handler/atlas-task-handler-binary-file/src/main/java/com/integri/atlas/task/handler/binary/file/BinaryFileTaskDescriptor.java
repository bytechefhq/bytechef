/*
 * Copyright 2021 <your company/name>.
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
 */

package com.integri.atlas.task.handler.binary.file;

import static com.integri.atlas.engine.core.task.description.TaskDescription.task;
import static com.integri.atlas.engine.core.task.description.TaskProperty.BINARY_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.JSON_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.SELECT_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.STRING_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.show;
import static com.integri.atlas.engine.core.task.description.TaskPropertyOption.option;

import com.integri.atlas.engine.core.task.TaskDescriptor;
import com.integri.atlas.engine.core.task.description.TaskDescription;
import org.springframework.stereotype.Component;

@Component
public class BinaryFileTaskDescriptor implements TaskDescriptor {

    public static final TaskDescription TASK_DESCRIPTION = task("binaryFile")
        .displayName("Binary File")
        .description("Reads or writes a binary file from/toto disk")
        .properties(
            SELECT_PROPERTY("operation")
                .displayName("Operation")
                .description("The operation to perform.")
                .options(option("Read to file", "READ"), option("Write from file", "WRITE"))
                .defaultValue("read")
                .required(true),
            STRING_PROPERTY("fileName")
                .displayName("File Name")
                .description("The path of the file to read.")
                .displayOption(show("operation", "READ"))
                .defaultValue("")
                .placeholder("/data/your_file.pdf")
                .required(true),
            STRING_PROPERTY("fileName")
                .displayName("File Name")
                .description("The path to which the file should be written.")
                .displayOption(show("operation", "WRITE"))
                .defaultValue("")
                .placeholder("/data/your_file.pdf")
                .required(true),
            BINARY_PROPERTY("binaryItem")
                .displayName("Binary")
                .description("The Binary property which contains the data for the file to be written.")
                .displayOption(show("operation", "WRITE"))
                .required(true)
        );

    @Override
    public TaskDescription getDescription() {
        return TASK_DESCRIPTION;
    }
}
