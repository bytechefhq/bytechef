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

package com.integri.atlas.task.handler.json.file;

import static com.integri.atlas.task.handler.json.file.JSONFileTaskConstants.PROPERTY_FILE_ENTRY;
import static com.integri.atlas.task.handler.json.file.JSONFileTaskConstants.PROPERTY_FILE_NAME;
import static com.integri.atlas.task.handler.json.file.JSONFileTaskConstants.PROPERTY_FILE_TYPE;
import static com.integri.atlas.task.handler.json.file.JSONFileTaskConstants.PROPERTY_IS_ARRAY;
import static com.integri.atlas.task.handler.json.file.JSONFileTaskConstants.PROPERTY_OPERATION;
import static com.integri.atlas.task.handler.json.file.JSONFileTaskConstants.PROPERTY_PAGE_NUMBER;
import static com.integri.atlas.task.handler.json.file.JSONFileTaskConstants.PROPERTY_PAGE_SIZE;
import static com.integri.atlas.task.handler.json.file.JSONFileTaskConstants.PROPERTY_PATH;
import static com.integri.atlas.task.handler.json.file.JSONFileTaskConstants.TASK_JSON_FILE;

import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.file.storage.FileEntry;
import com.integri.atlas.file.storage.FileStorageService;
import com.integri.atlas.task.handler.json.file.JSONFileTaskConstants.FileType;
import com.integri.atlas.task.handler.json.helper.JSONHelper;
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
@Component(TASK_JSON_FILE)
public class JSONFileTaskHandler implements TaskHandler<Object> {

    private final FileStorageService fileStorageService;
    private final JSONHelper jsonHelper;

    public JSONFileTaskHandler(FileStorageService fileStorageService, JSONHelper jsonHelper) {
        this.fileStorageService = fileStorageService;
        this.jsonHelper = jsonHelper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object handle(TaskExecution taskExecution) throws Exception {
        Object result;

        FileType fileType = FileType.valueOf(
            StringUtils.upperCase(taskExecution.get(PROPERTY_FILE_TYPE, String.class, FileType.JSON.name()))
        );
        JSONFileTaskConstants.Operation operation = JSONFileTaskConstants.Operation.valueOf(
            StringUtils.upperCase(taskExecution.getRequired(PROPERTY_OPERATION))
        );

        if (operation == JSONFileTaskConstants.Operation.READ) {
            boolean isArray = taskExecution.get(PROPERTY_IS_ARRAY, Boolean.class, true);
            FileEntry fileEntry = taskExecution.getRequired(PROPERTY_FILE_ENTRY, FileEntry.class);

            if (isArray) {
                String path = taskExecution.get(PROPERTY_PATH);
                InputStream inputStream = fileStorageService.getFileContentStream(fileEntry.getUrl());
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
                        items =
                            bufferedReader
                                .lines()
                                .map(line -> (Map<String, ?>) jsonHelper.read(line, Map.class))
                                .collect(Collectors.toList());
                    }
                }

                Integer pageSize = taskExecution.get(PROPERTY_PAGE_SIZE);
                Integer pageNumber = taskExecution.get(PROPERTY_PAGE_NUMBER);
                Integer rangeStartIndex = null;
                Integer rangeEndIndex = null;

                if (pageSize != null && pageNumber != null) {
                    rangeStartIndex = pageSize * pageNumber - pageSize;

                    rangeEndIndex = rangeStartIndex + pageSize;
                }

                if (
                    (rangeStartIndex != null && rangeStartIndex > 0) ||
                    (rangeEndIndex != null && rangeEndIndex < items.size())
                ) {
                    items = items.subList(rangeStartIndex, rangeEndIndex);
                }

                result = items;
            } else {
                result = jsonHelper.read(fileStorageService.readFileContent(fileEntry.getUrl()), Map.class);
            }
        } else {
            String fileName = taskExecution.get(PROPERTY_FILE_NAME, String.class, getDefaultFileName(fileType));
            Object input = jsonHelper.checkJSON(taskExecution.getRequired("input"));

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            if (fileType == FileType.JSON) {
                try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream)) {
                    printWriter.println(jsonHelper.write(input));
                }
            } else {
                try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream)) {
                    for (Map<String, ?> item : (List<Map<String, ?>>) input) {
                        printWriter.println(jsonHelper.write(item));
                    }
                }
            }

            try (InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray())) {
                result = fileStorageService.storeFileContent(fileName, inputStream);
            }
        }

        return result;
    }

    private String getDefaultFileName(FileType fileType) {
        return "file." + (fileType == FileType.JSON ? "json" : "jsonl");
    }
}
