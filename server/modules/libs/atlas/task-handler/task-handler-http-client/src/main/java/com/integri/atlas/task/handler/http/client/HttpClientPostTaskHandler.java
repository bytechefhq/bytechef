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

import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.TASK_HTTP_CLIENT;

import com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.RequestMethod;
import com.integri.atlas.task.handler.http.client.body.HttpBodyFactory;
import com.integri.atlas.task.handler.http.client.header.HttpHeaderFactory;
import com.integri.atlas.task.handler.http.client.params.HttpQueryParamsFactory;
import com.integri.atlas.task.handler.http.client.response.HttpResponseHandler;
import org.springframework.stereotype.Component;

/**
 * @author Iivca Cardic
 */
@Component(TASK_HTTP_CLIENT + "/post")
public class HttpClientPostTaskHandler extends HttpClientBaseTaskHandler {

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
