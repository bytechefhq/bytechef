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

import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.web.rest.model.ProjectModel;
import com.bytechef.automation.configuration.web.rest.model.ProjectStatusModel;
import com.bytechef.automation.configuration.web.rest.model.ProjectVersionModel;
import com.bytechef.automation.configuration.web.rest.model.PublishProjectRequestModel;
import com.bytechef.platform.annotation.ConditionalOnEndpoint;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}")
@ConditionalOnEndpoint
public class ProjectApiController implements ProjectApi {

    private final ConversionService conversionService;
    private final ProjectFacade projectFacade;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI2")
    public ProjectApiController(
        ConversionService conversionService, ProjectFacade projectFacade, ProjectService projectService) {

        this.conversionService = conversionService;
        this.projectFacade = projectFacade;
        this.projectService = projectService;
    }

    @Override
    public ResponseEntity<ProjectModel> createProject(ProjectModel projectModel) {

        return ResponseEntity.ok(
            conversionService.convert(
                projectFacade.createProject(
                    Validate.notNull(conversionService.convert(projectModel, ProjectDTO.class), "projectDTO")),
                ProjectModel.class));
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
    public ResponseEntity<List<ProjectVersionModel>> getProjectVersions(Long id) {
        return ResponseEntity.ok(
            projectService.getProjectVersions(id)
                .stream()
                .map(projectVersion -> conversionService.convert(projectVersion, ProjectVersionModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<List<ProjectModel>> getProjects(
        Long categoryId, Boolean projectInstances, Long tagId, ProjectStatusModel status) {

        return ResponseEntity.ok(
            projectFacade
                .getProjects(
                    categoryId, projectInstances != null, tagId, status == null ? null : Status.valueOf(status.name()))
                .stream()
                .map(project -> conversionService.convert(project, ProjectModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<Void> publishProject(Long id, PublishProjectRequestModel publishProjectRequestModel) {
        projectService.publishProject(
            id, publishProjectRequestModel == null ? null : publishProjectRequestModel.getDescription());

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<ProjectModel> updateProject(Long id, ProjectModel projectModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                projectFacade.updateProject(
                    Validate.notNull(conversionService.convert(projectModel.id(id), ProjectDTO.class), "projectDTO")),
                ProjectModel.class));
    }
}
