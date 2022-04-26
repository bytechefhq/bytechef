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

import com.fasterxml.jackson.core.type.TypeReference;
import com.integri.atlas.engine.core.json.JSONHelper;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.file.storage.FileEntry;
import com.integri.atlas.file.storage.FileStorageService;
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
@Component("spreadsheetFile")
public class SpreadsheetFileTaskHandler implements TaskHandler<Object> {

    private enum FileFormat {
        CSV,
        ODS,
        XLS,
        XLSX,
    }

    private enum Operation {
        READ,
        WRITE,
    }

    private final JSONHelper jsonHelper;
    private final FileStorageService fileStorageService;

    public SpreadsheetFileTaskHandler(JSONHelper jsonHelper, FileStorageService fileStorageService) {
        this.jsonHelper = jsonHelper;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public Object handle(TaskExecution taskExecution) throws Exception {
        Object result;

        Operation operation = Operation.valueOf(StringUtils.upperCase(taskExecution.getRequired("operation")));

        if (operation == Operation.READ) {
            String delimiter = taskExecution.getString("delimiter", ",");
            FileEntry fileEntry = taskExecution.getRequired("fileEntry", FileEntry.class);
            boolean headerRow = taskExecution.getBoolean("headerRow", true);
            boolean includeEmptyCells = taskExecution.getBoolean("includeEmptyCells", false);
            Integer pageSize = taskExecution.get("pageSize");
            Integer pageNumber = taskExecution.get("pageNumber");
            boolean readAsString = taskExecution.getBoolean("readAsString", false);
            String sheetName = taskExecution.get("sheetName", null);

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
            FileFormat fileFormat = FileFormat.valueOf(StringUtils.upperCase(taskExecution.getRequired("fileFormat")));
            String fileName = taskExecution.get(
                "fileName",
                String.class,
                "spreadsheet." + StringUtils.lowerCase(fileFormat.name())
            );
            List<Map<String, ?>> input = jsonHelper.checkJSONArray(
                taskExecution.getRequired("input"),
                new TypeReference<>() {}
            );

            String sheetName = taskExecution.get("sheetName", String.class, "Sheet");

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

    private SpreadsheetProcessor getSpreadsheetProcessor(FileFormat fileFormat) {
        return switch (fileFormat) {
            case CSV -> new CSVSpreadsheetProcessor();
            case ODS -> new ODSSpreadsheetProcessor();
            case XLS -> new XLSSpreadsheetProcessor();
            case XLSX -> new XLSXSpreadsheetProcessor();
        };
    }
}
