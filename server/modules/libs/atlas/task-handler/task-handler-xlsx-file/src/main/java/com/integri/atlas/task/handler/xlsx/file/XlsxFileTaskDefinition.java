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

package com.integri.atlas.task.handler.xlsx.file;

import static com.integri.atlas.task.definition.dsl.TaskParameterValue.parameterValues;
import static com.integri.atlas.task.definition.dsl.TaskProperty.BOOLEAN_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.COLLECTION_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.INTEGER_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.JSON_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.SELECT_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.STRING_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.show;
import static com.integri.atlas.task.definition.dsl.TaskPropertyOption.option;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.Operation;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.PROPERTY_FILE_ENTRY;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.PROPERTY_FILE_NAME;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.PROPERTY_HEADER_ROW;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.PROPERTY_INCLUDE_EMPTY_CELLS;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.PROPERTY_OPERATION;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.PROPERTY_PAGE_NUMBER;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.PROPERTY_PAGE_SIZE;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.PROPERTY_READ_AS_STRING;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.PROPERTY_ROWS;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.PROPERTY_SHEET_NAME;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.TASK_XLSX_FILE;

import com.integri.atlas.task.definition.TaskDefinition;
import com.integri.atlas.task.definition.dsl.TaskSpecification;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class XlsxFileTaskDefinition implements TaskDefinition {

    public static final TaskSpecification TASK_SPECIFICATION = TaskSpecification
        .create(TASK_XLSX_FILE)
        .displayName("XLSX File")
        .description("Reads and writes data from a XLS/XLSX file.")
        .properties(
            SELECT_PROPERTY(PROPERTY_OPERATION)
                .displayName("Operation")
                .description("The operation to perform.")
                .options(
                    option("Read from file", Operation.READ.name(), "Reads data from a XLS/XLSX file."),
                    option("Write to file", Operation.WRITE.name(), "Writes the data to a XLS/XLSX file.")
                )
                .defaultValue(Operation.READ.name())
                .required(true),
            JSON_PROPERTY(PROPERTY_FILE_ENTRY)
                .displayName("File")
                .description("The object property which contains a reference to the XLS/XLSX file to read from.")
                .displayOption(show(PROPERTY_OPERATION, Operation.READ.name()))
                .required(true),
            JSON_PROPERTY(PROPERTY_ROWS)
                .displayName("Rows")
                .description("The array of objects to write to the file.")
                .displayOption(show(PROPERTY_OPERATION, Operation.WRITE.name()))
                .required(true),
            COLLECTION_PROPERTY("options")
                .displayName("Options")
                .placeholder("Add Option")
                .options(
                    STRING_PROPERTY(PROPERTY_FILE_NAME)
                        .displayName("File Name")
                        .description("File name to set for binary data. By default, \"file.xlsx\" will be used.")
                        .displayOption(show(PROPERTY_OPERATION, Operation.WRITE.name()))
                        .defaultValue(""),
                    BOOLEAN_PROPERTY(PROPERTY_HEADER_ROW)
                        .displayName("Header Row")
                        .description("The first row of the file contains the header names.")
                        .displayOption(show(PROPERTY_OPERATION, Operation.READ.name()))
                        .defaultValue(true),
                    BOOLEAN_PROPERTY(PROPERTY_INCLUDE_EMPTY_CELLS)
                        .displayName("Include Empty Cells")
                        .description("When reading from file the empty cells will be filled with an empty string.")
                        .displayOption(show(PROPERTY_OPERATION, Operation.READ.name()))
                        .defaultValue(false),
                    INTEGER_PROPERTY(PROPERTY_PAGE_SIZE)
                        .displayName("Page Size")
                        .description("The amount of child elements to return in a page.")
                        .displayOption(show(PROPERTY_OPERATION, Operation.READ.name())),
                    INTEGER_PROPERTY(PROPERTY_PAGE_NUMBER)
                        .displayName("Page Number")
                        .description("The page number to get.")
                        .displayOption(show(PROPERTY_OPERATION, Operation.READ.name())),
                    BOOLEAN_PROPERTY(PROPERTY_READ_AS_STRING)
                        .displayName("Read As String")
                        .description(
                            "In some cases and file formats, it is necessary to read data specifically as string, otherwise some special characters are interpreted the wrong way."
                        )
                        .displayOption(show(PROPERTY_OPERATION, Operation.READ.name()))
                        .defaultValue(false),
                    STRING_PROPERTY(PROPERTY_SHEET_NAME)
                        .displayName("Sheet Name")
                        .description(
                            "The name of the sheet to read from in the spreadsheet. If not set, the first one gets chosen."
                        )
                        .displayOption(show(PROPERTY_OPERATION, parameterValues(Operation.READ.name())))
                        .defaultValue("Sheet"),
                    STRING_PROPERTY(PROPERTY_SHEET_NAME)
                        .displayName("Sheet Name")
                        .description("The name of the sheet to create in the spreadsheet.")
                        .displayOption(show(PROPERTY_OPERATION, parameterValues(Operation.WRITE.name())))
                        .defaultValue("Sheet")
                )
        );

    @Override
    public TaskSpecification getSpecification() {
        return TASK_SPECIFICATION;
    }
}
