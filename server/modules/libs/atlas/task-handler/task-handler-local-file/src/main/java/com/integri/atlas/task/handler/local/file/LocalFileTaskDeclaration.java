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

import com.integri.atlas.task.definition.TaskDeclaration;
import com.integri.atlas.task.definition.dsl.TaskSpecification;
import org.springframework.stereotype.Component;

@Component
public class LocalFileTaskDeclaration implements TaskDeclaration {

    public static final TaskSpecification TASK_SPECIFICATION = TaskSpecification
        .create("localFile")
        .displayName("Local File")
        .description("Reads or writes a binary file from/to disk")
        .properties(
            SELECT_PROPERTY("operation")
                .displayName("Operation")
                .description("The operation to perform.")
                .options(option("Read to file", "READ"), option("Write from file", "WRITE"))
                .defaultValue("READ")
                .required(true),
            JSON_PROPERTY("fileEntry")
                .displayName("File")
                .description("The object property which contains a reference to the file to be written.")
                .displayOption(show("operation", "WRITE"))
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
                .required(true)
        );

    @Override
    public TaskSpecification getSpecification() {
        return TASK_SPECIFICATION;
    }
}
