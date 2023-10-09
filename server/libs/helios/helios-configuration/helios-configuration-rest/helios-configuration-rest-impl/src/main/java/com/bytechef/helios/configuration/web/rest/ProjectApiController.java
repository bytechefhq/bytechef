
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.helios.configuration.web.rest;

import com.bytechef.helios.configuration.web.rest.model.WorkflowModel;
import com.bytechef.helios.configuration.web.rest.model.CreateProjectWorkflowRequestModel;
import com.bytechef.helios.configuration.web.rest.model.ProjectModel;
import com.bytechef.helios.configuration.dto.ProjectDTO;
import com.bytechef.helios.configuration.facade.ProjectFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}")
@ConditionalOnProperty(prefix = "bytechef", name = "coordinator.enabled", matchIfMissing = true)
public class ProjectApiController implements ProjectApi {

    private final ConversionService conversionService;
    private final ProjectFacade projectFacade;

    @SuppressFBWarnings("EI2")
    public ProjectApiController(ConversionService conversionService, ProjectFacade projectFacade) {
        this.conversionService = conversionService;
        this.projectFacade = projectFacade;
    }

    @Override
    public ResponseEntity<ProjectModel> createProject(ProjectModel projectModel) {

        return ResponseEntity.ok(
            conversionService.convert(
                projectFacade.createProject(conversionService.convert(projectModel, ProjectDTO.class)),
                ProjectModel.class));
    }

    @Override
    public ResponseEntity<WorkflowModel> createProjectWorkflow(
        Long id, CreateProjectWorkflowRequestModel createProjectWorkflowRequestModel) {

        return ResponseEntity.ok(conversionService.convert(
            projectFacade.addProjectWorkflow(
                id, createProjectWorkflowRequestModel.getLabel(), createProjectWorkflowRequestModel.getDescription(),
                createProjectWorkflowRequestModel.getDefinition()),
            WorkflowModel.class));
    }

    @Override
    public ResponseEntity<Void> deleteProject(Long id) {
        projectFacade.deleteProject(id);

        return ResponseEntity
            .ok()
            .build();
    }

    @Override
    public ResponseEntity<ProjectModel> duplicateProject(Long id) {
        return ResponseEntity.ok(conversionService.convert(projectFacade.duplicateProject(id), ProjectModel.class));
    }

    @Override
    public ResponseEntity<ProjectModel> getProject(Long id) {
        return ResponseEntity.ok(conversionService.convert(projectFacade.getProject(id), ProjectModel.class));
    }

    @Override
    public ResponseEntity<List<ProjectModel>> getProjects(
        Long categoryId, Boolean projectInstances, Long tagId, Boolean published) {

        return ResponseEntity.ok(
            projectFacade.getProjects(categoryId, projectInstances != null, tagId, published)
                .stream()
                .map(project -> conversionService.convert(project, ProjectModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<ProjectModel> updateProject(Long id, ProjectModel projectModel) {
        return ResponseEntity.ok(conversionService.convert(
            projectFacade.updateProject(conversionService.convert(projectModel.id(id), ProjectDTO.class)),
            ProjectModel.class));
    }
}
