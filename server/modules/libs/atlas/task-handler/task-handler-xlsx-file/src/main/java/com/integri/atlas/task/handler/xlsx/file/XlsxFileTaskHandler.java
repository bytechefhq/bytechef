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

package com.integri.atlas.task.handler.xlsx.file;

import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.*;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.Operation;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.PROPERTY_FILE_ENTRY;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.PROPERTY_FILE_NAME;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.PROPERTY_HEADER_ROW;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.PROPERTY_INCLUDE_EMPTY_CELLS;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.PROPERTY_PAGE_NUMBER;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.PROPERTY_PAGE_SIZE;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.PROPERTY_READ_AS_STRING;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.PROPERTY_SHEET_NAME;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.TASK_XLSX_FILE;

import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.file.storage.dto.FileEntry;
import com.integri.atlas.file.storage.service.FileStorageService;
import com.integri.atlas.task.handler.util.MapUtils;
import com.integri.atlas.task.handler.util.ValueUtils;
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
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(TASK_XLSX_FILE)
public class XlsxFileTaskHandler implements TaskHandler<Object> {

    private enum FileFormat {
        XLS,
        XLSX,
    }

    private final FileStorageService fileStorageService;

    public XlsxFileTaskHandler(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public Object handle(TaskExecution taskExecution) throws Exception {
        Object result;

        Operation operation = Operation.valueOf(StringUtils.upperCase(taskExecution.getRequired("operation")));

        if (operation == Operation.READ) {
            FileEntry fileEntry = taskExecution.getRequired(PROPERTY_FILE_ENTRY, FileEntry.class);
            boolean headerRow = taskExecution.getBoolean(PROPERTY_HEADER_ROW, true);
            boolean includeEmptyCells = taskExecution.getBoolean(PROPERTY_INCLUDE_EMPTY_CELLS, false);
            Integer pageSize = taskExecution.getInteger(PROPERTY_PAGE_SIZE);
            Integer pageNumber = taskExecution.getInteger(PROPERTY_PAGE_NUMBER);
            boolean readAsString = taskExecution.getBoolean(PROPERTY_READ_AS_STRING, false);
            String sheetName = taskExecution.get(PROPERTY_SHEET_NAME, null);

            try (InputStream inputStream = fileStorageService.getFileContentStream(fileEntry.getUrl())) {
                String extension = fileEntry.getExtension();

                FileFormat fileFormat = FileFormat.valueOf(extension.toUpperCase());

                Integer rangeStartRow = null;
                Integer rangeEndRow = null;

                if (pageSize != null && pageNumber != null) {
                    rangeStartRow = pageSize * pageNumber - pageSize;

                    rangeEndRow = rangeStartRow + pageSize;
                }

                result =
                    read(
                        fileFormat,
                        inputStream,
                        new ReadConfiguration(
                            headerRow,
                            includeEmptyCells,
                            rangeStartRow == null ? 0 : rangeStartRow,
                            rangeEndRow == null ? Integer.MAX_VALUE : rangeEndRow,
                            readAsString,
                            sheetName
                        )
                    );
            }
        } else {
            String fileName = taskExecution.get(PROPERTY_FILE_NAME, String.class, getaDefaultFileName());
            List<Map<String, ?>> rows = taskExecution.getRequired(PROPERTY_ROWS);

            String sheetName = taskExecution.get(PROPERTY_SHEET_NAME, String.class, "Sheet");

            return fileStorageService.storeFileContent(
                fileName,
                new ByteArrayInputStream(write(rows, new WriteConfiguration(fileName, sheetName)))
            );
        }

        return result;
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

    private Object processValue(Cell cell, boolean includeEmptyCells, boolean readAsString) {
        Object value = null;

        if (cell != null) {
            value =
                switch (cell.getCellType()) {
                    case BOOLEAN -> cell.getBooleanCellValue();
                    case FORMULA -> cell.getCellFormula();
                    case NUMERIC -> {
                        Object numericValue;

                        if (DateUtil.isCellDateFormatted(cell)) {
                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

                            numericValue = formatter.format(cell.getDateCellValue());
                        } else {
                            numericValue =
                                ValueUtils.getValueFromString(NumberToTextConverter.toText(cell.getNumericCellValue()));
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
                            key -> processValue(cell, configuration.includeEmptyCells(), configuration.readAsString())
                        );
                    }

                    rows.add(map);
                } else {
                    List<Object> values = new ArrayList<>();

                    for (int i = 0; i < lastColumn; i++) {
                        Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

                        values.add(processValue(cell, configuration.includeEmptyCells(), configuration.readAsString()));
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

            for (String key : item.keySet()) {
                Object value = item.get(key);

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
        String sheetName
    ) {}

    private record WriteConfiguration(String fileName, String sheetName) {}
}
