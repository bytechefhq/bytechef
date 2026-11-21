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

package com.bytechef.platform.configuration.web.rest;

import com.bytechef.platform.configuration.domain.ClusterElementNode;
import com.bytechef.platform.configuration.domain.ComponentOperation;
import com.bytechef.platform.configuration.web.rest.model.ComponentOperationPropertyRequestModel;
import com.bytechef.platform.configuration.web.rest.model.ComponentOperationPropertyRequestModel.OperationTypeEnum;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
final class ComponentOperationPropertyRequests {

    private ComponentOperationPropertyRequests() {
    }

    static @Nullable ClusterElementNode toClusterElementNode(
        ComponentOperationPropertyRequestModel componentOperationPropertyRequestModel) {

        String clusterElementType = componentOperationPropertyRequestModel.getClusterElementType();
        String clusterElementWorkflowNodeName =
            componentOperationPropertyRequestModel.getClusterElementWorkflowNodeName();

        if (clusterElementType == null || clusterElementWorkflowNodeName == null) {
            return null;
        }

        return new ClusterElementNode(clusterElementType, clusterElementWorkflowNodeName);
    }

    static ComponentOperation toComponentOperation(
        ComponentOperationPropertyRequestModel componentOperationPropertyRequestModel) {

        return new ComponentOperation(
            componentOperationPropertyRequestModel.getComponentName(),
            componentOperationPropertyRequestModel.getComponentVersion(),
            componentOperationPropertyRequestModel.getOperationName(),
            componentOperationPropertyRequestModel.getOperationType() == OperationTypeEnum.TRIGGER);
    }

    static List<String> lookupDependsOnPaths(
        ComponentOperationPropertyRequestModel componentOperationPropertyRequestModel) {

        List<String> lookupDependsOnPaths = componentOperationPropertyRequestModel.getLookupDependsOnPaths();

        return lookupDependsOnPaths == null ? List.of() : lookupDependsOnPaths;
    }

    static Map<String, Object> parameters(
        ComponentOperationPropertyRequestModel componentOperationPropertyRequestModel) {

        Map<String, Object> parameters = componentOperationPropertyRequestModel.getParameters();

        return parameters == null ? Map.of() : parameters;
    }
}
