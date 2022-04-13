/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import com.integri.atlas.task.handler.http.client.header.HttpHeadersFactory;
import com.integri.atlas.task.handler.http.client.params.QueryParamsFactory;
import com.integri.atlas.task.handler.http.client.response.HttpResponseHandler;
import java.net.http.HttpResponse;
import org.springframework.stereotype.Component;

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
    public Object handle(TaskExecution aTask) throws Exception {
        IntegriHttpClient integriHttpClient = new IntegriHttpClient(
            httpAuthenticationFactory.create(
                aTask.getRequiredString("authenticationType"),
                aTask.get("credentials", MapObject.class)
            ),
            aTask.getLong("timeout", 10000)
        );

        HttpResponse httpResponse = integriHttpClient.send(
            aTask.getRequiredString("requestMethod"),
            resolveURI(aTask.getRequiredString("uri"), queryParamsFactory.getQueryParams(aTask)),
            httpHeadersFactory.getHttpHeaders(aTask),
            getBody(aTask)
        );

        return httpResponseHandler.handle(aTask, httpResponse);
    }

    private String resolveURI(String uri, String queryParameters) {
        if (queryParameters.isEmpty()) {
            return uri;
        }

        return uri + '?' + queryParameters;
    }

    private String getBody(TaskExecution aTask) {
        if (aTask.containsKey("bodyParametersRaw")) {
            return jsonHelper.serialize(aTask.get("bodyParametersRaw"));
        } else if (aTask.containsKey("bodyParametersKeyValue")) {}

        return "";
    }

    private final HttpAuthenticationFactory httpAuthenticationFactory;
    private final HttpHeadersFactory httpHeadersFactory;
    private final QueryParamsFactory queryParamsFactory;
    private final JSONHelper jsonHelper;
    private final HttpResponseHandler httpResponseHandler;
}
