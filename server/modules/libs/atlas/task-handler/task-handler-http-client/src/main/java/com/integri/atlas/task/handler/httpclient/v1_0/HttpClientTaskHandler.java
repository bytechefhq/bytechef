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

package com.integri.atlas.task.handler.httpclient.v1_0;

import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.*;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.DELETE;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.GET;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.HEAD;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.HTTP_CLIENT;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.PATCH;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.POST;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.PUT;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.TIMEOUT;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.URI;

import com.integri.atlas.engine.Constants;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.task.auth.TaskAuth;
import com.integri.atlas.task.handler.httpclient.body.HttpBodyFactory;
import com.integri.atlas.task.handler.httpclient.header.HttpHeader;
import com.integri.atlas.task.handler.httpclient.header.HttpHeaderFactory;
import com.integri.atlas.task.handler.httpclient.http.HttpClientHelper;
import com.integri.atlas.task.handler.httpclient.params.HttpQueryParamsFactory;
import com.integri.atlas.task.handler.httpclient.response.HttpResponseHandler;
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
            HttpClientHelper httpClientHelper = new HttpClientHelper(taskExecution.getLong(TIMEOUT, 10000));

            List<HttpHeader> httpHeaders = httpHeadersFactory.getHttpHeaders(taskExecution);

            HttpResponse<?> httpResponse = httpClientHelper.send(
                getRequestMethod(),
                taskExecution.getRequiredString(URI),
                httpHeaders,
                queryParamsFactory.getQueryParams(taskExecution),
                httpBodyFactory.getBodyPublisher(taskExecution, httpHeaders),
                httpBodyFactory.getBodyHandler(taskExecution),
                taskExecution.get(Constants.AUTH, TaskAuth.class)
            );

            return httpResponseHandler.handle(taskExecution, httpResponse);
        }

        protected abstract RequestMethod getRequestMethod();
    }

    @Component(HTTP_CLIENT + "/" + VERSION_1_0 + "/" + DELETE)
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
        protected RequestMethod getRequestMethod() {
            return RequestMethod.DELETE;
        }
    }

    @Component(HTTP_CLIENT + "/" + VERSION_1_0 + "/" + GET)
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
        protected RequestMethod getRequestMethod() {
            return RequestMethod.GET;
        }
    }

    @Component(HTTP_CLIENT + "/" + VERSION_1_0 + "/" + HEAD)
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
        protected RequestMethod getRequestMethod() {
            return RequestMethod.HEAD;
        }
    }

    @Component(HTTP_CLIENT + "/" + VERSION_1_0 + "/" + PATCH)
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
        protected RequestMethod getRequestMethod() {
            return RequestMethod.PATCH;
        }
    }

    @Component(HTTP_CLIENT + "/" + VERSION_1_0 + "/" + POST)
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
        protected RequestMethod getRequestMethod() {
            return RequestMethod.POST;
        }
    }

    @Component(HTTP_CLIENT + "/" + VERSION_1_0 + "/" + PUT)
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
        protected RequestMethod getRequestMethod() {
            return RequestMethod.PUT;
        }
    }
}
