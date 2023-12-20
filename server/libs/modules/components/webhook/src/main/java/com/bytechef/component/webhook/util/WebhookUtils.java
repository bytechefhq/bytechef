/*
 * Copyright 2023-present ByteChef Inc.
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

import com.bytechef.hermes.component.definition.Parameters;
import com.bytechef.hermes.component.definition.TriggerContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.hermes.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.hermes.component.definition.TriggerDefinition.StaticWebhookRequestFunction;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookOutput;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookValidateFunction;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class WebhookUtils {

    public static StaticWebhookRequestFunction getStaticWebhookRequestFunction() {
        return (
            Parameters inputParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body,
            WebhookMethod method, TriggerContext triggerContext) -> {

            if (body == null) {
                return WebhookOutput.map(
                    Map.of(
                        METHOD, method,
                        HEADERS, headers,
                        PARAMETERS, parameters));
            } else {
                return WebhookOutput.map(
                    Map.of(
                        BODY, body.getContent(),
                        METHOD, method,
                        HEADERS, headers,
                        PARAMETERS, parameters));
            }
        };
    }

    public static WebhookValidateFunction getWebhookValidateFunction() {
        return (inputParameters, headers, parameters, body, method, triggerContext) -> Objects.equals(
            getCsrfToken(headers), inputParameters.getRequiredString(CSRF_TOKEN));
    }

    private static String getCsrfToken(HttpHeaders headers) {
        return headers
            .firstValue("x-csrf-token")
            .orElse(null);
    }
}
