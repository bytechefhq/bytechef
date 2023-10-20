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

import com.github.miachm.sods.Range;
import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;
import com.integri.atlas.engine.core.util.MapUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.ss.util.NumberToTextConverter;

/**
 * @author Ivica Cardic
 */
public class ODSSpreadsheetProcessor extends BaseSpreadsheetProcessor {

    @Override
    public List<Map<String, ?>> read(InputStream inputStream, ReadConfiguration configuration) throws IOException {
        List<Map<String, ?>> items = new ArrayList<>();

        SpreadSheet spreadSheet = new SpreadSheet(inputStream);

        Sheet sheet;

        if (configuration.sheetName() == null) {
            sheet = spreadSheet.getSheet(0);
        } else {
            sheet = spreadSheet.getSheet(configuration.sheetName());
        }

        if (sheet.getLastRow() == 0) {
            return items;
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
                            key ->
                                processValue(
                                    cell.getValue(),
                                    configuration.includeEmptyCells(),
                                    configuration.readAsString()
                                )
                        );
                    }

                    items.add(map);
                } else {
                    List<Object> values = new ArrayList<>();

                    for (int j = 0; j <= range.getLastColumn(); j++) {
                        Range cell = range.getCell(i, j);

                        values.add(
                            processValue(
                                cell.getValue(),
                                configuration.includeEmptyCells(),
                                configuration.readAsString()
                            )
                        );
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

        int columnCount;
        Sheet sheet = null;
        SpreadSheet spreadSheet = new SpreadSheet();
        Object[][] values = null;

        for (int i = 0; i < items.size(); i++) {
            Map<String, ?> item = items.get(i);

            Set<String> fieldNames = item.keySet();

            if (!headerRow) {
                headerRow = true;

                columnCount = fieldNames.size();

                sheet = new Sheet(configuration.sheetName(), items.size() + 1, columnCount);

                spreadSheet.appendSheet(sheet);

                values = new Object[items.size() + 1][columnCount];

                int column = 0;

                for (String fieldName : fieldNames) {
                    values[0][column++] = fieldName;
                }
            }

            int column = 0;

            for (String fieldName : fieldNames) {
                values[i + 1][column++] = item.get(fieldName);
            }
        }

        if (sheet != null) {
            Range range = sheet.getDataRange();

            range.setValues(values);
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        spreadSheet.save(byteArrayOutputStream);

        return byteArrayOutputStream.toByteArray();
    }

    private Object processValue(Object value, boolean includeEmptyCells, boolean readAsString) {
        if (ObjectUtils.isEmpty(value)) {
            if (includeEmptyCells) {
                value = "";
            }
        } else {
            if (value instanceof Number number) {
                value = getValueFromString(NumberToTextConverter.toText(number.doubleValue()));
            } else if (value instanceof LocalDate localDate) {
                value = localDate.toString();
            }

            if (readAsString) {
                value = String.valueOf(value);
            }
        }

        return value;
    }
}
