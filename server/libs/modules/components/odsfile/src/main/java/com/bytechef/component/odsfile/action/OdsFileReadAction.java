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

package com.bytechef.component.odsfile.action;

import static com.bytechef.component.odsfile.constant.OdsFileConstants.FILE_ENTRY;
import static com.bytechef.component.odsfile.constant.OdsFileConstants.HEADER_ROW;
import static com.bytechef.component.odsfile.constant.OdsFileConstants.INCLUDE_EMPTY_CELLS;
import static com.bytechef.component.odsfile.constant.OdsFileConstants.PAGE_NUMBER;
import static com.bytechef.component.odsfile.constant.OdsFileConstants.PAGE_SIZE;
import static com.bytechef.component.odsfile.constant.OdsFileConstants.READ;
import static com.bytechef.component.odsfile.constant.OdsFileConstants.READ_AS_STRING;
import static com.bytechef.component.odsfile.constant.OdsFileConstants.SHEET_NAME;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.ActionOutputSchemaFunction;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.OutputSchemaResponse;
import com.bytechef.hermes.component.definition.ParameterMap;
import com.bytechef.hermes.component.definition.SampleOutputDataSource.ActionSampleOutputFunction;
import com.bytechef.hermes.component.definition.SampleOutputDataSource.SampleOutputResponse;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
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

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public class OdsFileReadAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(READ)
        .title("Read from file")
        .description("Reads data from a ODS file.")
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
        .outputSchema(getOutputSchemaFunction())
        .sampleOutput(getSampleOutputSchemaFunction())
        .perform(OdsFileReadAction::perform);

    protected static ActionOutputSchemaFunction getOutputSchemaFunction() {
        return (inputParameters, connectionParameters, context) -> new OutputSchemaResponse(
            context.outputSchema(outputSchema -> outputSchema.get(
                perform(inputParameters, connectionParameters, context))));
    }

    protected static ActionSampleOutputFunction getSampleOutputSchemaFunction() {
        return (inputParameters, connectionParameters, context) -> new SampleOutputResponse(
            perform(inputParameters, connectionParameters, context));
    }

    protected static Object perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext context) {

        boolean headerRow = inputParameters.getBoolean(HEADER_ROW, true);
        boolean includeEmptyCells = inputParameters.getBoolean(INCLUDE_EMPTY_CELLS, false);
        Integer pageSize = inputParameters.getInteger(PAGE_SIZE);
        Integer pageNumber = inputParameters.getInteger(PAGE_NUMBER);
        boolean readAsString = inputParameters.getBoolean(READ_AS_STRING, false);
        String sheetName = inputParameters.getString(SHEET_NAME);

        try (InputStream inputStream = context.file(
            file -> file.getStream(inputParameters.getRequiredFileEntry(FILE_ENTRY)))) {

            if (inputStream == null) {
                throw new ComponentExecutionException("Unable to get file content from task " + inputParameters);
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
            throw new ComponentExecutionException("Unable to handle action " + inputParameters, exception);
        }
    }

    protected static List<Map<String, ?>> read(InputStream inputStream, ReadConfiguration configuration)
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

    public static boolean isEmpty(final Object object) {
        if (object == null) {
            return true;
        }
        if (object instanceof CharSequence) {
            return ((CharSequence) object).length() == 0;
        }

        return false;
    }

    private static Object processValue(Object value, boolean includeEmptyCells, boolean readAsString) {
        if (isEmpty(value)) {
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
