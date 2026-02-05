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

package com.bytechef.platform.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
class WorkflowTestConfigurationGraphQlController {

    private final WorkflowTestConfigurationFacade workflowTestConfigurationFacade;

    WorkflowTestConfigurationGraphQlController(WorkflowTestConfigurationFacade workflowTestConfigurationFacade) {
        this.workflowTestConfigurationFacade = workflowTestConfigurationFacade;
    }

    @MutationMapping
    boolean saveClusterElementTestConfigurationConnection(
        @Argument String workflowId, @Argument String workflowNodeName, @Argument String clusterElementType,
        @Argument String clusterElementWorkflowNodeName, @Argument String workflowConnectionKey,
        @Argument long connectionId, @Argument long environmentId) {

        workflowTestConfigurationFacade.saveClusterElementTestConfigurationConnection(
            workflowId, workflowNodeName, clusterElementType, clusterElementWorkflowNodeName, workflowConnectionKey,
            connectionId, environmentId);

        return true;
    }

    @MutationMapping
    boolean saveWorkflowTestConfigurationConnection(
        @Argument String workflowId, @Argument String workflowNodeName, @Argument String workflowConnectionKey,
        @Argument long connectionId, @Argument long environmentId) {

        workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
            workflowId, workflowNodeName, workflowConnectionKey, connectionId, environmentId);

        return true;
    }
}
