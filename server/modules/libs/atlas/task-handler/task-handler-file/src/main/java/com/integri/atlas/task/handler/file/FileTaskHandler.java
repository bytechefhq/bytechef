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

package com.integri.atlas.task.handler.file;

import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.file.storage.FileEntry;
import com.integri.atlas.file.storage.FileStorageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component("file")
public class FileTaskHandler implements TaskHandler<Object> {

    private enum Operation {
        READ,
        WRITE,
    }

    private final FileStorageService fileStorageService;

    public FileTaskHandler(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public Object handle(TaskExecution taskExecution) {
        Object result;

        Operation operation = Operation.valueOf(StringUtils.upperCase(taskExecution.getRequired("operation")));

        if (operation == Operation.READ) {
            FileEntry fileEntry = taskExecution.getRequired("fileEntry", FileEntry.class);

            result = fileStorageService.readFileContent(fileEntry.getUrl());
        } else {
            Object content = taskExecution.getRequired("content");
            String fileName = taskExecution.get("fileName", String.class, "file.txt");

            result =
                fileStorageService.storeFileContent(
                    fileName,
                    content instanceof String ? (String) content : content.toString()
                );
        }

        return result;
    }
}
