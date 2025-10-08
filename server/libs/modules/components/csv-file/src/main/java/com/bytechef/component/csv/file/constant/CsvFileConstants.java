/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.csv.file.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Property;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public class CsvFileConstants {

    public static final String DELIMITER = "delimiter";
    public static final String ENCLOSING_CHARACTER = "enclosingCharacter";
    public static final String FILE_ENTRY = "fileEntry";
    public static final String FILENAME = "filename";
    public static final String HEADER_ROW = "headerRow";
    public static final String INCLUDE_EMPTY_CELLS = "includeEmptyCells";
    public static final String PAGE_NUMBER = "pageNumber";
    public static final String PAGE_SIZE = "pageSize";
    public static final String READ_AS_STRING = "readAsString";
    public static final String ROWS = "rows";

    public static final CsvMapper CSV_MAPPER = new CsvMapper();

    public static final Property ROWS_PROPERTY = array(ROWS)
        .label("Rows")
        .description("The array of rows to append to the file.")
        .required(true)
        .placeholder("Add Row")
        .items(
            object()
                .placeholder("Add Column")
                .additionalProperties(bool(), dateTime(), number(), string()));

    @SuppressFBWarnings("MS")
    public static final Property[] READ_PROPERTIES = {
        fileEntry(FILE_ENTRY)
            .label("File Entry")
            .description("The object property which contains a reference to the csv file to read from.")
            .required(true),
        string(DELIMITER)
            .label("Delimiter")
            .description("Character used to separate values within the line red from the CSV file.")
            .defaultValue(",")
            .advancedOption(true),
        string(ENCLOSING_CHARACTER)
            .label("Enclosing Character")
            .description(
                "Character used to wrap/enclose values. It is usually applied to complex CSV files where values " +
                    "may include delimiter characters.")
            .placeholder("\" ' / ")
            .advancedOption(true),
        bool(HEADER_ROW)
            .label("Header Row")
            .description("The first row of the file contains the header names.")
            .defaultValue(true)
            .advancedOption(true),
        bool(INCLUDE_EMPTY_CELLS)
            .label("Include Empty Cells")
            .description("When reading from file the empty cells will be filled with an empty string.")
            .defaultValue(false)
            .advancedOption(true),
        integer(PAGE_SIZE)
            .label("Page Size")
            .description("The amount of child elements to return in a page.")
            .advancedOption(true),
        integer(PAGE_NUMBER)
            .label("Page Number")
            .description("The page number to get.")
            .advancedOption(true),
        bool(READ_AS_STRING)
            .label("Read as String")
            .description(
                "In some cases and file formats, it is necessary to read data specifically as string, " +
                    "otherwise some special characters are interpreted the wrong way.")
            .defaultValue(false)
            .advancedOption(true)
    };
}
