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

package com.integri.atlas.task.handler.ods.file;

import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.PROPERTY_FILE_ENTRY;
import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.PROPERTY_HEADER_ROW;
import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.PROPERTY_INCLUDE_EMPTY_CELLS;
import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.PROPERTY_PAGE_NUMBER;
import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.PROPERTY_PAGE_SIZE;
import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.PROPERTY_READ_AS_STRING;
import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.PROPERTY_SHEET_NAME;
import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.TASK_ODS_FILE;

import com.github.miachm.sods.Range;
import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.file.storage.dto.FileEntry;
import com.integri.atlas.file.storage.service.FileStorageService;
import com.integri.atlas.task.handler.util.MapUtils;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(TASK_ODS_FILE + "/read")
public class OdsFileReadTaskHandler implements TaskHandler<List<Map<String, ?>>> {

    private final FileStorageService fileStorageService;

    public OdsFileReadTaskHandler(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public List<Map<String, ?>> handle(TaskExecution taskExecution) throws Exception {
        FileEntry fileEntry = taskExecution.getRequired(PROPERTY_FILE_ENTRY, FileEntry.class);
        boolean headerRow = taskExecution.getBoolean(PROPERTY_HEADER_ROW, true);
        boolean includeEmptyCells = taskExecution.getBoolean(PROPERTY_INCLUDE_EMPTY_CELLS, false);
        Integer pageSize = taskExecution.getInteger(PROPERTY_PAGE_SIZE);
        Integer pageNumber = taskExecution.getInteger(PROPERTY_PAGE_NUMBER);
        boolean readAsString = taskExecution.getBoolean(PROPERTY_READ_AS_STRING, false);
        String sheetName = taskExecution.get(PROPERTY_SHEET_NAME, null);

        try (InputStream inputStream = fileStorageService.getFileContentStream(fileEntry.getUrl())) {
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
                    sheetName
                )
            );
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
                            key ->
                                processValue(
                                    cell.getValue(),
                                    configuration.includeEmptyCells(),
                                    configuration.readAsString()
                                )
                        );
                    }

                    rows.add(map);
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
        String sheetName
    ) {}
}
