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

package com.bytechef.component.csvfile;

import static com.bytechef.component.csvfile.constants.CsvFileConstants.AGE_NUMBER;
import static com.bytechef.component.csvfile.constants.CsvFileConstants.CSV_FILE;
import static com.bytechef.component.csvfile.constants.CsvFileConstants.DELIMITER;
import static com.bytechef.component.csvfile.constants.CsvFileConstants.HEADER_ROW;
import static com.bytechef.component.csvfile.constants.CsvFileConstants.INCLUDE_EMPTY_CELLS;
import static com.bytechef.component.csvfile.constants.CsvFileConstants.PAGE_SIZE;
import static com.bytechef.component.csvfile.constants.CsvFileConstants.READ;
import static com.bytechef.component.csvfile.constants.CsvFileConstants.READ_AS_STRING;
import static com.bytechef.component.csvfile.constants.CsvFileConstants.ROWS;
import static com.bytechef.component.csvfile.constants.CsvFileConstants.WRITE;
import static com.bytechef.hermes.component.constants.ComponentConstants.FILENAME;
import static com.bytechef.hermes.component.constants.ComponentConstants.FILE_ENTRY;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.dateTime;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.number;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.commons.collection.MapUtils;
import com.bytechef.commons.lang.ValueUtils;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.exception.ActionExecutionException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class CsvFileComponentHandler implements ComponentHandler {

    private static final Logger logger = LoggerFactory.getLogger(CsvFileComponentHandler.class);

    public final ComponentDefinition componentDefinition = component(CSV_FILE)
            .display(display("CSV File").description("Reads and writes data from a csv file."))
            .actions(
                    action(READ)
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
                                            .defaultValue(","),
                                    bool(HEADER_ROW)
                                            .label("Header Row")
                                            .description("The first row of the file contains the header names.")
                                            .defaultValue(true),
                                    bool(INCLUDE_EMPTY_CELLS)
                                            .label("Include Empty Cells")
                                            .description(
                                                    "When reading from file the empty cells will be filled with an empty string.")
                                            .defaultValue(false),
                                    integer(PAGE_SIZE)
                                            .label("Page Size")
                                            .description("The amount of child elements to return in a page."),
                                    integer(AGE_NUMBER).label("Page Number").description("The page number to get."),
                                    bool(READ_AS_STRING)
                                            .label("Read As String")
                                            .description(
                                                    "In some cases and file formats, it is necessary to read data specifically as string, otherwise some special characters are interpreted the wrong way.")
                                            .defaultValue(false))
                            .output(array())
                            .perform(this::performRead),
                    action(WRITE)
                            .display(display("Write to file").description("Writes the data to a csv file."))
                            .properties(
                                    array(ROWS)
                                            .label("Rows")
                                            .description("The array of objects to write to the file.")
                                            .required(true)
                                            .items(ComponentDSL.object()
                                                    .additionalProperties(true)
                                                    .properties(bool(), dateTime(), number(), string())),
                                    string(FILENAME)
                                            .label("Filename")
                                            .description(
                                                    "Filename to set for binary data. By default, \"file.csv\" will be used.")
                                            .defaultValue(""))
                            .output(fileEntry())
                            .perform(this::performWrite));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    protected List<Map<String, Object>> performRead(Context context, ExecutionParameters executionParameters) {
        String delimiter = executionParameters.getString(DELIMITER, ",");
        boolean headerRow = executionParameters.getBoolean(HEADER_ROW, true);
        boolean includeEmptyCells = executionParameters.getBoolean(INCLUDE_EMPTY_CELLS, false);
        Integer pageSize = executionParameters.getInteger(PAGE_SIZE);
        Integer pageNumber = executionParameters.getInteger(AGE_NUMBER);
        boolean readAsString = executionParameters.getBoolean(READ_AS_STRING, false);

        try (InputStream inputStream = context.getFileStream(executionParameters.getFileEntry(FILE_ENTRY))) {
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
            throw new ActionExecutionException("Unable to stream CSV file", ioException);
        }
    }

    protected FileEntry performWrite(Context context, ExecutionParameters executionParameters) {
        List<Map<String, ?>> rows = executionParameters.getRequiredList(ROWS);

        return context.storeFileContent("file.csv", new ByteArrayInputStream(write(rows)));
    }

    private List<Map<String, Object>> read(InputStream inputStream, ReadConfiguration configuration)
            throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();

        try (BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            int count = 0;
            String[] headers = null;
            long lastColumn = 0;
            String line;
            boolean firstRow = false;

            while ((line = bufferedReader.readLine()) != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("row: {}", line);
                }

                String[] lineValues = line.split(configuration.delimiter());

                if (!firstRow) {
                    firstRow = true;

                    if (configuration.headerRow()) {
                        headers = lineValues;
                        lastColumn = lineValues.length;

                        continue;
                    } else {
                        lastColumn = lineValues.length;
                    }
                }

                if (count >= configuration.rangeStartRow() && count < configuration.rangeEndRow()) {
                    if (configuration.headerRow()) {
                        Map<String, Object> map = new HashMap<>();

                        for (int i = 0; i < lastColumn; i++) {
                            String value = (i == lineValues.length) ? null : lineValues[i];

                            map.computeIfAbsent(
                                    headers[i],
                                    key -> processValue(
                                            value, configuration.includeEmptyCells(), configuration.readAsString()));
                        }

                        rows.add(map);
                    } else {
                        List<Object> values = new ArrayList<>();

                        for (int i = 0; i < lastColumn; i++) {
                            values.add(processValue(
                                    i == lineValues.length ? null : lineValues[i],
                                    configuration.includeEmptyCells(),
                                    configuration.readAsString()));
                        }

                        rows.add(MapUtils.of(values));
                    }
                } else {
                    if (count >= configuration.rangeEndRow()) {
                        break;
                    }
                }

                count++;
            }
        }

        return rows;
    }

    private Object processValue(String valueString, boolean includeEmptyCells, boolean readAsString) {
        Object value = null;

        if (StringUtils.isEmpty(valueString)) {
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

    private byte[] write(List<Map<String, ?>> rows) {
        boolean headerRow = false;

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream, false, StandardCharsets.UTF_8)) {
            for (Map<String, ?> item : rows) {
                List<String> fieldNames = new ArrayList<>(item.keySet());
                StringBuilder sb = new StringBuilder();

                if (!headerRow) {
                    headerRow = true;

                    for (int j = 0; j < fieldNames.size(); j++) {
                        sb.append(fieldNames.get(j));

                        if (j < fieldNames.size() - 1) {
                            sb.append(',');
                        }
                    }

                    printWriter.println(sb);
                }

                sb = new StringBuilder();

                for (int j = 0; j < fieldNames.size(); j++) {
                    sb.append(item.get(fieldNames.get(j)));

                    if (j < fieldNames.size() - 1) {
                        sb.append(',');
                    }
                }

                printWriter.println(sb);
            }
        }

        return byteArrayOutputStream.toByteArray();
    }

    private record ReadConfiguration(
            String delimiter,
            boolean headerRow,
            boolean includeEmptyCells,
            long rangeStartRow,
            long rangeEndRow,
            boolean readAsString) {}
}
