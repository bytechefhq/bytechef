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

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.configuration.facade.WorkflowNodeDescriptionFacade;
import com.bytechef.platform.configuration.web.rest.model.GetWorkflowNodeDescription200ResponseModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnCoordinator
public class WorkflowNodeDescriptionApiController implements WorkflowNodeDescriptionApi {

    private final WorkflowNodeDescriptionFacade workflowNodeDescriptionFacade;

    public WorkflowNodeDescriptionApiController(WorkflowNodeDescriptionFacade workflowNodeDescriptionFacade) {
        this.workflowNodeDescriptionFacade = workflowNodeDescriptionFacade;
    }

    @Override
    public ResponseEntity<GetWorkflowNodeDescription200ResponseModel> getClusterElementWorkflowNodeDescription(
        String workflowId, String workflowNodeName, String clusterElementName, Long environmentId) {

        return ResponseEntity.ok(
            new GetWorkflowNodeDescription200ResponseModel().description(
                workflowNodeDescriptionFacade.getClusterElementWorkflowNodeDescription(
                    workflowId, workflowNodeName, clusterElementName, environmentId)));
    }

    @Override
    public ResponseEntity<GetWorkflowNodeDescription200ResponseModel> getWorkflowNodeDescription(
        String workflowId, String workflowNodeName, Long environmentId) {

        return ResponseEntity.ok(
            new GetWorkflowNodeDescription200ResponseModel().description(
                workflowNodeDescriptionFacade.getWorkflowNodeDescription(workflowId, workflowNodeName, environmentId)));
    }
}
