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

package com.bytechef.task.handler.jsonfile.v1_0;

import static com.bytechef.hermes.descriptor.domain.DSL.ANY_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.ARRAY_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.BOOLEAN_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.FILE_ENTRY_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.INTEGER_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.OBJECT_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.OPERATION;
import static com.bytechef.hermes.descriptor.domain.DSL.OPTIONS;
import static com.bytechef.hermes.descriptor.domain.DSL.STRING_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.showWhen;

import com.bytechef.hermes.descriptor.domain.DSL;
import com.bytechef.hermes.descriptor.domain.TaskDescriptor;
import com.bytechef.hermes.descriptor.handler.TaskDescriptorHandler;
import com.bytechef.task.handler.jsonfile.JSONFileTaskConstants;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class JSONFileTaskDescriptorHandler implements TaskDescriptorHandler {

    private static final TaskDescriptor TASK_DESCRIPTOR = DSL.createTaskDescriptor(JSONFileTaskConstants.JSON_FILE)
            .displayName("JSON File")
            .description("Reads and writes data from a JSON file.")
            .version(JSONFileTaskConstants.VERSION_1_0)
            .operations(
                    OPERATION(JSONFileTaskConstants.READ)
                            .displayName("Read from file")
                            .description("Reads data from a JSON file.")
                            .inputs(
                                    STRING_PROPERTY(JSONFileTaskConstants.FILE_TYPE)
                                            .displayName("File Type")
                                            .description("The file type to choose.")
                                            .options(
                                                    DSL.option("JSON", JSONFileTaskConstants.FileType.JSON.name()),
                                                    DSL.option(
                                                            "JSON Line", JSONFileTaskConstants.FileType.JSONL.name()))
                                            .defaultValue(JSONFileTaskConstants.FileType.JSON.name())
                                            .required(true),
                                    FILE_ENTRY_PROPERTY(JSONFileTaskConstants.FILE_ENTRY)
                                            .displayName("File")
                                            .description(
                                                    "The object property which contains a reference to the JSON file to read from.")
                                            .required(true),
                                    BOOLEAN_PROPERTY(JSONFileTaskConstants.IS_ARRAY)
                                            .displayName("Is Array")
                                            .description("The object input is array?")
                                            .defaultValue(true),
                                    OPTIONS()
                                            .displayName("Options")
                                            .placeholder("Add Option")
                                            .options(
                                                    STRING_PROPERTY(JSONFileTaskConstants.PATH)
                                                            .displayName("Path")
                                                            .description(
                                                                    "The path where the array is e.g 'data'. Leave blank to use the top level object.")
                                                            .displayOption(showWhen(JSONFileTaskConstants.IS_ARRAY)
                                                                    .eq(true)),
                                                    INTEGER_PROPERTY(JSONFileTaskConstants.PAGE_SIZE)
                                                            .displayName("Page Size")
                                                            .description(
                                                                    "The amount of child elements to return in a page.")
                                                            .displayOption(showWhen(JSONFileTaskConstants.IS_ARRAY)
                                                                    .eq(true)),
                                                    INTEGER_PROPERTY(JSONFileTaskConstants.PAGE_NUMBER)
                                                            .displayName("Page Number")
                                                            .description("The page number to get.")
                                                            .displayOption(showWhen(JSONFileTaskConstants.IS_ARRAY)
                                                                    .eq(true))))
                            .outputs(ARRAY_PROPERTY(), OBJECT_PROPERTY()),
                    OPERATION(JSONFileTaskConstants.WRITE)
                            .displayName("Write to file")
                            .description("Writes the data to a JSON file.")
                            .inputs(
                                    STRING_PROPERTY(JSONFileTaskConstants.FILE_TYPE)
                                            .displayName("File Type")
                                            .description("The file type to choose.")
                                            .options(
                                                    DSL.option("JSON", JSONFileTaskConstants.FileType.JSON.name()),
                                                    DSL.option(
                                                            "JSON Line", JSONFileTaskConstants.FileType.JSONL.name()))
                                            .defaultValue(JSONFileTaskConstants.FileType.JSON.name())
                                            .required(true),
                                    ANY_PROPERTY(JSONFileTaskConstants.SOURCE)
                                            .displayName("Source")
                                            .description("The data to write to the file.")
                                            .required(true)
                                            .types(ARRAY_PROPERTY(), OBJECT_PROPERTY()),
                                    STRING_PROPERTY(JSONFileTaskConstants.FILE_NAME)
                                            .displayName("File Name")
                                            .description(
                                                    "File name to set for binary data. By default, \"file.json\" will be used.")
                                            .defaultValue("file.json"))
                            .outputs(FILE_ENTRY_PROPERTY()));

    @Override
    public TaskDescriptor getTaskDescriptor() {
        return TASK_DESCRIPTOR;
    }
}
