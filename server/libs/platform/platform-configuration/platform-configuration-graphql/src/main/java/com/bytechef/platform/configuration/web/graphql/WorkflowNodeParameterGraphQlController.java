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
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import java.util.Set;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class WorkflowNodeParameterGraphQlController {

    private final WorkflowNodeParameterFacade workflowNodeParameterFacade;

    public WorkflowNodeParameterGraphQlController(WorkflowNodeParameterFacade workflowNodeParameterFacade) {
        this.workflowNodeParameterFacade = workflowNodeParameterFacade;
    }

    @QueryMapping
    public Set<String> clusterElementMissingRequiredProperties(
        @Argument String workflowId, @Argument String workflowNodeName, @Argument String clusterElementTypeName,
        @Argument String clusterElementWorkflowNodeName) {

        return workflowNodeParameterFacade.getClusterElementMissingRequiredProperties(
            workflowId, workflowNodeName, clusterElementTypeName, clusterElementWorkflowNodeName);
    }

    @QueryMapping
    public Set<String> workflowNodeMissingRequiredProperties(
        @Argument String workflowId, @Argument String workflowNodeName) {

        return workflowNodeParameterFacade.getWorkflowNodeMissingRequiredProperties(workflowId, workflowNodeName);
    }
}
