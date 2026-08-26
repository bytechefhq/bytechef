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
import com.bytechef.platform.security.web.authentication.PrincipalEnvironment;
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

        // getPreviousWorkflowNodeOutputs is @Cacheable, keyed from the raw method arguments before the method body
        // runs -- resolving inside it would be too late (see the comment there). Resolved once here instead, and the
        // SAME effective value passed to both the cache eviction below and the cached read, so the two never
        // disagree about which environment's entry they touch. See PrincipalEnvironment.
        // Required by the OpenAPI contract (@NotNull, required = true), so Spring rejects a missing one before this
        // runs -- checked explicitly all the same, because the alternative is an unboxing NPE surfacing as a 500 if
        // that ever changes.
        if (environmentId == null) {
            throw new IllegalArgumentException("environmentId is required");
        }

        long effectiveEnvironmentId = PrincipalEnvironment.resolveEffectiveEnvironmentId(environmentId.longValue());

        workflowNodeOutputFacade.checkWorkflowCache(workflowId, lastWorkflowNodeName, effectiveEnvironmentId);

        return ResponseEntity.ok(
            CollectionUtils.map(
                workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(
                    workflowId, lastWorkflowNodeName, effectiveEnvironmentId),
                workflowNodeOutputDTO -> conversionService.convert(
                    workflowNodeOutputDTO, WorkflowNodeOutputModel.class)));
    }
}
