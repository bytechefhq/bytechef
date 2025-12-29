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

package com.bytechef.platform.configuration.dto;

import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public record WorkflowNodeOutputDTO(
    @Nullable ActionDefinition actionDefinition, @Nullable ClusterElementDefinition clusterElementDefinition,
    @Nullable OutputResponse outputResponse, @Nullable TaskDispatcherDefinition taskDispatcherDefinition,
    boolean testOutputResponse, @Nullable TriggerDefinition triggerDefinition,
    @Nullable OutputResponse variableOutputResponse, String workflowNodeName) {

    public WorkflowNodeOutputDTO(
        @Nullable ActionDefinition actionDefinition, @Nullable ClusterElementDefinition clusterElementDefinition,
        @Nullable OutputResponse outputResponse, @Nullable TaskDispatcherDefinition taskDispatcherDefinition,
        boolean testOutputResponse, @Nullable TriggerDefinition triggerDefinition, String workflowNodeName) {

        this(
            actionDefinition, clusterElementDefinition, outputResponse, taskDispatcherDefinition,
            testOutputResponse, triggerDefinition, null, workflowNodeName);
    }

    public Object getSampleOutput() {
        if (outputResponse != null) {
            return outputResponse.sampleOutput();
        }

        return null;
    }

    public Object getVariableSampleOutput() {
        if (variableOutputResponse != null) {
            return variableOutputResponse.sampleOutput();
        }

        return null;
    }
}
