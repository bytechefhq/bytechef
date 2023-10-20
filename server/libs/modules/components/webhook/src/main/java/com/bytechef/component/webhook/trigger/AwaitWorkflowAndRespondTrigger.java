
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

import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.StaticWebhookRequestContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookHeaders;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookValidateContext;

import java.util.Map;
import java.util.Objects;

import static com.bytechef.component.webhook.constant.WebhookConstants.BODY;
import static com.bytechef.component.webhook.constant.WebhookConstants.CSRF_TOKEN;
import static com.bytechef.component.webhook.constant.WebhookConstants.HEADERS;
import static com.bytechef.component.webhook.constant.WebhookConstants.METHOD;
import static com.bytechef.component.webhook.constant.WebhookConstants.PARAMETERS;
import static com.bytechef.component.webhook.constant.WebhookConstants.PATH;
import static com.bytechef.component.webhook.constant.WebhookConstants.X_CRSF_TOKEN;
import static com.bytechef.hermes.component.definition.ComponentDSL.trigger;
import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.oneOf;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class AwaitWorkflowAndRespondTrigger {

    public static final TriggerDefinition TRIGGER_DEFINITION = trigger("awaitWorkflowAndRespond")
        .display(display("Await workflow and respond").description(
            "You have the flexibility to set up your preferred response. After a webhook request is received, the webhook trigger enters a waiting state for the workflow's response."))
        .type(TriggerType.STATIC_WEBHOOK)
        .executeWorkflowSynchronously(true)
        .properties(
            string(CSRF_TOKEN)
                .label("CSRF Token")
                .description(
                    "To trigger the workflow successfully, the security token must match the X-Csrf-Token HTTP header value passed by the client."),
            integer("timeout")
                .label("Timeout (ms)")
                .description(
                    "The incoming request will time out after the specified number of milliseconds. The max wait time before a timeout is 5 minutes."))
        .outputSchema(
            object()
                .properties(
                    object(HEADERS),
                    object(PARAMETERS),
                    oneOf(BODY).types(array(), object())))
        .staticWebhookRequest(AwaitWorkflowAndRespondTrigger::staticWebhookRequest)
        .webhookValidate(AwaitWorkflowAndRespondTrigger::webhookValidate);

    protected static TriggerDefinition.WebhookOutput staticWebhookRequest(StaticWebhookRequestContext context) {
        TriggerDefinition.WebhookBody webhookBody = context.body();

        return TriggerDefinition.WebhookOutput.map(
            Map.of(
                BODY, webhookBody.getContent(),
                METHOD, context.method(),
                HEADERS, context.headers(),
                PARAMETERS, context.parameters(),
                PATH, context.path()));
    }

    protected static boolean webhookValidate(WebhookValidateContext context) {
        WebhookHeaders webhookHeaders = context.headers();
        InputParameters inputParameters = context.inputParameters();

        return Objects.equals(webhookHeaders.getValue(X_CRSF_TOKEN), inputParameters.get(CSRF_TOKEN));
    }
}
