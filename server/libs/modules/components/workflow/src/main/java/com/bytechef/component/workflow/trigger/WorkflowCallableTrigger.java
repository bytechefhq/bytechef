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

package com.bytechef.component.workflow.trigger;

import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.definition.Property.ControlType.JSON_SCHEMA_BUILDER;
import static com.bytechef.component.workflow.constant.WorkflowConstants.CALLABLE;
import static com.bytechef.component.workflow.constant.WorkflowConstants.INPUT_SCHEMA;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorkflowCallableTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger(CALLABLE)
        .title("Callable trigger")
        .description(
            "Triggers when this workflow is called from another workflow. Define the input schema to specify what data the calling workflow should provide.")
        .type(TriggerType.STATIC_WEBHOOK)
        .workflowSyncExecution(true)
        .properties(
            string(INPUT_SCHEMA)
                .label("Inputs")
                .placeholder("Edit Inputs schema")
                .description("The schema definition for the input data this workflow expects from callers.")
                .controlType(JSON_SCHEMA_BUILDER))
        .output(WorkflowCallableTrigger::output)
        .webhookRequest(WorkflowCallableTrigger::webhookResult);

    protected static OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, TriggerContext context) {

        String inputSchema = inputParameters.getString(INPUT_SCHEMA);
        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        ModifiableValueProperty<?, ?> input = (ModifiableValueProperty<?, ?>) context.outputSchema(
            outputSchema -> outputSchema.getOutputSchema(INPUT_SCHEMA, inputSchema));

        if (input != null) {
            properties.add(input);
        }

        return OutputResponse.of(object().properties(properties));
    }

    protected static Map<String, ?> webhookResult(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters webhookEnableOutput, TriggerContext context) {

        if (body != null) {
            return Map.of(INPUT_SCHEMA, body.getContent());
        }

        return Map.of();
    }
}
