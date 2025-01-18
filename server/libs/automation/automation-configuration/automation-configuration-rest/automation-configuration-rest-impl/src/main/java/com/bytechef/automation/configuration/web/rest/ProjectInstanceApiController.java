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
import com.bytechef.automation.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.automation.configuration.dto.ProjectInstanceDTO;
import com.bytechef.automation.configuration.facade.ProjectInstanceFacade;
import com.bytechef.automation.configuration.web.rest.model.CreateProjectInstanceWorkflowJob200ResponseModel;
import com.bytechef.automation.configuration.web.rest.model.EnvironmentModel;
import com.bytechef.automation.configuration.web.rest.model.ProjectInstanceModel;
import com.bytechef.automation.configuration.web.rest.model.ProjectInstanceWorkflowModel;
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
public class ProjectInstanceApiController implements ProjectInstanceApi {

    private final ConversionService conversionService;
    private final ProjectInstanceFacade projectInstanceFacade;

    @SuppressFBWarnings("EI")
    public ProjectInstanceApiController(
        ConversionService conversionService, ProjectInstanceFacade projectInstanceFacade) {

        this.conversionService = conversionService;
        this.projectInstanceFacade = projectInstanceFacade;
    }

    @Override
    public ResponseEntity<Long> createProjectInstance(ProjectInstanceModel projectInstanceModel) {
        return ResponseEntity.ok(
            projectInstanceFacade.createProjectInstance(
                conversionService.convert(projectInstanceModel, ProjectInstanceDTO.class)));
    }

    @Override
    public ResponseEntity<CreateProjectInstanceWorkflowJob200ResponseModel> createProjectInstanceWorkflowJob(
        Long id, String workflowId) {

        return ResponseEntity.ok(
            new CreateProjectInstanceWorkflowJob200ResponseModel().jobId(
                projectInstanceFacade.createProjectInstanceWorkflowJob(id, workflowId)));
    }

    @Override
    public ResponseEntity<Void> deleteProjectInstance(Long id) {
        projectInstanceFacade.deleteProjectInstance(id);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<Void> enableProjectInstance(Long id, Boolean enable) {
        projectInstanceFacade.enableProjectInstance(id, enable);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<Void> enableProjectInstanceWorkflow(Long id, String workflowId, Boolean enable) {
        projectInstanceFacade.enableProjectInstanceWorkflow(id, workflowId, enable);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<ProjectInstanceModel> getProjectInstance(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(projectInstanceFacade.getProjectInstance(id), ProjectInstanceModel.class));
    }

    @Override
    public ResponseEntity<List<ProjectInstanceModel>> getWorkspaceProjectInstances(
        Long id, EnvironmentModel environment, Long projectId, Long tagId, Boolean includeAllFields) {

        return ResponseEntity.ok(
            projectInstanceFacade
                .getWorkspaceProjectInstances(
                    id, environment == null ? null : Environment.valueOf(environment.getValue()), projectId, tagId,
                    includeAllFields)
                .stream()
                .map(projectInstance -> conversionService.convert(projectInstance, ProjectInstanceModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<Void> updateProjectInstance(Long id, ProjectInstanceModel projectInstanceModel) {
        projectInstanceFacade.updateProjectInstance(
            conversionService.convert(projectInstanceModel.id(id), ProjectInstanceDTO.class));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> updateProjectInstanceWorkflow(
        Long id, Long projectInstanceWorkflowId, ProjectInstanceWorkflowModel projectInstanceWorkflowModel) {

        projectInstanceFacade.updateProjectInstanceWorkflow(
            conversionService.convert(
                projectInstanceWorkflowModel.id(projectInstanceWorkflowId)
                    .projectInstanceId(id),
                ProjectInstanceWorkflow.class));

        return ResponseEntity.noContent()
            .build();
    }
}
