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

package com.bytechef.component.clickup.trigger;

import static com.bytechef.component.clickup.constant.ClickupConstants.ID;
import static com.bytechef.component.clickup.constant.ClickupConstants.NAME;
import static com.bytechef.component.clickup.constant.ClickupConstants.WORKSPACE_ID;
import static com.bytechef.component.clickup.util.ClickupUtils.getCreatedObject;
import static com.bytechef.component.clickup.util.ClickupUtils.subscribeWebhook;
import static com.bytechef.component.clickup.util.ClickupUtils.unsubscribeWebhook;
import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.clickup.util.ClickupUtils;
import com.bytechef.component.definition.OptionsDataSource.TriggerOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class ClickupNewListTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newList")
        .title("New List")
        .description("Triggers when new list is created.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(WORKSPACE_ID)
                .label("Workspace ID")
                .options((TriggerOptionsFunction<String>) ClickupUtils::getWorkspaceIdOptions)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID),
                        string(NAME),
                        object("folder")
                            .properties(
                                string(ID),
                                string(NAME)),
                        object("space")
                            .properties(
                                string(ID),
                                string(NAME)))))
        .webhookEnable(ClickupNewListTrigger::webhookEnable)
        .webhookDisable(ClickupNewListTrigger::webhookDisable)
        .webhookRequest(ClickupNewListTrigger::webhookRequest);

    private ClickupNewListTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        return new WebhookEnableOutput(
            Map.of(ID,
                subscribeWebhook(webhookUrl, context, inputParameters.getRequiredString(WORKSPACE_ID), "listCreated")),
            null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        unsubscribeWebhook(context, outputParameters.getString(ID));
    }

    protected static Map<String, Object> webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, WebhookEnableOutput output, TriggerContext context) {

        return getCreatedObject(body, context, "list_id", "/list/");
    }

}
