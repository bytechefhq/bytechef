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

package com.bytechef.task.handler.localfile.v1_0;

import static com.bytechef.hermes.descriptor.model.DSL.FILE_ENTRY_PROPERTY;
import static com.bytechef.hermes.descriptor.model.DSL.INTEGER_PROPERTY;
import static com.bytechef.hermes.descriptor.model.DSL.OBJECT_PROPERTY;
import static com.bytechef.hermes.descriptor.model.DSL.OPERATION;
import static com.bytechef.hermes.descriptor.model.DSL.STRING_PROPERTY;
import static com.bytechef.task.handler.localfile.LocalFileTaskConstants.FILE_ENTRY;
import static com.bytechef.task.handler.localfile.LocalFileTaskConstants.FILE_NAME;
import static com.bytechef.task.handler.localfile.LocalFileTaskConstants.LOCAL_FILE;
import static com.bytechef.task.handler.localfile.LocalFileTaskConstants.VERSION_1_0;

import com.bytechef.hermes.descriptor.handler.TaskDescriptorHandler;
import com.bytechef.hermes.descriptor.model.DSL;
import com.bytechef.hermes.descriptor.model.TaskDescriptor;
import com.bytechef.task.handler.localfile.LocalFileTaskConstants;
import org.springframework.stereotype.Component;

@Component
public class LocalFileTaskDescriptorHandler implements TaskDescriptorHandler {

    private static final TaskDescriptor TASK_DESCRIPTOR = DSL.createTaskDescriptor(LOCAL_FILE)
            .displayName("Local File")
            .description("Reads or writes a binary file from/to disk")
            .version(VERSION_1_0)
            .operations(
                    OPERATION(LocalFileTaskConstants.READ)
                            .displayName("Read to file")
                            .inputs(STRING_PROPERTY(FILE_NAME)
                                    .displayName("File Name")
                                    .description("The path of the file to read.")
                                    .placeholder("/data/your_file.pdf")
                                    .required(true))
                            .outputs(FILE_ENTRY_PROPERTY()),
                    OPERATION(LocalFileTaskConstants.WRITE)
                            .displayName("Write from file")
                            .inputs(
                                    FILE_ENTRY_PROPERTY(FILE_ENTRY)
                                            .displayName("File")
                                            .description(
                                                    "The object property which contains a reference to the file to be written.")
                                            .required(true),
                                    STRING_PROPERTY(FILE_NAME)
                                            .displayName("File Name")
                                            .description("The path to which the file should be written.")
                                            .placeholder("/data/your_file.pdf")
                                            .required(true))
                            .outputs(OBJECT_PROPERTY().properties(INTEGER_PROPERTY("bytes"))));

    @Override
    public TaskDescriptor getTaskDescriptor() {
        return TASK_DESCRIPTOR;
    }
}
