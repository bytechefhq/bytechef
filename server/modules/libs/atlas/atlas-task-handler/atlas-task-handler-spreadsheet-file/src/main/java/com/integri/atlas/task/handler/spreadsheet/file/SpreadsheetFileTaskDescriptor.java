/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.task.handler.spreadsheet.file;

import static com.integri.atlas.engine.core.task.description.TaskDescription.task;
import static com.integri.atlas.engine.core.task.description.TaskParameterValue.parameterValues;
import static com.integri.atlas.engine.core.task.description.TaskProperty.BOOLEAN_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.COLLECTION_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.GROUP_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.NUMBER_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.SELECT_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.STRING_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.show;
import static com.integri.atlas.engine.core.task.description.TaskPropertyOption.option;

import com.integri.atlas.engine.core.task.TaskDescriptor;
import com.integri.atlas.engine.core.task.description.TaskDescription;
import org.springframework.stereotype.Component;

@Component
public class SpreadsheetFileTaskDescriptor implements TaskDescriptor {

    public static final TaskDescription TASK_DESCRIPTION = task("spreadsheetFile")
        .displayName("Spreadsheet File")
        .description("Reads and writes data from a spreadsheet file")
        .properties(
            SELECT_PROPERTY("operation")
                .displayName("Operation")
                .description("The operation to perform.")
                .options(option("Read to file", "READ"), option("Write from file", "WRITE"))
                .defaultValue("read")
                .required(true),
            //
            // read from file
            //
            STRING_PROPERTY("binaryPropertyName")
                .displayName("Binary Property")
                .displayOption(show("operation", "READ"))
                .description("Name of the binary property from which to read the binary data of the spreadsheet file.")
                .defaultValue("data"),
            //
            // write to file
            //
            SELECT_PROPERTY("fileFormat")
                .displayName("FileFormat")
                .description("The format of the file to save the data.")
                .displayOption(show("operation", "WRITE"))
                .options(
                    option("CSV", "CSV", "Comma-separated value"),
                    option("HTML", "HTML", "HTML Table"),
                    option("ODS", "ODS", "OpenDocument Spreadsheet"),
                    option("RTF", "RTF", "Rich Text Format"),
                    option("XLS", "XLS", "Microsoft Excel"),
                    option("XLSX", "XLSX", "Microsoft Excel")
                )
                .defaultValue("CSV"),
            STRING_PROPERTY("binaryPropertyName")
                .displayName("Binary Property")
                .displayOption(show("operation", "WRITE"))
                .description("Name of the binary property in which to save the binary data of the spreadsheet file.")
                .defaultValue("data"),
            COLLECTION_PROPERTY("options")
                .displayName("Options")
                .placeholder("Add Option")
                .options(
                    BOOLEAN_PROPERTY("compression")
                        .displayName("Compression")
                        .displayOption(
                            show("operation", parameterValues("WRITE"), "fileFormat", parameterValues("XLSX", "ODS"))
                        )
                        .description("Weather compression will be applied or not.")
                        .defaultValue(false),
                    STRING_PROPERTY("fileName")
                        .displayName("File Name")
                        .displayOption(show("operation", "WRITE"))
                        .description(
                            "File name to set in binary data. By default will \"spreadsheet.<fileFormat>\" be used."
                        )
                        .defaultValue(""),
                    BOOLEAN_PROPERTY("headerRow")
                        .displayName("Header Row")
                        .displayOption(show("operation", "READ"))
                        .description("The first row of the file contains the header names.")
                        .defaultValue(true),
                    BOOLEAN_PROPERTY("includeEmptyCells")
                        .displayName("Include Empty Cells")
                        .displayOption(show("operation", "READ"))
                        .description("When reading from file the empty cells will be filled with an empty string.")
                        .defaultValue(false),
                    BOOLEAN_PROPERTY("rawData")
                        .displayName("RAW Data")
                        .displayOption(show("operation", "READ"))
                        .description("If the data should be returned RAW instead of parsed.")
                        .defaultValue(false),
                    BOOLEAN_PROPERTY("readAsString")
                        .displayName("Read As String")
                        .displayOption(show("operation", "READ"))
                        .description(
                            "In some cases and file formats, it is necessary to read data specifically as string, otherwise some special characters are interpreted the wrong way."
                        )
                        .defaultValue(false),
                    GROUP_PROPERTY("range")
                        .displayName("Range")
                        .displayOption(show("operation", "READ"))
                        .description(
                            "The range to read from the table. If set to a number it will be the starting row."
                        )
                        .fields(
                            NUMBER_PROPERTY("startRow").displayName("Start Row index"),
                            NUMBER_PROPERTY("endRow").displayName("End Row index")
                        ),
                    STRING_PROPERTY("sheetName")
                        .displayName("Sheet Name")
                        .displayOption(show("operation", "READ"))
                        .description(
                            "The name of the sheet to read from in the spreadsheet (if supported). If not set, the first one gets chosen."
                        )
                        .defaultValue("Sheet"),
                    STRING_PROPERTY("sheetName")
                        .displayName("Sheet Name")
                        .displayOption(
                            show(
                                "operation",
                                parameterValues("WRITE"),
                                "fileFormat",
                                parameterValues("ODS", "XLS", "XLSX")
                            )
                        )
                        .description("The name of the sheet to create in the spreadsheet.")
                        .defaultValue("Sheet")
                )
        );

    @Override
    public TaskDescription getDescription() {
        return TASK_DESCRIPTION;
    }
}
