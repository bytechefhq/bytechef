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

package com.integri.atlas.task.handler.spreadsheet.file;

import static com.integri.atlas.task.handler.spreadsheet.file.SpreadsheetFileTaskConstants.*;
import static com.integri.atlas.task.handler.spreadsheet.file.SpreadsheetFileTaskConstants.FileFormat;
import static com.integri.atlas.task.handler.spreadsheet.file.SpreadsheetFileTaskConstants.Operation;
import static com.integri.atlas.task.handler.spreadsheet.file.SpreadsheetFileTaskConstants.PROPERTY_DELIMITER;
import static com.integri.atlas.task.handler.spreadsheet.file.SpreadsheetFileTaskConstants.PROPERTY_FILE_ENTRY;
import static com.integri.atlas.task.handler.spreadsheet.file.SpreadsheetFileTaskConstants.PROPERTY_FILE_FORMAT;
import static com.integri.atlas.task.handler.spreadsheet.file.SpreadsheetFileTaskConstants.PROPERTY_FILE_NAME;
import static com.integri.atlas.task.handler.spreadsheet.file.SpreadsheetFileTaskConstants.PROPERTY_HEADER_ROW;
import static com.integri.atlas.task.handler.spreadsheet.file.SpreadsheetFileTaskConstants.PROPERTY_INCLUDE_EMPTY_CELLS;
import static com.integri.atlas.task.handler.spreadsheet.file.SpreadsheetFileTaskConstants.PROPERTY_OPERATION;
import static com.integri.atlas.task.handler.spreadsheet.file.SpreadsheetFileTaskConstants.PROPERTY_PAGE_NUMBER;
import static com.integri.atlas.task.handler.spreadsheet.file.SpreadsheetFileTaskConstants.PROPERTY_PAGE_SIZE;
import static com.integri.atlas.task.handler.spreadsheet.file.SpreadsheetFileTaskConstants.PROPERTY_READ_AS_STRING;
import static com.integri.atlas.task.handler.spreadsheet.file.SpreadsheetFileTaskConstants.PROPERTY_SHEET_NAME;
import static com.integri.atlas.task.handler.spreadsheet.file.SpreadsheetFileTaskConstants.PROPERTY_TASK_SPREADSHEET_FILE;

import com.fasterxml.jackson.core.type.TypeReference;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.file.storage.FileEntry;
import com.integri.atlas.file.storage.FileStorageService;
import com.integri.atlas.task.handler.json.helper.JSONHelper;
import com.integri.atlas.task.handler.spreadsheet.file.processor.CSVSpreadsheetProcessor;
import com.integri.atlas.task.handler.spreadsheet.file.processor.ODSSpreadsheetProcessor;
import com.integri.atlas.task.handler.spreadsheet.file.processor.SpreadsheetProcessor;
import com.integri.atlas.task.handler.spreadsheet.file.processor.XLSSpreadsheetProcessor;
import com.integri.atlas.task.handler.spreadsheet.file.processor.XLSXSpreadsheetProcessor;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(PROPERTY_TASK_SPREADSHEET_FILE)
public class SpreadsheetFileTaskHandler implements TaskHandler<Object> {

    private final JSONHelper jsonHelper;
    private final FileStorageService fileStorageService;

    public SpreadsheetFileTaskHandler(JSONHelper jsonHelper, FileStorageService fileStorageService) {
        this.jsonHelper = jsonHelper;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public Object handle(TaskExecution taskExecution) throws Exception {
        Object result;

        Operation operation = Operation.valueOf(StringUtils.upperCase(taskExecution.getRequired(PROPERTY_OPERATION)));

        if (operation == Operation.READ) {
            String delimiter = taskExecution.getString(PROPERTY_DELIMITER, ",");
            FileEntry fileEntry = taskExecution.getRequired(PROPERTY_FILE_ENTRY, FileEntry.class);
            boolean headerRow = taskExecution.getBoolean(PROPERTY_HEADER_ROW, true);
            boolean includeEmptyCells = taskExecution.getBoolean(PROPERTY_INCLUDE_EMPTY_CELLS, false);
            Integer pageSize = taskExecution.get(PROPERTY_PAGE_SIZE);
            Integer pageNumber = taskExecution.get(PROPERTY_PAGE_NUMBER);
            boolean readAsString = taskExecution.getBoolean(PROPERTY_READ_AS_STRING, false);
            String sheetName = taskExecution.get(PROPERTY_SHEET_NAME, null);

            try (InputStream inputStream = fileStorageService.getFileContentStream(fileEntry.getUrl())) {
                String extension = fileEntry.getExtension();

                FileFormat fileFormat = FileFormat.valueOf(extension.toUpperCase());

                SpreadsheetProcessor spreadsheetProcessor = getSpreadsheetProcessor(fileFormat);

                Integer rangeStartRow = null;
                Integer rangeEndRow = null;

                if (pageSize != null && pageNumber != null) {
                    rangeStartRow = pageSize * pageNumber - pageSize;

                    rangeEndRow = rangeStartRow + pageSize;
                }

                result =
                    spreadsheetProcessor.read(
                        inputStream,
                        new SpreadsheetProcessor.ReadConfiguration(
                            delimiter,
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
            FileFormat fileFormat = FileFormat.valueOf(
                StringUtils.upperCase(taskExecution.getRequired(PROPERTY_FILE_FORMAT))
            );
            String fileName = taskExecution.get(PROPERTY_FILE_NAME, String.class, getaDefaultFileName(fileFormat));
            List<Map<String, ?>> input = jsonHelper.checkJSONArray(
                taskExecution.getRequired(PROPERTY_INPUT),
                new TypeReference<>() {}
            );

            String sheetName = taskExecution.get(PROPERTY_SHEET_NAME, String.class, "Sheet");

            SpreadsheetProcessor spreadsheetProcessor = getSpreadsheetProcessor(fileFormat);

            return fileStorageService.storeFileContent(
                fileName,
                new ByteArrayInputStream(
                    spreadsheetProcessor.write(input, new SpreadsheetProcessor.WriteConfiguration(fileName, sheetName))
                )
            );
        }

        return result;
    }

    private String getaDefaultFileName(FileFormat fileFormat) {
        return "spreadsheet." + StringUtils.lowerCase(fileFormat.name());
    }

    private SpreadsheetProcessor getSpreadsheetProcessor(FileFormat fileFormat) {
        return switch (fileFormat) {
            case CSV -> new CSVSpreadsheetProcessor();
            case ODS -> new ODSSpreadsheetProcessor();
            case XLS -> new XLSSpreadsheetProcessor();
            case XLSX -> new XLSXSpreadsheetProcessor();
        };
    }
}
