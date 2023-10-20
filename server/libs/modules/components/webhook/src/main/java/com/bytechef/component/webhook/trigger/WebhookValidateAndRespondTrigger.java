
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

package com.bytechef.component.webhook.trigger;

import com.bytechef.hermes.component.definition.OutputSchemaDataSource;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.StaticWebhookRequestContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookHeaders;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookOutput;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookValidateContext;

import java.util.Map;
import java.util.Objects;

import static com.bytechef.component.webhook.constant.WebhookConstants.BODY;
import static com.bytechef.component.webhook.constant.WebhookConstants.CSRF_TOKEN;
import static com.bytechef.component.webhook.constant.WebhookConstants.HEADERS;
import static com.bytechef.component.webhook.constant.WebhookConstants.METHOD;
import static com.bytechef.component.webhook.constant.WebhookConstants.PARAMETERS;
import static com.bytechef.component.webhook.constant.WebhookConstants.X_CRSF_TOKEN;
import static com.bytechef.hermes.component.definition.ComponentDSL.trigger;

import static com.bytechef.hermes.definition.DefinitionDSL.any;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class WebhookValidateAndRespondTrigger {

    public static final TriggerDefinition TRIGGER_DEFINITION = trigger("validateAndRespond")
        .title("Validate and respond")
        .description(
            "Upon receiving a webhook request, it goes through a validation process. Once validated, the webhook trigger responds to the sender with an appropriate HTTP status code.")
        .type(TriggerDefinition.TriggerType.WEBHOOK_STATIC)
        .properties(
            string(CSRF_TOKEN)
                .label("CSRF Token")
                .description(
                    "To trigger the workflow successfully, the security token must match the X-Csrf-Token HTTP header value passed by the client."))
        .outputSchema(
            object()
                .properties(
                    string(METHOD),
                    object(HEADERS),
                    object(PARAMETERS),
                    any(BODY)))
        .outputSchema(getOutputSchemaFunction())
        .staticWebhookRequest(WebhookValidateAndRespondTrigger::staticWebhookRequest)
        .webhookValidate(WebhookValidateAndRespondTrigger::webhookValidate);

    protected static WebhookOutput staticWebhookRequest(StaticWebhookRequestContext context) {
        WebhookBody webhookBody = context.body();

        return WebhookOutput.map(
            Map.of(
                BODY, webhookBody.getContent(),
                METHOD, context.method(),
                HEADERS, context.headers(),
                PARAMETERS, context.parameters()));
    }

    protected static boolean webhookValidate(WebhookValidateContext context) {
        WebhookHeaders webhookHeaders = context.headers();
        Map<String, ?> inputParameters = context.inputParameters();

        return Objects.equals(webhookHeaders.getValue(X_CRSF_TOKEN), inputParameters.get(CSRF_TOKEN));
    }

    protected static OutputSchemaDataSource.OutputSchemaFunction getOutputSchemaFunction() {
        // TODO
        return (connection, inputParameters) -> null;
    }
}
