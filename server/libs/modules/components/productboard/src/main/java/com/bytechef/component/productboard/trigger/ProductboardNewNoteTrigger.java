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

package com.bytechef.component.productboard.trigger;

import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.productboard.constant.ProductboardConstants.ID;
import static com.bytechef.component.productboard.util.ProductboardUtils.createSubscription;
import static com.bytechef.component.productboard.util.ProductboardUtils.deleteSubscription;
import static com.bytechef.component.productboard.util.ProductboardUtils.getContent;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.productboard.util.ProductboardUtils;

/**
 * @author Monika Kušter
 */
public class ProductboardNewNoteTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newNote")
        .title("New Note")
        .description("Triggers when a note is created.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID)
                            .description("ID of the note."),
                        string("eventType")
                            .description("Type of the event that triggered the webhook."),
                        object("links")
                            .description("Links to the updated entity.")
                            .properties(
                                string("target")
                                    .description(
                                        "Link to the entity whose change triggered this webhook notification.")))))
        .webhookEnable(ProductboardNewNoteTrigger::webhookEnable)
        .webhookDisable(ProductboardNewNoteTrigger::webhookDisable)
        .webhookRequest(ProductboardNewNoteTrigger::webhookRequest)
        .webhookValidateOnEnable(ProductboardUtils::webhookValidateOnEnable);

    private ProductboardNewNoteTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        return createSubscription(webhookUrl, workflowExecutionId, context, "note.created");
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        deleteSubscription(context, outputParameters.getRequiredString(ID));
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {

        return getContent(body);
    }
}
