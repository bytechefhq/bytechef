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

package com.bytechef.task.handler.localfile.v1_0;

import static com.bytechef.hermes.file.storage.FileStorageConstants.FILE_NAME;
import static com.bytechef.task.handler.localfile.LocalFileTaskConstants.LOCAL_FILE;
import static com.bytechef.task.handler.localfile.LocalFileTaskConstants.READ;
import static com.bytechef.task.handler.localfile.LocalFileTaskConstants.VERSION_1_0;
import static com.bytechef.task.handler.localfile.LocalFileTaskConstants.WRITE;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.hermes.file.storage.dto.FileEntry;
import com.bytechef.task.commons.file.storage.FileStorageHelper;
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

    @Component(LOCAL_FILE + "/" + VERSION_1_0 + "/" + READ)
    public static class LocalFileReadTaskHandler implements TaskHandler<FileEntry> {

        private final FileStorageHelper fileStorageHelper;

        public LocalFileReadTaskHandler(FileStorageHelper fileStorageHelper) {
            this.fileStorageHelper = fileStorageHelper;
        }

        @Override
        public FileEntry handle(TaskExecution taskExecution) throws Exception {
            try (InputStream inputStream = new FileInputStream(taskExecution.getRequiredString(FILE_NAME))) {
                return fileStorageHelper.storeFileContent(taskExecution, inputStream);
            }
        }
    }

    @Component(LOCAL_FILE + "/" + VERSION_1_0 + "/" + WRITE)
    public static class LocalFileWriteTaskHandler implements TaskHandler<Map<String, Long>> {

        private final FileStorageHelper fileStorageHelper;

        public LocalFileWriteTaskHandler(FileStorageHelper fileStorageHelper) {
            this.fileStorageHelper = fileStorageHelper;
        }

        @Override
        public Map<String, Long> handle(TaskExecution taskExecution) throws Exception {
            String fileName = taskExecution.getRequired(FILE_NAME);

            try (InputStream inputStream = fileStorageHelper.getFileContentStream(taskExecution)) {
                return Map.of("bytes", Files.copy(inputStream, Path.of(fileName), StandardCopyOption.REPLACE_EXISTING));
            }
        }
    }
}
