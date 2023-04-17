
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

package com.bytechef.component.csvfile.action;

import com.bytechef.component.csvfile.CsvFileComponentHandler;
import com.bytechef.component.csvfile.constant.CsvFileConstants;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.component.util.ValueUtils;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.bytechef.component.csvfile.constant.CsvFileConstants.DELIMITER;
import static com.bytechef.component.csvfile.constant.CsvFileConstants.FILE_ENTRY;
import static com.bytechef.component.csvfile.constant.CsvFileConstants.HEADER_ROW;
import static com.bytechef.component.csvfile.constant.CsvFileConstants.INCLUDE_EMPTY_CELLS;
import static com.bytechef.component.csvfile.constant.CsvFileConstants.PAGE_NUMBER;
import static com.bytechef.component.csvfile.constant.CsvFileConstants.PAGE_SIZE;
import static com.bytechef.component.csvfile.constant.CsvFileConstants.READ;
import static com.bytechef.component.csvfile.constant.CsvFileConstants.READ_AS_STRING;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.bool;
import static com.bytechef.hermes.definition.DefinitionDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class CsvFileReadAction {

    private static final Logger logger = LoggerFactory.getLogger(CsvFileComponentHandler.class);

    public static final ActionDefinition ACTION_DEFINITION = action(READ)
        .display(display("Read from file").description("Reads data from a csv file."))
        .properties(
            fileEntry(FILE_ENTRY)
                .label("File")
                .description(
                    "The object property which contains a reference to the csv file to read from.")
                .required(true),
            string(DELIMITER)
                .label("Delimiter")
                .description("Delimiter to use when reading a csv file.")
                .defaultValue(",")
                .advancedOption(true),
            bool(HEADER_ROW)
                .label("Header Row")
                .description("The first row of the file contains the header names.")
                .defaultValue(true)
                .advancedOption(true),
            bool(INCLUDE_EMPTY_CELLS)
                .label("Include Empty Cells")
                .description(
                    "When reading from file the empty cells will be filled with an empty string.")
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
                .label("Read As String")
                .description(
                    "In some cases and file formats, it is necessary to read data specifically as string, otherwise some special characters are interpreted the wrong way.")
                .defaultValue(false)
                .advancedOption(true))
        .outputSchema(array())
        .execute(CsvFileReadAction::executeRead);

    public static List<Map<String, Object>> executeRead(Context context, InputParameters inputParameters) {
        String delimiter = inputParameters.getString(DELIMITER, ",");
        boolean headerRow = inputParameters.getBoolean(HEADER_ROW, true);
        boolean includeEmptyCells = inputParameters.getBoolean(INCLUDE_EMPTY_CELLS, false);
        Integer pageSize = inputParameters.getInteger(PAGE_SIZE);
        Integer pageNumber = inputParameters.getInteger(PAGE_NUMBER);
        boolean readAsString = inputParameters.getBoolean(READ_AS_STRING, false);

        try (
            InputStream inputStream = context
                .getFileStream(inputParameters.get(FILE_ENTRY, Context.FileEntry.class))) {
            Integer rangeStartRow = null;
            Integer rangeEndRow = null;

            if (pageSize != null && pageNumber != null) {
                rangeStartRow = pageSize * pageNumber - pageSize;

                rangeEndRow = rangeStartRow + pageSize;
            }

            return read(
                inputStream,
                new ReadConfiguration(
                    delimiter,
                    headerRow,
                    includeEmptyCells,
                    rangeStartRow == null ? 0 : rangeStartRow,
                    rangeEndRow == null ? Integer.MAX_VALUE : rangeEndRow,
                    readAsString));
        } catch (IOException ioException) {
            throw new ComponentExecutionException("Unable to stream CSV file", ioException);
        }
    }

    public static List<Map<String, Object>> read(InputStream inputStream, ReadConfiguration configuration)
        throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        int count = 0;

        try (BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            if (configuration.headerRow()) {
                CsvSchema headerSchema = CsvSchema.emptySchema()
                    .withHeader();

                MappingIterator<Map<String, String>> iterator = CsvFileConstants.CSV_MAPPER
                    .readerForMapOf(String.class)
                    .with(headerSchema)
                    .readValues(bufferedReader);

                while (iterator.hasNext()) {
                    Map<String, String> row = iterator.nextValue();

                    if (logger.isTraceEnabled()) {
                        logger.trace("row: {}", row);
                    }

                    if (count >= configuration.rangeStartRow() && count < configuration.rangeEndRow()) {
                        Map<String, Object> map = new LinkedHashMap<>();

                        for (Map.Entry<String, String> entry : row.entrySet()) {
                            map.put(
                                entry.getKey(),
                                processValue(
                                    entry.getValue(), configuration.includeEmptyCells(), configuration.readAsString()));
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

                    if (logger.isTraceEnabled()) {
                        logger.trace("row: {}", row);
                    }

                    if (count >= configuration.rangeStartRow() && count < configuration.rangeEndRow()) {
                        Map<String, Object> map = new LinkedHashMap<>();

                        for (int i = 0; i < row.size(); i++) {
                            map.put(
                                "column_" + (i + 1),
                                processValue(
                                    row.get(i), configuration.includeEmptyCells(), configuration.readAsString()));
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

    private static Object processValue(String valueString, boolean includeEmptyCells, boolean readAsString) {
        Object value = null;

        if (valueString == null || valueString.length() == 0) {
            if (includeEmptyCells) {
                value = "";
            }
        } else {
            if (readAsString) {
                value = valueString;
            } else {
                value = ValueUtils.valueOF(valueString);
            }
        }

        return value;
    }

    public record ReadConfiguration(
        String delimiter,
        boolean headerRow,
        boolean includeEmptyCells,
        long rangeStartRow,
        long rangeEndRow,
        boolean readAsString) {
    }
}
