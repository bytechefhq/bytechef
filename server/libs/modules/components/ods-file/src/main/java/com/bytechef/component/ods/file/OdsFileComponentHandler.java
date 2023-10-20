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

package com.bytechef.component.ods.file;

import static com.bytechef.component.ods.file.constants.OdsFileConstants.HEADER_ROW;
import static com.bytechef.component.ods.file.constants.OdsFileConstants.INCLUDE_EMPTY_CELLS;
import static com.bytechef.component.ods.file.constants.OdsFileConstants.ODS_FILE;
import static com.bytechef.component.ods.file.constants.OdsFileConstants.PAGE_NUMBER;
import static com.bytechef.component.ods.file.constants.OdsFileConstants.PAGE_SIZE;
import static com.bytechef.component.ods.file.constants.OdsFileConstants.READ;
import static com.bytechef.component.ods.file.constants.OdsFileConstants.READ_AS_STRING;
import static com.bytechef.component.ods.file.constants.OdsFileConstants.ROWS;
import static com.bytechef.component.ods.file.constants.OdsFileConstants.SHEET_NAME;
import static com.bytechef.component.ods.file.constants.OdsFileConstants.WRITE;
import static com.bytechef.hermes.component.ComponentDSL.action;
import static com.bytechef.hermes.component.ComponentDSL.array;
import static com.bytechef.hermes.component.ComponentDSL.bool;
import static com.bytechef.hermes.component.ComponentDSL.createComponent;
import static com.bytechef.hermes.component.ComponentDSL.dateTime;
import static com.bytechef.hermes.component.ComponentDSL.display;
import static com.bytechef.hermes.component.ComponentDSL.fileEntry;
import static com.bytechef.hermes.component.ComponentDSL.integer;
import static com.bytechef.hermes.component.ComponentDSL.number;
import static com.bytechef.hermes.component.ComponentDSL.object;
import static com.bytechef.hermes.component.ComponentDSL.options;
import static com.bytechef.hermes.component.ComponentDSL.string;
import static com.bytechef.hermes.component.constants.ComponentConstants.FILENAME;
import static com.bytechef.hermes.component.constants.ComponentConstants.FILE_ENTRY;

import com.bytechef.commons.collection.MapUtils;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.exception.ActionExecutionException;
import com.github.miachm.sods.Range;
import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.ObjectUtils;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public class OdsFileComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition = createComponent(ODS_FILE)
            .display(display("ODS File").description("Reads and writes data from a ODS file."))
            .actions(
                    action(READ)
                            .display(display("Read from file").description("Reads data from a ODS file."))
                            .inputs(
                                    fileEntry(FILE_ENTRY)
                                            .label("File")
                                            .description(
                                                    "The object property which contains a reference to the ODS file to read from.")
                                            .required(true),
                                    options()
                                            .label("Options")
                                            .placeholder("Add Option")
                                            .options(
                                                    bool(HEADER_ROW)
                                                            .label("Header Row")
                                                            .description(
                                                                    "The first row of the file contains the header names.")
                                                            .defaultValue(true),
                                                    bool(INCLUDE_EMPTY_CELLS)
                                                            .label("Include Empty Cells")
                                                            .description(
                                                                    "When reading from file the empty cells will be filled with an empty string.")
                                                            .defaultValue(false),
                                                    integer(PAGE_SIZE)
                                                            .label("Page Size")
                                                            .description(
                                                                    "The amount of child elements to return in a page."),
                                                    integer(PAGE_NUMBER)
                                                            .label("Page Number")
                                                            .description("The page number to get."),
                                                    bool(READ_AS_STRING)
                                                            .label("Read As String")
                                                            .description(
                                                                    "In some cases and file formats, it is necessary to read data specifically as string, otherwise some special characters are interpreted the wrong way.")
                                                            .defaultValue(false),
                                                    string(SHEET_NAME)
                                                            .label("Sheet Name")
                                                            .description(
                                                                    "The name of the sheet to read from in the spreadsheet. If not set, the first one gets chosen.")
                                                            .defaultValue("Sheet")))
                            .outputSchema(array())
                            .performFunction(this::performRead),
                    action(WRITE)
                            .display(display("Write to file").description("Writes the data to a ODS file."))
                            .inputs(
                                    array(ROWS)
                                            .label("Rows")
                                            .description("The array of objects to write to the file.")
                                            .required(true)
                                            .items(object().additionalProperties(true)
                                                    .properties(bool(), dateTime(), number(), string())),
                                    options()
                                            .label("Options")
                                            .placeholder("Add Option")
                                            .options(
                                                    string(FILENAME)
                                                            .label("Filename")
                                                            .description(
                                                                    "Filename to set for binary data. By default, \"file.ods\" will be used.")
                                                            .required(true)
                                                            .defaultValue("file.ods"),
                                                    string(SHEET_NAME)
                                                            .label("Sheet Name")
                                                            .description(
                                                                    "The name of the sheet to create in the spreadsheet.")
                                                            .defaultValue("Sheet")))
                            .outputSchema(fileEntry())
                            .performFunction(this::performWrite));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    protected List<Map<String, ?>> performRead(Context context, ExecutionParameters executionParameters) {
        boolean headerRow = executionParameters.getBoolean(HEADER_ROW, true);
        boolean includeEmptyCells = executionParameters.getBoolean(INCLUDE_EMPTY_CELLS, false);
        Integer pageSize = executionParameters.getInteger(PAGE_SIZE);
        Integer pageNumber = executionParameters.getInteger(PAGE_NUMBER);
        boolean readAsString = executionParameters.getBoolean(READ_AS_STRING, false);
        String sheetName = executionParameters.getString(SHEET_NAME);

        try (InputStream inputStream = context.getFileStream(executionParameters.getFileEntry(FILE_ENTRY))) {
            if (inputStream == null) {
                throw new ActionExecutionException("Unable to get file content from task " + executionParameters);
            }

            Integer rangeStartRow = null;
            Integer rangeEndRow = null;

            if (pageSize != null && pageNumber != null) {
                rangeStartRow = pageSize * pageNumber - pageSize;

                rangeEndRow = rangeStartRow + pageSize;
            }

            return read(
                    inputStream,
                    new ReadConfiguration(
                            headerRow,
                            includeEmptyCells,
                            rangeStartRow == null ? 0 : rangeStartRow,
                            rangeEndRow == null ? Integer.MAX_VALUE : rangeEndRow,
                            readAsString,
                            sheetName));
        } catch (Exception exception) {
            throw new ActionExecutionException("Unable to handle task " + executionParameters, exception);
        }
    }

    protected FileEntry performWrite(Context context, ExecutionParameters executionParameters) {
        String fileName = executionParameters.getString(FILENAME, "file.ods");
        List<Map<String, ?>> rows = executionParameters.getRequiredList(ROWS);

        String sheetName = executionParameters.getString(SHEET_NAME, "Sheet");

        try {
            return context.storeFileContent(
                    fileName, new ByteArrayInputStream(write(rows, new WriteConfiguration(fileName, sheetName))));
        } catch (IOException ioException) {
            throw new ActionExecutionException("Unable to handle task " + executionParameters, ioException);
        }
    }

    private Object[] getHeaderValues(Set<String> names) {
        Objects.requireNonNull(names);

        if (names.isEmpty()) {
            throw new IllegalArgumentException("Unable to create header values with empty names collection");
        }

        Object[] values = new Object[names.size()];

        int idx = 0;

        for (Object value : names) {
            values[idx++] = value;
        }

        return values;
    }

    private List<Map<String, ?>> read(InputStream inputStream, ReadConfiguration configuration) throws IOException {
        List<Map<String, ?>> rows = new ArrayList<>();

        SpreadSheet spreadSheet = new SpreadSheet(inputStream);

        Sheet sheet;

        if (configuration.sheetName() == null) {
            sheet = spreadSheet.getSheet(0);
        } else {
            sheet = spreadSheet.getSheet(configuration.sheetName());
        }

        if (sheet.getMaxRows() == 0) {
            return rows;
        }

        int count = 0;
        List<String> headers = null;
        boolean firstRow = false;

        Range range = sheet.getDataRange();

        for (int i = 0; i <= range.getLastRow(); i++) {
            if (!firstRow) {
                firstRow = true;

                if (configuration.headerRow()) {
                    headers = new ArrayList<>();

                    for (int k = 0; k <= range.getLastColumn(); k++) {
                        Range cell = range.getCell(0, k);

                        headers.add((String) cell.getValue());
                    }

                    continue;
                }
            }

            if (count >= configuration.rangeStartRow() && count < configuration.rangeEndRow()) {
                if (configuration.headerRow()) {
                    Map<String, Object> map = new HashMap<>();

                    for (int j = 0; j <= range.getLastColumn(); j++) {
                        Range cell = range.getCell(i, j);

                        map.computeIfAbsent(
                                headers.get(j),
                                key -> processValue(
                                        cell.getValue(),
                                        configuration.includeEmptyCells(),
                                        configuration.readAsString()));
                    }

                    rows.add(map);
                } else {
                    List<Object> values = new ArrayList<>();

                    for (int j = 0; j <= range.getLastColumn(); j++) {
                        Range cell = range.getCell(i, j);

                        values.add(processValue(
                                cell.getValue(), configuration.includeEmptyCells(), configuration.readAsString()));
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

        return rows;
    }

    private Object processValue(Object value, boolean includeEmptyCells, boolean readAsString) {
        if (ObjectUtils.isEmpty(value)) {
            if (includeEmptyCells) {
                value = "";
            }
        } else {
            if (value instanceof LocalDate localDate) {
                value = localDate.toString();
            }

            if (readAsString) {
                value = String.valueOf(value);
            }
        }

        return value;
    }

    private byte[] write(List<Map<String, ?>> rows, WriteConfiguration configuration) throws IOException {
        Map<String, ?> rowMap = rows.get(0);

        Object[] headerValues = getHeaderValues(rowMap.keySet());
        Object[][] values = new Object[rows.size() + 1][headerValues.length];

        values[0] = headerValues;

        for (int i = 0; i < rows.size(); i++) {
            Map<String, ?> row = rows.get(i);

            for (int j = 0; j < headerValues.length; j++) {
                values[i + 1][j] = row.get(headerValues[j]);
            }
        }

        Sheet sheet = new Sheet(configuration.sheetName(), rows.size() + 1, headerValues.length);

        SpreadSheet spreadSheet = new SpreadSheet();

        spreadSheet.appendSheet(sheet);

        Range range = sheet.getDataRange();

        range.setValues(values);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        spreadSheet.save(byteArrayOutputStream);

        return byteArrayOutputStream.toByteArray();
    }

    private record WriteConfiguration(String fileName, String sheetName) {}

    private record ReadConfiguration(
            boolean headerRow,
            boolean includeEmptyCells,
            long rangeStartRow,
            long rangeEndRow,
            boolean readAsString,
            String sheetName) {}
}
