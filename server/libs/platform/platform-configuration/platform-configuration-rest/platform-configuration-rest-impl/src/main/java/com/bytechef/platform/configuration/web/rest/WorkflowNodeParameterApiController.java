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
import com.bytechef.platform.configuration.dto.DisplayConditionResultDTO;
import com.bytechef.platform.configuration.dto.UpdateParameterResultDTO;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.web.rest.model.DeleteClusterElementParameter200ResponseModel;
import com.bytechef.platform.configuration.web.rest.model.DeleteClusterElementParameterRequestModel;
import com.bytechef.platform.configuration.web.rest.model.GetClusterElementParameterDisplayConditions200ResponseModel;
import com.bytechef.platform.configuration.web.rest.model.UpdateClusterElementParameter200ResponseModel;
import com.bytechef.platform.configuration.web.rest.model.UpdateClusterElementParameterRequestModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
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
public class WorkflowNodeParameterApiController implements WorkflowNodeParameterApi {

    private final ConversionService conversionService;
    private final WorkflowNodeParameterFacade workflowNodeParameterFacade;

    @SuppressFBWarnings("EI")
    public WorkflowNodeParameterApiController(
        ConversionService conversionService, WorkflowNodeParameterFacade workflowNodeParameterFacade) {

        this.conversionService = conversionService;
        this.workflowNodeParameterFacade = workflowNodeParameterFacade;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ResponseEntity<DeleteClusterElementParameter200ResponseModel> deleteClusterElementParameter(
        String id, String workflowNodeName, String clusterElementType, String clusterElementWorkflowNodeName,
        DeleteClusterElementParameterRequestModel deleteWorkflowNodeParameterRequestModel) {

        return ResponseEntity.ok(
            new DeleteClusterElementParameter200ResponseModel().parameters(
                (Map<String, Object>) workflowNodeParameterFacade.deleteClusterElementParameter(
                    id, workflowNodeName, clusterElementType.toUpperCase(), clusterElementWorkflowNodeName,
                    deleteWorkflowNodeParameterRequestModel.getPath())));
    }

    @Override
    @SuppressWarnings("unchecked")
    public ResponseEntity<DeleteClusterElementParameter200ResponseModel> deleteWorkflowNodeParameter(
        String id, String workflowNodeName,
        DeleteClusterElementParameterRequestModel deleteWorkflowNodeParameterRequestModel) {

        return ResponseEntity.ok(
            new DeleteClusterElementParameter200ResponseModel().parameters(
                (Map<String, Object>) workflowNodeParameterFacade.deleteWorkflowNodeParameter(
                    id, workflowNodeName, deleteWorkflowNodeParameterRequestModel.getPath())));
    }

    @Override
    public ResponseEntity<GetClusterElementParameterDisplayConditions200ResponseModel>
        getClusterElementParameterDisplayConditions(
            String id, String workflowNodeName, String clusterElementType, String clusterElementWorkflowNodeName) {

        DisplayConditionResultDTO displayConditionResultDTO =
            workflowNodeParameterFacade.getClusterElementDisplayConditions(
                id, workflowNodeName, clusterElementType.toUpperCase(), clusterElementWorkflowNodeName);

        return ResponseEntity.ok(
            new GetClusterElementParameterDisplayConditions200ResponseModel()
                .displayConditions(displayConditionResultDTO.displayConditions())
                .missingRequiredProperties(displayConditionResultDTO.missingRequiredProperties()));
    }

    @Override
    public ResponseEntity<GetClusterElementParameterDisplayConditions200ResponseModel>
        getWorkflowNodeParameterDisplayConditions(String id, String workflowNodeName) {

        DisplayConditionResultDTO displayConditionResultDTO =
            workflowNodeParameterFacade.getWorkflowNodeDisplayConditions(id, workflowNodeName);

        return ResponseEntity.ok(
            new GetClusterElementParameterDisplayConditions200ResponseModel()
                .displayConditions(displayConditionResultDTO.displayConditions())
                .missingRequiredProperties(displayConditionResultDTO.missingRequiredProperties()));
    }

    @Override
    public ResponseEntity<UpdateClusterElementParameter200ResponseModel> updateClusterElementParameter(
        String id, String workflowNodeName, String clusterElementType, String clusterElementWorkflowNodeName,
        UpdateClusterElementParameterRequestModel updateWorkflowNodeParameterRequestModel) {

        UpdateParameterResultDTO updateParameterResultDTO = workflowNodeParameterFacade.updateClusterElementParameter(
            id, workflowNodeName, clusterElementType.toUpperCase(), clusterElementWorkflowNodeName,
            updateWorkflowNodeParameterRequestModel.getPath(), updateWorkflowNodeParameterRequestModel.getValue(),
            updateWorkflowNodeParameterRequestModel.getType(),
            updateWorkflowNodeParameterRequestModel.getIncludeInMetadata());

        return ResponseEntity.ok(
            conversionService.convert(updateParameterResultDTO, UpdateClusterElementParameter200ResponseModel.class));
    }

    @Override
    public ResponseEntity<UpdateClusterElementParameter200ResponseModel> updateWorkflowNodeParameter(
        String id, String workflowNodeName,
        UpdateClusterElementParameterRequestModel updateWorkflowNodeParameterRequestModel) {

        UpdateParameterResultDTO updateParameterResultDTO = workflowNodeParameterFacade.updateWorkflowNodeParameter(
            id, workflowNodeName,
            updateWorkflowNodeParameterRequestModel.getPath(), updateWorkflowNodeParameterRequestModel.getValue(),
            updateWorkflowNodeParameterRequestModel.getType(),
            updateWorkflowNodeParameterRequestModel.getIncludeInMetadata());

        return ResponseEntity.ok(
            conversionService.convert(updateParameterResultDTO, UpdateClusterElementParameter200ResponseModel.class));
    }
}
