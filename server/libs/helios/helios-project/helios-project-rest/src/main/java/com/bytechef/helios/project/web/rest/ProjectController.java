
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

import com.bytechef.atlas.web.rest.model.WorkflowModel;
import com.bytechef.autoconfigure.annotation.ConditionalOnApi;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnApi
@RequestMapping("${openapi.openAPIDefinition.base-path:}")
public class ProjectController implements ProjectsApi {

    private final ConversionService conversionService;
    private final ProjectFacade projectFacade;

    @SuppressFBWarnings("EI2")
    public ProjectController(ConversionService conversionService, ProjectFacade projectFacade) {
        this.conversionService = conversionService;
        this.projectFacade = projectFacade;
    }

    @Override
    @Transactional
    public Mono<ResponseEntity<ProjectModel>> createProject(
        Mono<ProjectModel> projectModelMono, ServerWebExchange exchange) {

        return projectModelMono.map(projectModel -> conversionService.convert(
            projectFacade.createProject(conversionService.convert(projectModel, ProjectDTO.class)),
            ProjectModel.class))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<WorkflowModel>> createProjectWorkflow(
        Long id, Mono<CreateProjectWorkflowRequestModel> createProjectWorkflowRequestModelMono,
        ServerWebExchange exchange) {

        return createProjectWorkflowRequestModelMono.map(requestModel -> conversionService.convert(
            projectFacade.addWorkflow(
                id, requestModel.getLabel(), requestModel.getDescription(), requestModel.getDefinition()),
            WorkflowModel.class))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteProject(Long id, ServerWebExchange exchange) {
        projectFacade.deleteProject(id);

        return Mono.just(
            ResponseEntity.ok()
                .build());
    }

    @Override
    public Mono<ResponseEntity<ProjectModel>> duplicateProject(Long id, ServerWebExchange exchange) {
        return Mono.just(
            conversionService.convert(projectFacade.duplicateProject(id), ProjectModel.class))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<ProjectModel>> getProject(Long id, ServerWebExchange exchange) {
        return Mono.just(
            conversionService.convert(projectFacade.getProject(id), ProjectModel.class))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<ProjectModel>>> getProjects(
        List<Long> categoryIds, Boolean projectInstances, List<Long> tagIds, ServerWebExchange exchange) {

        return Mono.just(
            Flux.fromIterable(
                projectFacade.searchProjects(categoryIds, projectInstances != null, tagIds)
                    .stream()
                    .map(project -> conversionService.convert(project, ProjectModel.class))
                    .toList()))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<WorkflowModel>>> getProjectWorkflows(Long id, ServerWebExchange exchange) {
        return Mono.just(
            Flux.fromIterable(
                projectFacade.getProjectWorkflows(id)
                    .stream()
                    .map(workflow -> conversionService.convert(workflow, WorkflowModel.class))
                    .toList()))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<ProjectModel>> updateProject(
        Long id, Mono<ProjectModel> projectModelMono, ServerWebExchange exchange) {

        return projectModelMono.map(projectModel -> conversionService.convert(
            projectFacade.update(conversionService.convert(projectModel.id(id), ProjectDTO.class)),
            ProjectModel.class))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> updateProjectTags(
        Long id, Mono<UpdateTagsRequestModel> updateTagsRequestModelMono, ServerWebExchange exchange) {

        return updateTagsRequestModelMono.map(updateTagsRequestModel -> {
            List<TagModel> tagModels = updateTagsRequestModel.getTags();

            projectFacade.updateProjectTags(
                id,
                tagModels.stream()
                    .map(tagModel -> conversionService.convert(tagModel, Tag.class))
                    .toList());

            return ResponseEntity.noContent()
                .build();
        });
    }
}
