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

package com.integri.atlas.task.handler.http.client.params;

import com.integri.atlas.engine.core.task.TaskExecution;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

/**
 * @author Matija Petanjek
 */
@Component
public class QueryParamsFactory {

    public String getQueryParams(TaskExecution taskExecution) {
        if (taskExecution.containsKey("queryParametersRaw")) {
            return taskExecution.getString("queryParametersRaw");
        } else if (taskExecution.containsKey("queryParametersKeyValue")) {
            return fromQueryParametersKeyValue(taskExecution);
        }

        return "";
    }

    private String fromQueryParametersKeyValue(TaskExecution taskExecution) {
        MultiValueMap<String, String> queryParameters = taskExecution.get(
            "queryParametersKeyValue",
            MultiValueMap.class
        );

        List<String> queryParameterList = new ArrayList<>();

        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(StringUtils.join(entry.getValue(), ","));

            queryParameterList.add(sb.toString());
        }

        return StringUtils.join(queryParameterList, "&");
    }
}
