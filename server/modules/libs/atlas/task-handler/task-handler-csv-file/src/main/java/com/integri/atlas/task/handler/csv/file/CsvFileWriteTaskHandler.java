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

import static com.integri.atlas.task.handler.csv.file.CsvFileTaskConstants.PROPERTY_FILE_NAME;
import static com.integri.atlas.task.handler.csv.file.CsvFileTaskConstants.PROPERTY_ROWS;
import static com.integri.atlas.task.handler.csv.file.CsvFileTaskConstants.TASK_CSV_FILE;

import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.file.storage.dto.FileEntry;
import com.integri.atlas.file.storage.service.FileStorageService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(TASK_CSV_FILE + "/write")
public class CsvFileWriteTaskHandler implements TaskHandler<FileEntry> {

    private static final Logger logger = LoggerFactory.getLogger(CsvFileWriteTaskHandler.class);

    private final FileStorageService fileStorageService;

    public CsvFileWriteTaskHandler(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public FileEntry handle(TaskExecution taskExecution) throws Exception {
        String fileName = taskExecution.get(PROPERTY_FILE_NAME, String.class, "file.csv");
        List<Map<String, ?>> rows = taskExecution.getRequired(PROPERTY_ROWS);

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
