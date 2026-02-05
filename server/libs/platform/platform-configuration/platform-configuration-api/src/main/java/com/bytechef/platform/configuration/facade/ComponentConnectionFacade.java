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

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface ComponentConnectionFacade {

    ComponentConnection getClusterElementComponentConnection(
        String workflowId, String workflowNodeName, String clusterElementType,
        String clusterElementWorkflowNodeName, String key);

    List<ComponentConnection> getClusterElementComponentConnections(
        String workflowId, String workflowNodeName, String clusterElementType, String clusterElementWorkflowNodeName);

    ComponentConnection getComponentConnection(String workflowId, String workflowNodeName, String key);

    List<ComponentConnection> getComponentConnections(WorkflowTask workflowTask);

    List<ComponentConnection> getComponentConnections(WorkflowTrigger workflowTrigger);

    List<ComponentConnection> getWorkflowNodeComponentConnections(String workflowId, String workflowNodeName);
}
