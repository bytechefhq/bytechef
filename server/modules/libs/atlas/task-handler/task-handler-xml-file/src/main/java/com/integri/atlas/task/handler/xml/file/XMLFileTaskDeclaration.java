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

package com.integri.atlas.task.handler.xml.file;

import static com.integri.atlas.task.definition.dsl.TaskParameterValue.parameterValues;
import static com.integri.atlas.task.definition.dsl.TaskProperty.BOOLEAN_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.COLLECTION_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.FILE_ENTRY_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.GROUP_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.JSON_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.NUMBER_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.OPTION_PROPERTY;
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
public class XMLFileTaskDeclaration implements TaskDeclaration {

    public static final TaskSpecification TASK_SPECIFICATION = TaskSpecification
        .create("xmlFile")
        .displayName("XML File")
        .description("Reads and writes data from a XML file")
        .properties(
            OPTION_PROPERTY("operation")
                .displayName("Operation")
                .description("The operation to perform.")
                .options(
                    option("Read from file", "READ", "Reads data from a XML file."),
                    option("Write to file", "WRITE", "Writes the data to a XML file.")
                )
                .defaultValue("READ")
                .required(true),
            FILE_ENTRY_PROPERTY("fileEntry")
                .displayName("File")
                .description("The object property which contains a reference to the XML file to read from.")
                .displayOption(show("operation", "READ"))
                .required(true),
            JSON_PROPERTY("items")
                .displayName("JSON object or array of items")
                .description("Data to write to the file.")
                .displayOption(show("operation", parameterValues("WRITE")))
                .required(true),
            BOOLEAN_PROPERTY("isArray")
                .displayName("Is Array")
                .description("The input JSON is array?")
                .displayOption(show("operation", "READ"))
                .defaultValue(true),
            COLLECTION_PROPERTY("options")
                .displayName("Options")
                .placeholder("Add Option")
                .options(
                    STRING_PROPERTY("fileName")
                        .displayName("File Name")
                        .description("File name to set for binary data. By default, \"file.xml\" will be used.")
                        .displayOption(show("operation", "WRITE"))
                        .defaultValue(""),
                    NUMBER_PROPERTY("pageSize")
                        .displayName("Page Size")
                        .description("The amount of child elements to return in a page.")
                        .displayOption(show("operation", parameterValues("READ"), "isArray", parameterValues(true))),
                    NUMBER_PROPERTY("pageNumber")
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
