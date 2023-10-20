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

package com.integri.atlas.task.handler.http.client.params;

import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.task.handler.json.helper.JSONHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

/**
 * @author Matija Petanjek
 */
@Component
public class QueryParamsFactory {

    private final JSONHelper jsonHelper;

    public QueryParamsFactory(JSONHelper jsonHelper) {
        this.jsonHelper = jsonHelper;
    }

    @SuppressWarnings("unchecked")
    public String getQueryParams(TaskExecution taskExecution) {
        String queryParams = "";

        if (taskExecution.containsKey("queryParameters")) {
            if (taskExecution.getBoolean("rawParameters", false)) {
                queryParams =
                    fromQueryParameters(
                        jsonHelper.checkJSONObject(taskExecution.get("queryParameters"), String.class),
                        (String value) -> value
                    );
            } else {
                queryParams =
                    fromQueryParameters(
                        taskExecution.get("queryParameters", MultiValueMap.class),
                        (List<String> values) -> StringUtils.join(values, ",")
                    );
            }
        }

        return queryParams;
    }

    private <T> String fromQueryParameters(Map<String, T> queryParameters, Function<T, String> entryValueFunction) {
        List<String> queryParameterList = new ArrayList<>();

        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, T> entry : queryParameters.entrySet()) {
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entryValueFunction.apply(entry.getValue()));

            queryParameterList.add(sb.toString());
        }

        return StringUtils.join(queryParameterList, "&");
    }
}
