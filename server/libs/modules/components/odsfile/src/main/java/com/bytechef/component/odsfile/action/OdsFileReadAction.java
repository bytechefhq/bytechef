
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

package com.bytechef.component.odsfile.action;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.Parameters;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.exception.ActionExecutionException;
import com.bytechef.hermes.component.util.ObjectUtils;
import com.github.miachm.sods.Range;
import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bytechef.component.odsfile.constant.OdsFileConstants.FILE_ENTRY;
import static com.bytechef.component.odsfile.constant.OdsFileConstants.HEADER_ROW;
import static com.bytechef.component.odsfile.constant.OdsFileConstants.INCLUDE_EMPTY_CELLS;
import static com.bytechef.component.odsfile.constant.OdsFileConstants.PAGE_NUMBER;
import static com.bytechef.component.odsfile.constant.OdsFileConstants.PAGE_SIZE;
import static com.bytechef.component.odsfile.constant.OdsFileConstants.READ;
import static com.bytechef.component.odsfile.constant.OdsFileConstants.READ_AS_STRING;
import static com.bytechef.component.odsfile.constant.OdsFileConstants.SHEET_NAME;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.bool;
import static com.bytechef.hermes.definition.DefinitionDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public class OdsFileReadAction {

    public static final ActionDefinition ACTION_DEFINITION = action(READ)
        .display(display("Read from file").description("Reads data from a ODS file."))
        .properties(
            fileEntry(FILE_ENTRY)
                .label("File")
                .description(
                    "The object property which contains a reference to the ODS file to read from.")
                .required(true),
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
                .advancedOption(true),
            string(SHEET_NAME)
                .label("Sheet Name")
                .description(
                    "The name of the sheet to read from in the spreadsheet. If not set, the first one gets chosen.")
                .defaultValue("Sheet")
                .advancedOption(true))
        .outputSchema(array())
        .perform(OdsFileReadAction::performRead);

    public static List<Map<String, ?>> performRead(Context context, Parameters parameters) {
        boolean headerRow = parameters.getBoolean(HEADER_ROW, true);
        boolean includeEmptyCells = parameters.getBoolean(INCLUDE_EMPTY_CELLS, false);
        Integer pageSize = parameters.getInteger(PAGE_SIZE);
        Integer pageNumber = parameters.getInteger(PAGE_NUMBER);
        boolean readAsString = parameters.getBoolean(READ_AS_STRING, false);
        String sheetName = parameters.getString(SHEET_NAME);

        try (InputStream inputStream = context.getFileStream(parameters.get(FILE_ENTRY, FileEntry.class))) {
            if (inputStream == null) {
                throw new ActionExecutionException("Unable to get file content from task " + parameters);
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
            throw new ActionExecutionException("Unable to handle task " + parameters, exception);
        }
    }

    public static List<Map<String, ?>> read(InputStream inputStream, ReadConfiguration configuration)
        throws IOException {
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
                    Map<String, Object> map = new HashMap<>();

                    for (int j = 0; j <= range.getLastColumn(); j++) {
                        Range cell = range.getCell(i, j);

                        map.put(
                            "column_" + (j + 1),
                            processValue(
                                cell.getValue(),
                                configuration.includeEmptyCells(),
                                configuration.readAsString()));
                    }

                    rows.add(map);
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

    private static Object processValue(Object value, boolean includeEmptyCells, boolean readAsString) {
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

    public record ReadConfiguration(
        boolean headerRow,
        boolean includeEmptyCells,
        long rangeStartRow,
        long rangeEndRow,
        boolean readAsString,
        String sheetName) {
    }
}
