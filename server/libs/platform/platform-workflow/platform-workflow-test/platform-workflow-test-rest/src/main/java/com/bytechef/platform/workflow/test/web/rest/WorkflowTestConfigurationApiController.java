/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.workflow.test.web.rest;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.annotation.ConditionalOnEndpoint;
import com.bytechef.platform.workflow.test.domain.WorkflowTestConfiguration;
import com.bytechef.platform.workflow.test.service.WorkflowTestConfigurationService;
import com.bytechef.platform.workflow.test.web.rest.model.UpdateWorkflowTestConfigurationConnectionRequestModel;
import com.bytechef.platform.workflow.test.web.rest.model.UpdateWorkflowTestConfigurationInputsRequestModel;
import com.bytechef.platform.workflow.test.web.rest.model.WorkflowTestConfigurationConnectionModel;
import com.bytechef.platform.workflow.test.web.rest.model.WorkflowTestConfigurationModel;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}")
@ConditionalOnEndpoint
public class WorkflowTestConfigurationApiController implements WorkflowTestConfigurationApi {

    private final WorkflowTestConfigurationService workflowTestConfigurationService;
    private final ConversionService conversionService;

    public WorkflowTestConfigurationApiController(
        WorkflowTestConfigurationService workflowTestConfigurationService, ConversionService conversionService) {

        this.workflowTestConfigurationService = workflowTestConfigurationService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<WorkflowTestConfigurationModel> createWorkflowTestConfiguration(
        String workflowId, WorkflowTestConfigurationModel workflowTestConfigurationModel) {

        return ResponseEntity.ok(
            conversionService.convert(
                workflowTestConfigurationService.create(
                    conversionService.convert(
                        workflowTestConfigurationModel.workflowId(workflowId), WorkflowTestConfiguration.class)),
                WorkflowTestConfigurationModel.class));
    }

    @Override
    public ResponseEntity<WorkflowTestConfigurationModel> getWorkflowTestConfiguration(String workflowId) {
        return ResponseEntity.ok(
            conversionService.convert(
                OptionalUtils.orElse(workflowTestConfigurationService.fetchWorkflowTestConfiguration(workflowId), null),
                WorkflowTestConfigurationModel.class));
    }

    @Override
    public ResponseEntity<List<WorkflowTestConfigurationConnectionModel>> getWorkflowTestConfigurationConnections(
        String workflowId, String workflowNodeName) {

        return ResponseEntity.ok(
            CollectionUtils.map(
                workflowTestConfigurationService.getWorkflowTestConfigurationConnections(workflowId, workflowNodeName),
                workflowTestConfigurationConnection -> conversionService.convert(
                    workflowTestConfigurationConnection, WorkflowTestConfigurationConnectionModel.class)));
    }

    @Override
    public ResponseEntity<WorkflowTestConfigurationModel> updateWorkflowTestConfiguration(
        String workflowId, WorkflowTestConfigurationModel workflowTestConfigurationModel) {

        return ResponseEntity.ok(
            conversionService.convert(
                workflowTestConfigurationService.updateWorkflowTestConfiguration(
                    conversionService.convert(
                        workflowTestConfigurationModel.workflowId(workflowId), WorkflowTestConfiguration.class)),
                WorkflowTestConfigurationModel.class));
    }

    @Override
    public ResponseEntity<Void> updateWorkflowTestConfigurationConnection(
        String workflowId, String workflowNodeName, String key,
        UpdateWorkflowTestConfigurationConnectionRequestModel updateWorkflowTestConfigurationConnectionRequestModel) {

        workflowTestConfigurationService.updateWorkflowTestConfigurationConnection(
            workflowId, workflowNodeName, key,
            updateWorkflowTestConfigurationConnectionRequestModel.getConnectionId());

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> updateWorkflowTestConfigurationInputs(
        String workflowId,
        UpdateWorkflowTestConfigurationInputsRequestModel updateWorkflowTestConfigurationInputsRequestModel) {

        workflowTestConfigurationService.updateWorkflowTestConfigurationInputs(
            workflowId, updateWorkflowTestConfigurationInputsRequestModel.getInputs());

        return ResponseEntity.noContent()
            .build();
    }
}
