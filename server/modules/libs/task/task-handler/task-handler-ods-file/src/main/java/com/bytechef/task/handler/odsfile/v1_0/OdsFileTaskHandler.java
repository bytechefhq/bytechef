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

package com.bytechef.task.handler.odsfile.v1_0;

import static com.bytechef.hermes.file.storage.FileStorageConstants.FILE_NAME;
import static com.bytechef.task.handler.odsfile.OdsFileTaskConstants.*;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.hermes.file.storage.dto.FileEntry;
import com.bytechef.task.commons.file.storage.FileStorageHelper;
import com.bytechef.task.commons.util.MapUtils;
import com.github.miachm.sods.Range;
import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public class OdsFileTaskHandler {

    @Component(ODS_FILE + "/" + VERSION_1_0 + "/" + READ)
    public static class OdsFileReadTaskHandler implements TaskHandler<List<Map<String, ?>>> {

        private final FileStorageHelper fileStorageHelper;

        public OdsFileReadTaskHandler(FileStorageHelper fileStorageHelper) {
            this.fileStorageHelper = fileStorageHelper;
        }

        @Override
        public List<Map<String, ?>> handle(TaskExecution taskExecution) throws TaskExecutionException {
            boolean headerRow = taskExecution.getBoolean(HEADER_ROW, true);
            boolean includeEmptyCells = taskExecution.getBoolean(INCLUDE_EMPTY_CELLS, false);
            Integer pageSize = taskExecution.getInteger(PAGE_SIZE);
            Integer pageNumber = taskExecution.getInteger(PAGE_NUMBER);
            boolean readAsString = taskExecution.getBoolean(READ_AS_STRING, false);
            String sheetName = taskExecution.get(SHEET_NAME, null);

            try (InputStream inputStream = fileStorageHelper.getFileContentStream(taskExecution)) {
                if (inputStream == null) {
                    throw new TaskExecutionException("Unable to get file content from task " + taskExecution);
                }

                Integer rangeStartRow = null;
                Integer rangeEndRow = null;

                if (pageSize != null && pageNumber != null) {
                    rangeStartRow = pageSize * pageNumber - pageSize;

                    rangeEndRow = rangeStartRow + pageSize;
                }

                return read(
                        inputStream,
                        new ReadConfiguration(
                                headerRow,
                                includeEmptyCells,
                                rangeStartRow == null ? 0 : rangeStartRow,
                                rangeEndRow == null ? Integer.MAX_VALUE : rangeEndRow,
                                readAsString,
                                sheetName));
            } catch (Exception exception) {
                throw new TaskExecutionException("Unable to handle task " + taskExecution, exception);
            }
        }

        private List<Map<String, ?>> read(InputStream inputStream, ReadConfiguration configuration) throws IOException {
            List<Map<String, ?>> rows = new ArrayList<>();

            SpreadSheet spreadSheet = new SpreadSheet(inputStream);

            Sheet sheet;

            if (configuration.sheetName() == null) {
                sheet = spreadSheet.getSheet(0);
            } else {
                sheet = spreadSheet.getSheet(configuration.sheetName());
            }

            if (sheet.getLastRow() == 0) {
                return rows;
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
                                    key -> processValue(
                                            cell.getValue(),
                                            configuration.includeEmptyCells(),
                                            configuration.readAsString()));
                        }

                        rows.add(map);
                    } else {
                        List<Object> values = new ArrayList<>();

                        for (int j = 0; j <= range.getLastColumn(); j++) {
                            Range cell = range.getCell(i, j);

                            values.add(processValue(
                                    cell.getValue(), configuration.includeEmptyCells(), configuration.readAsString()));
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

        private Object processValue(Object value, boolean includeEmptyCells, boolean readAsString) {
            if (ObjectUtils.isEmpty(value)) {
                if (includeEmptyCells) {
                    value = "";
                }
            } else {
                if (value instanceof LocalDate localDate) {
                    value = localDate.toString();
                }

                if (readAsString) {
                    value = String.valueOf(value);
                }
            }

            return value;
        }

        private record ReadConfiguration(
                boolean headerRow,
                boolean includeEmptyCells,
                long rangeStartRow,
                long rangeEndRow,
                boolean readAsString,
                String sheetName) {}
    }

    @Component(ODS_FILE + "/" + VERSION_1_0 + "/" + WRITE)
    public static class OdsFileWriteTaskHandler implements TaskHandler<FileEntry> {

        private final FileStorageHelper fileStorageHelper;

        public OdsFileWriteTaskHandler(FileStorageHelper fileStorageHelper) {
            this.fileStorageHelper = fileStorageHelper;
        }

        @Override
        public FileEntry handle(TaskExecution taskExecution) throws TaskExecutionException {
            String fileName = taskExecution.getString(FILE_NAME, "file.ods");
            List<Map<String, ?>> rows = taskExecution.getRequired(ROWS);

            String sheetName = taskExecution.get(SHEET_NAME, String.class, "Sheet");

            try {
                return fileStorageHelper.storeFileContent(
                        fileName, new ByteArrayInputStream(write(rows, new WriteConfiguration(fileName, sheetName))));
            } catch (IOException ioException) {
                throw new TaskExecutionException("Unable to handle task " + taskExecution, ioException);
            }
        }

        private Object[] getHeaderValues(Set<String> names) {
            Objects.requireNonNull(names);

            if (names.isEmpty()) {
                throw new IllegalArgumentException("Unable to create header values with empty names collection");
            }

            Object[] values = new Object[names.size()];

            int idx = 0;

            for (Object value : values) {
                values[idx++] = value;
            }

            return values;
        }

        private byte[] write(List<Map<String, ?>> rows, WriteConfiguration configuration) throws IOException {
            Map<String, ?> rowMap = rows.get(0);

            Object[] headerValues = getHeaderValues(rowMap.keySet());
            Object[][] values = new Object[rows.size() + 1][headerValues.length];

            values[0] = headerValues;

            for (int i = 0; i < rows.size(); i++) {
                Map<String, ?> row = rows.get(i);

                Collection<?> rowValues = row.values();

                int column = 0;

                for (Object rowValue : rowValues) {
                    values[i + 1][column++] = rowValue;
                }
            }

            Sheet sheet = new Sheet(configuration.sheetName(), rows.size() + 1, headerValues.length);

            SpreadSheet spreadSheet = new SpreadSheet();

            spreadSheet.appendSheet(sheet);

            Range range = sheet.getDataRange();

            range.setValues(values);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            spreadSheet.save(byteArrayOutputStream);

            return byteArrayOutputStream.toByteArray();
        }

        private record WriteConfiguration(String fileName, String sheetName) {}
    }
}
