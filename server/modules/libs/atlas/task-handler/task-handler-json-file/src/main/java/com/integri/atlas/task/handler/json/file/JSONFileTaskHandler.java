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

package com.integri.atlas.task.handler.json.file;

import com.integri.atlas.engine.core.json.JSONHelper;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.file.storage.FileEntry;
import com.integri.atlas.file.storage.FileStorageService;
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

    public JSONFileTaskHandler(FileStorageService fileStorageService, JSONHelper jsonHelper) {
        this.fileStorageService = fileStorageService;
        this.jsonHelper = jsonHelper;
    }

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

            if (isArray) {
                Map<String, Integer> range = taskExecution.get("range");
                Integer rangeStartIndex = null;
                List<Map<String, ?>> items;

                if (range != null) {
                    rangeStartIndex = range.get("startIndex");
                }

                Integer rangeEndIndex = null;

                if (range != null) {
                    rangeEndIndex = range.get("endIndex");
                }

                if (fileType == FileType.JSON) {
                    try (
                        Stream<Map<String, ?>> stream = jsonHelper.stream(
                            fileStorageService.getFileContentStream(fileEntry.getUrl())
                        )
                    ) {
                        items = stream.toList();
                    }
                } else {
                    try (
                        BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(fileStorageService.getFileContentStream(fileEntry.getUrl()))
                        )
                    ) {
                        items =
                            bufferedReader
                                .lines()
                                .map(line -> (Map<String, ?>) jsonHelper.deserialize(line, Map.class))
                                .collect(Collectors.toList());
                    }
                }

                if (
                    (rangeStartIndex != null && rangeStartIndex > 0) ||
                    (rangeEndIndex != null && rangeEndIndex < items.size())
                ) {
                    items = items.subList(rangeStartIndex == null ? 0 : rangeStartIndex, rangeEndIndex);
                }

                result = items;
            } else {
                result = jsonHelper.deserialize(fileStorageService.readFileContent(fileEntry.getUrl()), Map.class);
            }
        } else {
            String fileName = taskExecution.get(
                "fileName",
                String.class,
                "file." + (fileType == FileType.JSON ? "json" : "jsonl")
            );
            Object object = taskExecution.get("items");

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            if (fileType == FileType.JSON) {
                try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream)) {
                    printWriter.println(jsonHelper.serialize(object));
                }
            } else {
                try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream)) {
                    for (Map<String, ?> item : (List<Map<String, ?>>)object) {
                        printWriter.println(jsonHelper.serialize(item));
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
