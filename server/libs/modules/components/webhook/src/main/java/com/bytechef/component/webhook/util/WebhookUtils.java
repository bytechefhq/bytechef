
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

package com.bytechef.component.webhook.util;

import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.StaticWebhookRequestFunction;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookOutput;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookValidateFunction;
import com.bytechef.hermes.component.util.MapUtils;

import java.util.Map;
import java.util.Objects;

import static com.bytechef.component.webhook.constant.WebhookConstants.BODY;
import static com.bytechef.component.webhook.constant.WebhookConstants.CSRF_TOKEN;
import static com.bytechef.component.webhook.constant.WebhookConstants.HEADERS;
import static com.bytechef.component.webhook.constant.WebhookConstants.METHOD;
import static com.bytechef.component.webhook.constant.WebhookConstants.PARAMETERS;

/**
 * @author Ivica Cardic
 */
public class WebhookUtils {

    public static StaticWebhookRequestFunction getStaticWebhookRequestFunction() {
        return context -> {
            WebhookBody webhookBody = context.body();

            if (webhookBody == null) {
                return WebhookOutput.map(
                    Map.of(
                        METHOD, context.method(),
                        HEADERS, context.headers(),
                        PARAMETERS, context.parameters()));
            } else {
                return WebhookOutput.map(
                    Map.of(
                        BODY, webhookBody.content(),
                        METHOD, context.method(),
                        HEADERS, context.headers(),
                        PARAMETERS, context.parameters()));
            }
        };
    }

    public static WebhookValidateFunction getWebhookValidateFunction() {
        return context -> Objects.equals(
            getCsrfToken(context), MapUtils.getRequiredString(context.inputParameters(), CSRF_TOKEN));
    }

    private static String getCsrfToken(TriggerDefinition.WebhookValidateContext context) {
        return context.headers()
            .firstValue("x-csrf-token")
            .orElse(null);
    }
}
