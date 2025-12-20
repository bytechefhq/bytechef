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

package com.bytechef.component.example.trigger;

import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.example.constant.ExampleConstants.ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import java.util.List;
import java.util.Objects;

public class ExampleDummyTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("dummyTrigger")
        .title("Updated Issue")
        .description("Triggers when an issue is updated.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties()
        .output(outputSchema(string()))
        .webhookEnable(ExampleDummyTrigger::webhookEnable)
        .webhookDisable(ExampleDummyTrigger::webhookDisable)
        .webhookRequest(ExampleDummyTrigger::webhookRequest)
        .webhookValidate(ExampleDummyTrigger::webhookValidate);

    private ExampleDummyTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
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
            .body(Http.Body.of("webhookIds", List.of(outputParameters.getInteger(ID))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();
    }

    protected static Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {
        // TODO

        return null;
    }

    protected static WebhookValidateResponse webhookValidate(
        Parameters parameters, HttpHeaders httpHeaders, HttpParameters httpParameters, WebhookBody webhookBody,
        WebhookMethod webhookMethod, TriggerContext triggerContext) {

        if (Objects.equals("uselessCode", "betterExamplesInActualComponents")) {
            return WebhookValidateResponse.ok(); // OK
        } else {
            return WebhookValidateResponse.badRequest(); // Bad Request
        }
    }
}
