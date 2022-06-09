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

package com.integri.atlas.task.handler.httpclient.params;

import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.KEY;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.PROPERTY_QUERY_PARAMETERS;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.VALUE;

import com.integri.atlas.engine.task.execution.TaskExecution;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 */
@Component
public class HttpQueryParamsFactory {

    @SuppressWarnings("unchecked")
    public String getQueryParams(TaskExecution taskExecution) {
        String queryParams = "";

        if (taskExecution.containsKey(PROPERTY_QUERY_PARAMETERS)) {
            queryParams = fromQueryParameters(taskExecution.get(PROPERTY_QUERY_PARAMETERS, List.class));
        }

        return queryParams;
    }

    private String fromQueryParameters(List<Map<String, String>> queryParameters) {
        List<String> queryParameterList = new ArrayList<>();

        StringBuilder sb = new StringBuilder();

        for (Map<String, String> parameter : queryParameters) {
            sb.append(parameter.get(KEY));
            sb.append("=");
            sb.append(parameter.get(VALUE));

            queryParameterList.add(sb.toString());
        }

        return StringUtils.join(queryParameterList, "&");
    }
}
