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

package com.bytechef.task.handler.xmlfile.v1_0;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.hermes.file.storage.dto.FileEntry;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import com.bytechef.task.commons.xml.XMLHelper;
import com.bytechef.task.handler.xmlfile.XMLFileTaskConstants;
import com.fasterxml.jackson.core.type.TypeReference;
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
public class XMLFileTaskHandler {

    @Component(XMLFileTaskConstants.XML_FILE + "/" + XMLFileTaskConstants.VERSION_1_0 + "/" + XMLFileTaskConstants.READ)
    public static class XMLFileReadTaskHandler implements TaskHandler<Object> {

        private final XMLHelper xmlHelper;

        public XMLFileReadTaskHandler(FileStorageService fileStorageService, XMLHelper xmlHelper) {
            this.fileStorageService = fileStorageService;
            this.xmlHelper = xmlHelper;
        }

        private final FileStorageService fileStorageService;

        @Override
        public Object handle(TaskExecution taskExecution) throws Exception {
            Object result;

            boolean isArray = taskExecution.get(XMLFileTaskConstants.IS_ARRAY, Boolean.class, true);
            FileEntry fileEntry = taskExecution.getRequired(XMLFileTaskConstants.FILE_ENTRY, FileEntry.class);

            if (isArray) {
                String path = taskExecution.get(XMLFileTaskConstants.PATH);
                InputStream inputStream = fileStorageService.getFileContentStream(fileEntry.getUrl());
                List<Map<String, ?>> items;

                if (path == null) {
                    try (Stream<Map<String, ?>> stream =
                            xmlHelper.stream(fileStorageService.getFileContentStream(fileEntry.getUrl()))) {
                        items = stream.toList();
                    }
                } else {
                    items = xmlHelper.read(inputStream, path, new TypeReference<>() {});
                }

                Integer pageSize = taskExecution.getInteger(XMLFileTaskConstants.PAGE_SIZE);
                Integer pageNumber = taskExecution.getInteger(XMLFileTaskConstants.PAGE_NUMBER);
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
                result = xmlHelper.read(fileStorageService.readFileContent(fileEntry.getUrl()), Map.class);
            }

            return result;
        }
    }

    @Component(
            XMLFileTaskConstants.XML_FILE + "/" + XMLFileTaskConstants.VERSION_1_0 + "/" + XMLFileTaskConstants.WRITE)
    public static class XMLFileWriteTaskHandler implements TaskHandler<FileEntry> {

        private final XMLHelper xmlHelper;

        public XMLFileWriteTaskHandler(FileStorageService fileStorageService, XMLHelper xmlHelper) {
            this.fileStorageService = fileStorageService;
            this.xmlHelper = xmlHelper;
        }

        private final FileStorageService fileStorageService;

        @Override
        public FileEntry handle(TaskExecution taskExecution) throws Exception {
            String fileName = taskExecution.get(XMLFileTaskConstants.FILE_NAME, String.class, "file.xml");
            Object source = taskExecution.getRequired(XMLFileTaskConstants.SOURCE);

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
