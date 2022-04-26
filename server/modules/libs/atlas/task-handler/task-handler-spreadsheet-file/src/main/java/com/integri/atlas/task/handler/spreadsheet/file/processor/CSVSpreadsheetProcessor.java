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

package com.integri.atlas.task.handler.spreadsheet.file.processor;

import com.integri.atlas.task.handler.spreadsheet.file.util.MapUtil;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class CSVSpreadsheetProcessor extends BaseSpreadsheetProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CSVSpreadsheetProcessor.class);

    @Override
    public List<Map<String, ?>> read(InputStream inputStream, ReadConfiguration configuration) throws IOException {
        List<Map<String, ?>> items = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            int count = 0;
            String[] headers = null;
            long lastColumn = 0;
            String line;
            boolean firstRow = false;

            while ((line = bufferedReader.readLine()) != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("row: {}", line);
                }

                String[] lineValues = line.split(configuration.delimiter());

                if (!firstRow) {
                    firstRow = true;

                    if (configuration.headerRow()) {
                        headers = lineValues;
                        lastColumn = lineValues.length;

                        continue;
                    } else {
                        lastColumn = lineValues.length;
                    }
                }

                if (count >= configuration.rangeStartRow() && count < configuration.rangeEndRow()) {
                    if (configuration.headerRow()) {
                        Map<String, Object> map = new HashMap<>();

                        for (int i = 0; i < lastColumn; i++) {
                            String value = (i == lineValues.length) ? null : lineValues[i];

                            map.computeIfAbsent(
                                headers[i],
                                key ->
                                    processValue(value, configuration.includeEmptyCells(), configuration.readAsString())
                            );
                        }

                        items.add(map);
                    } else {
                        List<Object> values = new ArrayList<>();

                        for (int i = 0; i < lastColumn; i++) {
                            values.add(
                                processValue(
                                    i == lineValues.length ? null : lineValues[i],
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
        }

        return items;
    }

    @Override
    public byte[] write(List<Map<String, ?>> items, WriteConfiguration configuration) {
        boolean headerRow = false;

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream)) {
            for (Map<String, ?> item : items) {
                List<String> fieldNames = new ArrayList<>(item.keySet());
                StringBuilder sb = new StringBuilder();

                if (!headerRow) {
                    headerRow = true;

                    for (int j = 0; j < fieldNames.size(); j++) {
                        sb.append(fieldNames.get(j));

                        if (j < fieldNames.size() - 1) {
                            sb.append(',');
                        }
                    }

                    printWriter.println(sb);
                }

                sb = new StringBuilder();

                for (int j = 0; j < fieldNames.size(); j++) {
                    sb.append(item.get(fieldNames.get(j)));

                    if (j < fieldNames.size() - 1) {
                        sb.append(',');
                    }
                }

                printWriter.println(sb);
            }
        }

        return byteArrayOutputStream.toByteArray();
    }

    private Object processValue(String valueString, boolean includeEmptyCells, boolean readAsString) {
        Object value = null;

        if (StringUtils.isEmpty(valueString)) {
            if (includeEmptyCells) {
                value = "";
            }
        } else {
            if (readAsString) {
                value = valueString;
            } else {
                value = getValueFromString(valueString);
            }
        }

        return value;
    }
}
