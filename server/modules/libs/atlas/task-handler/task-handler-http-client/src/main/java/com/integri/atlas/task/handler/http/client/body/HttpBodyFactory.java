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

package com.integri.atlas.task.handler.http.client.body;

import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.BodyContentType.FORM_DATA;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_BODY_CONTENT_TYPE;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_BODY_PARAMETERS;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_FILE_ENTRY;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_KEY;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_RESPONSE_FORMAT;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_VALUE;

import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.file.storage.service.FileStorageService;
import com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.*;
import com.integri.atlas.task.handler.http.client.body.multipart.MultiPartHttpBodyPublisher;
import com.integri.atlas.task.handler.http.client.header.HttpHeader;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class HttpBodyFactory {

    private final FileStorageService fileStorageService;

    public HttpBodyFactory(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public HttpRequest.BodyPublisher getBodyPublisher(TaskExecution taskExecution, List<HttpHeader> httpHeaders)
        throws IOException {
        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();

        if (taskExecution.containsKey(PROPERTY_BODY_PARAMETERS)) {
            if (taskExecution.getString(PROPERTY_BODY_CONTENT_TYPE).equals(FORM_DATA.name())) {
                return HttpRequest.BodyPublishers.ofByteArray(
                    new MultiPartHttpBodyPublisher(fileStorageService, httpHeaders, taskExecution).build()
                );
            } else {
                return HttpRequest.BodyPublishers.ofString(
                    fromBodyParameters(taskExecution.get(PROPERTY_BODY_PARAMETERS))
                );
            }
        } else if (taskExecution.containsKey(PROPERTY_FILE_ENTRY)) {
            bodyPublisher =
                HttpRequest.BodyPublishers.ofInputStream(() ->
                    fileStorageService.getFileContentStream(taskExecution.getString(PROPERTY_FILE_ENTRY))
                );
        }

        return bodyPublisher;
    }

    public HttpResponse.BodyHandler<?> getBodyHandler(TaskExecution taskExecution) {
        if (!taskExecution.containsKey(PROPERTY_RESPONSE_FORMAT)) {
            return HttpResponse.BodyHandlers.discarding();
        }

        BodyContentType bodyContentType = BodyContentType.valueOf(taskExecution.getString(PROPERTY_RESPONSE_FORMAT));

        if (bodyContentType == BodyContentType.BINARY) {
            return HttpResponse.BodyHandlers.ofInputStream();
        }

        return HttpResponse.BodyHandlers.ofString();
    }

    private String fromBodyParameters(List<Map<String, String>> bodyParameters) {
        List<String> bodyParameterList = new ArrayList<>();

        StringBuilder sb = new StringBuilder();

        for (Map<String, String> parameter : bodyParameters) {
            sb.append(parameter.get(PROPERTY_KEY));
            sb.append("=");
            sb.append(parameter.get(PROPERTY_VALUE));

            bodyParameterList.add(sb.toString());
        }

        return StringUtils.join(bodyParameterList, "&");
    }
}
