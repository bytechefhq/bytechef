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

package com.integri.atlas.task.handler.odsfile.v1_0;

import static com.integri.atlas.task.definition.model.DSL.ARRAY_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.BOOLEAN_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.DATE_TIME_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.FILE_ENTRY_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.INTEGER_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.NUMBER_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.OBJECT_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.OPERATION;
import static com.integri.atlas.task.definition.model.DSL.OPTIONS;
import static com.integri.atlas.task.definition.model.DSL.STRING_PROPERTY;
import static com.integri.atlas.task.handler.odsfile.OdsFileTaskConstants.*;

import com.integri.atlas.task.definition.handler.AbstractTaskDefinitionHandler;
import com.integri.atlas.task.definition.model.DSL;
import com.integri.atlas.task.definition.model.TaskDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class OdsFileTaskDefinitionHandler extends AbstractTaskDefinitionHandler {

    private static final TaskDefinition TASK_DEFINITION = DSL
        .createTaskDefinition(ODS_FILE)
        .displayName("ODS File")
        .description("Reads and writes data from a ODS file.")
        .version(VERSION_1_0)
        .operations(
            OPERATION(READ)
                .displayName("Read from file")
                .description("Reads data from a ODS file.")
                .inputs(
                    FILE_ENTRY_PROPERTY(FILE_ENTRY)
                        .displayName("File")
                        .description("The object property which contains a reference to the ODS file to read from.")
                        .required(true),
                    OPTIONS()
                        .displayName("Options")
                        .placeholder("Add Option")
                        .options(
                            BOOLEAN_PROPERTY(HEADER_ROW)
                                .displayName("Header Row")
                                .description("The first row of the file contains the header names.")
                                .defaultValue(true),
                            BOOLEAN_PROPERTY(INCLUDE_EMPTY_CELLS)
                                .displayName("Include Empty Cells")
                                .description(
                                    "When reading from file the empty cells will be filled with an empty string."
                                )
                                .defaultValue(false),
                            INTEGER_PROPERTY(PAGE_SIZE)
                                .displayName("Page Size")
                                .description("The amount of child elements to return in a page."),
                            INTEGER_PROPERTY(PAGE_NUMBER)
                                .displayName("Page Number")
                                .description("The page number to get."),
                            BOOLEAN_PROPERTY(READ_AS_STRING)
                                .displayName("Read As String")
                                .description(
                                    "In some cases and file formats, it is necessary to read data specifically as string, otherwise some special characters are interpreted the wrong way."
                                )
                                .defaultValue(false),
                            STRING_PROPERTY(SHEET_NAME)
                                .displayName("Sheet Name")
                                .description(
                                    "The name of the sheet to read from in the spreadsheet. If not set, the first one gets chosen."
                                )
                                .defaultValue("Sheet")
                        )
                )
                .outputs(ARRAY_PROPERTY()),
            OPERATION(WRITE)
                .displayName("Write to file")
                .description("Writes the data to a ODS file.")
                .inputs(
                    ARRAY_PROPERTY(ROWS)
                        .displayName("Rows")
                        .description("The array of objects to write to the file.")
                        .required(true)
                        .items(
                            OBJECT_PROPERTY()
                                .additionalProperties(true)
                                .properties(
                                    BOOLEAN_PROPERTY(),
                                    DATE_TIME_PROPERTY(),
                                    NUMBER_PROPERTY(),
                                    STRING_PROPERTY()
                                )
                        ),
                    OPTIONS()
                        .displayName("Options")
                        .placeholder("Add Option")
                        .options(
                            STRING_PROPERTY(FILE_NAME)
                                .displayName("File Name")
                                .description("File name to set for binary data. By default, \"file.ods\" will be used.")
                                .defaultValue(""),
                            STRING_PROPERTY(SHEET_NAME)
                                .displayName("Sheet Name")
                                .description("The name of the sheet to create in the spreadsheet.")
                                .defaultValue("Sheet")
                        )
                )
                .outputs(FILE_ENTRY_PROPERTY())
        );

    @Override
    public TaskDefinition getTaskDefinition() {
        return TASK_DEFINITION;
    }
}
