
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

package com.bytechef.dione.integration.web.rest;

import com.bytechef.atlas.web.rest.model.WorkflowModel;
import com.bytechef.autoconfigure.annotation.ConditionalOnApi;
import com.bytechef.dione.integration.domain.Integration;
import com.bytechef.dione.integration.facade.IntegrationFacade;
import com.bytechef.dione.integration.web.rest.model.CreateIntegrationWorkflowRequestModel;
import com.bytechef.dione.integration.web.rest.model.IntegrationModel;
import com.bytechef.dione.integration.web.rest.model.UpdateIntegrationTagsRequestModel;
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
public class IntegrationController implements IntegrationsApi {

    private final ConversionService conversionService;
    private final IntegrationFacade integrationFacade;

    @SuppressFBWarnings("EI2")
    public IntegrationController(ConversionService conversionService, IntegrationFacade integrationFacade) {
        this.conversionService = conversionService;
        this.integrationFacade = integrationFacade;
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteIntegration(Long id, ServerWebExchange exchange) {
        integrationFacade.delete(id);

        return Mono.just(
            ResponseEntity.ok()
                .build());
    }

    @Override
    public Mono<ResponseEntity<IntegrationModel>> getIntegration(Long id, ServerWebExchange exchange) {
        return Mono.just(
            ResponseEntity.ok(
                conversionService.convert(integrationFacade.getIntegration(id), IntegrationModel.class)));
    }

    @Override
    public Mono<ResponseEntity<Flux<IntegrationModel>>> getIntegrations(
        List<Long> categoryIds, List<Long> tagIds, ServerWebExchange exchange) {
        return Mono.just(
            ResponseEntity.ok(
                Flux.fromIterable(
                    integrationFacade.getIntegrations(categoryIds, tagIds)
                        .stream()
                        .map(integration -> conversionService.convert(integration, IntegrationModel.class))
                        .toList())));
    }

    @Override
    public Mono<ResponseEntity<Flux<WorkflowModel>>> getIntegrationWorkflows(Long id, ServerWebExchange exchange) {
        return Mono.just(
            ResponseEntity.ok(
                Flux.fromIterable(
                    integrationFacade.getIntegrationWorkflows(id)
                        .stream()
                        .map(workflow -> conversionService.convert(workflow, WorkflowModel.class))
                        .toList())));
    }

    @Override
    @Transactional
    public Mono<ResponseEntity<IntegrationModel>> createIntegration(
        Mono<IntegrationModel> integrationModelMono, ServerWebExchange exchange) {

        return integrationModelMono.map(integrationModel -> ResponseEntity.ok(
            conversionService.convert(
                integrationFacade.create(
                    conversionService.convert(integrationModel, Integration.class)),
                IntegrationModel.class)));
    }

    @Override
    public Mono<ResponseEntity<IntegrationModel>> createIntegrationWorkflow(
        Long id, Mono<CreateIntegrationWorkflowRequestModel> createIntegrationWorkflowRequestModelMono,
        ServerWebExchange exchange) {

        return createIntegrationWorkflowRequestModelMono.map(requestModel -> ResponseEntity.ok(
            conversionService.convert(
                integrationFacade.addWorkflow(
                    id, requestModel.getName(), requestModel.getDescription(), requestModel.getDefinition()),
                IntegrationModel.class)));
    }

    @Override
    public Mono<ResponseEntity<IntegrationModel>> updateIntegration(
        Long id, Mono<IntegrationModel> integrationModelMono, ServerWebExchange exchange) {

        return integrationModelMono.map(integrationModel -> ResponseEntity.ok(
            conversionService.convert(
                integrationFacade.update(conversionService.convert(integrationModel.id(id), Integration.class)),
                IntegrationModel.class)));
    }

    @Override
    public Mono<ResponseEntity<Void>> updateIntegrationTags(
        Long id, Mono<UpdateIntegrationTagsRequestModel> updateIntegrationTagsRequestModelMono,
        ServerWebExchange exchange) {

        return updateIntegrationTagsRequestModelMono.map(putIntegrationTagsRequestModel -> {
            List<TagModel> tagModels = putIntegrationTagsRequestModel.getTags();

            integrationFacade.update(
                id,
                tagModels.stream()
                    .map(tagModel -> conversionService.convert(tagModel, Tag.class))
                    .toList());

            return ResponseEntity.noContent()
                .build();
        });
    }
}
