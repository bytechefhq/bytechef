
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

package com.bytechef.hermes.project.web.rest;

import com.bytechef.atlas.web.rest.model.WorkflowModel;
import com.bytechef.autoconfigure.annotation.ConditionalOnApi;
import com.bytechef.category.web.rest.model.CategoryModel;
import com.bytechef.hermes.project.domain.Project;
import com.bytechef.hermes.project.facade.ProjectFacade;
import com.bytechef.hermes.project.web.rest.model.CreateProjectWorkflowRequestModel;
import com.bytechef.hermes.project.web.rest.model.ProjectModel;
import com.bytechef.hermes.project.web.rest.model.UpdateProjectTagsRequestModel;
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
    public Mono<ResponseEntity<Void>> deleteProject(Long id, ServerWebExchange exchange) {
        projectFacade.delete(id);

        return Mono.just(
            ResponseEntity.ok()
                .build());
    }

    @Override
    public Mono<ResponseEntity<ProjectModel>> duplicateProject(Long id, ServerWebExchange exchange) {
        return Mono.just(
            ResponseEntity.ok(
                conversionService.convert(projectFacade.duplicate(id), ProjectModel.class)));
    }

    @Override
    public Mono<ResponseEntity<ProjectModel>> getProject(Long id, ServerWebExchange exchange) {
        return Mono.just(
            ResponseEntity.ok(
                conversionService.convert(projectFacade.getProject(id), ProjectModel.class)));
    }

    @Override
    public Mono<ResponseEntity<Flux<CategoryModel>>> getProjectCategories(ServerWebExchange exchange) {
        return Mono.just(
            ResponseEntity.ok(
                Flux.fromIterable(
                    projectFacade.getProjectCategories()
                        .stream()
                        .map(category -> conversionService.convert(category, CategoryModel.class))
                        .toList())));
    }

    @Override
    public Mono<ResponseEntity<Flux<ProjectModel>>> getProjects(
        List<Long> categoryIds, List<Long> tagIds, ServerWebExchange exchange) {

        return Mono.just(
            ResponseEntity.ok(
                Flux.fromIterable(
                    projectFacade.getProjects(categoryIds, tagIds)
                        .stream()
                        .map(project -> conversionService.convert(project, ProjectModel.class))
                        .toList())));
    }

    @Override
    public Mono<ResponseEntity<Flux<TagModel>>> getProjectTags(ServerWebExchange exchange) {
        return Mono.just(
            ResponseEntity.ok(
                Flux.fromIterable(
                    projectFacade.getProjectTags()
                        .stream()
                        .map(tag -> conversionService.convert(tag, TagModel.class))
                        .toList())));
    }

    @Override
    public Mono<ResponseEntity<Flux<WorkflowModel>>> getProjectWorkflows(Long id, ServerWebExchange exchange) {
        return Mono.just(
            ResponseEntity.ok(
                Flux.fromIterable(
                    projectFacade.getProjectWorkflows(id)
                        .stream()
                        .map(workflow -> conversionService.convert(workflow, WorkflowModel.class))
                        .toList())));
    }

    @Override
    @Transactional
    public Mono<ResponseEntity<ProjectModel>> createProject(
        Mono<ProjectModel> projectModelMono, ServerWebExchange exchange) {

        return projectModelMono.map(projectModel -> ResponseEntity.ok(
            conversionService.convert(
                projectFacade.create(conversionService.convert(projectModel, Project.class)),
                ProjectModel.class)));
    }

    @Override
    public Mono<ResponseEntity<ProjectModel>> createProjectWorkflow(
        Long id, Mono<CreateProjectWorkflowRequestModel> createProjectWorkflowRequestModelMono,
        ServerWebExchange exchange) {

        return createProjectWorkflowRequestModelMono.map(requestModel -> ResponseEntity.ok(
            conversionService.convert(
                projectFacade.addWorkflow(
                    id, requestModel.getName(), requestModel.getDescription(), requestModel.getDefinition()),
                ProjectModel.class)));
    }

    @Override
    public Mono<ResponseEntity<ProjectModel>> updateProject(
        Long id, Mono<ProjectModel> projectModelMono, ServerWebExchange exchange) {

        return projectModelMono.map(projectModel -> ResponseEntity.ok(
            conversionService.convert(
                projectFacade.update(conversionService.convert(projectModel.id(id), Project.class)),
                ProjectModel.class)));
    }

    @Override
    public Mono<ResponseEntity<Void>> updateProjectTags(
        Long id, Mono<UpdateProjectTagsRequestModel> updateProjectTagsRequestModelMono, ServerWebExchange exchange) {

        return updateProjectTagsRequestModelMono.map(putProjectTagsRequestModel -> {
            List<TagModel> tagModels = putProjectTagsRequestModel.getTags();

            projectFacade.update(
                id,
                tagModels.stream()
                    .map(tagModel -> conversionService.convert(tagModel, Tag.class))
                    .toList());

            return ResponseEntity.noContent()
                .build();
        });
    }
}
