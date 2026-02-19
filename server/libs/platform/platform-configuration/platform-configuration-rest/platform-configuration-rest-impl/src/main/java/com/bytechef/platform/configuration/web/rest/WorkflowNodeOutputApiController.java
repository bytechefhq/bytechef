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
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.configuration.dto.ClusterElementOutputDTO;
import com.bytechef.platform.configuration.dto.WorkflowNodeOutputDTO;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.configuration.web.rest.model.WorkflowNodeOutputModel;
import com.bytechef.platform.domain.OutputResponse;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnCoordinator
public class WorkflowNodeOutputApiController implements WorkflowNodeOutputApi {

    private final ConversionService conversionService;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;

    public WorkflowNodeOutputApiController(
        ConversionService conversionService, WorkflowNodeOutputFacade workflowNodeOutputFacade) {

        this.conversionService = conversionService;
        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
    }

    @Override
    public ResponseEntity<WorkflowNodeOutputModel> getClusterElementOutput(
        String workflowId, String workflowNodeName, String clusterElementType, String clusterElementName,
        Long environmentId) {

        ClusterElementOutputDTO clusterElementOutputDTO = workflowNodeOutputFacade.getClusterElementOutput(
            workflowId, workflowNodeName, clusterElementType, clusterElementName, environmentId);

        if (clusterElementOutputDTO == null) {
            return ResponseEntity.notFound()
                .build();
        }

        OutputResponse outputResponse = clusterElementOutputDTO.outputSchema() != null
            ? new OutputResponse(
                clusterElementOutputDTO.outputSchema(), clusterElementOutputDTO.sampleOutput(),
                clusterElementOutputDTO.placeholder())
            : null;

        WorkflowNodeOutputDTO workflowNodeOutputDTO = new WorkflowNodeOutputDTO(
            null, clusterElementOutputDTO.clusterElementDefinition(),
            outputResponse, null, false, null,
            clusterElementOutputDTO.clusterElementName());

        return ResponseEntity.ok(
            conversionService.convert(workflowNodeOutputDTO, WorkflowNodeOutputModel.class));
    }

    @Override
    public ResponseEntity<WorkflowNodeOutputModel> getWorkflowNodeOutput(
        String workflowId, String workflowNodeName, Long environmentId) {

        return ResponseEntity.ok(
            conversionService.convert(
                workflowNodeOutputFacade.getWorkflowNodeOutput(workflowId, workflowNodeName, environmentId),
                WorkflowNodeOutputModel.class));
    }

    @Override
    public ResponseEntity<List<WorkflowNodeOutputModel>> getPreviousWorkflowNodeOutputs(
        String workflowId, Long environmentId, String lastWorkflowNodeName) {

        workflowNodeOutputFacade.checkWorkflowCache(workflowId, lastWorkflowNodeName, environmentId);

        return ResponseEntity.ok(
            CollectionUtils.map(
                workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(
                    workflowId, lastWorkflowNodeName, environmentId),
                workflowNodeOutputDTO -> conversionService.convert(
                    workflowNodeOutputDTO, WorkflowNodeOutputModel.class)));
    }
}
