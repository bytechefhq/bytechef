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

package com.integri.atlas.task.handler.http.client;

import com.integri.atlas.engine.core.MapObject;
import com.integri.atlas.engine.core.json.JSONHelper;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.task.handler.http.client.authentication.HttpAuthenticationFactory;
import com.integri.atlas.task.handler.http.client.header.ContentType;
import com.integri.atlas.task.handler.http.client.header.HttpHeadersFactory;
import com.integri.atlas.task.handler.http.client.params.QueryParamsFactory;
import com.integri.atlas.task.handler.http.client.response.HttpResponseHandler;
import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

@Component("httpClient")
public class HttpClientTaskHandler implements TaskHandler<Object> {

    public HttpClientTaskHandler(
        HttpAuthenticationFactory httpAuthenticationFactory,
        HttpHeadersFactory httpHeadersFactory,
        HttpResponseHandler httpResponseHandler,
        QueryParamsFactory queryParamsFactory,
        JSONHelper jsonHelper
    ) {
        this.httpAuthenticationFactory = httpAuthenticationFactory;
        this.httpHeadersFactory = httpHeadersFactory;
        this.httpResponseHandler = httpResponseHandler;
        this.queryParamsFactory = queryParamsFactory;
        this.jsonHelper = jsonHelper;
    }

    @Override
    public Object handle(TaskExecution taskExecution) throws Exception {
        IntegriHttpClient integriHttpClient = new IntegriHttpClient(
            httpAuthenticationFactory.create(
                taskExecution.getRequiredString("authenticationType"),
                taskExecution.get("credentials", MapObject.class)
            ),
            taskExecution.getLong("timeout", 10000)
        );

        HttpResponse httpResponse = integriHttpClient.send(
            taskExecution.getRequiredString("requestMethod"),
            resolveURI(taskExecution.getRequiredString("uri"), queryParamsFactory.getQueryParams(taskExecution)),
            httpHeadersFactory.getHttpHeaders(taskExecution),
            getBodyPublisher(taskExecution),
            getBodyHandler(taskExecution)
        );

        return httpResponseHandler.handle(taskExecution, httpResponse);
    }

    private String resolveURI(String uri, String queryParameters) {
        if (queryParameters.isEmpty()) {
            return uri;
        }

        return uri + '?' + queryParameters;
    }

    private HttpRequest.BodyPublisher getBodyPublisher(TaskExecution taskExecution) {
        if (taskExecution.containsKey("bodyParametersRaw")) {
            return HttpRequest.BodyPublishers.ofString(taskExecution.get("bodyParametersRaw"));
        } else if (taskExecution.containsKey("bodyParametersKeyValue")) {
            MultiValueMap<String, String> bodyParameters = taskExecution.get(
                "bodyParametersKeyValue",
                MultiValueMap.class
            );

            List<String> bodyParametersList = new ArrayList<>();

            StringBuilder sb = new StringBuilder();

            for (Map.Entry<String, List<String>> entry : bodyParameters.entrySet()) {
                sb.append(entry.getKey());
                sb.append("=");
                sb.append(StringUtils.join(entry.getValue(), ","));

                bodyParametersList.add(sb.toString());
            }

            return HttpRequest.BodyPublishers.ofString(StringUtils.join(bodyParametersList, "&"));
        }

        return HttpRequest.BodyPublishers.noBody();
    }

    private HttpResponse.BodyHandler getBodyHandler(TaskExecution taskExecution) {
        if (!taskExecution.containsKey("responseFormat")) {
            return HttpResponse.BodyHandlers.discarding();
        }

        ContentType contentType = ContentType.valueOf(taskExecution.getString("responseFormat"));

        if (contentType == ContentType.BINARY) {
            return HttpResponse.BodyHandlers.ofInputStream();
        }

        return HttpResponse.BodyHandlers.ofString();
    }

    private final HttpAuthenticationFactory httpAuthenticationFactory;
    private final HttpHeadersFactory httpHeadersFactory;
    private final QueryParamsFactory queryParamsFactory;
    private final JSONHelper jsonHelper;
    private final HttpResponseHandler httpResponseHandler;
}
