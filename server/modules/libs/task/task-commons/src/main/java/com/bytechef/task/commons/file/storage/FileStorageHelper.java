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

package com.bytechef.task.commons.file.storage;

import static com.bytechef.hermes.file.storage.FileStorageConstants.FILE_ENTRY;
import static com.bytechef.hermes.file.storage.FileStorageConstants.FILE_NAME;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.hermes.file.storage.dto.FileEntry;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import java.io.InputStream;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class FileStorageHelper {

    private final FileStorageService fileStorageService;

    public FileStorageHelper(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public InputStream getFileContentStream(TaskExecution taskExecution) {
        FileEntry fileEntry = taskExecution.getRequired(FILE_ENTRY, FileEntry.class);

        return fileStorageService.getFileContentStream(fileEntry.getUrl());
    }

    public InputStream getFileContentStream(FileEntry fileEntry) {
        return fileStorageService.getFileContentStream(fileEntry.getUrl());
    }

    public String readFileContent(FileEntry fileEntry) {
        return fileStorageService.readFileContent(fileEntry.getUrl());
    }

    public String readFileContent(TaskExecution taskExecution) {
        FileEntry fileEntry = taskExecution.getRequired(FILE_ENTRY, FileEntry.class);

        return fileStorageService.readFileContent(fileEntry.getUrl());
    }

    public FileEntry storeFileContent(String fileName, String content) {
        return fileStorageService.storeFileContent(fileName, content);
    }

    public FileEntry storeFileContent(String fileName, InputStream inputStream) {
        try {
            return fileStorageService.storeFileContent(fileName, inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public FileEntry storeFileContent(TaskExecution taskExecution, InputStream inputStream) {
        String fileName = taskExecution.getRequired(FILE_NAME);

        return storeFileContent(fileName, inputStream);
    }

    public FileEntry storeFileContent(TaskExecution taskExecution, String defaultFileName, InputStream inputStream) {
        String fileName = taskExecution.getString(FILE_NAME, defaultFileName);

        return storeFileContent(fileName, inputStream);
    }
}
