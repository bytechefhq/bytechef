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

package com.bytechef.task.handler.file.v1_0;

import static com.bytechef.hermes.descriptor.domain.DSL.FILE_ENTRY_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.OPERATION;
import static com.bytechef.hermes.descriptor.domain.DSL.STRING_PROPERTY;
import static com.bytechef.task.handler.file.FileTaskConstants.CONTENT;
import static com.bytechef.task.handler.file.FileTaskConstants.FILE;
import static com.bytechef.task.handler.file.FileTaskConstants.FILE_ENTRY;
import static com.bytechef.task.handler.file.FileTaskConstants.FILE_NAME;
import static com.bytechef.task.handler.file.FileTaskConstants.READ;
import static com.bytechef.task.handler.file.FileTaskConstants.VERSION;
import static com.bytechef.task.handler.file.FileTaskConstants.WRITE;

import com.bytechef.hermes.descriptor.domain.DSL;
import com.bytechef.hermes.descriptor.domain.TaskDescriptor;
import com.bytechef.hermes.descriptor.handler.TaskDescriptorHandler;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class FileTaskDescriptorHandler implements TaskDescriptorHandler {

    private static final TaskDescriptor TASK_DESCRIPTOR = DSL.createTaskDescriptor(FILE)
            .displayName("File")
            .description("Reads and writes data from a file")
            .version(VERSION)
            .operations(
                    OPERATION(READ)
                            .displayName("Read from file")
                            .description("Reads data from a csv file.")
                            .inputs(FILE_ENTRY_PROPERTY(FILE_ENTRY)
                                    .displayName("File")
                                    .description(
                                            "The object property which contains a reference to the file to read from.")
                                    .required(true))
                            .outputs(STRING_PROPERTY()),
                    OPERATION(WRITE)
                            .displayName("Write to file")
                            .description("Writes the data to a csv file.")
                            .inputs(
                                    STRING_PROPERTY(CONTENT)
                                            .displayName("Content")
                                            .description("String to write to the file.")
                                            .required(true),
                                    STRING_PROPERTY(FILE_NAME)
                                            .displayName("File Name")
                                            .description(
                                                    "File name to set for binary data. By default, \"file.txt\" will be used.")
                                            .defaultValue("file.txt"))
                            .outputs(FILE_ENTRY_PROPERTY()));

    @Override
    public TaskDescriptor getTaskDescriptor() {
        return TASK_DESCRIPTOR;
    }
}
