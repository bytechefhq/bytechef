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

import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.domain.BaseProperty;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface WorkflowNodeDynamicPropertiesFacade {

    List<Property> getClusterElementDynamicProperties(
        String workflowId, String workflowNodeName, String clusterElementTypeName,
        String clusterElementWorkflowNodeName, String propertyName, List<String> lookupDependsOnPath,
        long environmentId);

    List<Property> getWorkflowNodeComponentActionDynamicProperties(
        String workflowId, String workflowNodeName, String componentName, int componentVersion, String actionName,
        String propertyName, @Nullable String connectionKey, Map<String, ?> parameters,
        List<String> lookupDependsOnPaths, long environmentId);

    List<? extends BaseProperty> getWorkflowNodeDynamicProperties(
        String workflowId, String workflowNodeName, String propertyName, List<String> lookupDependsOnPaths,
        long environmentId);
}
