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

package com.bytechef.platform.configuration.facade;

import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.workflow.WorkflowExecutionId;

/**
 * @author Ivica Cardic
 */
public interface WorkflowNodeTestOutputFacade {

    WorkflowNodeTestOutput saveClusterElementTestOutput(
        String workflowId, String workflowNodeName, String clusterElementType,
        String clusterElementWorkflowNodeName, long environmentId);

    WorkflowNodeTestOutput saveWorkflowNodeSampleOutput(
        String workflowId, String workflowNodeName, Object sampleOutput, long environmentId);

    WorkflowNodeTestOutput saveWorkflowNodeTestOutput(String workflowId, String workflowNodeName, long environmentId);

    void saveWorkflowNodeTestOutput(
        WorkflowExecutionId workflowExecutionId, long environmentId, WebhookRequest webhookRequest);
}
