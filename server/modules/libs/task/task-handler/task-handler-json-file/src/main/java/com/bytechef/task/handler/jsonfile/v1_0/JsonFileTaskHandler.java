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

package com.bytechef.task.handler.jsonfile.v1_0;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.hermes.file.storage.dto.FileEntry;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import com.bytechef.task.commons.json.JsonHelper;
import com.bytechef.task.handler.jsonfile.JsonFileTaskConstants;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
public class JsonFileTaskHandler {

    @Component(JsonFileTaskConstants.JSON_FILE
            + "/"
            + JsonFileTaskConstants.VERSION_1_0
            + "/"
            + JsonFileTaskConstants.READ)
    public static class JsonFileReadTaskHandler implements TaskHandler<Object> {

        private final FileStorageService fileStorageService;
        private final JsonHelper jsonHelper;

        public JsonFileReadTaskHandler(FileStorageService fileStorageService, JsonHelper jsonHelper) {
            this.fileStorageService = fileStorageService;
            this.jsonHelper = jsonHelper;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object handle(TaskExecution taskExecution) throws Exception {
            Object result;

            JsonFileTaskConstants.FileType fileType =
                    JsonFileTaskConstants.FileType.valueOf(StringUtils.upperCase(taskExecution.get(
                            JsonFileTaskConstants.FILE_TYPE,
                            String.class,
                            JsonFileTaskConstants.FileType.JSON.name())));
            boolean isArray = taskExecution.get(JsonFileTaskConstants.IS_ARRAY, Boolean.class, true);
            FileEntry fileEntry = taskExecution.getRequired(JsonFileTaskConstants.FILE_ENTRY, FileEntry.class);

            if (isArray) {
                String path = taskExecution.get(JsonFileTaskConstants.PATH);
                InputStream inputStream = fileStorageService.getFileContentStream(fileEntry.getUrl());
                List<Map<String, ?>> items;

                if (fileType == JsonFileTaskConstants.FileType.JSON) {
                    if (path == null) {
                        try (Stream<Map<String, ?>> stream = jsonHelper.stream(inputStream)) {
                            items = stream.toList();
                        }
                    } else {
                        items = jsonHelper.read(inputStream, path);
                    }
                } else {
                    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                        items = bufferedReader
                                .lines()
                                .map(line -> (Map<String, ?>) jsonHelper.read(line, Map.class))
                                .collect(Collectors.toList());
                    }
                }

                Integer pageSize = taskExecution.getInteger(JsonFileTaskConstants.PAGE_SIZE);
                Integer pageNumber = taskExecution.getInteger(JsonFileTaskConstants.PAGE_NUMBER);
                Integer rangeStartIndex = null;
                Integer rangeEndIndex = null;

                if (pageSize != null && pageNumber != null) {
                    rangeStartIndex = pageSize * pageNumber - pageSize;

                    rangeEndIndex = rangeStartIndex + pageSize;
                }

                if ((rangeStartIndex != null && rangeStartIndex > 0)
                        || (rangeEndIndex != null && rangeEndIndex < items.size())) {
                    items = items.subList(rangeStartIndex, rangeEndIndex);
                }

                result = items;
            } else {
                result = jsonHelper.read(fileStorageService.readFileContent(fileEntry.getUrl()), Map.class);
            }

            return result;
        }
    }

    @Component(JsonFileTaskConstants.JSON_FILE
            + "/"
            + JsonFileTaskConstants.VERSION_1_0
            + "/"
            + JsonFileTaskConstants.WRITE)
    public static class JsonFileWriteTaskHandler implements TaskHandler<FileEntry> {

        private final FileStorageService fileStorageService;
        private final JsonHelper jsonHelper;

        public JsonFileWriteTaskHandler(FileStorageService fileStorageService, JsonHelper jsonHelper) {
            this.fileStorageService = fileStorageService;
            this.jsonHelper = jsonHelper;
        }

        @Override
        @SuppressWarnings("unchecked")
        public FileEntry handle(TaskExecution taskExecution) throws Exception {
            JsonFileTaskConstants.FileType fileType =
                    JsonFileTaskConstants.FileType.valueOf(StringUtils.upperCase(taskExecution.get(
                            JsonFileTaskConstants.FILE_TYPE,
                            String.class,
                            JsonFileTaskConstants.FileType.JSON.name())));
            String fileName =
                    taskExecution.get(JsonFileTaskConstants.FILE_NAME, String.class, getDefaultFileName(fileType));
            Object source = taskExecution.getRequired(JsonFileTaskConstants.SOURCE);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            if (fileType == JsonFileTaskConstants.FileType.JSON) {
                try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream)) {
                    printWriter.println(jsonHelper.write(source));
                }
            } else {
                try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream)) {
                    for (Map<String, ?> item : (List<Map<String, ?>>) source) {
                        printWriter.println(jsonHelper.write(item));
                    }
                }
            }

            try (InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray())) {
                return fileStorageService.storeFileContent(fileName, inputStream);
            }
        }

        private String getDefaultFileName(JsonFileTaskConstants.FileType fileType) {
            return "file." + (fileType == JsonFileTaskConstants.FileType.JSON ? "json" : "jsonl");
        }
    }
}
