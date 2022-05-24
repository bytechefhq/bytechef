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

package com.integri.atlas.task.handler.ods.file;

import static com.integri.atlas.task.definition.dsl.DSL.ARRAY_PROPERTY;
import static com.integri.atlas.task.definition.dsl.DSL.BOOLEAN_PROPERTY;
import static com.integri.atlas.task.definition.dsl.DSL.DATE_TIME_PROPERTY;
import static com.integri.atlas.task.definition.dsl.DSL.FILE_ENTRY_PROPERTY;
import static com.integri.atlas.task.definition.dsl.DSL.INTEGER_PROPERTY;
import static com.integri.atlas.task.definition.dsl.DSL.NUMBER_PROPERTY;
import static com.integri.atlas.task.definition.dsl.DSL.OBJECT_PROPERTY;
import static com.integri.atlas.task.definition.dsl.DSL.OPERATION;
import static com.integri.atlas.task.definition.dsl.DSL.OPTIONS;
import static com.integri.atlas.task.definition.dsl.DSL.STRING_PROPERTY;
import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.Operation;
import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.PROPERTY_FILE_ENTRY;
import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.PROPERTY_FILE_NAME;
import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.PROPERTY_HEADER_ROW;
import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.PROPERTY_INCLUDE_EMPTY_CELLS;
import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.PROPERTY_PAGE_NUMBER;
import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.PROPERTY_PAGE_SIZE;
import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.PROPERTY_READ_AS_STRING;
import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.PROPERTY_ROWS;
import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.PROPERTY_SHEET_NAME;
import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.TASK_ODS_FILE;

import com.integri.atlas.task.definition.TaskDefinitionHandler;
import com.integri.atlas.task.definition.dsl.DSL;
import com.integri.atlas.task.definition.dsl.TaskDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class OdsFileTaskDefinitionHandler implements TaskDefinitionHandler {

    private static final TaskDefinition TASK_DEFINITION = DSL
        .create(TASK_ODS_FILE)
        .displayName("ODS File")
        .description("Reads and writes data from a ODS file.")
        .operations(
            OPERATION(Operation.READ.name())
                .displayName("Read from file")
                .description("Reads data from a ODS file.")
                .inputs(
                    FILE_ENTRY_PROPERTY(PROPERTY_FILE_ENTRY)
                        .displayName("File")
                        .description("The object property which contains a reference to the ODS file to read from.")
                        .required(true),
                    OPTIONS()
                        .displayName("Options")
                        .placeholder("Add Option")
                        .options(
                            BOOLEAN_PROPERTY(PROPERTY_HEADER_ROW)
                                .displayName("Header Row")
                                .description("The first row of the file contains the header names.")
                                .defaultValue(true),
                            BOOLEAN_PROPERTY(PROPERTY_INCLUDE_EMPTY_CELLS)
                                .displayName("Include Empty Cells")
                                .description(
                                    "When reading from file the empty cells will be filled with an empty string."
                                )
                                .defaultValue(false),
                            INTEGER_PROPERTY(PROPERTY_PAGE_SIZE)
                                .displayName("Page Size")
                                .description("The amount of child elements to return in a page."),
                            INTEGER_PROPERTY(PROPERTY_PAGE_NUMBER)
                                .displayName("Page Number")
                                .description("The page number to get."),
                            BOOLEAN_PROPERTY(PROPERTY_READ_AS_STRING)
                                .displayName("Read As String")
                                .description(
                                    "In some cases and file formats, it is necessary to read data specifically as string, otherwise some special characters are interpreted the wrong way."
                                )
                                .defaultValue(false),
                            STRING_PROPERTY(PROPERTY_SHEET_NAME)
                                .displayName("Sheet Name")
                                .description(
                                    "The name of the sheet to read from in the spreadsheet. If not set, the first one gets chosen."
                                )
                                .defaultValue("Sheet")
                        )
                )
                .outputs(ARRAY_PROPERTY()),
            OPERATION(Operation.WRITE.name())
                .displayName("Write to file")
                .description("Writes the data to a ODS file.")
                .inputs(
                    ARRAY_PROPERTY(PROPERTY_ROWS)
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
                            STRING_PROPERTY(PROPERTY_FILE_NAME)
                                .displayName("File Name")
                                .description("File name to set for binary data. By default, \"file.ods\" will be used.")
                                .defaultValue(""),
                            STRING_PROPERTY(PROPERTY_SHEET_NAME)
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
