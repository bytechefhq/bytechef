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

import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.workflow.constant.WorkflowConstants.NEW_WORKFLOW_ERROR;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;

/**
 * Marks a workflow as an error handler. A workflow carrying this trigger can be selected as the error workflow for a
 * project or a single workflow; it is started by the coordinator when a run fails, not over HTTP.
 *
 * @author Ivica Cardic
 */
public class WorkflowNewWorkflowErrorTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger(NEW_WORKFLOW_ERROR)
        .title("New Workflow Error")
        .description(
            "Triggers when a workflow run fails. Set this workflow as the error workflow of a project or of a " +
                "single workflow to receive its failures.")
        .type(TriggerType.STATIC_WEBHOOK)
        .output(
            outputSchema(
                object()
                    .properties(
                        object("execution")
                            .properties(
                                string("jobId"),
                                string("url"),
                                object("error")
                                    .properties(string("message"), string("stackTrace")),
                                string("lastTaskExecuted"),
                                integer("autoRecoveryAttempts")),
                        object("workflow")
                            .properties(
                                string("projectId"),
                                string("projectWorkflowId"),
                                string("workflowId"),
                                string("label")),
                        string("environment"))));
}
