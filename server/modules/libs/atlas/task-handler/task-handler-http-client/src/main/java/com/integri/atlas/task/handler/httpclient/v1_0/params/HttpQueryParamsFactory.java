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

package com.integri.atlas.task.handler.httpclient.v1_0.params;

import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.KEY;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.PROPERTY_QUERY_PARAMETERS;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.VALUE;

import com.integri.atlas.engine.task.execution.TaskExecution;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 */
@Component
public class HttpQueryParamsFactory {

    @SuppressWarnings("unchecked")
    public List<HttpQueryParam> getQueryParams(TaskExecution taskExecution) {
        List<HttpQueryParam> queryParams = new ArrayList<>();

        if (taskExecution.containsKey(PROPERTY_QUERY_PARAMETERS)) {
            List<Map<String, String>> queryParameters = taskExecution.get(PROPERTY_QUERY_PARAMETERS, List.class);

            for (Map<String, String> queryParameter : queryParameters) {
                queryParams.add(new HttpQueryParam(queryParameter.get(KEY), queryParameter.get(VALUE)));
            }
        }

        return queryParams;
    }
}
