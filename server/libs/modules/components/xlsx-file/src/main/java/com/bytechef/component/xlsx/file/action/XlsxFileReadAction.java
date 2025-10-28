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

package com.bytechef.component.xlsx.file.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.xlsx.file.constant.XlsxFileConstants.FILE_ENTRY;
import static com.bytechef.component.xlsx.file.constant.XlsxFileConstants.HEADER_ROW;
import static com.bytechef.component.xlsx.file.constant.XlsxFileConstants.INCLUDE_EMPTY_CELLS;
import static com.bytechef.component.xlsx.file.constant.XlsxFileConstants.PAGE_NUMBER;
import static com.bytechef.component.xlsx.file.constant.XlsxFileConstants.PAGE_SIZE;
import static com.bytechef.component.xlsx.file.constant.XlsxFileConstants.READ_AS_STRING;
import static com.bytechef.component.xlsx.file.constant.XlsxFileConstants.SHEET_NAME;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.xlsx.file.constant.XlsxFileConstants.FileFormat;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * @author Ivica Cardic
 */
public class XlsxFileReadAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("read")
        .title("Read from File")
        .description("Reads data from a XLS/XLSX file.")
        .properties(
            fileEntry(FILE_ENTRY)
                .label("File Entry")
                .description("The object property which contains a reference to the XLS/XLSX file to read from.")
                .required(true),
            string(SHEET_NAME)
                .label("Sheet Name")
                .description(
                    "The name of the sheet to read from in the spreadsheet. If not set, the first one gets chosen.")
                .defaultValue("Sheet")
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
                .label("Read As String")
                .description(
                    "In some cases and file formats, it is necessary to read data specifically as string, otherwise " +
                        "some special characters are interpreted the wrong way.")
                .defaultValue(false)
                .advancedOption(true))
        .output()
        .perform(XlsxFileReadAction::perform);

    protected static List<Map<String, ?>> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) throws IOException {

        FileEntry fileEntry = inputParameters.getRequired(FILE_ENTRY, FileEntry.class);
        boolean headerRow = inputParameters.getBoolean(HEADER_ROW, true);
        boolean includeEmptyCells = inputParameters.getBoolean(INCLUDE_EMPTY_CELLS, false);
        Integer pageSize = inputParameters.getInteger(PAGE_SIZE);
        Integer pageNumber = inputParameters.getInteger(PAGE_NUMBER);
        boolean readAsString = inputParameters.getBoolean(READ_AS_STRING, false);
        String sheetName = inputParameters.getString(SHEET_NAME);

        try (InputStream inputStream = context.file(file -> file.getInputStream(fileEntry))) {
            String extension = fileEntry.getExtension();

            FileFormat fileFormat = FileFormat.valueOf(extension.toUpperCase());

            Integer rangeStartRow = null;
            Integer rangeEndRow = null;

            if (pageSize != null && pageNumber != null) {
                rangeStartRow = pageSize * pageNumber - pageSize;

                rangeEndRow = rangeStartRow + pageSize;
            }

            return read(
                fileFormat, inputStream,
                new ReadConfiguration(
                    headerRow, includeEmptyCells, rangeStartRow == null ? 0 : rangeStartRow,
                    rangeEndRow == null ? Integer.MAX_VALUE : rangeEndRow, readAsString, sheetName),
                context);
        }
    }

    protected static List<Map<String, ?>> read(
        FileFormat fileFormat, InputStream inputStream, ReadConfiguration configuration, Context context)
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
                                cell, configuration.includeEmptyCells(), configuration.readAsString(), context));
                    }

                    rows.add(map);
                } else {
                    Map<String, Object> map = new LinkedHashMap<>();

                    for (int i = 0; i < lastColumn; i++) {
                        Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

                        map.put(
                            "column_" + columnToLabel(i + 1),
                            processValue(
                                cell, configuration.includeEmptyCells(), configuration.readAsString(), context));
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

    private static Workbook getWorkbook(FileFormat fileFormat, InputStream inputStream)
        throws IOException {
        return fileFormat == FileFormat.XLS ? new HSSFWorkbook(inputStream)
            : new XSSFWorkbook(inputStream);
    }

    private static boolean isEmpty(final Object object) {
        if (object == null) {
            return true;
        }

        if (object instanceof CharSequence) {
            return ((CharSequence) object).length() == 0;
        }

        return false;
    }

    @SuppressWarnings("checkstyle:whitespaceafter")
    private static Object processValue(Cell cell, boolean includeEmptyCells, boolean readAsString, Context context) {
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
                        numericValue = valueOf(NumberToTextConverter.toText(cell.getNumericCellValue()), context);
                    }

                    yield numericValue;
                }
                case STRING -> cell.getStringCellValue();
                default -> throw new IllegalArgumentException("Unexpected value: %s".formatted(cell.getCellType()));
            };
        }

        if (isEmpty(value)) {
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

    private static Object valueOf(String string, Context context) {
        Object value = null;

        try {
            value = Integer.parseInt(string);
        } catch (NumberFormatException nfe) {
            context.log(log -> log.trace(nfe.getMessage(), nfe));
        }

        if (value == null) {
            try {
                value = Long.parseLong(string);
            } catch (NumberFormatException nfe) {
                context.log(log -> log.trace(nfe.getMessage(), nfe));
            }
        }

        if (value == null) {
            try {
                value = Double.parseDouble(string);
            } catch (NumberFormatException nfe) {
                context.log(log -> log.trace(nfe.getMessage(), nfe));
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

    private static String columnToLabel(int columnNumber) {
        StringBuilder columnName = new StringBuilder();

        while (columnNumber > 0) {
            int modulo = (columnNumber - 1) % 26;
            columnName.insert(0, (char) (65 + modulo));
            columnNumber = (columnNumber - modulo) / 26;
        }

        return columnName.toString();
    }

    protected record ReadConfiguration(
        boolean headerRow,
        boolean includeEmptyCells,
        long rangeStartRow,
        long rangeEndRow,
        boolean readAsString,
        String sheetName) {
    }
}
