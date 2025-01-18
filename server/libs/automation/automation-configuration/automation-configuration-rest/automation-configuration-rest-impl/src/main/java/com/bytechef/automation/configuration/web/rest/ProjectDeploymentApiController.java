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

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.web.rest.model.CreateProjectDeploymentWorkflowJob200ResponseModel;
import com.bytechef.automation.configuration.web.rest.model.EnvironmentModel;
import com.bytechef.automation.configuration.web.rest.model.ProjectDeploymentModel;
import com.bytechef.automation.configuration.web.rest.model.ProjectDeploymentWorkflowModel;
import com.bytechef.platform.constant.Environment;
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
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/internal")
@ConditionalOnCoordinator
public class ProjectDeploymentApiController implements ProjectDeploymentApi {

    private final ConversionService conversionService;
    private final ProjectDeploymentFacade projectDeploymentFacade;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentApiController(
        ConversionService conversionService, ProjectDeploymentFacade projectDeploymentFacade) {

        this.conversionService = conversionService;
        this.projectDeploymentFacade = projectDeploymentFacade;
    }

    @Override
    public ResponseEntity<Long> createProjectDeployment(ProjectDeploymentModel projectDeploymentModel) {
        return ResponseEntity.ok(
            projectDeploymentFacade.createProjectDeployment(
                conversionService.convert(projectDeploymentModel, ProjectDeploymentDTO.class)));
    }

    @Override
    public ResponseEntity<CreateProjectDeploymentWorkflowJob200ResponseModel> createProjectDeploymentWorkflowJob(
        Long id, String workflowId) {

        return ResponseEntity.ok(
            new CreateProjectDeploymentWorkflowJob200ResponseModel().jobId(
                projectDeploymentFacade.createProjectDeploymentWorkflowJob(id, workflowId)));
    }

    @Override
    public ResponseEntity<Void> deleteProjectDeployment(Long id) {
        projectDeploymentFacade.deleteProjectDeployment(id);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<Void> enableProjectDeployment(Long id, Boolean enable) {
        projectDeploymentFacade.enableProjectDeployment(id, enable);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<Void> enableProjectDeploymentWorkflow(Long id, String workflowId, Boolean enable) {
        projectDeploymentFacade.enableProjectDeploymentWorkflow(id, workflowId, enable);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<ProjectDeploymentModel> getProjectDeployment(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(projectDeploymentFacade.getProjectDeployment(id), ProjectDeploymentModel.class));
    }

    @Override
    public ResponseEntity<List<ProjectDeploymentModel>> getWorkspaceProjectDeployments(
        Long id, EnvironmentModel environment, Long projectId, Long tagId, Boolean includeAllFields) {

        return ResponseEntity.ok(
            projectDeploymentFacade
                .getWorkspaceProjectDeployments(
                    id, environment == null ? null : Environment.valueOf(environment.getValue()), projectId, tagId,
                    includeAllFields)
                .stream()
                .map(projectDeployment -> conversionService.convert(projectDeployment, ProjectDeploymentModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<Void> updateProjectDeployment(Long id, ProjectDeploymentModel projectDeploymentModel) {
        projectDeploymentFacade.updateProjectDeployment(
            conversionService.convert(projectDeploymentModel.id(id), ProjectDeploymentDTO.class));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> updateProjectDeploymentWorkflow(
        Long id, Long projectDeploymentWorkflowId, ProjectDeploymentWorkflowModel projectDeploymentWorkflowModel) {

        projectDeploymentFacade.updateProjectDeploymentWorkflow(
            conversionService.convert(
                projectDeploymentWorkflowModel.id(projectDeploymentWorkflowId)
                    .projectDeploymentId(id),
                ProjectDeploymentWorkflow.class));

        return ResponseEntity.noContent()
            .build();
    }
}
