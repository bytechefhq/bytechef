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
import com.bytechef.platform.configuration.dto.UpdateParameterResultDTO;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public interface WorkflowNodeParameterFacade {

    Map<String, ?> deleteClusterElementParameter(
        String workflowId, String workflowNodeName, String clusterElementTypeName, String clusterElementName,
        String path);

    Map<String, ?> deleteWorkflowNodeParameter(String workflowId, String workflowNodeName, String path);

    DisplayConditionResultDTO getClusterElementDisplayConditions(
        String workflowId, String workflowNodeName, String clusterElementTypeName, String clusterElementName);

    DisplayConditionResultDTO getWorkflowNodeDisplayConditions(String workflowId, String workflowNodeName);

    UpdateParameterResultDTO updateClusterElementParameter(
        String workflowId, String workflowNodeName, String clusterElementTypeName, String clusterElementName,
        String parameterPath, Object value, String type, boolean includeInMetadata);

    UpdateParameterResultDTO updateWorkflowNodeParameter(
        String workflowId, String workflowNodeName, String parameterPath, Object value, String type,
        boolean includeInMetadata);
}
