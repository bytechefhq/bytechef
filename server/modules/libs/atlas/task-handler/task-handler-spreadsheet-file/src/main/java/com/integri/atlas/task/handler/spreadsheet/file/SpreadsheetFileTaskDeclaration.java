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

package com.integri.atlas.task.handler.spreadsheet.file;

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
public class SpreadsheetFileTaskDeclaration implements TaskDeclaration {

    public static final TaskSpecification TASK_SPECIFICATION = TaskSpecification
        .create("spreadsheetFile")
        .displayName("Spreadsheet File")
        .description("Reads and writes data from a spreadsheet file")
        .properties(
            OPTION_PROPERTY("operation")
                .displayName("Operation")
                .description("The operation to perform.")
                .options(
                    option("Read from file", "READ", "Reads data from a spreadsheet file."),
                    option("Write to file", "WRITE", "Writes the data to a spreadsheet file.")
                )
                .defaultValue("READ")
                .required(true),
            FILE_ENTRY_PROPERTY("fileEntry")
                .displayName("File")
                .description("The object property which contains a reference to the spreadsheet file to read from.")
                .displayOption(show("operation", "READ"))
                .required(true),
            FILE_ENTRY_PROPERTY("fileEntry")
                .displayName("File")
                .description("The object property which contains reference to the file with JSON data.")
                .displayOption(show("operation", parameterValues("WRITE"), "inputType", parameterValues("FILE")))
                .required(true),
            OPTION_PROPERTY("fileFormat")
                .displayName("FileFormat")
                .description("The format of the file to save the data.")
                .displayOption(show("operation", "WRITE"))
                .options(
                    option("CSV", "CSV", "Comma-separated value"),
                    option("XLS", "XLS", "Microsoft Excel"),
                    option("XLSX", "XLSX", "Microsoft Excel")
                )
                .defaultValue("CSV"),
            JSON_PROPERTY("items")
                .displayName("JSON array of items")
                .description("Data to write to the file.")
                .displayOption(show("operation", parameterValues("WRITE"), "inputType", parameterValues("JSON")))
                .required(true),
            COLLECTION_PROPERTY("options")
                .displayName("Options")
                .placeholder("Add Option")
                .options(
                    STRING_PROPERTY("delimiter")
                        .displayName("Delimiter")
                        .description("Delimiter to use when reading a csv file.")
                        .displayOption(show("operation", parameterValues("READ"), "fileFormat", parameterValues("CSV")))
                        .defaultValue(","),
                    STRING_PROPERTY("fileName")
                        .displayName("File Name")
                        .description(
                            "File name to set for binary data. By default, \"spreadsheet.<fileFormat>\" will be used."
                        )
                        .displayOption(show("operation", "WRITE"))
                        .defaultValue(""),
                    BOOLEAN_PROPERTY("headerRow")
                        .displayName("Header Row")
                        .description("The first row of the file contains the header names.")
                        .displayOption(show("operation", "READ"))
                        .defaultValue(true),
                    BOOLEAN_PROPERTY("includeEmptyCells")
                        .displayName("Include Empty Cells")
                        .description("When reading from file the empty cells will be filled with an empty string.")
                        .displayOption(show("operation", "READ"))
                        .defaultValue(false),
                    OPTION_PROPERTY("inputType")
                        .displayName("Input Content Type")
                        .description("Input type to use when writing data.")
                        .displayOption(show("operation", "WRITE"))
                        .options(option("JSON", "JSON"), option("File", "FILE"))
                        .defaultValue("JSON"),
                    GROUP_PROPERTY("range")
                        .displayName("Range")
                        .description(
                            "The range to read from the table. If set to a number it will be the starting row."
                        )
                        .displayOption(show("operation", "READ"))
                        .fields(
                            NUMBER_PROPERTY("startRow").displayName("Start Row index"),
                            NUMBER_PROPERTY("endRow").displayName("End Row index")
                        ),
                    BOOLEAN_PROPERTY("readAsString")
                        .displayName("Read As String")
                        .description(
                            "In some cases and file formats, it is necessary to read data specifically as string, otherwise some special characters are interpreted the wrong way."
                        )
                        .displayOption(show("operation", "READ"))
                        .defaultValue(false),
                    STRING_PROPERTY("sheetName")
                        .displayName("Sheet Name")
                        .description(
                            "The name of the sheet to read from in the spreadsheet. If not set, the first one gets chosen."
                        )
                        .displayOption(
                            show("operation", parameterValues("READ"), "fileFormat", parameterValues("XLS", "XLSX"))
                        )
                        .defaultValue("Sheet"),
                    STRING_PROPERTY("sheetName")
                        .displayName("Sheet Name")
                        .description("The name of the sheet to create in the spreadsheet.")
                        .displayOption(
                            show("operation", parameterValues("WRITE"), "fileFormat", parameterValues("XLS", "XLSX"))
                        )
                        .defaultValue("Sheet")
                )
        );

    @Override
    public TaskSpecification getSpecification() {
        return TASK_SPECIFICATION;
    }
}
