
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

import com.bytechef.helios.project.dto.ProjectInstanceDTO;
import com.bytechef.helios.project.facade.ProjectInstanceFacade;
import com.bytechef.helios.project.web.rest.model.CreateProjectInstanceJob200ResponseModel;
import com.bytechef.helios.project.web.rest.model.CreateProjectInstanceJobRequestModel;
import com.bytechef.helios.project.web.rest.model.ProjectInstanceModel;
import com.bytechef.helios.project.web.rest.model.UpdateTagsRequestModel;
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
public class ProjectInstanceController implements ProjectInstancesApi {

    private final ConversionService conversionService;
    private final ProjectInstanceFacade projectInstanceFacade;

    @SuppressFBWarnings("EI")
    public ProjectInstanceController(ConversionService conversionService, ProjectInstanceFacade projectInstanceFacade) {
        this.conversionService = conversionService;
        this.projectInstanceFacade = projectInstanceFacade;
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<ProjectInstanceModel> createProjectInstance(ProjectInstanceModel projectInstanceModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                projectInstanceFacade.createProjectInstance(
                    conversionService.convert(projectInstanceModel, ProjectInstanceDTO.class)),
                ProjectInstanceModel.class));
    }

    @Override
    public ResponseEntity<CreateProjectInstanceJob200ResponseModel> createProjectInstanceJob(
        Long id, CreateProjectInstanceJobRequestModel createProjectInstanceJobRequestModel) {

        return ResponseEntity.ok(
            new CreateProjectInstanceJob200ResponseModel()
                .jobId(
                    projectInstanceFacade.createProjectInstanceJob(
                        id, createProjectInstanceJobRequestModel.getWorkflowId())));
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
    public ResponseEntity<Void> enableProjectInstanceWorkflow(
        Long id, String workflowId, Boolean enable) {

        projectInstanceFacade.enableProjectInstanceWorkflow(id, workflowId, enable);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<ProjectInstanceModel> getProjectInstance(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(projectInstanceFacade.getProjectInstance(id), ProjectInstanceModel.class));
    }

    @Override
    public ResponseEntity<List<ProjectInstanceModel>> getProjectInstances(
        List<Long> projectIds, List<Long> tagIds) {

        return ResponseEntity.ok(
            projectInstanceFacade.searchProjectInstances(projectIds, tagIds)
                .stream()
                .map(projectInstance -> conversionService.convert(projectInstance, ProjectInstanceModel.class))
                .toList());
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<ProjectInstanceModel> updateProjectInstance(
        Long id, ProjectInstanceModel projectInstanceModel) {

        return ResponseEntity.ok(conversionService.convert(
            projectInstanceFacade.updateProjectInstance(
                conversionService.convert(
                    projectInstanceModel.id(id), ProjectInstanceDTO.class)),
            ProjectInstanceModel.class));
    }

    @Override
    public ResponseEntity<Void> updateProjectInstanceTags(Long id, UpdateTagsRequestModel updateTagsRequestModel) {
        List<TagModel> tagModels = updateTagsRequestModel.getTags();

        projectInstanceFacade.updateProjectInstanceTags(
            id,
            tagModels.stream()
                .map(tagModel -> conversionService.convert(tagModel, Tag.class))
                .toList());

        return ResponseEntity
            .noContent()
            .build();
    }
}
