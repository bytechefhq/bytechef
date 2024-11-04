/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.csv.file.action;

import static com.bytechef.component.csv.file.constant.CsvFileConstants.DELIMITER;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.ENCLOSING_CHARACTER;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.FILE_ENTRY;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.HEADER_ROW;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.INCLUDE_EMPTY_CELLS;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.PAGE_NUMBER;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.PAGE_SIZE;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.READ_AS_STRING;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.csv.file.util.CsvFileReadUtils;
import com.bytechef.component.csv.file.util.ReadConfiguration;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.fasterxml.jackson.databind.MappingIterator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public class CsvFileReadAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("read")
        .title("Read from File")
        .description("Reads data from a csv file.")
        .properties(
            fileEntry(FILE_ENTRY)
                .label("File")
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
                    """
                            Character used to wrap/enclose values. It is usually applied to complex CSV files where
                            values may include delimiter characters.
                        """)
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
                .advancedOption(true))
        .output(outputSchema(array().items(object())))
        .perform(CsvFileReadAction::perform);

    protected static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) throws IOException {

        ReadConfiguration readConfiguration = CsvFileReadUtils.getReadConfiguration(inputParameters);

        try (
            InputStream inputStream = context.file(
                file -> file.getStream(inputParameters.getRequiredFileEntry(FILE_ENTRY)))) {

            return read(inputStream, readConfiguration, context);
        }
    }

    protected static List<Map<String, Object>> read(
        InputStream inputStream, ReadConfiguration configuration, Context context)
        throws IOException {

        List<Map<String, Object>> rows = new ArrayList<>();
        int count = 0;

        try (BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            char enclosingCharacter = CsvFileReadUtils.getEnclosingCharacter(configuration);

            MappingIterator<Object> iterator = CsvFileReadUtils.getIterator(bufferedReader, configuration);

            if (configuration.headerRow()) {
                while (iterator.hasNext()) {
                    Map<String, String> row = (Map<String, String>) iterator.nextValue();

                    context.logger(logger -> logger.trace("row: {}", row));

                    if (count >= configuration.rangeStartRow() && count < configuration.rangeEndRow()) {
                        Map<String, Object> map = CsvFileReadUtils.getHeaderRow(
                            configuration, context, row, enclosingCharacter);

                        rows.add(map);
                    } else {
                        if (count >= configuration.rangeEndRow()) {
                            break;
                        }
                    }

                    count++;
                }
            } else {
                while (iterator.hasNext()) {
                    List<String> row = (List<String>) iterator.nextValue();

                    context.logger(logger -> logger.trace("row: {}", row));

                    if (count >= configuration.rangeStartRow() && count < configuration.rangeEndRow()) {
                        Map<String, Object> map = CsvFileReadUtils.getColumnRow(
                            configuration, context, row, enclosingCharacter);

                        rows.add(map);
                    } else {
                        if (count >= configuration.rangeEndRow()) {
                            break;
                        }
                    }

                    count++;
                }
            }
        }

        return rows;
    }
}
