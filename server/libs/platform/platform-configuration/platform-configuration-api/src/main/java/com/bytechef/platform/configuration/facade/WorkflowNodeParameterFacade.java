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

/**
 * @author Ivica Cardic
 */
public interface WorkflowNodeParameterFacade {

    ParameterResultDTO deleteClusterElementParameter(
        String workflowId, String workflowNodeName, String clusterElementTypeName,
        String clusterElementWorkflowNodeName, String parameterPath, long environmentId);

    ParameterResultDTO deleteWorkflowNodeParameter(
        String workflowId, String workflowNodeName, String parameterPath, long environmentId);

    DisplayConditionResultDTO getClusterElementDisplayConditions(
        String workflowId, String workflowNodeName, String clusterElementTypeName,
        String clusterElementWorkflowNodeName, long environmentId);

    DisplayConditionResultDTO
        getWorkflowNodeDisplayConditions(String workflowId, String workflowNodeName, long environmentId);

    ParameterResultDTO updateClusterElementParameter(
        String workflowId, String workflowNodeName, String clusterElementTypeName,
        String clusterElementWorkflowNodeName, String parameterPath, Object value, String type,
        boolean fromAiInMetadata, boolean includeInMetadata, long environmentId);

    ParameterResultDTO updateWorkflowNodeParameter(
        String workflowId, String workflowNodeName, String parameterPath, Object value, String type,
        boolean includeInMetadata, long environmentId);
}
