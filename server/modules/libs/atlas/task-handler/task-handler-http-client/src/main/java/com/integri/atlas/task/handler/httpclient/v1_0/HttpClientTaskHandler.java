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

import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.task.handler.httpclient.v1_0.http.HttpClientHelper;
import com.integri.atlas.task.handler.httpclient.v1_0.response.HttpResponseHandler;
import java.net.http.HttpResponse;

import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
public class HttpClientTaskHandler {

    public abstract static class HttpClientBaseTaskHandler implements TaskHandler<Object> {

        private final HttpClientHelper httpClientHelper;
        private final HttpResponseHandler httpResponseHandler;

        public HttpClientBaseTaskHandler(
            HttpClientHelper httpClientHelper,
            HttpResponseHandler httpResponseHandler
        ) {
            this.httpResponseHandler = httpResponseHandler;
            this.httpClientHelper = httpClientHelper;
        }

        @Override
        public Object handle(TaskExecution taskExecution) throws Exception {
            HttpResponse<?> httpResponse = httpClientHelper.send(taskExecution, getRequestMethod());

            return httpResponseHandler.handle(taskExecution, httpResponse);
        }

        protected abstract RequestMethod getRequestMethod();
    }

    @Component(HTTP_CLIENT + "/" + VERSION_1_0 + "/" + DELETE)
    public static class HttpClientDeleteTaskHandler extends HttpClientBaseTaskHandler {

        public HttpClientDeleteTaskHandler(
            HttpClientHelper httpClientHelper,
            HttpResponseHandler httpResponseHandler
        ) {
            super(httpClientHelper, httpResponseHandler);
        }

        @Override
        protected RequestMethod getRequestMethod() {
            return RequestMethod.DELETE;
        }
    }

    @Component(HTTP_CLIENT + "/" + VERSION_1_0 + "/" + GET)
    public static class HttpClientGetTaskHandler extends HttpClientBaseTaskHandler {

        public HttpClientGetTaskHandler(
            HttpClientHelper httpClientHelper,
            HttpResponseHandler httpResponseHandler
        ) {
            super(httpClientHelper, httpResponseHandler);
        }

        @Override
        protected RequestMethod getRequestMethod() {
            return RequestMethod.GET;
        }
    }

    @Component(HTTP_CLIENT + "/" + VERSION_1_0 + "/" + HEAD)
    public static class HttpClientHeadTaskHandler extends HttpClientBaseTaskHandler {

        public HttpClientHeadTaskHandler(
            HttpClientHelper httpClientHelper,
            HttpResponseHandler httpResponseHandler
        ) {
            super(httpClientHelper, httpResponseHandler);
        }

        @Override
        protected RequestMethod getRequestMethod() {
            return RequestMethod.HEAD;
        }
    }

    @Component(HTTP_CLIENT + "/" + VERSION_1_0 + "/" + PATCH)
    public static class HttpClientPatchTaskHandler extends HttpClientBaseTaskHandler {

        public HttpClientPatchTaskHandler(
            HttpClientHelper httpClientHelper,
            HttpResponseHandler httpResponseHandler
        ) {
            super(httpClientHelper, httpResponseHandler);
        }

        @Override
        protected RequestMethod getRequestMethod() {
            return RequestMethod.PATCH;
        }
    }

    @Component(HTTP_CLIENT + "/" + VERSION_1_0 + "/" + POST)
    public static class HttpClientPostTaskHandler extends HttpClientBaseTaskHandler {

        public HttpClientPostTaskHandler(
            HttpClientHelper httpClientHelper,
            HttpResponseHandler httpResponseHandler
        ) {
            super(httpClientHelper, httpResponseHandler);
        }

        @Override
        protected RequestMethod getRequestMethod() {
            return RequestMethod.POST;
        }
    }

    @Component(HTTP_CLIENT + "/" + VERSION_1_0 + "/" + PUT)
    public static class HttpClientPutTaskHandler extends HttpClientBaseTaskHandler {

        public HttpClientPutTaskHandler(
            HttpClientHelper httpClientHelper,
            HttpResponseHandler httpResponseHandler
        ) {
            super(httpClientHelper, httpResponseHandler);
        }

        @Override
        protected RequestMethod getRequestMethod() {
            return RequestMethod.PUT;
        }
    }
}
