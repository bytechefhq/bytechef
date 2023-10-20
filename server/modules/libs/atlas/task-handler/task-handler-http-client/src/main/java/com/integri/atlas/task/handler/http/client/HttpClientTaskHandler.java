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

import com.integri.atlas.engine.Constants;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.task.auth.TaskAuth;
import com.integri.atlas.task.handler.http.client.body.HttpBodyFactory;
import com.integri.atlas.task.handler.http.client.header.HttpHeader;
import com.integri.atlas.task.handler.http.client.header.HttpHeaderFactory;
import com.integri.atlas.task.handler.http.client.http.HttpClientHelper;
import com.integri.atlas.task.handler.http.client.params.HttpQueryParamsFactory;
import com.integri.atlas.task.handler.http.client.response.HttpResponseHandler;
import java.net.http.HttpResponse;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
public class HttpClientTaskHandler {

    public abstract static class HttpClientBaseTaskHandler implements TaskHandler<Object> {

        private final HttpBodyFactory httpBodyFactory;
        private final HttpHeaderFactory httpHeadersFactory;
        private final HttpQueryParamsFactory queryParamsFactory;
        private final HttpResponseHandler httpResponseHandler;

        public HttpClientBaseTaskHandler(
            HttpBodyFactory httpBodyFactory,
            HttpHeaderFactory httpHeadersFactory,
            HttpResponseHandler httpResponseHandler,
            HttpQueryParamsFactory queryParamsFactory
        ) {
            this.httpBodyFactory = httpBodyFactory;
            this.httpHeadersFactory = httpHeadersFactory;
            this.httpResponseHandler = httpResponseHandler;
            this.queryParamsFactory = queryParamsFactory;
        }

        @Override
        public Object handle(TaskExecution taskExecution) throws Exception {
            HttpClientHelper httpClientHelper = new HttpClientHelper(taskExecution.getLong(PROPERTY_TIMEOUT, 10000));

            List<HttpHeader> httpHeaders = httpHeadersFactory.getHttpHeaders(taskExecution);

            HttpResponse<?> httpResponse = httpClientHelper.send(
                getRequestMethod(),
                resolveURI(
                    taskExecution.getRequiredString(PROPERTY_PROPERTY_URI),
                    queryParamsFactory.getQueryParams(taskExecution)
                ),
                httpHeaders,
                httpBodyFactory.getBodyPublisher(taskExecution, httpHeaders),
                httpBodyFactory.getBodyHandler(taskExecution),
                taskExecution.get(Constants.AUTH, TaskAuth.class)
            );

            return httpResponseHandler.handle(taskExecution, httpResponse);
        }

        protected abstract HttpClientTaskConstants.RequestMethod getRequestMethod();

        private String resolveURI(String uri, String queryParameters) {
            if (queryParameters.isEmpty()) {
                return uri;
            }

            return uri + '?' + queryParameters;
        }
    }

    @Component(TASK_HTTP_CLIENT + "/delete")
    public static class HttpClientDeleteTaskHandler extends HttpClientBaseTaskHandler {

        public HttpClientDeleteTaskHandler(
            HttpBodyFactory httpBodyFactory,
            HttpHeaderFactory httpHeadersFactory,
            HttpResponseHandler httpResponseHandler,
            HttpQueryParamsFactory queryParamsFactory
        ) {
            super(httpBodyFactory, httpHeadersFactory, httpResponseHandler, queryParamsFactory);
        }

        @Override
        protected HttpClientTaskConstants.RequestMethod getRequestMethod() {
            return HttpClientTaskConstants.RequestMethod.DELETE;
        }
    }

    @Component(TASK_HTTP_CLIENT + "/get")
    public static class HttpClientGetTaskHandler extends HttpClientBaseTaskHandler {

        public HttpClientGetTaskHandler(
            HttpBodyFactory httpBodyFactory,
            HttpHeaderFactory httpHeadersFactory,
            HttpResponseHandler httpResponseHandler,
            HttpQueryParamsFactory queryParamsFactory
        ) {
            super(httpBodyFactory, httpHeadersFactory, httpResponseHandler, queryParamsFactory);
        }

        @Override
        protected HttpClientTaskConstants.RequestMethod getRequestMethod() {
            return HttpClientTaskConstants.RequestMethod.GET;
        }
    }

    @Component(TASK_HTTP_CLIENT + "/head")
    public static class HttpClientHeadTaskHandler extends HttpClientBaseTaskHandler {

        public HttpClientHeadTaskHandler(
            HttpBodyFactory httpBodyFactory,
            HttpHeaderFactory httpHeadersFactory,
            HttpResponseHandler httpResponseHandler,
            HttpQueryParamsFactory queryParamsFactory
        ) {
            super(httpBodyFactory, httpHeadersFactory, httpResponseHandler, queryParamsFactory);
        }

        @Override
        protected HttpClientTaskConstants.RequestMethod getRequestMethod() {
            return HttpClientTaskConstants.RequestMethod.HEAD;
        }
    }

    @Component(TASK_HTTP_CLIENT + "/patch")
    public static class HttpClientPatchTaskHandler extends HttpClientBaseTaskHandler {

        public HttpClientPatchTaskHandler(
            HttpBodyFactory httpBodyFactory,
            HttpHeaderFactory httpHeadersFactory,
            HttpResponseHandler httpResponseHandler,
            HttpQueryParamsFactory queryParamsFactory
        ) {
            super(httpBodyFactory, httpHeadersFactory, httpResponseHandler, queryParamsFactory);
        }

        @Override
        protected HttpClientTaskConstants.RequestMethod getRequestMethod() {
            return HttpClientTaskConstants.RequestMethod.PATCH;
        }
    }

    @Component(TASK_HTTP_CLIENT + "/post")
    public static class HttpClientPostTaskHandler extends HttpClientBaseTaskHandler {

        public HttpClientPostTaskHandler(
            HttpBodyFactory httpBodyFactory,
            HttpHeaderFactory httpHeadersFactory,
            HttpResponseHandler httpResponseHandler,
            HttpQueryParamsFactory queryParamsFactory
        ) {
            super(httpBodyFactory, httpHeadersFactory, httpResponseHandler, queryParamsFactory);
        }

        @Override
        protected HttpClientTaskConstants.RequestMethod getRequestMethod() {
            return HttpClientTaskConstants.RequestMethod.POST;
        }
    }

    @Component(TASK_HTTP_CLIENT + "/put")
    public static class HttpClientPutTaskHandler extends HttpClientBaseTaskHandler {

        public HttpClientPutTaskHandler(
            HttpBodyFactory httpBodyFactory,
            HttpHeaderFactory httpHeadersFactory,
            HttpResponseHandler httpResponseHandler,
            HttpQueryParamsFactory queryParamsFactory
        ) {
            super(httpBodyFactory, httpHeadersFactory, httpResponseHandler, queryParamsFactory);
        }

        @Override
        protected HttpClientTaskConstants.RequestMethod getRequestMethod() {
            return HttpClientTaskConstants.RequestMethod.PUT;
        }
    }
}
