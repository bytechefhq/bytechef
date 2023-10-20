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

package com.integri.atlas.task.handler.spreadsheet.file;

import com.integri.atlas.engine.core.binary.Binary;
import com.integri.atlas.engine.core.binary.BinaryHelper;
import com.integri.atlas.engine.core.json.JSONHelper;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
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

    private final BinaryHelper binaryHelper;
    private final JSONHelper jsonHelper;

    public SpreadsheetFileTaskHandler(BinaryHelper binaryHelper, JSONHelper jsonHelper) {
        this.binaryHelper = binaryHelper;
        this.jsonHelper = jsonHelper;
    }

    @Override
    public Object handle(TaskExecution taskExecution) throws Exception {
        Object result;

        Operation operation = Operation.valueOf(StringUtils.upperCase(taskExecution.getRequired("operation")));

        if (operation == Operation.READ) {
            Binary binary = taskExecution.getRequired("binary", Binary.class);
            String delimiter = taskExecution.getString("delimiter", ",");
            boolean headerRow = taskExecution.getBoolean("headerRow", true);
            boolean includeEmptyCells = taskExecution.getBoolean("includeEmptyCells", false);
            boolean readAsString = taskExecution.getBoolean("readAsString", false);
            Map<String, Integer> range = taskExecution.get("range");
            String sheetName = taskExecution.get("sheetName", null);

            Integer rangeStartRow = null;

            if (range != null) {
                rangeStartRow = range.get("startRow");
            }

            Integer rangeEndRow = null;

            if (range != null) {
                rangeEndRow = range.get("endRow");
            }

            String extension = binary.getExtension();

            FileFormat fileFormat = FileFormat.valueOf(extension.toUpperCase());

            rangeStartRow = rangeStartRow == null ? 0 : rangeStartRow;
            rangeEndRow = rangeEndRow == null ? Integer.MAX_VALUE : rangeEndRow;

            SpreadsheetProcessor spreadsheetProcessor = getSpreadsheetProcessor(fileFormat);

            try (InputStream inputStream = binaryHelper.openDataInputStream(binary)) {
                result =
                    spreadsheetProcessor.read(
                        inputStream,
                        new SpreadsheetProcessor.ReadConfiguration(
                            delimiter,
                            headerRow,
                            includeEmptyCells,
                            rangeStartRow,
                            rangeEndRow,
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
            List<Map<String, ?>> items;

            if (taskExecution.containsKey("binary")) {
                Binary binary = taskExecution.get("binary", Binary.class);

                items = jsonHelper.read(binaryHelper.readBinaryData(binary));
            } else {
                items = taskExecution.get("items");
            }

            String sheetName = taskExecution.get("sheetName", String.class, "Sheet");

            SpreadsheetProcessor spreadsheetProcessor = getSpreadsheetProcessor(fileFormat);

            return binaryHelper.writeBinaryData(
                fileName,
                new ByteArrayInputStream(
                    spreadsheetProcessor.write(items, new SpreadsheetProcessor.WriteConfiguration(fileName, sheetName))
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
