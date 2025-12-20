/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.request.util;

import static com.bytechef.component.request.constant.RequestConstants.BODY;
import static com.bytechef.component.request.constant.RequestConstants.HEADERS;
import static com.bytechef.component.request.constant.RequestConstants.METHOD;
import static com.bytechef.component.request.constant.RequestConstants.PARAMETERS;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
public class RequestUtils {

    public static Map<String, ?> getRequestResult(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {

        Map<String, ?> headerMap = headers.toMap();
        Map<String, ?> parameterMap = parameters.toMap();

        if (body == null) {
            return Map.of(
                METHOD, method,
                HEADERS, headerMap
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, RequestUtils::checkList)),
                PARAMETERS, parameterMap
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, RequestUtils::checkList)));
        } else {
            return Map.of(
                BODY, body.getContent(),
                METHOD, method,
                HEADERS, headerMap
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, RequestUtils::checkList)),
                PARAMETERS, parameterMap
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, RequestUtils::checkList)));
        }
    }

    private static Object checkList(Map.Entry<String, ?> entry) {
        Object value = entry.getValue();

        if (value instanceof List<?> list && list.size() == 1) {
            value = list.getFirst();
        }

        return value;
    }
}
