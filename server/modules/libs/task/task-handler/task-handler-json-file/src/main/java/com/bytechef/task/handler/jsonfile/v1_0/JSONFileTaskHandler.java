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
import com.bytechef.task.commons.json.JSONHelper;
import com.bytechef.task.handler.jsonfile.JSONFileTaskConstants;
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
public class JSONFileTaskHandler {

    @Component(JSONFileTaskConstants.JSON_FILE
            + "/"
            + JSONFileTaskConstants.VERSION_1_0
            + "/"
            + JSONFileTaskConstants.READ)
    public static class JSONFileReadTaskHandler implements TaskHandler<Object> {

        private final FileStorageService fileStorageService;
        private final JSONHelper jsonHelper;

        public JSONFileReadTaskHandler(FileStorageService fileStorageService, JSONHelper jsonHelper) {
            this.fileStorageService = fileStorageService;
            this.jsonHelper = jsonHelper;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object handle(TaskExecution taskExecution) throws Exception {
            Object result;

            JSONFileTaskConstants.FileType fileType =
                    JSONFileTaskConstants.FileType.valueOf(StringUtils.upperCase(taskExecution.get(
                            JSONFileTaskConstants.FILE_TYPE,
                            String.class,
                            JSONFileTaskConstants.FileType.JSON.name())));
            boolean isArray = taskExecution.get(JSONFileTaskConstants.IS_ARRAY, Boolean.class, true);
            FileEntry fileEntry = taskExecution.getRequired(JSONFileTaskConstants.FILE_ENTRY, FileEntry.class);

            if (isArray) {
                String path = taskExecution.get(JSONFileTaskConstants.PATH);
                InputStream inputStream = fileStorageService.getFileContentStream(fileEntry.getUrl());
                List<Map<String, ?>> items;

                if (fileType == JSONFileTaskConstants.FileType.JSON) {
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

                Integer pageSize = taskExecution.getInteger(JSONFileTaskConstants.PAGE_SIZE);
                Integer pageNumber = taskExecution.getInteger(JSONFileTaskConstants.PAGE_NUMBER);
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

    @Component(JSONFileTaskConstants.JSON_FILE
            + "/"
            + JSONFileTaskConstants.VERSION_1_0
            + "/"
            + JSONFileTaskConstants.WRITE)
    public static class JSONFileWriteTaskHandler implements TaskHandler<FileEntry> {

        private final FileStorageService fileStorageService;
        private final JSONHelper jsonHelper;

        public JSONFileWriteTaskHandler(FileStorageService fileStorageService, JSONHelper jsonHelper) {
            this.fileStorageService = fileStorageService;
            this.jsonHelper = jsonHelper;
        }

        @Override
        @SuppressWarnings("unchecked")
        public FileEntry handle(TaskExecution taskExecution) throws Exception {
            JSONFileTaskConstants.FileType fileType =
                    JSONFileTaskConstants.FileType.valueOf(StringUtils.upperCase(taskExecution.get(
                            JSONFileTaskConstants.FILE_TYPE,
                            String.class,
                            JSONFileTaskConstants.FileType.JSON.name())));
            String fileName =
                    taskExecution.get(JSONFileTaskConstants.FILE_NAME, String.class, getDefaultFileName(fileType));
            Object source = taskExecution.getRequired(JSONFileTaskConstants.SOURCE);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            if (fileType == JSONFileTaskConstants.FileType.JSON) {
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

        private String getDefaultFileName(JSONFileTaskConstants.FileType fileType) {
            return "file." + (fileType == JSONFileTaskConstants.FileType.JSON ? "json" : "jsonl");
        }
    }
}
