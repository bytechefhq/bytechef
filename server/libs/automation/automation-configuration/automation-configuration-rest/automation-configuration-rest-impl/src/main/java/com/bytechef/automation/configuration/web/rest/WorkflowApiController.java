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

package com.bytechef.automation.configuration.web.rest;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.web.rest.model.CreateProjectWorkflow200ResponseModel;
import com.bytechef.automation.configuration.web.rest.model.DuplicateWorkflow200ResponseModel;
import com.bytechef.automation.configuration.web.rest.model.WorkflowModel;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.configuration.web.rest.AbstractWorkflowApiController;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController("com.bytechef.automation.configuration.web.rest.WorkflowApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/internal")
@ConditionalOnCoordinator
public class WorkflowApiController extends AbstractWorkflowApiController implements WorkflowApi {

    private final ConversionService conversionService;
    private final ProjectWorkflowFacade projectWorkflowFacade;

    @SuppressFBWarnings("EI2")
    public WorkflowApiController(
        ConversionService conversionService, ProjectWorkflowFacade projectWorkflowFacade,
        WorkflowService workflowService) {

        super(workflowService);

        this.conversionService = conversionService;
        this.projectWorkflowFacade = projectWorkflowFacade;
    }

    @Override
    public ResponseEntity<CreateProjectWorkflow200ResponseModel> createProjectWorkflow(
        Long id, WorkflowModel workflowModel) {

        ProjectWorkflow projectWorkflow = projectWorkflowFacade.addWorkflow(id, workflowModel.getDefinition());

        return ResponseEntity.ok(
            new CreateProjectWorkflow200ResponseModel().projectWorkflowId(projectWorkflow.getId()));
    }

    @Override
    public ResponseEntity<Void> deleteWorkflow(String id) {
        projectWorkflowFacade.deleteWorkflow(id);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<DuplicateWorkflow200ResponseModel> duplicateWorkflow(Long id, String workflowId) {
        return ResponseEntity.ok(
            new DuplicateWorkflow200ResponseModel().workflowId(
                projectWorkflowFacade.duplicateWorkflow(id, workflowId)));
    }

    @GetMapping("/workflows/{id}/export")
    @ResponseBody
    public ResponseEntity<Resource> exportWorkflow(@PathVariable("id") String id) {
        return doExportWorkflow(id);
    }

    @Override
    public ResponseEntity<WorkflowModel> getProjectWorkflow(Long projectWorkflowId) {
        return ResponseEntity.ok(
            conversionService.convert(
                projectWorkflowFacade.getProjectWorkflow(projectWorkflowId), WorkflowModel.class));
    }

    @Override
    public ResponseEntity<List<WorkflowModel>> getProjectWorkflows(Long id) {
        return ResponseEntity.ok(
            CollectionUtils.map(
                projectWorkflowFacade.getProjectWorkflows(id),
                workflow -> conversionService.convert(workflow, WorkflowModel.class)));
    }

    @Override
    public ResponseEntity<List<WorkflowModel>> getProjectVersionWorkflows(
        Long id, Integer projectVersion, Boolean includeAllFields) {

        return ResponseEntity.ok(
            CollectionUtils.map(
                projectWorkflowFacade.getProjectVersionWorkflows(id, projectVersion, includeAllFields),
                workflow -> conversionService.convert(workflow, WorkflowModel.class)));
    }

    @Override
    public ResponseEntity<WorkflowModel> getWorkflow(String id) {
        // TODO Add check regarding platform type

        return ResponseEntity.ok(
            conversionService.convert(projectWorkflowFacade.getProjectWorkflow(id), WorkflowModel.class));
    }

    @Override
    public ResponseEntity<List<WorkflowModel>> getWorkflows() {
        return ResponseEntity.ok(
            CollectionUtils.map(
                projectWorkflowFacade.getProjectWorkflows(),
                workflow -> conversionService.convert(workflow, WorkflowModel.class)));
    }

    @Override
    public ResponseEntity<Void> updateWorkflow(String id, WorkflowModel workflowModel) {
        // TODO Add check regarding platform type

        projectWorkflowFacade.updateWorkflow(
            id, workflowModel.getDefinition(), Objects.requireNonNull(workflowModel.getVersion()));

        return ResponseEntity.noContent()
            .build();
    }
}
