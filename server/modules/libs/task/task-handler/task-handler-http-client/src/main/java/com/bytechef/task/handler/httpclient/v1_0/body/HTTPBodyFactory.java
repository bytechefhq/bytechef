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

package com.bytechef.task.handler.httpclient.v1_0.body;

import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.BODY_CONTENT_TYPE;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.BODY_PARAMETERS;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.BodyContentType.FORM_DATA;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.FILE_ENTRY;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.KEY;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.VALUE;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import com.bytechef.task.handler.httpclient.v1_0.body.multipart.MultiPartHTTPBodyPublisher;
import com.bytechef.task.handler.httpclient.v1_0.header.HTTPHeader;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class HTTPBodyFactory {

    private final FileStorageService fileStorageService;

    public HTTPBodyFactory(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public HttpRequest.BodyPublisher getBodyPublisher(TaskExecution taskExecution, List<HTTPHeader> httpHeaders)
            throws IOException {
        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();

        if (taskExecution.containsKey(BODY_PARAMETERS)) {
            if (taskExecution.getString(BODY_CONTENT_TYPE).equals(FORM_DATA.name())) {
                return HttpRequest.BodyPublishers.ofByteArray(
                        new MultiPartHTTPBodyPublisher(fileStorageService, httpHeaders, taskExecution).build());
            } else {
                return HttpRequest.BodyPublishers.ofString(fromBodyParameters(taskExecution.get(BODY_PARAMETERS)));
            }
        } else if (taskExecution.containsKey(FILE_ENTRY)) {
            bodyPublisher = HttpRequest.BodyPublishers.ofInputStream(
                    () -> fileStorageService.getFileContentStream(taskExecution.getString(FILE_ENTRY)));
        }

        return bodyPublisher;
    }

    private String fromBodyParameters(List<Map<String, String>> bodyParameters) {
        List<String> bodyParameterList = new ArrayList<>();

        StringBuilder sb = new StringBuilder();

        for (Map<String, String> parameter : bodyParameters) {
            sb.append(parameter.get(KEY));
            sb.append("=");
            sb.append(parameter.get(VALUE));

            bodyParameterList.add(sb.toString());
        }

        return StringUtils.join(bodyParameterList, "&");
    }
}
