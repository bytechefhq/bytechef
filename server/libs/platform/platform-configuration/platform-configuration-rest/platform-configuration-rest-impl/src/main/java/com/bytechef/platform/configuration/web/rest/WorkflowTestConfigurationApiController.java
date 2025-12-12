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
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.configuration.web.rest.model.SaveWorkflowTestConfigurationConnectionRequestModel;
import com.bytechef.platform.configuration.web.rest.model.SaveWorkflowTestConfigurationInputsRequestModel;
import com.bytechef.platform.configuration.web.rest.model.WorkflowTestConfigurationConnectionModel;
import com.bytechef.platform.configuration.web.rest.model.WorkflowTestConfigurationModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
public class WorkflowTestConfigurationApiController implements WorkflowTestConfigurationApi {

    private final WorkflowTestConfigurationFacade workflowTestConfigurationFacade;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public WorkflowTestConfigurationApiController(
        WorkflowTestConfigurationFacade workflowTestConfigurationFacade,
        WorkflowTestConfigurationService workflowTestConfigurationService, ConversionService conversionService) {

        this.workflowTestConfigurationFacade = workflowTestConfigurationFacade;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<WorkflowTestConfigurationModel> getWorkflowTestConfiguration(
        String workflowId, Long environmentId) {
        return ResponseEntity.ok(
            conversionService.convert(
                workflowTestConfigurationService.fetchWorkflowTestConfiguration(workflowId, environmentId)
                    .orElse(null),
                WorkflowTestConfigurationModel.class));
    }

    @Override
    public ResponseEntity<List<WorkflowTestConfigurationConnectionModel>> getWorkflowTestConfigurationConnections(
        String workflowId, String workflowNodeName, Long environmentId) {

        return ResponseEntity.ok(
            CollectionUtils.map(
                workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
                    workflowId, workflowNodeName, environmentId),
                workflowTestConfigurationConnection -> conversionService.convert(
                    workflowTestConfigurationConnection, WorkflowTestConfigurationConnectionModel.class)));
    }

    @Override
    public ResponseEntity<WorkflowTestConfigurationModel> saveWorkflowTestConfiguration(
        String workflowId, WorkflowTestConfigurationModel workflowTestConfigurationModel) {

        return ResponseEntity.ok(
            conversionService.convert(
                workflowTestConfigurationFacade.saveWorkflowTestConfiguration(
                    conversionService.convert(
                        workflowTestConfigurationModel.workflowId(workflowId), WorkflowTestConfiguration.class)),
                WorkflowTestConfigurationModel.class));
    }

    @Override
    public ResponseEntity<Void> saveWorkflowTestConfigurationConnection(
        String workflowId, String workflowNodeName, String workflowConnectionKey, Long environmentId,
        SaveWorkflowTestConfigurationConnectionRequestModel saveWorkflowTestConfigurationConnectionRequestModel) {

        workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
            workflowId, workflowNodeName, workflowConnectionKey,
            saveWorkflowTestConfigurationConnectionRequestModel.getConnectionId(), environmentId);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> deleteWorkflowTestConfigurationConnection(
        String workflowId, String workflowNodeName, String workflowConnectionKey, Long environmentId,
        SaveWorkflowTestConfigurationConnectionRequestModel saveWorkflowTestConfigurationConnectionRequestModel) {

        workflowTestConfigurationFacade.deleteWorkflowTestConfigurationConnection(
            workflowId, workflowNodeName, workflowConnectionKey,
            saveWorkflowTestConfigurationConnectionRequestModel.getConnectionId(), environmentId);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> saveWorkflowTestConfigurationInputs(
        String workflowId, Long environmentId,
        SaveWorkflowTestConfigurationInputsRequestModel saveWorkflowTestConfigurationInputsRequestModel) {

        workflowTestConfigurationFacade.saveWorkflowTestConfigurationInputs(
            workflowId, saveWorkflowTestConfigurationInputsRequestModel.getKey(),
            saveWorkflowTestConfigurationInputsRequestModel.getValue(), environmentId);

        return ResponseEntity.noContent()
            .build();
    }
}
