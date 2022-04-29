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

import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.file.storage.FileEntry;
import com.integri.atlas.file.storage.FileStorageService;
import com.integri.atlas.task.handler.json.helper.JSONHelper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
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
@Component("jsonFile")
public class JSONFileTaskHandler implements TaskHandler<Object> {

    private enum FileType {
        JSON,
        JSONL,
    }

    private enum Operation {
        READ,
        WRITE,
    }

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
            StringUtils.upperCase(taskExecution.get("fileType", String.class, "JSON"))
        );
        Operation operation = Operation.valueOf(StringUtils.upperCase(taskExecution.getRequired("operation")));

        if (operation == Operation.READ) {
            boolean isArray = taskExecution.get("isArray", Boolean.class, true);
            FileEntry fileEntry = taskExecution.getRequired("fileEntry", FileEntry.class);
            String path = taskExecution.get("path");

            if (isArray) {
                InputStream inputStream = fileStorageService.getFileContentStream(fileEntry.getUrl());
                List<Map<String, ?>> items;

                if (fileType == FileType.JSON) {
                    if (path == null) {
                        try (Stream<Map<String, ?>> stream = jsonHelper.stream(inputStream)) {
                            items = stream.toList();
                        }
                    } else {
                        DocumentContext documentContext = JsonPath.parse(inputStream);

                        items = documentContext.read(path);
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

                Integer pageSize = taskExecution.get("pageSize");
                Integer pageNumber = taskExecution.get("pageNumber");
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
            String fileName = taskExecution.get(
                "fileName",
                String.class,
                "file." + (fileType == FileType.JSON ? "json" : "jsonl")
            );
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
}
