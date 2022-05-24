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

import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_PROPERTY_URI;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_TIMEOUT;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.TASK_HTTP_CLIENT;

import com.integri.atlas.engine.MapObject;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.task.handler.http.client.authentication.HttpAuthenticationFactory;
import com.integri.atlas.task.handler.http.client.body.HttpBodyFactory;
import com.integri.atlas.task.handler.http.client.header.HttpHeader;
import com.integri.atlas.task.handler.http.client.header.HttpHeadersFactory;
import com.integri.atlas.task.handler.http.client.http.HttpClientHelper;
import com.integri.atlas.task.handler.http.client.params.QueryParamsFactory;
import com.integri.atlas.task.handler.http.client.response.HttpResponseHandler;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component(TASK_HTTP_CLIENT)
public class HttpClientTaskHandler implements TaskHandler<Object> {

    private final HttpBodyFactory httpBodyFactory;
    private final HttpAuthenticationFactory httpAuthenticationFactory;
    private final HttpHeadersFactory httpHeadersFactory;
    private final QueryParamsFactory queryParamsFactory;
    private final HttpResponseHandler httpResponseHandler;

    public HttpClientTaskHandler(
        HttpBodyFactory httpBodyFactory,
        HttpAuthenticationFactory httpAuthenticationFactory,
        HttpHeadersFactory httpHeadersFactory,
        HttpResponseHandler httpResponseHandler,
        QueryParamsFactory queryParamsFactory
    ) {
        this.httpBodyFactory = httpBodyFactory;
        this.httpAuthenticationFactory = httpAuthenticationFactory;
        this.httpHeadersFactory = httpHeadersFactory;
        this.httpResponseHandler = httpResponseHandler;
        this.queryParamsFactory = queryParamsFactory;
    }

    @Override
    public Object handle(TaskExecution taskExecution) throws Exception {
        HttpClientHelper httpClientHelper = new HttpClientHelper(
            httpAuthenticationFactory.create(
                taskExecution.getRequiredString("authentication"),
                taskExecution.get("credentials", MapObject.class)
            ),
            taskExecution.getLong(PROPERTY_TIMEOUT, 10000)
        );

        List<HttpHeader> httpHeaders = httpHeadersFactory.getHttpHeaders(taskExecution);

        HttpResponse<?> httpResponse = httpClientHelper.send(
            taskExecution.getRequiredString("operation"),
            resolveURI(
                taskExecution.getRequiredString(PROPERTY_PROPERTY_URI),
                queryParamsFactory.getQueryParams(taskExecution)
            ),
            httpHeaders,
            httpBodyFactory.getBodyPublisher(taskExecution, httpHeaders),
            httpBodyFactory.getBodyHandler(taskExecution)
        );

        return httpResponseHandler.handle(taskExecution, httpResponse);
    }

    private String resolveURI(String uri, String queryParameters) {
        if (queryParameters.isEmpty()) {
            return uri;
        }

        return uri + '?' + queryParameters;
    }
}
