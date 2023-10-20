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

package com.bytechef.task.handler.httpclient.v1_0.body.multipart;

import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.BODY_PARAMETERS;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import com.bytechef.task.handler.httpclient.v1_0.header.HTTPHeader;
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
public class MultiPartHTTPBodyPublisher {

    private final String boundary = UUID.randomUUID().toString();
    private final FileStorageService fileStorageService;
    private final List<HTTPHeader> httpHeaders;
    private final TaskExecution taskExecution;

    public MultiPartHTTPBodyPublisher(
            FileStorageService fileStorageService, List<HTTPHeader> httpHeaders, TaskExecution taskExecution) {
        this.fileStorageService = fileStorageService;
        this.httpHeaders = httpHeaders;
        this.taskExecution = taskExecution;
    }

    public byte[] build() throws IOException {
        updateMultipartHTTPHeader(httpHeaders, boundary);

        return getHttpBodyPartsBytes(taskExecution);
    }

    private byte[] getFinalBoundaryBytes() {
        return ("--" + boundary + "--").getBytes(StandardCharsets.UTF_8);
    }

    private byte[] getHttpBodyPartsBytes(TaskExecution taskExecution) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        Map<String, String> bodyParametersMap = taskExecution.get(BODY_PARAMETERS);

        for (Map.Entry<String, String> entry : bodyParametersMap.entrySet()) {
            byteArrayOutputStream.write(
                    ("--" + boundary + "\r\n Content-Disposition: form-data; name=\"" + entry.getKey() + "\"")
                            .getBytes(StandardCharsets.UTF_8));

            String value = entry.getValue();

            if (fileStorageService.fileExists(value)) {
                byteArrayOutputStream.write(("; filename=\"" + FilenameUtils.getName(value) + "\"\r\n\r\n")
                        .getBytes(StandardCharsets.UTF_8));
                byteArrayOutputStream.write(IOUtils.toByteArray(fileStorageService.getFileContentStream(value)));
                byteArrayOutputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
            } else {
                byteArrayOutputStream.write(("\r\n\r\n" + entry.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
            }
        }

        byteArrayOutputStream.write(getFinalBoundaryBytes());

        return byteArrayOutputStream.toByteArray();
    }

    private HTTPHeader getMultiPartHTTPHeader(List<HTTPHeader> httpHeaders) {
        for (HTTPHeader httpHeader : httpHeaders) {
            if (httpHeader.getValue().startsWith("multipart/form-data")) {
                return httpHeader;
            }
        }

        throw new IllegalStateException("Missing multipart/form-data header");
    }

    private void updateMultipartHTTPHeader(List<HTTPHeader> httpHeaders, String boundary) {
        HTTPHeader multipartHTTPHeader = getMultiPartHTTPHeader(httpHeaders);

        multipartHTTPHeader.setValue(multipartHTTPHeader.getValue().replace(HTTPHeader.BOUNDARY_TMPL, boundary));
    }
}
