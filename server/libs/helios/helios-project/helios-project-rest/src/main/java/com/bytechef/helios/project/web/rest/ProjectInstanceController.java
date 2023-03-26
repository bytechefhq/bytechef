
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

import com.bytechef.autoconfigure.annotation.ConditionalOnApi;
import com.bytechef.helios.project.dto.ProjectInstanceDTO;
import com.bytechef.helios.project.facade.ProjectFacade;
import com.bytechef.helios.project.web.rest.model.ProjectInstanceModel;
import com.bytechef.helios.project.web.rest.model.UpdateTagsRequestModel;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.web.rest.model.TagModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
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
public class ProjectInstanceController implements ProjectInstancesApi {

    private final ConversionService conversionService;
    private final ProjectFacade projectFacade;

    @SuppressFBWarnings("EI")
    public ProjectInstanceController(ConversionService conversionService, ProjectFacade projectFacade) {
        this.conversionService = conversionService;
        this.projectFacade = projectFacade;
    }

    @Override
    public Mono<ResponseEntity<ProjectInstanceModel>> createProjectInstance(
        Mono<ProjectInstanceModel> projectInstanceModelMono, ServerWebExchange exchange) {

        return projectInstanceModelMono.map(projectInstanceModel -> ResponseEntity.ok(
            conversionService.convert(
                projectFacade.createProjectInstance(
                    conversionService.convert(projectInstanceModel, ProjectInstanceDTO.class)),
                ProjectInstanceModel.class)));
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteProjectInstance(Long id, ServerWebExchange exchange) {
        projectFacade.deleteProjectInstance(id);

        return Mono.just(
            ResponseEntity.ok()
                .build());
    }

    @Override
    public Mono<ResponseEntity<ProjectInstanceModel>> getProjectInstance(Long id, ServerWebExchange exchange) {
        return Mono.just(
            ResponseEntity.ok(
                conversionService.convert(projectFacade.getProjectInstance(id), ProjectInstanceModel.class)));
    }

    @Override
    public Mono<ResponseEntity<Flux<ProjectInstanceModel>>> getProjectInstances(
        List<Long> projectIds, List<Long> tagIds, ServerWebExchange exchange) {

        return Mono.just(
            ResponseEntity.ok(
                Flux.fromIterable(
                    projectFacade.searchProjectInstances(projectIds, tagIds)
                        .stream()
                        .map(projectInstance -> conversionService.convert(projectInstance, ProjectInstanceModel.class))
                        .toList())));
    }

    @Override
    public Mono<ResponseEntity<ProjectInstanceModel>> updateProjectInstance(
        Long id, Mono<ProjectInstanceModel> projectInstanceModelMono, ServerWebExchange exchange) {

        return projectInstanceModelMono.map(projectInstanceModel -> ResponseEntity.ok(
            conversionService.convert(
                projectFacade.update(conversionService.convert(projectInstanceModel.id(id), ProjectInstanceDTO.class)),
                ProjectInstanceModel.class)));
    }

    @Override
    public Mono<ResponseEntity<Void>> updateProjectInstanceTags(
        Long id, Mono<UpdateTagsRequestModel> updateTagsRequestModelMono, ServerWebExchange exchange) {

        return updateTagsRequestModelMono.map(updateTagsRequestModel -> {
            List<TagModel> tagModels = updateTagsRequestModel.getTags();

            projectFacade.updateProjectInstanceTags(
                id,
                tagModels.stream()
                    .map(tagModel -> conversionService.convert(tagModel, Tag.class))
                    .toList());

            return ResponseEntity.noContent()
                .build();
        });
    }
}
