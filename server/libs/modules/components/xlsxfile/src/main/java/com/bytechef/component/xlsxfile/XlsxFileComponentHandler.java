
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

package com.bytechef.component.xlsxfile;

import static com.bytechef.component.xlsxfile.constants.XlsxFileConstants.FILENAME;
import static com.bytechef.component.xlsxfile.constants.XlsxFileConstants.FILE_ENTRY;
import static com.bytechef.component.xlsxfile.constants.XlsxFileConstants.HEADER_ROW;
import static com.bytechef.component.xlsxfile.constants.XlsxFileConstants.INCLUDE_EMPTY_CELLS;
import static com.bytechef.component.xlsxfile.constants.XlsxFileConstants.PAGE_NUMBER;
import static com.bytechef.component.xlsxfile.constants.XlsxFileConstants.PAGE_SIZE;
import static com.bytechef.component.xlsxfile.constants.XlsxFileConstants.READ_AS_STRING;
import static com.bytechef.component.xlsxfile.constants.XlsxFileConstants.ROWS;
import static com.bytechef.component.xlsxfile.constants.XlsxFileConstants.SHEET_NAME;
import static com.bytechef.component.xlsxfile.constants.XlsxFileConstants.WRITE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.definition.DefinitionDSL.oneOf;

import com.bytechef.component.xlsxfile.constants.XlsxFileConstants;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.exception.ActionExecutionException;
import com.bytechef.hermes.component.utils.ValueUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
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
public class XlsxFileComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition = component(XlsxFileConstants.XLSX_FILE)
        .display(display("XLSX File").description("Reads and writes data from a XLS/XLSX file."))
        .actions(
            action(XlsxFileConstants.READ)
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
                .output(array())
                .perform(this::performRead),
            action(WRITE)
                .display(display("Write to file").description("Writes the data to a XLS/XLSX file."))
                .properties(
                    array(ROWS)
                        .label("Rows")
                        .description("The array of objects to write to the file.")
                        .required(true)
                        .items(object().additionalProperties(oneOf())),
                    string(FILENAME)
                        .label("Filename")
                        .description(
                            "Filename to set for binary data. By default, \"file.xlsx\" will be used.")
                        .required(true)
                        .defaultValue("file.xlsx")
                        .advancedOption(true),
                    string(SHEET_NAME)
                        .label("Sheet Name")
                        .description("The name of the sheet to create in the spreadsheet.")
                        .defaultValue("Sheet")
                        .advancedOption(true))
                .output(fileEntry())
                .perform(this::performWrite));

    private enum FileFormat {
        XLS,
        XLSX,
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    protected List<Map<String, ?>> performRead(Context context, ExecutionParameters executionParameters) {
        FileEntry fileEntry = executionParameters.get(FILE_ENTRY, FileEntry.class);
        boolean headerRow = executionParameters.getBoolean(HEADER_ROW, true);
        boolean includeEmptyCells = executionParameters.getBoolean(INCLUDE_EMPTY_CELLS, false);
        Integer pageSize = executionParameters.getInteger(PAGE_SIZE);
        Integer pageNumber = executionParameters.getInteger(PAGE_NUMBER);
        boolean readAsString = executionParameters.getBoolean(READ_AS_STRING, false);
        String sheetName = executionParameters.getString(SHEET_NAME);

        try (InputStream inputStream = context.getFileStream(fileEntry)) {
            String extension = fileEntry.getExtension();

            FileFormat fileFormat = FileFormat.valueOf(extension.toUpperCase());

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
            throw new ActionExecutionException("Unable to handle task " + executionParameters, ioException);
        }
    }

    protected FileEntry performWrite(Context context, ExecutionParameters executionParameters) {
        String fileName = executionParameters.getString(FILENAME, getaDefaultFileName());
        @SuppressWarnings("unchecked")
        List<Map<String, ?>> rows = (List) executionParameters.getList(ROWS, Map.class, List.of());

        String sheetName = executionParameters.getString(SHEET_NAME, "Sheet");

        try {
            return context.storeFileContent(
                fileName, new ByteArrayInputStream(write(rows, new WriteConfiguration(fileName, sheetName))));
        } catch (IOException ioException) {
            throw new ActionExecutionException("Unable to handle task " + executionParameters, ioException);
        }
    }

    public List<Map<String, ?>> read(InputStream inputStream) throws IOException {
        return read(
            FileFormat.XLSX, inputStream, new ReadConfiguration(true, true, 0, Integer.MAX_VALUE, false, "Sheet"));
    }

    private String getaDefaultFileName() {
        return "file." + StringUtils.lowerCase(FileFormat.XLSX.name());
    }

    private Workbook getWorkbook() {
        return new XSSFWorkbook();
    }

    private Workbook getWorkbook(FileFormat fileFormat, InputStream inputStream) throws IOException {
        return fileFormat == FileFormat.XLS ? new HSSFWorkbook(inputStream) : new XSSFWorkbook(inputStream);
    }

    @SuppressWarnings("checkstyle:whitespaceafter")
    private Object processValue(Cell cell, boolean includeEmptyCells, boolean readAsString) {
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

    private List<Map<String, ?>> read(FileFormat fileFormat, InputStream inputStream, ReadConfiguration configuration)
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

    private byte[] write(List<Map<String, ?>> rows, WriteConfiguration configuration) throws IOException {
        boolean headerRow = false;
        Workbook workbook = getWorkbook();

        Sheet sheet = workbook.createSheet(configuration.sheetName());

        for (int i = 0; i < rows.size(); i++) {
            Map<String, ?> item = rows.get(i);

            if (!headerRow) {
                headerRow = true;

                int columnCount = 0;
                Row row = sheet.createRow(0);

                for (String fieldName : item.keySet()) {
                    Cell cell = row.createCell(columnCount++);

                    cell.setCellValue(fieldName);
                }
            }

            int columnCount = 0;
            Row row = sheet.createRow(i + 1);

            for (Object value : item.values()) {
                Cell cell = row.createCell(columnCount++);

                if (value instanceof Boolean) {
                    cell.setCellValue((Boolean) value);
                } else if (value instanceof Integer) {
                    cell.setCellValue((Integer) value);
                } else if (value instanceof Long) {
                    cell.setCellValue((Long) value);
                } else if (value instanceof Double) {
                    cell.setCellValue((Double) value);
                } else if (value instanceof BigDecimal) {
                    cell.setCellValue(((BigDecimal) value).doubleValue());
                } else {
                    cell.setCellValue((String) value);
                }
            }
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        workbook.write(byteArrayOutputStream);

        workbook.close();

        return byteArrayOutputStream.toByteArray();
    }

    private record ReadConfiguration(
        boolean headerRow,
        boolean includeEmptyCells,
        long rangeStartRow,
        long rangeEndRow,
        boolean readAsString,
        String sheetName) {
    }

    private record WriteConfiguration(String fileName, String sheetName) {
    }
}
