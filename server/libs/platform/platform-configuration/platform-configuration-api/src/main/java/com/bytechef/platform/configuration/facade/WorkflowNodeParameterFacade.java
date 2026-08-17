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

import com.bytechef.platform.configuration.dto.DisplayConditionResultDTO;
import com.bytechef.platform.configuration.dto.ParameterResultDTO;
import java.util.Map;
import java.util.Set;

/**
 * @author Ivica Cardic
 */
public interface WorkflowNodeParameterFacade {

    ParameterResultDTO deleteClusterElementParameter(
        String workflowId, String workflowNodeName, String clusterElementTypeName,
        String clusterElementWorkflowNodeName, String parameterPath, long environmentId);

    ParameterResultDTO deleteWorkflowNodeParameter(
        String workflowId, String workflowNodeName, String parameterPath, long environmentId);

    /**
     * Evaluates an operation's display conditions against a standalone parameter map, with no workflow involved.
     *
     * <p>
     * Every other display-condition path here starts from a workflow: it loads the definition to find the node's
     * properties and parameters, and builds a test context for data pills and upstream outputs. The property forms
     * outside the workflow editor — a tool config dialog, an MCP tool popover, a connection dialog — have none of that,
     * so they could not evaluate conditions at all and rendered every conditional property unconditionally. A
     * standalone form has no data pills and no upstream nodes, so the input map and previous outputs are empty and only
     * the supplied parameters decide.
     * </p>
     *
     * @param parameters the form's current values, which is what the conditions are evaluated against
     */
    Map<String, Boolean> getDisplayConditions(
        String componentName, int componentVersion, String operationName, OperationType operationType,
        Map<String, ?> parameters);

    /**
     * Which kind of operation the properties belong to. Mirrors the workflow-backed paths' own distinction: a trigger's
     * conditions are evaluated the same way a task's are, but the definition they come from differs.
     */
    enum OperationType {
        ACTION, CLUSTER_ELEMENT, TRIGGER
    }

    DisplayConditionResultDTO getClusterElementDisplayConditions(
        String workflowId, String workflowNodeName, String clusterElementTypeName,
        String clusterElementWorkflowNodeName, long environmentId);

    Set<String> getClusterElementMissingRequiredProperties(
        String workflowId, String workflowNodeName, String clusterElementTypeName,
        String clusterElementWorkflowNodeName);

    DisplayConditionResultDTO
        getWorkflowNodeDisplayConditions(String workflowId, String workflowNodeName, long environmentId);

    Set<String> getWorkflowNodeMissingRequiredProperties(String workflowId, String workflowNodeName);

    ParameterResultDTO updateClusterElementParameter(
        String workflowId, String workflowNodeName, String clusterElementTypeName,
        String clusterElementWorkflowNodeName, String parameterPath, Object value, String type,
        boolean fromAiInMetadata, boolean includeInMetadata, long environmentId);

    ParameterResultDTO updateWorkflowNodeParameter(
        String workflowId, String workflowNodeName, String parameterPath, Object value, String type,
        boolean includeInMetadata, long environmentId);
}
