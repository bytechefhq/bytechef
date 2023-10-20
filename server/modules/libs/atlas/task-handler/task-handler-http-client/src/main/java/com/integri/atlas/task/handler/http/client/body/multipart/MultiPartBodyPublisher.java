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

package com.integri.atlas.task.handler.http.client.body.multipart;

import static com.integri.atlas.task.handler.http.client.header.HttpHeader.BOUNDARY_TMPL;

import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.file.storage.FileStorageService;
import com.integri.atlas.task.handler.http.client.header.HttpHeader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.util.Assert;

/**
 * @author Matija Petanjek
 */
public class MultiPartBodyPublisher {

    private String boundary = UUID.randomUUID().toString();
    private TaskExecution taskExecution;
    private FileStorageService fileStorageService;
    private List<HttpHeader> httpHeaders;

    public MultiPartBodyPublisher(
        TaskExecution taskExecution,
        FileStorageService fileStorageService,
        List<HttpHeader> httpHeaders
    ) {
        this.taskExecution = taskExecution;
        this.fileStorageService = fileStorageService;
        this.httpHeaders = httpHeaders;
    }

    private HttpHeader getMultiPartHttpHeader(List<HttpHeader> httpHeaders) {
        for (HttpHeader httpHeader : httpHeaders) {
            if (httpHeader.getValue().startsWith("multipart/form-data")) {
                return httpHeader;
            }
        }

        throw new IllegalStateException("Missing multipart/form-data header");
    }

    private void updateMultipartHttpHeader(List<HttpHeader> httpHeaders, String boundary) {
        HttpHeader multipartHttpHeader = getMultiPartHttpHeader(httpHeaders);

        multipartHttpHeader.setValue(multipartHttpHeader.getValue().replace(BOUNDARY_TMPL, boundary));
    }

    public byte[] build() throws IOException {
        updateMultipartHttpHeader(httpHeaders, boundary);

        return getHttpBodyPartsBytes(taskExecution);
    }

    private byte[] getHttpBodyPartsBytes(TaskExecution taskExecution) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Map<String, String> bodyParametersMap = taskExecution.get("bodyParameters", Map.class);

        for (Map.Entry<String, String> entry : bodyParametersMap.entrySet()) {
            byteArrayOutputStream.write(
                ("--" + boundary + "\r\n Content-Disposition: form-data; name=\"" + entry.getKey() + "\"").getBytes(
                        StandardCharsets.UTF_8
                    )
            );

            String value = entry.getValue();

            if (fileStorageService.fileExists(value)) {
                byteArrayOutputStream.write(
                    ("; filename=\"" + fileStorageService.getFilename(value) + "\"\r\n\r\n").getBytes(
                            StandardCharsets.UTF_8
                        )
                );
                byteArrayOutputStream.write(IOUtils.toByteArray(fileStorageService.getFileContentStream(value)));
                byteArrayOutputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
            } else {
                byteArrayOutputStream.write(("\r\n\r\n" + entry.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
            }
        }

        byteArrayOutputStream.write(getFinalBoundaryBytes());

        return byteArrayOutputStream.toByteArray();
    }

    private byte[] getFinalBoundaryBytes() {
        return ("--" + boundary + "--").getBytes(StandardCharsets.UTF_8);
    }
}
