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

package com.integri.atlas.task.handler.json.file;

import static com.integri.atlas.task.definition.dsl.TaskParameterValue.parameterValues;
import static com.integri.atlas.task.definition.dsl.TaskProperty.BOOLEAN_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.COLLECTION_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.INTEGER_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.JSON_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.SELECT_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.STRING_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.show;
import static com.integri.atlas.task.definition.dsl.TaskPropertyOption.option;
import static com.integri.atlas.task.handler.json.file.JSONFileTaskConstants.*;
import static com.integri.atlas.task.handler.json.file.JSONFileTaskConstants.FileType.JSON;
import static com.integri.atlas.task.handler.json.file.JSONFileTaskConstants.FileType.JSONL;
import static com.integri.atlas.task.handler.json.file.JSONFileTaskConstants.Operation;

import com.integri.atlas.task.definition.TaskDeclaration;
import com.integri.atlas.task.definition.dsl.TaskSpecification;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class JSONFileTaskDeclaration implements TaskDeclaration {

    public static final TaskSpecification TASK_SPECIFICATION = TaskSpecification
        .create(TASK_JSON_FILE)
        .displayName("JSON File")
        .description("Reads and writes data from a JSON file.")
        .properties(
            SELECT_PROPERTY(PROPERTY_FILE_TYPE)
                .displayName("File Type")
                .description("The file type to choose.")
                .options(option("JSON", JSON.name()), option("JSON Line", JSONL.name()))
                .defaultValue(JSON.name())
                .required(true),
            SELECT_PROPERTY(PROPERTY_OPERATION)
                .displayName("Operation")
                .description("The operation to perform.")
                .options(
                    option("Read from file", Operation.READ.name(), "Reads data from a JSON file."),
                    option("Write to file", Operation.WRITE.name(), "Writes the data to a JSON file.")
                )
                .defaultValue(Operation.READ.name())
                .required(true),
            JSON_PROPERTY(PROPERTY_FILE_ENTRY)
                .displayName("File")
                .description("The object property which contains a reference to the JSON file to read from.")
                .displayOption(show(PROPERTY_OPERATION, Operation.READ.name()))
                .required(true),
            JSON_PROPERTY(PROPERTY_INPUT)
                .displayName("Input")
                .description("Object or array of objects to write to the file.")
                .displayOption(show(PROPERTY_OPERATION, parameterValues(Operation.WRITE.name())))
                .required(true),
            BOOLEAN_PROPERTY(PROPERTY_IS_ARRAY)
                .displayName("Is Array")
                .description("The object input is array?")
                .displayOption(show(PROPERTY_OPERATION, Operation.READ.name()))
                .defaultValue(true),
            COLLECTION_PROPERTY("options")
                .displayName("Options")
                .placeholder("Add Option")
                .options(
                    STRING_PROPERTY(PROPERTY_FILE_NAME)
                        .displayName("File Name")
                        .description("File name to set for binary data. By default, \"file.json\" will be used.")
                        .displayOption(show(PROPERTY_OPERATION, Operation.WRITE.name()))
                        .defaultValue("file.json"),
                    STRING_PROPERTY(PROPERTY_PATH)
                        .displayName("Path")
                        .description("The path where the array is e.g 'data'. Leave blank to use the top level object.")
                        .displayOption(
                            show(
                                PROPERTY_OPERATION,
                                parameterValues(Operation.READ.name()),
                                PROPERTY_IS_ARRAY,
                                parameterValues(true)
                            )
                        ),
                    INTEGER_PROPERTY(PROPERTY_PAGE_SIZE)
                        .displayName("Page Size")
                        .description("The amount of child elements to return in a page.")
                        .displayOption(
                            show(
                                PROPERTY_OPERATION,
                                parameterValues(Operation.READ.name()),
                                PROPERTY_IS_ARRAY,
                                parameterValues(true)
                            )
                        ),
                    INTEGER_PROPERTY(JSONFileTaskConstants.PROPERTY_PAGE_NUMBER)
                        .displayName("Page Number")
                        .description("The page number to get.")
                        .displayOption(
                            show(
                                PROPERTY_OPERATION,
                                parameterValues(Operation.READ.name()),
                                PROPERTY_IS_ARRAY,
                                parameterValues(true)
                            )
                        )
                )
        );

    @Override
    public TaskSpecification getSpecification() {
        return TASK_SPECIFICATION;
    }
}
