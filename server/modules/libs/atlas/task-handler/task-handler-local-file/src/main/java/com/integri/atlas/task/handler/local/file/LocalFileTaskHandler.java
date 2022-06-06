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

package com.integri.atlas.task.handler.local.file;

import static com.integri.atlas.task.handler.local.file.LocalFileTaskConstants.PROPERTY_FILE_ENTRY;
import static com.integri.atlas.task.handler.local.file.LocalFileTaskConstants.PROPERTY_FILE_NAME;
import static com.integri.atlas.task.handler.local.file.LocalFileTaskConstants.TASK_LOCAL_FILE;

import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.file.storage.dto.FileEntry;
import com.integri.atlas.file.storage.service.FileStorageService;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
public class LocalFileTaskHandler {

    @Component(TASK_LOCAL_FILE + "/read")
    public static class LocalFileReadTaskHandler implements TaskHandler<FileEntry> {

        private final FileStorageService fileStorageService;

        public LocalFileReadTaskHandler(FileStorageService fileStorageService) {
            this.fileStorageService = fileStorageService;
        }

        @Override
        public FileEntry handle(TaskExecution taskExecution) throws Exception {
            String fileName = taskExecution.getRequired(PROPERTY_FILE_NAME);

            try (InputStream inputStream = new FileInputStream(fileName)) {
                return fileStorageService.storeFileContent(fileName, inputStream);
            }
        }
    }

    @Component(TASK_LOCAL_FILE + "/write")
    public static class LocalFileWriteTaskHandler implements TaskHandler<Map<String, Long>> {

        private final FileStorageService fileStorageService;

        public LocalFileWriteTaskHandler(FileStorageService fileStorageService) {
            this.fileStorageService = fileStorageService;
        }

        @Override
        public Map<String, Long> handle(TaskExecution taskExecution) throws Exception {
            String fileName = taskExecution.getRequired(PROPERTY_FILE_NAME);

            FileEntry fileEntry = taskExecution.getRequired(PROPERTY_FILE_ENTRY, FileEntry.class);

            try (InputStream inputStream = fileStorageService.getFileContentStream(fileEntry.getUrl())) {
                return Map.of("bytes", Files.copy(inputStream, Path.of(fileName), StandardCopyOption.REPLACE_EXISTING));
            }
        }
    }
}
