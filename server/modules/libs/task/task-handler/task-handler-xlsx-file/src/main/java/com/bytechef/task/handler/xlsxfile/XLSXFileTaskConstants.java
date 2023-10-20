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

package com.bytechef.task.handler.xlsxfile;

import static com.bytechef.hermes.descriptor.domain.DSL.ARRAY_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.BOOLEAN_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.DATE_TIME_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.FILE_ENTRY_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.INTEGER_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.NUMBER_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.OBJECT_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.OPERATION;
import static com.bytechef.hermes.descriptor.domain.DSL.OPTIONS;
import static com.bytechef.hermes.descriptor.domain.DSL.STRING_PROPERTY;

import com.bytechef.hermes.descriptor.domain.DSL;
import com.bytechef.hermes.descriptor.domain.TaskDescriptor;

/**
 * @author Ivica Cardic
 */
public class XLSXFileTaskConstants {

    public static final String FILE_ENTRY = "fileEntry";
    public static final String ROWS = "rows";
    public static final String FILE_NAME = "fileName";
    public static final String PROPERTY_HEADER_ROW = "headerRow";
    public static final String PAGE_SIZE = "pageSize";
    public static final String PAGE_NUMBER = "pageNumber";
    public static final String READ_AS_STRING = "readAsString";
    public static final String SHEET_NAME = "sheetName";
    public static final String INCLUDE_EMPTY_CELLS = "includeEmptyCells";
    public static final String XLSX_FILE = "xlsxFile";
    public static final float VERSION_1_0 = 1.0f;
    public static final String WRITE = "write";
    public static final String READ = "read";

    public static final TaskDescriptor TASK_DESCRIPTOR = DSL.createTaskDescriptor(XLSX_FILE)
            .displayName("XLSX File")
            .description("Reads and writes data from a XLS/XLSX file.")
            .version(VERSION_1_0)
            .operations(
                    OPERATION(READ)
                            .displayName("Read from file")
                            .description("Reads data from a XLS/XLSX file.")
                            .inputs(
                                    FILE_ENTRY_PROPERTY(FILE_ENTRY)
                                            .displayName("File")
                                            .description(
                                                    "The object property which contains a reference to the XLS/XLSX file to read from.")
                                            .required(true),
                                    OPTIONS()
                                            .displayName("Options")
                                            .placeholder("Add Option")
                                            .options(
                                                    BOOLEAN_PROPERTY(PROPERTY_HEADER_ROW)
                                                            .displayName("Header Row")
                                                            .description(
                                                                    "The first row of the file contains the header names.")
                                                            .defaultValue(true),
                                                    BOOLEAN_PROPERTY(INCLUDE_EMPTY_CELLS)
                                                            .displayName("Include Empty Cells")
                                                            .description(
                                                                    "When reading from file the empty cells will be filled with an empty string.")
                                                            .defaultValue(false),
                                                    INTEGER_PROPERTY(PAGE_SIZE)
                                                            .displayName("Page Size")
                                                            .description(
                                                                    "The amount of child elements to return in a page."),
                                                    INTEGER_PROPERTY(PAGE_NUMBER)
                                                            .displayName("Page Number")
                                                            .description("The page number to get."),
                                                    BOOLEAN_PROPERTY(READ_AS_STRING)
                                                            .displayName("Read As String")
                                                            .description(
                                                                    "In some cases and file formats, it is necessary to read data specifically as string, otherwise some special characters are interpreted the wrong way.")
                                                            .defaultValue(false),
                                                    STRING_PROPERTY(SHEET_NAME)
                                                            .displayName("Sheet Name")
                                                            .description(
                                                                    "The name of the sheet to read from in the spreadsheet. If not set, the first one gets chosen.")
                                                            .defaultValue("Sheet")))
                            .outputs(ARRAY_PROPERTY()),
                    OPERATION(WRITE)
                            .displayName("Write to file")
                            .description("Writes the data to a XLS/XLSX file.")
                            .inputs(
                                    ARRAY_PROPERTY(ROWS)
                                            .displayName("Rows")
                                            .description("The array of objects to write to the file.")
                                            .required(true)
                                            .items(OBJECT_PROPERTY()
                                                    .additionalProperties(true)
                                                    .properties(
                                                            BOOLEAN_PROPERTY(),
                                                            DATE_TIME_PROPERTY(),
                                                            NUMBER_PROPERTY(),
                                                            STRING_PROPERTY())),
                                    OPTIONS()
                                            .displayName("Options")
                                            .placeholder("Add Option")
                                            .options(
                                                    STRING_PROPERTY(FILE_NAME)
                                                            .displayName("File Name")
                                                            .description(
                                                                    "File name to set for binary data. By default, \"file.xlsx\" will be used.")
                                                            .defaultValue(""),
                                                    STRING_PROPERTY(SHEET_NAME)
                                                            .displayName("Sheet Name")
                                                            .description(
                                                                    "The name of the sheet to create in the spreadsheet.")
                                                            .defaultValue("Sheet")))
                            .outputs(FILE_ENTRY_PROPERTY()));
}
