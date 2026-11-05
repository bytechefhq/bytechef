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

package com.bytechef.component.form.trigger;

import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;

/**
 * @author Ivica Cardic
 */
public class NewFormRequestRespondImmediatelyTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION =
        trigger("newFormRequestRespondImmediately")
            .title("New Form Request (Respond Immediately)")
            .description(
                "Triggers when a new form request is received. The submitter gets a response right away while the " +
                    "workflow runs in the background.")
            .type(TriggerType.STATIC_WEBHOOK)
            .properties(NewFormRequestTrigger.PROPERTIES)
            .output(NewFormRequestTrigger::getOutput)
            .help("", "https://docs.bytechef.io/reference/components/form_v1#new-form-request-respond-immediately")
            .webhookRequest(NewFormRequestTrigger::getWebhookResult);
}
