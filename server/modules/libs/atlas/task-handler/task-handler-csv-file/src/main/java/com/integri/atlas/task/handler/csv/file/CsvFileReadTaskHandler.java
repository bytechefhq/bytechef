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

package com.integri.atlas.task.handler.csv.file;

import static com.integri.atlas.task.handler.csv.file.CsvFileTaskConstants.PROPERTY_DELIMITER;
import static com.integri.atlas.task.handler.csv.file.CsvFileTaskConstants.PROPERTY_FILE_ENTRY;
import static com.integri.atlas.task.handler.csv.file.CsvFileTaskConstants.PROPERTY_HEADER_ROW;
import static com.integri.atlas.task.handler.csv.file.CsvFileTaskConstants.PROPERTY_INCLUDE_EMPTY_CELLS;
import static com.integri.atlas.task.handler.csv.file.CsvFileTaskConstants.PROPERTY_PAGE_NUMBER;
import static com.integri.atlas.task.handler.csv.file.CsvFileTaskConstants.PROPERTY_PAGE_SIZE;
import static com.integri.atlas.task.handler.csv.file.CsvFileTaskConstants.PROPERTY_READ_AS_STRING;
import static com.integri.atlas.task.handler.csv.file.CsvFileTaskConstants.TASK_CSV_FILE;

import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.file.storage.dto.FileEntry;
import com.integri.atlas.file.storage.service.FileStorageService;
import com.integri.atlas.task.handler.util.MapUtils;
import com.integri.atlas.task.handler.util.ValueUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(TASK_CSV_FILE + "/read")
public class CsvFileReadTaskHandler implements TaskHandler<List<Map<String, ?>>> {

    private static final Logger logger = LoggerFactory.getLogger(CsvFileReadTaskHandler.class);

    private final FileStorageService fileStorageService;

    public CsvFileReadTaskHandler(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public List<Map<String, ?>> handle(TaskExecution taskExecution) throws Exception {
        List<Map<String, ?>> result;

        String delimiter = taskExecution.getString(PROPERTY_DELIMITER, ",");
        FileEntry fileEntry = taskExecution.getRequired(PROPERTY_FILE_ENTRY, FileEntry.class);
        boolean headerRow = taskExecution.getBoolean(PROPERTY_HEADER_ROW, true);
        boolean includeEmptyCells = taskExecution.getBoolean(PROPERTY_INCLUDE_EMPTY_CELLS, false);
        Integer pageSize = taskExecution.getInteger(PROPERTY_PAGE_SIZE);
        Integer pageNumber = taskExecution.getInteger(PROPERTY_PAGE_NUMBER);
        boolean readAsString = taskExecution.getBoolean(PROPERTY_READ_AS_STRING, false);

        try (InputStream inputStream = fileStorageService.getFileContentStream(fileEntry.getUrl())) {
            Integer rangeStartRow = null;
            Integer rangeEndRow = null;

            if (pageSize != null && pageNumber != null) {
                rangeStartRow = pageSize * pageNumber - pageSize;

                rangeEndRow = rangeStartRow + pageSize;
            }

            result =
                read(
                    inputStream,
                    new ReadConfiguration(
                        delimiter,
                        headerRow,
                        includeEmptyCells,
                        rangeStartRow == null ? 0 : rangeStartRow,
                        rangeEndRow == null ? Integer.MAX_VALUE : rangeEndRow,
                        readAsString
                    )
                );
        }

        return result;
    }

    private List<Map<String, ?>> read(InputStream inputStream, ReadConfiguration configuration) throws IOException {
        List<Map<String, ?>> rows = new ArrayList<>();

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

                        rows.add(map);
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

                        rows.add(MapUtils.of(values));
                    }
                } else {
                    if (count >= configuration.rangeEndRow()) {
                        break;
                    }
                }

                count++;
            }
        }

        return rows;
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
                value = ValueUtils.getValueFromString(valueString);
            }
        }

        return value;
    }

    private record ReadConfiguration(
        String delimiter,
        boolean headerRow,
        boolean includeEmptyCells,
        long rangeStartRow,
        long rangeEndRow,
        boolean readAsString
    ) {}
}
