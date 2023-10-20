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

package com.integri.atlas.task.handler.xmlfile.v1_0;

import static com.integri.atlas.task.definition.model.DSL.ANY_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.ARRAY_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.BOOLEAN_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.FILE_ENTRY_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.INTEGER_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.OBJECT_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.OPERATION;
import static com.integri.atlas.task.definition.model.DSL.OPTIONS;
import static com.integri.atlas.task.definition.model.DSL.STRING_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.showWhen;
import static com.integri.atlas.task.handler.xmlfile.XmlFileTaskConstants.*;

import com.integri.atlas.task.definition.handler.AbstractTaskDefinitionHandler;
import com.integri.atlas.task.definition.model.DSL;
import com.integri.atlas.task.definition.model.TaskDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class XmlFileTaskDefinitionHandler extends AbstractTaskDefinitionHandler {

    private static final TaskDefinition TASK_DEFINITION = DSL
        .createTaskDefinition(XML_FILE)
        .displayName("XML File")
        .description("Reads and writes data from a XML file.")
        .version(VERSION_1_0)
        .operations(
            OPERATION(READ)
                .displayName("Read from file")
                .description("Reads data from a XML file.")
                .inputs(
                    FILE_ENTRY_PROPERTY(FILE_ENTRY)
                        .displayName("File")
                        .description("The object property which contains a reference to the XML file to read from.")
                        .required(true),
                    BOOLEAN_PROPERTY(IS_ARRAY)
                        .displayName("Is Array")
                        .description("The object input is array?")
                        .defaultValue(true),
                    OPTIONS()
                        .displayName("Options")
                        .placeholder("Add Option")
                        .options(
                            STRING_PROPERTY(PATH)
                                .displayName("Path")
                                .description(
                                    "The path where the array is e.g 'data'. Leave blank to use the top level object."
                                )
                                .displayOption(showWhen(IS_ARRAY).in(true)),
                            INTEGER_PROPERTY(PAGE_SIZE)
                                .displayName("Page Size")
                                .description("The amount of child elements to return in a page.")
                                .displayOption(showWhen(IS_ARRAY).in(true)),
                            INTEGER_PROPERTY(PAGE_NUMBER)
                                .displayName("Page Number")
                                .description("The page number to get.")
                                .displayOption(showWhen(IS_ARRAY).in(true))
                        )
                )
                .outputs(ARRAY_PROPERTY(), OBJECT_PROPERTY()),
            OPERATION(WRITE)
                .displayName("Write to file")
                .description("Writes the data to a XML file.")
                .inputs(
                    ANY_PROPERTY(SOURCE)
                        .displayName("Source")
                        .description("The data to write to the file.")
                        .required(true)
                        .types(ARRAY_PROPERTY(), OBJECT_PROPERTY()),
                    STRING_PROPERTY(FILE_NAME)
                        .displayName("File Name")
                        .description("File name to set for binary data. By default, \"file.xml\" will be used.")
                        .defaultValue("file.xml")
                )
                .outputs(FILE_ENTRY_PROPERTY())
        );

    @Override
    public TaskDefinition getTaskDefinition() {
        return TASK_DEFINITION;
    }
}
