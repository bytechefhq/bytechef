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

import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.PROPERTY_FILE_NAME;
import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.PROPERTY_ROWS;
import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.PROPERTY_SHEET_NAME;
import static com.integri.atlas.task.handler.ods.file.OdsFileTaskConstants.TASK_ODS_FILE;

import com.github.miachm.sods.Range;
import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.file.storage.dto.FileEntry;
import com.integri.atlas.file.storage.service.FileStorageService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(TASK_ODS_FILE + "/write")
public class OdsFileWriteTaskHandler implements TaskHandler<FileEntry> {

    private final FileStorageService fileStorageService;

    public OdsFileWriteTaskHandler(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public FileEntry handle(TaskExecution taskExecution) throws Exception {
        String fileName = taskExecution.get(PROPERTY_FILE_NAME, String.class, "file.ods");
        List<Map<String, ?>> rows = taskExecution.getRequired(PROPERTY_ROWS);

        String sheetName = taskExecution.get(PROPERTY_SHEET_NAME, String.class, "Sheet");

        return fileStorageService.storeFileContent(
            fileName,
            new ByteArrayInputStream(write(rows, new WriteConfiguration(fileName, sheetName)))
        );
    }

    private byte[] write(List<Map<String, ?>> rows, WriteConfiguration configuration) throws IOException {
        boolean headerRow = false;

        int columnCount;
        Sheet sheet = null;
        SpreadSheet spreadSheet = new SpreadSheet();
        Object[][] values = null;

        for (int i = 0; i < rows.size(); i++) {
            Map<String, ?> item = rows.get(i);

            Set<String> fieldNames = item.keySet();

            if (!headerRow) {
                headerRow = true;

                columnCount = fieldNames.size();

                sheet = new Sheet(configuration.sheetName(), rows.size() + 1, columnCount);

                spreadSheet.appendSheet(sheet);

                values = new Object[rows.size() + 1][columnCount];

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

    private record WriteConfiguration(String fileName, String sheetName) {}
}
