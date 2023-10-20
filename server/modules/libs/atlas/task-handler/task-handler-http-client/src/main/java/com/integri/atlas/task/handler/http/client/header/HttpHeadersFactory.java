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

package com.integri.atlas.task.handler.http.client.header;

import com.integri.atlas.engine.core.json.JSONHelper;
import com.integri.atlas.engine.core.task.TaskExecution;
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
public class HttpHeadersFactory {

    private final JSONHelper jsonHelper;

    public HttpHeadersFactory(JSONHelper jsonHelper) {
        this.jsonHelper = jsonHelper;
    }

    @SuppressWarnings("unchecked")
    public List<HttpHeader> getHttpHeaders(TaskExecution taskExecution) {
        List<HttpHeader> httpHeaders = new ArrayList<>();

        if (taskExecution.containsKey("headerParameters")) {
            if (taskExecution.getBoolean("rawParameters", false)) {
                httpHeaders.addAll(
                    fromHeaderParameters(
                        jsonHelper.checkJSONObject(taskExecution.get("headerParameters"), String.class),
                        (String value) -> value
                    )
                );
            } else {
                httpHeaders.addAll(
                    fromHeaderParameters(
                        taskExecution.get("headerParameters", MultiValueMap.class),
                        (List<String> values) -> StringUtils.join(values, ',')
                    )
                );
            }
        }

        if (taskExecution.containsKey("responseFormat")) {
            httpHeaders.add(
                new HttpHeader("Accept", ContentType.valueOf(taskExecution.get("responseFormat")).getMimeType())
            );
        }

        if (taskExecution.containsKey("bodyContentType")) {
            httpHeaders.add(
                new HttpHeader("Content-Type", ContentType.valueOf(taskExecution.get("bodyContentType")).getMimeType())
            );
        }

        return httpHeaders;
    }

    private <T> List<HttpHeader> fromHeaderParameters(
        Map<String, T> headerParameters,
        Function<T, String> entryValueFunction
    ) {
        List<HttpHeader> httpHeaders = new ArrayList<>();

        for (Map.Entry<String, T> entry : headerParameters.entrySet()) {
            httpHeaders.add(new HttpHeader(entry.getKey(), entryValueFunction.apply(entry.getValue())));
        }

        return httpHeaders;
    }
}
