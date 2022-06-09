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

package com.integri.atlas.task.handler.httpclient.body.multipart;

import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.BODY_PARAMETERS;

import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.file.storage.service.FileStorageService;
import com.integri.atlas.task.handler.httpclient.header.HttpHeader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

/**
 * @author Matija Petanjek
 */
public class MultiPartHttpBodyPublisher {

    private final String boundary = UUID.randomUUID().toString();
    private final FileStorageService fileStorageService;
    private final List<HttpHeader> httpHeaders;
    private final TaskExecution taskExecution;

    public MultiPartHttpBodyPublisher(
        FileStorageService fileStorageService,
        List<HttpHeader> httpHeaders,
        TaskExecution taskExecution
    ) {
        this.fileStorageService = fileStorageService;
        this.httpHeaders = httpHeaders;
        this.taskExecution = taskExecution;
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

        multipartHttpHeader.setValue(multipartHttpHeader.getValue().replace(HttpHeader.BOUNDARY_TMPL, boundary));
    }

    public byte[] build() throws IOException {
        updateMultipartHttpHeader(httpHeaders, boundary);

        return getHttpBodyPartsBytes(taskExecution);
    }

    private byte[] getHttpBodyPartsBytes(TaskExecution taskExecution) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Map<String, String> bodyParametersMap = taskExecution.get(BODY_PARAMETERS);

        for (Map.Entry<String, String> entry : bodyParametersMap.entrySet()) {
            byteArrayOutputStream.write(
                ("--" + boundary + "\r\n Content-Disposition: form-data; name=\"" + entry.getKey() + "\"").getBytes(
                        StandardCharsets.UTF_8
                    )
            );

            String value = entry.getValue();

            if (fileStorageService.fileExists(value)) {
                byteArrayOutputStream.write(
                    ("; filename=\"" + FilenameUtils.getName(value) + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8)
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
