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

package com.integri.atlas.task.handler.http.client.response;

import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_FULL_RESPONSE;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_RESPONSE_FORMAT;

import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.file.storage.FileStorageService;
import com.integri.atlas.file.storage.service.FileStorageService;
import com.integri.atlas.task.handler.http.client.HttpClientTaskConstants;
import com.integri.atlas.task.handler.http.client.header.ContentType;
import com.integri.atlas.task.handler.json.helper.JSONHelper;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 */
@Component
public class HttpResponseHandler {

    private final FileStorageService fileStorageService;
    private final JSONHelper jsonHelper;

    public HttpResponseHandler(FileStorageService fileStorageService, JSONHelper jsonHelper) {
        this.fileStorageService = fileStorageService;
        this.jsonHelper = jsonHelper;
    }

    public Object handle(TaskExecution taskExecution, HttpResponse httpResponse) {
        if (taskExecution.getString(PROPERTY_RESPONSE_FORMAT) == null) {
            return null;
        }

        HttpClientTaskConstants.ResponseFormat responseFormat = HttpClientTaskConstants.ResponseFormat.valueOf(
            taskExecution.getString(PROPERTY_RESPONSE_FORMAT)
        );

        Object body = null;

        if (responseFormat == HttpClientTaskConstants.ResponseFormat.JSON) {
            return jsonHelper.read(httpResponse.body().toString(), Map.class);
        } else if (responseFormat == HttpClientTaskConstants.ResponseFormat.TEXT) {
            body = httpResponse.body().toString();
        } else if (responseFormat == HttpClientTaskConstants.ResponseFormat.FILE) {
            body =
                fileStorageService.storeFileContent(
                    taskExecution.getString(HttpClientTaskConstants.PROPERTY_RESPONSE_FILE_NAME),
                    (InputStream) httpResponse.body()
                );
        }

        boolean fullResponse = taskExecution.getBoolean(PROPERTY_FULL_RESPONSE, false);

        if (fullResponse) {
            return new HttpResponseEntry(body, httpResponse);
        }

        return body;
    }
}
