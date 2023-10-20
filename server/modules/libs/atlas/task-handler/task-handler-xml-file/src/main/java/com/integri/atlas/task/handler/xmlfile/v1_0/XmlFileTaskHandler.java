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

package com.integri.atlas.task.handler.xmlfile.v1_0;

import static com.integri.atlas.task.handler.xmlfile.XmlFileTaskConstants.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.file.storage.dto.FileEntry;
import com.integri.atlas.file.storage.service.FileStorageService;
import com.integri.atlas.task.commons.xml.XmlHelper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
public class XmlFileTaskHandler {

    @Component(XML_FILE + "/" + VERSION_1_0 + "/" + READ)
    public static class XmlFileReadTaskHandler implements TaskHandler<Object> {

        private final XmlHelper xmlHelper;

        public XmlFileReadTaskHandler(FileStorageService fileStorageService, XmlHelper xmlHelper) {
            this.fileStorageService = fileStorageService;
            this.xmlHelper = xmlHelper;
        }

        private final FileStorageService fileStorageService;

        @Override
        public Object handle(TaskExecution taskExecution) throws Exception {
            Object result;

            boolean isArray = taskExecution.get(IS_ARRAY, Boolean.class, true);
            FileEntry fileEntry = taskExecution.getRequired(FILE_ENTRY, FileEntry.class);

            if (isArray) {
                String path = taskExecution.get(PATH);
                InputStream inputStream = fileStorageService.getFileContentStream(fileEntry.getUrl());
                List<Map<String, ?>> items;

                if (path == null) {
                    try (
                        Stream<Map<String, ?>> stream = xmlHelper.stream(
                            fileStorageService.getFileContentStream(fileEntry.getUrl())
                        )
                    ) {
                        items = stream.toList();
                    }
                } else {
                    items = xmlHelper.read(inputStream, path, new TypeReference<>() {});
                }

                Integer pageSize = taskExecution.getInteger(PAGE_SIZE);
                Integer pageNumber = taskExecution.getInteger(PAGE_NUMBER);
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
                result = xmlHelper.read(fileStorageService.readFileContent(fileEntry.getUrl()), Map.class);
            }

            return result;
        }
    }

    @Component(XML_FILE + "/" + VERSION_1_0 + "/" + WRITE)
    public static class XmlFileWriteTaskHandler implements TaskHandler<FileEntry> {

        private final XmlHelper xmlHelper;

        public XmlFileWriteTaskHandler(FileStorageService fileStorageService, XmlHelper xmlHelper) {
            this.fileStorageService = fileStorageService;
            this.xmlHelper = xmlHelper;
        }

        private final FileStorageService fileStorageService;

        @Override
        public FileEntry handle(TaskExecution taskExecution) throws Exception {
            String fileName = taskExecution.get(FILE_NAME, String.class, "file.xml");
            Object source = taskExecution.getRequired(SOURCE);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream)) {
                printWriter.println(xmlHelper.write(source));
            }

            try (InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray())) {
                return fileStorageService.storeFileContent(fileName, inputStream);
            }
        }
    }
}
