/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.integri.atlas.task.handler.spreadsheet.file.processor;

import com.integri.atlas.engine.core.util.MapUtil;
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
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.NumberToTextConverter;

/**
 * @author Ivica Cardic
 */
public class XLSSpreadsheetProcessor extends BaseSpreadsheetProcessor {

    public List<Map<String, ?>> read(InputStream inputStream, ReadConfiguration configuration) throws IOException {
        List<Map<String, ?>> items = new ArrayList<>();

        Workbook workbook = getWorkbook(inputStream);

        Sheet sheet;

        if (configuration.sheetName() == null) {
            sheet = workbook.getSheetAt(0);
        } else {
            sheet = workbook.getSheet(configuration.sheetName());
        }

        if (sheet.getLastRowNum() == 0) {
            return items;
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
                if (configuration.headerRow()) {
                    Map<String, Object> map = new HashMap<>();

                    for (int i = 0; i < lastColumn; i++) {
                        Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

                        map.computeIfAbsent(
                            headers.get(i),
                            key -> processValue(cell, configuration.includeEmptyCells(), configuration.readAsString())
                        );
                    }

                    items.add(map);
                } else {
                    List<Object> values = new ArrayList<>();

                    for (int i = 0; i < lastColumn; i++) {
                        Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

                        values.add(processValue(cell, configuration.includeEmptyCells(), configuration.readAsString()));
                    }

                    items.add(MapUtil.of(values));
                }
            } else {
                if (count >= configuration.rangeEndRow()) {
                    break;
                }
            }

            count++;
        }

        return items;
    }

    @Override
    public byte[] write(List<Map<String, ?>> items, WriteConfiguration configuration) throws IOException {
        boolean headerRow = false;
        Workbook workbook = getWorkbook();

        Sheet sheet = workbook.createSheet(configuration.sheetName());

        for (int i = 0; i < items.size(); i++) {
            Map<String, ?> item = items.get(i);

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

    protected Workbook getWorkbook() {
        return new HSSFWorkbook();
    }

    protected Workbook getWorkbook(InputStream inputStream) throws IOException {
        return new HSSFWorkbook(inputStream);
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
                            numericValue = getValueFromString(NumberToTextConverter.toText(cell.getNumericCellValue()));
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
}
