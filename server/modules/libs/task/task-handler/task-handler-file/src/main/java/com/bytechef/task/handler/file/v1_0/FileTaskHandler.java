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

package com.bytechef.task.handler.file.v1_0;

import static com.bytechef.hermes.file.storage.FileStorageConstants.FILE_NAME;
import static com.bytechef.task.handler.file.FileTaskConstants.*;
import static com.bytechef.task.handler.file.FileTaskConstants.CONTENT;
import static com.bytechef.task.handler.file.FileTaskConstants.FILE;
import static com.bytechef.task.handler.file.FileTaskConstants.READ;
import static com.bytechef.task.handler.file.FileTaskConstants.WRITE;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.hermes.file.storage.dto.FileEntry;
import com.bytechef.task.commons.file.storage.FileStorageHelper;
import com.bytechef.task.handler.file.FileTaskConstants;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
public class FileTaskHandler {

    @Component(FILE + "/" + READ + "/" + VERSION)
    public static class FileReadTaskHandler implements TaskHandler<String> {

        private final FileStorageHelper fileStorageHelper;

        public FileReadTaskHandler(FileStorageHelper fileStorageHelper) {
            this.fileStorageHelper = fileStorageHelper;
        }

        @Override
        public String handle(TaskExecution taskExecution) {
            return fileStorageHelper.readFileContent(taskExecution);
        }
    }

    @Component(FILE + "/" + WRITE + "/" + VERSION)
    public static class FileWriteTaskHandler implements TaskHandler<Object> {

        private final FileStorageHelper fileStorageHelper;

        public FileWriteTaskHandler(FileStorageHelper fileStorageHelper) {
            this.fileStorageHelper = fileStorageHelper;
        }

        @Override
        public FileEntry handle(TaskExecution taskExecution) {
            Object content = taskExecution.getRequired(CONTENT);
            String fileName = taskExecution.get(FILE_NAME, String.class, "file.txt");

            return fileStorageHelper.storeFileContent(
                    fileName, content instanceof String ? (String) content : content.toString());
        }
    }
}
