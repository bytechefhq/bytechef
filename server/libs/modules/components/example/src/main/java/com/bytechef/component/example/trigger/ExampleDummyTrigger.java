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

package com.bytechef.component.example.trigger;

import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.example.constant.ExampleConstants.DUMMY_TRIGGER;
import static com.bytechef.component.example.constant.ExampleConstants.ID;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import java.util.List;
import java.util.Objects;

public class ExampleDummyTrigger {
    public static final ComponentDsl.ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger(DUMMY_TRIGGER)
        .title("Updated Issue")
        .description("Triggers when an issue is updated.")
        .type(TriggerDefinition.TriggerType.DYNAMIC_WEBHOOK)
        .properties()
        .output(outputSchema(string()))
        .webhookEnable(ExampleDummyTrigger::webhookEnable) // TriggerType.DYNAMIC_WEBHOOK
        .webhookDisable(ExampleDummyTrigger::webhookDisable) // TriggerType.DYNAMIC_WEBHOOK
        .webhookRequest(ExampleDummyTrigger::webhookRequest) // every type
        .webhookValidate(ExampleDummyTrigger::webhookValidate); // TriggerType.STATIC_WEBHOOK

    private ExampleDummyTrigger() {
    }

    protected static TriggerDefinition.WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {
        // TODO

        return null;
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        context
            .http(http -> http.delete("/webhook"))
            .body(Context.Http.Body.of("webhookIds", List.of(outputParameters.getInteger(ID))))
            .configuration(Context.Http.responseType(Context.Http.ResponseType.JSON))
            .execute();
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, TriggerDefinition.HttpHeaders headers,
        TriggerDefinition.HttpParameters parameters, TriggerDefinition.WebhookBody body,
        TriggerDefinition.WebhookMethod method, TriggerDefinition.WebhookEnableOutput output,
        TriggerContext context) {
        // TODO

        return null;
    }

    protected static TriggerDefinition.WebhookValidateResponse webhookValidate(
        Parameters parameters, TriggerDefinition.HttpHeaders httpHeaders,
        TriggerDefinition.HttpParameters httpParameters, TriggerDefinition.WebhookBody webhookBody,
        TriggerDefinition.WebhookMethod webhookMethod, TriggerContext triggerContext) {
        if (Objects.equals("uselessCode", "betterExamplesInActualComponents")) {
            return TriggerDefinition.WebhookValidateResponse.ok(); // OK
        } else {
            return TriggerDefinition.WebhookValidateResponse.badRequest(); // Bad Request
        }
    }
}
