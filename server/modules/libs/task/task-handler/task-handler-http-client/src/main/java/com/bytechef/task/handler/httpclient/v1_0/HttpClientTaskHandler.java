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

package com.bytechef.task.handler.httpclient.v1_0;

import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.*;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.task.handler.httpclient.v1_0.http.HttpClientHelper;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
public class HttpClientTaskHandler {

    public abstract static class AbstractHTTPClientTaskHandler implements TaskHandler<Object> {

        private final HttpClientHelper httpClientHelper;

        public AbstractHTTPClientTaskHandler(HttpClientHelper httpClientHelper) {
            this.httpClientHelper = httpClientHelper;
        }

        @Override
        public Object handle(TaskExecution taskExecution) throws TaskExecutionException {
            try {
                return httpClientHelper.send(taskExecution, getRequestMethod());
            } catch (Exception exception) {
                throw new TaskExecutionException("Unable to send payload", exception);
            }
        }

        protected abstract RequestMethod getRequestMethod();
    }

    @Component(HTTP_CLIENT + "/" + VERSION_1_0 + "/" + DELETE)
    public static class HtttpClientDeleteTaskHandler extends AbstractHTTPClientTaskHandler {

        public HtttpClientDeleteTaskHandler(HttpClientHelper httpClientHelper) {
            super(httpClientHelper);
        }

        @Override
        protected RequestMethod getRequestMethod() {
            return RequestMethod.DELETE;
        }
    }

    @Component(HTTP_CLIENT + "/" + VERSION_1_0 + "/" + GET)
    public static class HttpClientGetTaskHandler extends AbstractHTTPClientTaskHandler {

        public HttpClientGetTaskHandler(HttpClientHelper httpClientHelper) {
            super(httpClientHelper);
        }

        @Override
        protected RequestMethod getRequestMethod() {
            return RequestMethod.GET;
        }
    }

    @Component(HTTP_CLIENT + "/" + VERSION_1_0 + "/" + HEAD)
    public static class HttpClientHeadTaskHandler extends AbstractHTTPClientTaskHandler {

        public HttpClientHeadTaskHandler(HttpClientHelper httpClientHelper) {
            super(httpClientHelper);
        }

        @Override
        protected RequestMethod getRequestMethod() {
            return RequestMethod.HEAD;
        }
    }

    @Component(HTTP_CLIENT + "/" + VERSION_1_0 + "/" + PATCH)
    public static class HttpClientPatchTaskHandler extends AbstractHTTPClientTaskHandler {

        public HttpClientPatchTaskHandler(HttpClientHelper httpClientHelper) {
            super(httpClientHelper);
        }

        @Override
        protected RequestMethod getRequestMethod() {
            return RequestMethod.PATCH;
        }
    }

    @Component(HTTP_CLIENT + "/" + VERSION_1_0 + "/" + POST)
    public static class HttpClientPostTaskHandler extends AbstractHTTPClientTaskHandler {

        public HttpClientPostTaskHandler(HttpClientHelper httpClientHelper) {
            super(httpClientHelper);
        }

        @Override
        protected RequestMethod getRequestMethod() {
            return RequestMethod.POST;
        }
    }

    @Component(HTTP_CLIENT + "/" + VERSION_1_0 + "/" + PUT)
    public static class HttpClientPutTaskHandler extends AbstractHTTPClientTaskHandler {

        public HttpClientPutTaskHandler(HttpClientHelper httpClientHelper) {
            super(httpClientHelper);
        }

        @Override
        protected RequestMethod getRequestMethod() {
            return RequestMethod.PUT;
        }
    }
}
