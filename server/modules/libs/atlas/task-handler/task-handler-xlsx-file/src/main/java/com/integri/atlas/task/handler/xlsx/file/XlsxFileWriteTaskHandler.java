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
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.PROPERTY_FILE_NAME;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.PROPERTY_SHEET_NAME;
import static com.integri.atlas.task.handler.xlsx.file.XlsxFileTaskConstants.TASK_XLSX_FILE;

import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.file.storage.dto.FileEntry;
import com.integri.atlas.file.storage.service.FileStorageService;
import com.integri.atlas.task.handler.commons.util.ValueUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
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
@Component(TASK_XLSX_FILE + "/write")
public class XlsxFileWriteTaskHandler implements TaskHandler<FileEntry> {

    private enum FileFormat {
        XLS,
        XLSX,
    }

    private final FileStorageService fileStorageService;

    public XlsxFileWriteTaskHandler(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public FileEntry handle(TaskExecution taskExecution) throws Exception {
        String fileName = taskExecution.get(PROPERTY_FILE_NAME, String.class, getaDefaultFileName());
        List<Map<String, ?>> rows = taskExecution.getRequired(PROPERTY_ROWS);

        String sheetName = taskExecution.get(PROPERTY_SHEET_NAME, String.class, "Sheet");

        return fileStorageService.storeFileContent(
            fileName,
            new ByteArrayInputStream(write(rows, new WriteConfiguration(fileName, sheetName)))
        );
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

    private record WriteConfiguration(String fileName, String sheetName) {}
}
