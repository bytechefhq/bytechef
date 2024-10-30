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

package com.bytechef.component.webhook.trigger;

import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.webhook.constant.WebhookConstants.CSRF_TOKEN;
import static com.bytechef.component.webhook.constant.WebhookConstants.HEADERS;
import static com.bytechef.component.webhook.constant.WebhookConstants.METHOD;
import static com.bytechef.component.webhook.constant.WebhookConstants.PARAMETERS;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.webhook.util.WebhookUtils;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class WebhookValidateAndRespondTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("validateAndRespond")
        .title("Validate and respond")
        .description(
            "Upon receiving a webhook request, it goes through a validation process. Once validated, the webhook trigger responds to the sender with an appropriate HTTP status code.")
        .type(TriggerDefinition.TriggerType.STATIC_WEBHOOK)
        .properties(
            string(CSRF_TOKEN)
                .label("CSRF Token")
                .description(
                    "To trigger the workflow successfully, the security token must match the X-Csrf-Token HTTP header value passed by the client.")
                .required(true))
        .output(
            sampleOutput(
                Map.of(METHOD, "POST", HEADERS, Map.of("Header1", "value"), PARAMETERS, Map.of("parameter1", "value"))))
        .webhookRequest(WebhookUtils::getWebhookResult)
        .webhookValidate(WebhookUtils.getWebhookValidateFunction());
}
