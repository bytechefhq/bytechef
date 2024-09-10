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

package com.bytechef.platform.configuration.dto;

import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.registry.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.registry.domain.TaskDispatcherDefinition;

/**
 * @author Ivica Cardic
 */
public record WorkflowNodeOutputDTO(
    ActionDefinition actionDefinition, Property outputSchema, Object sampleOutput,
    TaskDispatcherDefinition taskDispatcherDefinition, TriggerDefinition triggerDefinition, String workflowNodeName) {

    public WorkflowNodeOutputDTO(
        ActionDefinition actionDefinition, OutputResponse outputResponse,
        TaskDispatcherDefinition taskDispatcherDefinition, TriggerDefinition triggerDefinition,
        String workflowNodeName) {

        this(
            actionDefinition, outputResponse == null ? null : (Property) outputResponse.outputSchema(),
            outputResponse == null ? null : outputResponse.sampleOutput(), taskDispatcherDefinition,
            triggerDefinition, workflowNodeName);
    }
}
