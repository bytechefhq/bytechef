
/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.web.rest;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.atlas.web.rest.model.WorkflowFormatModel;
import com.bytechef.atlas.web.rest.model.WorkflowModel;
import com.bytechef.autoconfigure.annotation.ConditionalOnApi;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnApi
@RequestMapping("${openapi.openAPIDefinition.base-path:}")
public class WorkflowController implements WorkflowsApi {

    private final ConversionService conversionService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public WorkflowController(ConversionService conversionService, WorkflowService workflowService) {
        this.conversionService = conversionService;
        this.workflowService = workflowService;
    }

    @Override
    public Mono<ResponseEntity<Void>> clearCache(ServerWebExchange exchange) {
        workflowService.clearCache();

        return Mono.just(ResponseEntity.ok()
            .build());
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteWorkflow(String id, ServerWebExchange exchange) {
        workflowService.delete(id);

        return Mono.just(ResponseEntity.ok()
            .build());
    }

    @Override
    public Mono<ResponseEntity<WorkflowModel>> getWorkflow(String id, ServerWebExchange exchange) {
        return Mono.just(
            ResponseEntity.ok(conversionService.convert(workflowService.getWorkflow(id), WorkflowModel.class)));
    }

    @Override
    public Mono<ResponseEntity<Flux<WorkflowModel>>> getWorkflows(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(Flux.fromIterable(workflowService.getWorkflows()
            .stream()
            .map(workflow -> conversionService.convert(workflow, WorkflowModel.class))
            .toList())));
    }

    @Override
    public Mono<ResponseEntity<WorkflowModel>> postWorkflow(
        Mono<WorkflowModel> workflowModelMono, ServerWebExchange exchange) {
        return workflowModelMono.map(workflowModel -> {
            WorkflowFormatModel workflowFormatModel = workflowModel.getFormat();
            WorkflowModel.SourceTypeEnum sourceTypeEnum = workflowModel.getSourceType();

            return ResponseEntity.ok(
                conversionService.convert(
                    workflowService.create(
                        workflowModel.getDefinition(),
                        Workflow.Format.valueOf(workflowFormatModel.name()),
                        Workflow.SourceType.valueOf(sourceTypeEnum.name())),
                    WorkflowModel.class));
        });
    }

    @Override
    public Mono<ResponseEntity<WorkflowModel>> putWorkflow(
        String id, Mono<WorkflowModel> workflowModelMono, ServerWebExchange exchange) {
        return workflowModelMono.map(workflowModel -> ResponseEntity.ok(
            conversionService.convert(workflowService.update(id, workflowModel.getDefinition()),
                WorkflowModel.class)));
    }
}
