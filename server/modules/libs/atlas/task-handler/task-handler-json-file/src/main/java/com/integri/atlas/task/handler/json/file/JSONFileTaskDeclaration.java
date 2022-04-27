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

import com.integri.atlas.task.definition.TaskDeclaration;
import com.integri.atlas.task.definition.dsl.TaskSpecification;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class JSONFileTaskDeclaration implements TaskDeclaration {

    public static final TaskSpecification TASK_SPECIFICATION = TaskSpecification
        .create("jsonFile")
        .displayName("JSON File")
        .description("Reads and writes data from a JSON file.")
        .properties(
            SELECT_PROPERTY("fileType")
                .displayName("File Type")
                .description("The file type to choose.")
                .options(option("JSON", "JSON"), option("JSON Line", "JSONL"))
                .defaultValue("JSON")
                .required(true),
            SELECT_PROPERTY("operation")
                .displayName("Operation")
                .description("The operation to perform.")
                .options(
                    option("Read from file", "READ", "Reads data from a JSON file."),
                    option("Write to file", "WRITE", "Writes the data to a JSON file.")
                )
                .defaultValue("READ")
                .required(true),
            JSON_PROPERTY("fileEntry")
                .displayName("File")
                .description("The object property which contains a reference to the JSON file to read from.")
                .displayOption(show("operation", "READ"))
                .required(true),
            JSON_PROPERTY("input")
                .displayName("Input")
                .description("Object or array of objects to write to the file.")
                .displayOption(show("operation", parameterValues("WRITE")))
                .required(true),
            BOOLEAN_PROPERTY("isArray")
                .displayName("Is Array")
                .description("The object input is array?")
                .displayOption(show("operation", "READ"))
                .defaultValue(true),
            COLLECTION_PROPERTY("options")
                .displayName("Options")
                .placeholder("Add Option")
                .options(
                    STRING_PROPERTY("fileName")
                        .displayName("File Name")
                        .description("File name to set for binary data. By default, \"file.json\" will be used.")
                        .displayOption(show("operation", "WRITE"))
                        .defaultValue("file.json"),
                    STRING_PROPERTY("path")
                        .displayName("Path")
                        .description("The path where the array is e.g 'data'. Leave blank to use the top level object.")
                        .displayOption(show("operation", parameterValues("READ"), "isArray", parameterValues(true))),
                    INTEGER_PROPERTY("pageSize")
                        .displayName("Page Size")
                        .description("The amount of child elements to return in a page.")
                        .displayOption(show("operation", parameterValues("READ"), "isArray", parameterValues(true))),
                    INTEGER_PROPERTY("pageNumber")
                        .displayName("Page Number")
                        .description("The page number to get.")
                        .displayOption(show("operation", parameterValues("READ"), "isArray", parameterValues(true)))
                )
        );

    @Override
    public TaskSpecification getSpecification() {
        return TASK_SPECIFICATION;
    }
}
