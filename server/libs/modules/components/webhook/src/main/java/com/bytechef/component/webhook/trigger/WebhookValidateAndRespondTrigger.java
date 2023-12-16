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

import static com.bytechef.component.webhook.constant.WebhookConstants.CSRF_TOKEN;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.definition.ComponentDSL.trigger;

import com.bytechef.component.webhook.util.WebhookUtils;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource;
import com.bytechef.hermes.component.definition.TriggerDefinition;

/**
 * @author Ivica Cardic
 */
public class WebhookValidateAndRespondTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("validateAndRespond")
        .title("Validate and respond")
        .description(
            "Upon receiving a webhook request, it goes through a validation process. Once validated, the webhook trigger responds to the sender with an appropriate HTTP status code.")
        .type(TriggerDefinition.TriggerType.STATIC_WEBHOOK)
        .workflowSyncValidation(true)
        .properties(
            string(CSRF_TOKEN)
                .label("CSRF Token")
                .description(
                    "To trigger the workflow successfully, the security token must match the X-Csrf-Token HTTP header value passed by the client.")
                .required(true))
        .outputSchema(getOutputSchemaFunction())
        .staticWebhookRequest(WebhookUtils.getStaticWebhookRequestFunction())
        .webhookValidate(WebhookUtils.getWebhookValidateFunction());

    protected static OutputSchemaDataSource.TriggerOutputSchemaFunction getOutputSchemaFunction() {
        // TODO
        return (inputParameters, connectionParameters, context) -> null;
//            object()
//                .properties(
//                    string(METHOD),
//                    object(HEADERS),
//                    object(PARAMETERS),
//                    any(BODY));
    }
}
