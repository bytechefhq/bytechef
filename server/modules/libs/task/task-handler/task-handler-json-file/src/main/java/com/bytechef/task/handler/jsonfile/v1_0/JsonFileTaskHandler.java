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

import static com.bytechef.hermes.file.storage.FileStorageConstants.FILE_ENTRY;
import static com.bytechef.task.handler.jsonfile.JsonFileTaskConstants.FILE_TYPE;
import static com.bytechef.task.handler.jsonfile.JsonFileTaskConstants.FileType;
import static com.bytechef.task.handler.jsonfile.JsonFileTaskConstants.IS_ARRAY;
import static com.bytechef.task.handler.jsonfile.JsonFileTaskConstants.JSON_FILE;
import static com.bytechef.task.handler.jsonfile.JsonFileTaskConstants.PAGE_NUMBER;
import static com.bytechef.task.handler.jsonfile.JsonFileTaskConstants.PAGE_SIZE;
import static com.bytechef.task.handler.jsonfile.JsonFileTaskConstants.PATH;
import static com.bytechef.task.handler.jsonfile.JsonFileTaskConstants.READ;
import static com.bytechef.task.handler.jsonfile.JsonFileTaskConstants.SOURCE;
import static com.bytechef.task.handler.jsonfile.JsonFileTaskConstants.VERSION_1_0;
import static com.bytechef.task.handler.jsonfile.JsonFileTaskConstants.WRITE;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.hermes.file.storage.dto.FileEntry;
import com.bytechef.task.commons.file.storage.FileStorageHelper;
import com.bytechef.task.commons.json.JsonHelper;
import com.bytechef.task.handler.jsonfile.JsonFileTaskConstants.FileType;
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

    @Component(JSON_FILE + "/" + VERSION_1_0 + "/" + READ)
    public static class JsonFileReadTaskHandler implements TaskHandler<Object> {

        private final FileStorageHelper fileStorageHelper;
        private final JsonHelper jsonHelper;

        public JsonFileReadTaskHandler(FileStorageHelper fileStorageHelper, JsonHelper jsonHelper) {
            this.fileStorageHelper = fileStorageHelper;
            this.jsonHelper = jsonHelper;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object handle(TaskExecution taskExecution) throws Exception {
            Object result;

            FileType fileType = getFileType(taskExecution);
            boolean isArray = taskExecution.get(IS_ARRAY, Boolean.class, true);
            FileEntry fileEntry = taskExecution.getRequired(FILE_ENTRY, FileEntry.class);

            if (isArray) {
                String path = taskExecution.get(PATH);
                InputStream inputStream = fileStorageHelper.getFileContentStream(fileEntry);
                List<Map<String, ?>> items;

                if (fileType == FileType.JSON) {
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

                Integer pageSize = taskExecution.getInteger(PAGE_SIZE);
                Integer pageNumber = taskExecution.getInteger(PAGE_NUMBER);
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
                result = jsonHelper.read(fileStorageHelper.readFileContent(fileEntry), Map.class);
            }

            return result;
        }
    }

    @Component(JSON_FILE + "/" + VERSION_1_0 + "/" + WRITE)
    public static class JsonFileWriteTaskHandler implements TaskHandler<FileEntry> {

        private final FileStorageHelper fileStorageHelper;
        private final JsonHelper jsonHelper;

        public JsonFileWriteTaskHandler(FileStorageHelper fileStorageHelper, JsonHelper jsonHelper) {
            this.fileStorageHelper = fileStorageHelper;
            this.jsonHelper = jsonHelper;
        }

        @Override
        @SuppressWarnings("unchecked")
        public FileEntry handle(TaskExecution taskExecution) throws Exception {
            FileType fileType = getFileType(taskExecution);
            Object source = taskExecution.getRequired(SOURCE);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            if (fileType == FileType.JSON) {
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
                return fileStorageHelper.storeFileContent(taskExecution, getDefaultFileName(fileType), inputStream);
            }
        }

        private String getDefaultFileName(FileType fileType) {
            return "file." + (fileType == FileType.JSON ? "json" : "jsonl");
        }
    }

    private static FileType getFileType(TaskExecution taskExecution) {
        return FileType.valueOf(
                StringUtils.upperCase(taskExecution.get(FILE_TYPE, String.class, FileType.JSON.name())));
    }
}
