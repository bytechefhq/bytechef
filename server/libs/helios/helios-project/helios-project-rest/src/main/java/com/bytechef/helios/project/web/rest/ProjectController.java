
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

package com.bytechef.helios.project.web.rest;

import com.bytechef.hermes.workflow.web.rest.model.WorkflowModel;
import com.bytechef.helios.project.web.rest.model.CreateProjectWorkflowRequestModel;
import com.bytechef.helios.project.web.rest.model.ProjectModel;
import com.bytechef.helios.project.web.rest.model.UpdateTagsRequestModel;
import com.bytechef.helios.project.dto.ProjectDTO;
import com.bytechef.helios.project.facade.ProjectFacade;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.web.rest.model.TagModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@RestController

@RequestMapping("${openapi.openAPIDefinition.base-path:}/automation")
public class ProjectController implements ProjectsApi {

    private final ConversionService conversionService;
    private final ProjectFacade projectFacade;

    @SuppressFBWarnings("EI2")
    public ProjectController(ConversionService conversionService, ProjectFacade projectFacade) {
        this.conversionService = conversionService;
        this.projectFacade = projectFacade;
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<ProjectModel> createProject(ProjectModel projectModel) {

        return ResponseEntity.ok(
            conversionService.convert(
                projectFacade.createProject(conversionService.convert(projectModel, ProjectDTO.class)),
                ProjectModel.class));
    }

    @Override
    @SuppressFBWarnings("NP")
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
    @SuppressFBWarnings("NP")
    public ResponseEntity<ProjectModel> duplicateProject(Long id) {
        return ResponseEntity.ok(conversionService.convert(projectFacade.duplicateProject(id), ProjectModel.class));
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<ProjectModel> getProject(Long id) {
        return ResponseEntity.ok(conversionService.convert(projectFacade.getProject(id), ProjectModel.class));
    }

    @Override
    public ResponseEntity<List<ProjectModel>> getProjects(
        List<Long> categoryIds, Boolean projectInstances, List<Long> tagIds) {

        return ResponseEntity.ok(
            projectFacade.searchProjects(categoryIds, projectInstances != null, tagIds)
                .stream()
                .map(project -> conversionService.convert(project, ProjectModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<List<WorkflowModel>> getProjectWorkflows(Long id) {
        return ResponseEntity.ok(
            projectFacade.getProjectWorkflows(id)
                .stream()
                .map(workflow -> conversionService.convert(workflow, WorkflowModel.class))
                .toList());
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<ProjectModel> updateProject(Long id, ProjectModel projectModel) {
        return ResponseEntity.ok(conversionService.convert(
            projectFacade.updateProject(conversionService.convert(projectModel.id(id), ProjectDTO.class)),
            ProjectModel.class));
    }

    @Override
    public ResponseEntity<Void> updateProjectTags(Long id, UpdateTagsRequestModel updateTagsRequestModel) {
        List<TagModel> tagModels = updateTagsRequestModel.getTags();

        projectFacade.updateProjectTags(
            id,
            tagModels.stream()
                .map(tagModel -> conversionService.convert(tagModel, Tag.class))
                .toList());

        return ResponseEntity
            .noContent()
            .build();
    }
}
