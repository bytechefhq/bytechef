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

package com.bytechef.component.webhook.util;

import static com.bytechef.component.webhook.constant.WebhookConstants.BODY;
import static com.bytechef.component.webhook.constant.WebhookConstants.CSRF_TOKEN;
import static com.bytechef.component.webhook.constant.WebhookConstants.HEADERS;
import static com.bytechef.component.webhook.constant.WebhookConstants.METHOD;
import static com.bytechef.component.webhook.constant.WebhookConstants.PARAMETERS;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
public class WebhookUtils {

    public static Map<String, ?> getWebhookResult(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {

        Map<String, ?> headerMap = headers.toMap();
        Map<String, ?> parameterMap = parameters.toMap();

        if (body == null) {
            return Map.of(
                METHOD, method,
                HEADERS, headerMap.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, WebhookUtils::checkList)),
                PARAMETERS, parameterMap.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, WebhookUtils::checkList)));
        } else {
            return Map.of(
                BODY, body.getContent(),
                METHOD, method,
                HEADERS, headerMap.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, WebhookUtils::checkList)),
                PARAMETERS, parameterMap.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, WebhookUtils::checkList)));
        }
    }

    public static WebhookValidateResponse getWebhookValidate(
        Parameters inputParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body,
        WebhookMethod method, TriggerContext context) {

        if (Objects.equals(getCsrfToken(headers), inputParameters.getRequiredString(CSRF_TOKEN))) {
            return WebhookValidateResponse.ok(); // OK
        } else {
            return WebhookValidateResponse.badRequest(); // Bad Request
        }
    }

    private static Object checkList(Map.Entry<String, ?> entry) {
        Object value = entry.getValue();

        if (value instanceof List<?> list && list.size() == 1) {
            value = list.getFirst();
        }

        return value;
    }

    private static String getCsrfToken(HttpHeaders headers) {
        return headers
            .firstValue("x-csrf-token")
            .orElse(null);
    }
}
