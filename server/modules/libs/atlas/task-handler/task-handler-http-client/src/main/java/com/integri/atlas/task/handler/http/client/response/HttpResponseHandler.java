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

import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.file.storage.FileStorageService;
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

    public HttpResponseHandler(FileStorageService fileStorageService, JSONHelper jsonHelper) {
        this.fileStorageService = fileStorageService;
        this.jsonHelper = jsonHelper;
    }

    public Object handle(TaskExecution taskExecution, HttpResponse httpResponse) {
        boolean fullResponse = taskExecution.getBoolean("fullResponse", false);

        String responseFormat = taskExecution.getString("responseFormat");

        if (responseFormat == null) {
            return null;
        }

        ContentType contentType = ContentType.valueOf(responseFormat);

        if (contentType == ContentType.JSON) {
            return jsonHelper.deserialize(httpResponse.body().toString(), Map.class);
        } else if (contentType == ContentType.STRING) {
            return httpResponse.body().toString();
        } else if (contentType == ContentType.BINARY) {
            return fileStorageService.storeFileContent("Moj-File", (InputStream) httpResponse.body());
        }

        throw new IllegalArgumentException("Invalid response format " + responseFormat);
    }

    private final FileStorageService fileStorageService;
    private final JSONHelper jsonHelper;
}
