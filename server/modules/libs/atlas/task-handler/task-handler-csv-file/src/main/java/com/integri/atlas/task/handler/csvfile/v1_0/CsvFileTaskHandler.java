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

package com.integri.atlas.task.handler.csvfile.v1_0;

import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.file.storage.dto.FileEntry;
import com.integri.atlas.file.storage.service.FileStorageService;
import com.integri.atlas.task.commons.util.MapUtils;
import com.integri.atlas.task.commons.util.ValueUtils;
import com.integri.atlas.task.handler.csvfile.CsvFileTaskConstants;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
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
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
class CsvFileTaskHandler {

    @Component(CsvFileTaskConstants.CSV_FILE + "/" + CsvFileTaskConstants.READ + "/" + CsvFileTaskConstants.VERSION)
    public static class CsvFileReadTaskHandler implements TaskHandler<List<Map<String, ?>>> {

        private static final Logger logger = LoggerFactory.getLogger(CsvFileReadTaskHandler.class);

        private final FileStorageService fileStorageService;

        public CsvFileReadTaskHandler(FileStorageService fileStorageService) {
            this.fileStorageService = fileStorageService;
        }

        @Override
        public List<Map<String, ?>> handle(TaskExecution taskExecution) throws Exception {
            List<Map<String, ?>> result;

            String delimiter = taskExecution.getString(CsvFileTaskConstants.DELIMITER, ",");
            FileEntry fileEntry = taskExecution.getRequired(CsvFileTaskConstants.FILE_ENTRY, FileEntry.class);
            boolean headerRow = taskExecution.getBoolean(CsvFileTaskConstants.HEADER_ROW, true);
            boolean includeEmptyCells = taskExecution.getBoolean(CsvFileTaskConstants.INCLUDE_EMPTY_CELLS, false);
            Integer pageSize = taskExecution.getInteger(CsvFileTaskConstants.PAGE_SIZE);
            Integer pageNumber = taskExecution.getInteger(CsvFileTaskConstants.AGE_NUMBER);
            boolean readAsString = taskExecution.getBoolean(CsvFileTaskConstants.READ_AS_STRING, false);

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
                                        processValue(
                                            value,
                                            configuration.includeEmptyCells(),
                                            configuration.readAsString()
                                        )
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

    @Component(CsvFileTaskConstants.CSV_FILE + "/" + CsvFileTaskConstants.WRITE + "/" + CsvFileTaskConstants.VERSION)
    public static class CsvFileWriteTaskHandler implements TaskHandler<FileEntry> {

        private static final Logger logger = LoggerFactory.getLogger(CsvFileWriteTaskHandler.class);

        private final FileStorageService fileStorageService;

        public CsvFileWriteTaskHandler(FileStorageService fileStorageService) {
            this.fileStorageService = fileStorageService;
        }

        @Override
        public FileEntry handle(TaskExecution taskExecution) throws Exception {
            String fileName = taskExecution.get(CsvFileTaskConstants.FILE_NAME, String.class, "file.csv");
            List<Map<String, ?>> rows = taskExecution.getRequired(CsvFileTaskConstants.ROWS);

            return fileStorageService.storeFileContent(fileName, new ByteArrayInputStream(write(rows)));
        }

        private byte[] write(List<Map<String, ?>> rows) {
            boolean headerRow = false;

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream)) {
                for (Map<String, ?> item : rows) {
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
    }
}
