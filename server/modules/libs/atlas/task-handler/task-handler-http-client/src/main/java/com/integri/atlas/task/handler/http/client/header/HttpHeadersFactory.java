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
public class HttpHeadersFactory {

    public List<HttpHeader> getHttpHeaders(TaskExecution taskExecution) {
        List<HttpHeader> httpHeaders = new ArrayList<>();

        if (taskExecution.containsKey("headerParametersRaw")) {
            httpHeaders.addAll(fromHeaderParametersRaw(taskExecution.getString("headerParametersRaw")));
        } else if (taskExecution.containsKey("headerParametersKeyValue")) {
            httpHeaders.addAll(
                fromHeaderParametersKeyValue(taskExecution.get("headerParametersKeyValue", MultiValueMap.class))
            );
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

    private List<HttpHeader> fromHeaderParametersKeyValue(MultiValueMap<String, String> headerParametersKeyValue) {
        List<HttpHeader> httpHeaders = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : headerParametersKeyValue.entrySet()) {
            httpHeaders.add(new HttpHeader(entry.getKey(), StringUtils.join(entry.getValue(), ',')));
        }

        return httpHeaders;
    }

    private List<HttpHeader> fromHeaderParametersRaw(String headerParametersRaw) {
        List<HttpHeader> httpHeaders = new ArrayList<>();

        String[] headersArray = headerParametersRaw.split(System.lineSeparator());

        for (String header : headersArray) {
            String[] headerParts = header.split(":");

            httpHeaders.add(new HttpHeader(headerParts[0], headerParts[1]));
        }

        return httpHeaders;
    }
}
