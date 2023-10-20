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

package com.integri.atlas.task.handler.xml.file;

import static com.integri.atlas.task.handler.xml.file.XmlFileTaskConstants.PROPERTY_FILE_NAME;
import static com.integri.atlas.task.handler.xml.file.XmlFileTaskConstants.PROPERTY_SOURCE;
import static com.integri.atlas.task.handler.xml.file.XmlFileTaskConstants.TASK_XML_FILE;

import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.file.storage.dto.FileEntry;
import com.integri.atlas.file.storage.service.FileStorageService;
import com.integri.atlas.task.commons.xml.XmlHelper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(TASK_XML_FILE + "/write")
public class XmlFileWriteTaskHandler implements TaskHandler<FileEntry> {

    private final XmlHelper xmlHelper;

    public XmlFileWriteTaskHandler(FileStorageService fileStorageService, XmlHelper xmlHelper) {
        this.fileStorageService = fileStorageService;
        this.xmlHelper = xmlHelper;
    }

    private final FileStorageService fileStorageService;

    @Override
    public FileEntry handle(TaskExecution taskExecution) throws Exception {
        String fileName = taskExecution.get(PROPERTY_FILE_NAME, String.class, "file.xml");
        Object source = taskExecution.getRequired(PROPERTY_SOURCE);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream)) {
            printWriter.println(xmlHelper.write(source));
        }

        try (InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray())) {
            return fileStorageService.storeFileContent(fileName, inputStream);
        }
    }
}
