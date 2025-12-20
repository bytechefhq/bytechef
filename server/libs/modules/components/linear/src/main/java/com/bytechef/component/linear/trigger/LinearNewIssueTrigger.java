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

package com.bytechef.component.linear.trigger;

import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.linear.constant.LinearConstants.ALL_PUBLIC_TEAMS_PROPERTY;
import static com.bytechef.component.linear.constant.LinearConstants.ID;
import static com.bytechef.component.linear.constant.LinearConstants.TEAM_ID_TRIGGER_PROPERTY;
import static com.bytechef.component.linear.constant.LinearConstants.TRIGGER_OUTPUT_PROPERTY;
import static com.bytechef.component.linear.util.LinearUtils.createWebhook;
import static com.bytechef.component.linear.util.LinearUtils.deleteWebhook;
import static com.bytechef.component.linear.util.LinearUtils.executeIssueTriggerQuery;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;

/**
 * @author Marija Horvat
 */
public class LinearNewIssueTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newIssue")
        .title("New Issue")
        .description("Triggers when new issue is created.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            ALL_PUBLIC_TEAMS_PROPERTY,
            TEAM_ID_TRIGGER_PROPERTY)
        .output(outputSchema(TRIGGER_OUTPUT_PROPERTY))
        .webhookEnable(LinearNewIssueTrigger::webhookEnable)
        .webhookDisable(LinearNewIssueTrigger::webhookDisable)
        .webhookRequest(LinearNewIssueTrigger::webhookRequest);

    private LinearNewIssueTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        return createWebhook(webhookUrl, context, inputParameters);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        deleteWebhook(outputParameters.getString(ID), context);
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {

        return executeIssueTriggerQuery("create", body, context);
    }
}
