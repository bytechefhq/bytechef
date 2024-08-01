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

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.csv.file.constant.CsvFileConstants;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public class CsvFileReadAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CsvFileConstants.READ)
        .title("Read from file")
        .description("Reads data from a csv file.")
        .properties(
            fileEntry(CsvFileConstants.FILE_ENTRY)
                .label("File")
                .description(
                    "The object property which contains a reference to the csv file to read from.")
                .required(true),
            string(CsvFileConstants.DELIMITER)
                .label("Delimiter")
                .description("Character used to separate values within the line red from the CSV file.")
                .defaultValue(",")
                .advancedOption(true),
            string(CsvFileConstants.ENCLOSING_CHARACTER)
                .label("Enclosing character")
                .description(
                    """
                            Character used to wrap/enclose values. It is usually applied to complex CSV files where
                            values may include delimiter characters.
                        """)
                .placeholder("\" ' / ")
                .advancedOption(true),
            bool(CsvFileConstants.HEADER_ROW)
                .label("Header Row")
                .description("The first row of the file contains the header names.")
                .defaultValue(true)
                .advancedOption(true),
            bool(CsvFileConstants.INCLUDE_EMPTY_CELLS)
                .label("Include Empty Cells")
                .description(
                    "When reading from file the empty cells will be filled with an empty string.")
                .defaultValue(false)
                .advancedOption(true),
            integer(CsvFileConstants.PAGE_SIZE)
                .label("Page Size")
                .description("The amount of child elements to return in a page.")
                .advancedOption(true),
            integer(CsvFileConstants.PAGE_NUMBER)
                .label("Page Number")
                .description("The page number to get.")
                .advancedOption(true),
            bool(CsvFileConstants.READ_AS_STRING)
                .label("Read As String")
                .description(
                    "In some cases and file formats, it is necessary to read data specifically as string, otherwise some special characters are interpreted the wrong way.")
                .defaultValue(false)
                .advancedOption(true))
        .output()
        .perform(CsvFileReadAction::perform);

    protected static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) throws IOException {

        String delimiter = inputParameters.getString(CsvFileConstants.DELIMITER, ",");
        String enclosingCharacter = inputParameters.getString(CsvFileConstants.ENCLOSING_CHARACTER, "");
        boolean headerRow = inputParameters.getBoolean(CsvFileConstants.HEADER_ROW, true);
        boolean includeEmptyCells = inputParameters.getBoolean(CsvFileConstants.INCLUDE_EMPTY_CELLS, false);
        Integer pageNumber = inputParameters.getInteger(CsvFileConstants.PAGE_NUMBER);
        Integer pageSize = inputParameters.getInteger(CsvFileConstants.PAGE_SIZE);
        boolean readAsString = inputParameters.getBoolean(CsvFileConstants.READ_AS_STRING, false);

        try (
            InputStream inputStream = context.file(
                file -> file.getStream(inputParameters.getRequiredFileEntry(CsvFileConstants.FILE_ENTRY)))) {
            Integer rangeStartRow = null;
            Integer rangeEndRow = null;

            if (pageSize != null && pageNumber != null) {
                rangeStartRow = pageSize * pageNumber - pageSize;

                rangeEndRow = rangeStartRow + pageSize;
            }

            return read(
                inputStream,
                new ReadConfiguration(
                    delimiter, enclosingCharacter, headerRow, includeEmptyCells,
                    rangeStartRow == null ? 0 : rangeStartRow,
                    rangeEndRow == null ? Integer.MAX_VALUE : rangeEndRow, readAsString),
                context);
        }
    }

    protected static List<Map<String, Object>> read(
        InputStream inputStream, ReadConfiguration configuration, Context context)
        throws IOException {

        List<Map<String, Object>> rows = new ArrayList<>();
        int count = 0;

        try (BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            char enclosingCharacter = (char) -1;

            if (!StringUtils.isEmpty(configuration.enclosingCharacter)) {
                enclosingCharacter = configuration.enclosingCharacter.charAt(0);
            }

            if (configuration.headerRow()) {
                CsvSchema headerSchema = CsvSchema
                    .emptySchema()
                    .withHeader()
                    .withColumnSeparator(configuration.delimiter.charAt(0));

                MappingIterator<Map<String, String>> iterator = CsvFileConstants.CSV_MAPPER
                    .readerForMapOf(String.class)
                    .with(headerSchema)
                    .readValues(bufferedReader);

                while (iterator.hasNext()) {
                    Map<String, String> row = iterator.nextValue();

                    context.logger(logger -> logger.trace("row: {}", row));

                    if (count >= configuration.rangeStartRow() && count < configuration.rangeEndRow()) {
                        Map<String, Object> map = new LinkedHashMap<>();

                        for (Map.Entry<String, String> entry : row.entrySet()) {
                            map.put(
                                strip(entry.getKey(), enclosingCharacter),
                                processValue(
                                    entry.getValue(), enclosingCharacter, configuration.includeEmptyCells(),
                                    configuration.readAsString(),
                                    context));
                        }

                        rows.add(map);
                    } else {
                        if (count >= configuration.rangeEndRow()) {
                            break;
                        }
                    }

                    count++;
                }
            } else {
                MappingIterator<List<String>> iterator = CsvFileConstants.CSV_MAPPER
                    .readerForListOf(String.class)
                    .with(CsvParser.Feature.WRAP_AS_ARRAY)
                    .readValues(bufferedReader);

                while (iterator.hasNext()) {
                    List<String> row = iterator.nextValue();

                    context.logger(logger -> logger.trace("row: {}", row));

                    if (count >= configuration.rangeStartRow() && count < configuration.rangeEndRow()) {
                        Map<String, Object> map = new LinkedHashMap<>();

                        for (int i = 0; i < row.size(); i++) {
                            map.put(
                                "column_" + (i + 1),
                                processValue(
                                    row.get(i), enclosingCharacter, configuration.includeEmptyCells(),
                                    configuration.readAsString(),
                                    context));
                        }

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

    private static Object processValue(
        String valueString, char enclosingCharacter, boolean includeEmptyCells, boolean readAsString, Context context) {

        Object value = null;

        if (valueString == null || valueString.isEmpty()) {
            if (includeEmptyCells) {
                value = "";
            }
        } else {
            if (enclosingCharacter != (char) -1) {
                valueString = strip(valueString, enclosingCharacter);
            }

            if (readAsString) {
                value = valueString;
            } else {
                value = valueOF(valueString, context);
            }
        }

        return value;
    }

    private static String strip(String valueString, char enclosingCharacter) {
        valueString = valueString.strip();
        valueString = StringUtils.removeStart(valueString, enclosingCharacter);
        return StringUtils.removeEnd(valueString, String.valueOf(enclosingCharacter));
    }

    private static Object valueOF(String string, Context context) {
        Object value = null;

        try {
            value = Integer.parseInt(string);
        } catch (NumberFormatException nfe) {
            context.logger(logger -> logger.trace(nfe.getMessage(), nfe));
        }

        if (value == null) {
            try {
                value = Long.parseLong(string);
            } catch (NumberFormatException nfe) {
                context.logger(logger -> logger.trace(nfe.getMessage(), nfe));
            }
        }

        if (value == null) {
            try {
                value = Double.parseDouble(string);
            } catch (NumberFormatException nfe) {
                context.logger(logger -> logger.trace(nfe.getMessage(), nfe));
            }
        }

        if (value == null) {
            value = BooleanUtils.toBooleanObject(string);
        }

        if (value == null) {
            value = string;
        }

        return value;
    }

    protected record ReadConfiguration(
        String delimiter, String enclosingCharacter, boolean headerRow, boolean includeEmptyCells, long rangeStartRow,
        long rangeEndRow,
        boolean readAsString) {
    }
}
