/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.apiplatform.util;

import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.BODY;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.HEADERS;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.METHOD;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.PARAMETERS;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ApiPlatformUtils {

    public static Map<String, ?> getWebhookResult(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, WebhookEnableOutput webhookEnableOutput, TriggerContext context) {

        Map<String, ?> headerMap = headers.toMap();
        Map<String, ?> parameterMap = parameters.toMap();

        if (body == null) {
            return Map.of(
                METHOD, method,
                HEADERS, headerMap.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, ApiPlatformUtils::checkList)),
                PARAMETERS, parameterMap.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, ApiPlatformUtils::checkList)));
        } else {
            return Map.of(
                BODY, body.getContent(),
                METHOD, method,
                HEADERS, headerMap.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, ApiPlatformUtils::checkList)),
                PARAMETERS, parameterMap.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, ApiPlatformUtils::checkList)));
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
