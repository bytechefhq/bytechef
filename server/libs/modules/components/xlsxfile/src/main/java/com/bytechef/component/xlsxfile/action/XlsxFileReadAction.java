
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

package com.bytechef.component.xlsxfile.action;

import com.bytechef.component.xlsxfile.constant.XlsxFileConstants;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.Parameters;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.exception.ActionExecutionException;
import com.bytechef.hermes.component.util.ObjectUtils;
import com.bytechef.hermes.component.util.ValueUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.bytechef.component.xlsxfile.constant.XlsxFileConstants.FILE_ENTRY;
import static com.bytechef.component.xlsxfile.constant.XlsxFileConstants.HEADER_ROW;
import static com.bytechef.component.xlsxfile.constant.XlsxFileConstants.INCLUDE_EMPTY_CELLS;
import static com.bytechef.component.xlsxfile.constant.XlsxFileConstants.PAGE_NUMBER;
import static com.bytechef.component.xlsxfile.constant.XlsxFileConstants.PAGE_SIZE;
import static com.bytechef.component.xlsxfile.constant.XlsxFileConstants.READ_AS_STRING;
import static com.bytechef.component.xlsxfile.constant.XlsxFileConstants.SHEET_NAME;
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
public class XlsxFileReadAction {

    public static final ActionDefinition ACTION_DEFINITION = action(XlsxFileConstants.READ)
        .display(display("Read from file").description("Reads data from a XLS/XLSX file."))
        .properties(
            fileEntry(FILE_ENTRY)
                .label("File")
                .description(
                    "The object property which contains a reference to the XLS/XLSX file to read from.")
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
        .perform(XlsxFileReadAction::performRead);

    public static List<Map<String, ?>> performRead(Context context, Parameters parameters) {
        FileEntry fileEntry = parameters.get(FILE_ENTRY, FileEntry.class);
        boolean headerRow = parameters.getBoolean(HEADER_ROW, true);
        boolean includeEmptyCells = parameters.getBoolean(INCLUDE_EMPTY_CELLS, false);
        Integer pageSize = parameters.getInteger(PAGE_SIZE);
        Integer pageNumber = parameters.getInteger(PAGE_NUMBER);
        boolean readAsString = parameters.getBoolean(READ_AS_STRING, false);
        String sheetName = parameters.getString(SHEET_NAME);

        try (InputStream inputStream = context.getFileStream(fileEntry)) {
            String extension = fileEntry.getExtension();

            XlsxFileConstants.FileFormat fileFormat = XlsxFileConstants.FileFormat.valueOf(extension.toUpperCase());

            Integer rangeStartRow = null;
            Integer rangeEndRow = null;

            if (pageSize != null && pageNumber != null) {
                rangeStartRow = pageSize * pageNumber - pageSize;

                rangeEndRow = rangeStartRow + pageSize;
            }

            return read(
                fileFormat,
                inputStream,
                new ReadConfiguration(
                    headerRow,
                    includeEmptyCells,
                    rangeStartRow == null ? 0 : rangeStartRow,
                    rangeEndRow == null ? Integer.MAX_VALUE : rangeEndRow,
                    readAsString,
                    sheetName));
        } catch (IOException ioException) {
            throw new ActionExecutionException("Unable to handle task " + parameters, ioException);
        }
    }

    private static Workbook getWorkbook(XlsxFileConstants.FileFormat fileFormat, InputStream inputStream)
        throws IOException {
        return fileFormat == XlsxFileConstants.FileFormat.XLS ? new HSSFWorkbook(inputStream)
            : new XSSFWorkbook(inputStream);
    }

    @SuppressWarnings("checkstyle:whitespaceafter")
    private static Object processValue(Cell cell, boolean includeEmptyCells, boolean readAsString) {
        Object value = null;

        if (cell != null) {
            value = switch (cell.getCellType()) {
                case BOOLEAN -> cell.getBooleanCellValue();
                case FORMULA -> cell.getCellFormula();
                case NUMERIC -> {
                    Object numericValue;

                    if (DateUtil.isCellDateFormatted(cell)) {
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

                        numericValue = formatter.format(cell.getDateCellValue());
                    } else {
                        numericValue = ValueUtils.valueOF(NumberToTextConverter.toText(cell.getNumericCellValue()));
                    }

                    yield numericValue;
                }
                case STRING -> cell.getStringCellValue();
                default -> throw new IllegalStateException("Unexpected value: " + cell.getCellType());
            };
        }

        if (ObjectUtils.isEmpty(value)) {
            if (includeEmptyCells) {
                value = "";
            }
        } else {
            if (readAsString) {
                value = String.valueOf(value);
            }
        }

        return value;
    }

    public static List<Map<String, ?>> read(
        XlsxFileConstants.FileFormat fileFormat, InputStream inputStream, ReadConfiguration configuration)
        throws IOException {
        List<Map<String, ?>> rows = new ArrayList<>();

        Workbook workbook = getWorkbook(fileFormat, inputStream);

        Sheet sheet;

        if (configuration.sheetName() == null) {
            sheet = workbook.getSheetAt(0);
        } else {
            sheet = workbook.getSheet(configuration.sheetName());
        }

        if (sheet.getLastRowNum() == 0) {
            return rows;
        }

        int count = 0;
        List<String> headers = null;
        long lastColumn = 0;
        boolean firstRow = false;

        for (Row row : sheet) {
            if (!firstRow) {
                firstRow = true;
                lastColumn = row.getLastCellNum();

                if (configuration.headerRow()) {
                    headers = new ArrayList<>();

                    Iterator<Cell> cellIterator = row.cellIterator();

                    while (cellIterator.hasNext()) {
                        Cell cell = cellIterator.next();

                        headers.add(cell.getStringCellValue());
                    }

                    continue;
                }
            }

            if (count >= configuration.rangeStartRow() && count < configuration.rangeEndRow()) {
                if (headers != null && configuration.headerRow()) {
                    Map<String, Object> map = new HashMap<>();

                    for (int i = 0; i < lastColumn; i++) {
                        Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

                        map.computeIfAbsent(
                            headers.get(i),
                            key -> processValue(
                                cell, configuration.includeEmptyCells(), configuration.readAsString()));
                    }

                    rows.add(map);
                } else {
                    Map<String, Object> map = new HashMap<>();

                    for (int i = 0; i < lastColumn; i++) {
                        Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

                        map.put(
                            "column_" + (i + 1),
                            processValue(cell, configuration.includeEmptyCells(), configuration.readAsString()));
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

    public record ReadConfiguration(
        boolean headerRow,
        boolean includeEmptyCells,
        long rangeStartRow,
        long rangeEndRow,
        boolean readAsString,
        String sheetName) {
    }
}
