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

package com.bytechef.automation.configuration.web.rest;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.annotation.ConditionalOnEndpoint;
import com.bytechef.platform.configuration.web.rest.util.WorkflowApiControllerUtils;
import com.bytechef.platform.configuration.web.rest.model.WorkflowModel;
import com.bytechef.platform.constant.Type;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController("com.bytechef.helios.configuration.web.rest.WorkflowApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}")
@ConditionalOnEndpoint
public class WorkflowApiController implements WorkflowApi {

    private final ConversionService conversionService;
    private final ProjectFacade projectFacade;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public WorkflowApiController(
        ConversionService conversionService, ProjectFacade projectFacade, WorkflowService workflowService) {

        this.conversionService = conversionService;
        this.projectFacade = projectFacade;
        this.workflowService = workflowService;
    }

    @Override
    public ResponseEntity<Void> deleteProjectWorkflow(Long id, String workflowId) {
        projectFacade.deleteWorkflow(id, workflowId);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<WorkflowModel> duplicateWorkflow(Long id, String workflowId) {
        return ResponseEntity.ok(
            conversionService.convert(projectFacade.duplicateWorkflow(id, workflowId), WorkflowModel.class));
    }

    @Override
    public ResponseEntity<List<WorkflowModel>> getProjectWorkflows(Long id) {
        return ResponseEntity.ok(
            CollectionUtils.map(
                projectFacade.getProjectWorkflows(id),
                workflow -> conversionService.convert(workflow, WorkflowModel.class)));
    }

    @Override
    public ResponseEntity<WorkflowModel> getWorkflow(String id) {
        return WorkflowApiControllerUtils.getWorkflow(id, conversionService, workflowService);
    }

    @Override
    public ResponseEntity<List<WorkflowModel>> getWorkflows() {
        List<WorkflowModel> workflowModels = new ArrayList<>();

        for (Workflow workflow : workflowService.getWorkflows(Type.AUTOMATION.getId())) {
            workflowModels.add(conversionService.convert(workflow, WorkflowModel.class));
        }

        return ResponseEntity.ok(workflowModels);
    }

    @Override
    public ResponseEntity<WorkflowModel> updateWorkflow(String id, WorkflowModel workflowModel) {
        return WorkflowApiControllerUtils.updateWorkflow(id, workflowModel, conversionService, workflowService);
    }
}
