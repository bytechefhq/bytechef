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

package com.bytechef.task.handler.httpclient.v1_0.response;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import com.bytechef.task.commons.json.JSONHelper;
import com.bytechef.task.handler.httpclient.HTTPClientTaskConstants;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 */
@Component
public class HTTPResponseHandler {

    private final FileStorageService fileStorageService;
    private final JSONHelper jsonHelper;

    public HTTPResponseHandler(FileStorageService fileStorageService, JSONHelper jsonHelper) {
        this.fileStorageService = fileStorageService;
        this.jsonHelper = jsonHelper;
    }

    public Object handle(TaskExecution taskExecution, HttpResponse<?> httpResponse) {
        if (taskExecution.getString(HTTPClientTaskConstants.RESPONSE_FORMAT) == null) {
            return null;
        }

        HTTPClientTaskConstants.ResponseFormat responseFormat = HTTPClientTaskConstants.ResponseFormat.valueOf(
                taskExecution.getString(HTTPClientTaskConstants.RESPONSE_FORMAT));

        Object body = null;

        if (responseFormat == HTTPClientTaskConstants.ResponseFormat.JSON) {
            return jsonHelper.read(httpResponse.body().toString(), Map.class);
        } else if (responseFormat == HTTPClientTaskConstants.ResponseFormat.TEXT) {
            body = httpResponse.body().toString();
        } else if (responseFormat == HTTPClientTaskConstants.ResponseFormat.FILE) {
            body = fileStorageService.storeFileContent(
                    taskExecution.getString(HTTPClientTaskConstants.RESPONSE_FILE_NAME),
                    (InputStream) httpResponse.body());
        }

        boolean fullResponse = taskExecution.getBoolean(HTTPClientTaskConstants.FULL_RESPONSE, false);

        if (fullResponse) {
            return new HTTPResponseEntry(body, httpResponse);
        }

        return body;
    }
}
