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

package com.integri.atlas.task.handler.xml.file;

import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.xml.XMLHelper;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.file.storage.FileEntry;
import com.integri.atlas.file.storage.FileStorageService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component("xmlFile")
public class XMLFileTaskHandler implements TaskHandler<Object> {

    private final XMLHelper xmlHelper;

    public XMLFileTaskHandler(FileStorageService fileStorageService, XMLHelper xmlHelper) {
        this.fileStorageService = fileStorageService;
        this.xmlHelper = xmlHelper;
    }

    private enum Operation {
        READ,
        WRITE,
    }

    private final FileStorageService fileStorageService;

    @Override
    public Object handle(TaskExecution taskExecution) throws Exception {
        Object result;

        Operation operation = Operation.valueOf(StringUtils.upperCase(taskExecution.getRequired("operation")));

        if (operation == Operation.READ) {
            FileEntry fileEntry = taskExecution.getRequired("fileEntry", FileEntry.class);
            Map<String, Integer> range = taskExecution.get("range");

            Integer rangeStartIndex = null;

            if (range != null) {
                rangeStartIndex = range.get("startIndex");
            }

            Integer rangeEndIndex = null;

            if (range != null) {
                rangeEndIndex = range.get("endIndex");
            }

            List<Map<String, ?>> items = xmlHelper.deserialize(
                fileStorageService.readFileContent(fileEntry.getUrl()),
                List.class
            );

            if (
                (rangeStartIndex != null && rangeStartIndex > 0) ||
                (rangeEndIndex != null && rangeEndIndex < items.size())
            ) {
                items = items.subList(rangeStartIndex == null ? 0 : rangeStartIndex, rangeEndIndex);
            }

            result = items;
        } else {
            String fileName = taskExecution.get("fileName", String.class, "file.xml");
            List<Map<String, ?>> items = taskExecution.get("items");

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream)) {
                printWriter.println(xmlHelper.serialize(items));
            }

            try (InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray())) {
                return fileStorageService.storeFileContent(fileName, inputStream);
            }
        }

        return result;
    }
}
