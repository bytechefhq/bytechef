
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

package com.bytechef.hermes.workflow.web.rest;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.hermes.workflow.test.executor.WorkflowExecutor;
import com.bytechef.hermes.workflow.web.rest.model.WorkflowFormatModel;
import com.bytechef.hermes.workflow.web.rest.model.WorkflowModel;
import com.bytechef.autoconfigure.annotation.ConditionalOnApi;
import com.bytechef.hermes.workflow.web.rest.model.WorkflowResponseModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private final WorkflowExecutor testWorkflowExecutor;

    @SuppressFBWarnings("EI2")
    public WorkflowController(
        ConversionService conversionService, WorkflowService workflowService, WorkflowExecutor testWorkflowExecutor) {

        this.conversionService = conversionService;
        this.workflowService = workflowService;
        this.testWorkflowExecutor = testWorkflowExecutor;
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteWorkflow(String id, ServerWebExchange exchange) {
        workflowService.delete(id);

        return Mono.just(
            ResponseEntity
                .ok()
                .build());
    }

    @Override
    @SuppressFBWarnings("NP")
    public Mono<ResponseEntity<WorkflowModel>> getWorkflow(String id, ServerWebExchange exchange) {
        return Mono
            .just(
                conversionService.convert(workflowService.getWorkflow(id), WorkflowModel.class)
                    .definition(null))
            .map(ResponseEntity::ok);
    }

    @Override
    @SuppressFBWarnings("NP")
    public Mono<ResponseEntity<Flux<WorkflowModel>>> getWorkflows(ServerWebExchange exchange) {
        List<WorkflowModel> workflowModels = new ArrayList<>();

        for (Workflow workflow : workflowService.getWorkflows()) {
            workflowModels.add(
                conversionService.convert(workflow, WorkflowModel.class)
                    .definition(null));
        }

        return Mono
            .just(Flux.fromIterable(workflowModels))
            .map(ResponseEntity::ok);
    }

    @Override
    @SuppressFBWarnings("NP")
    public Mono<ResponseEntity<WorkflowModel>> createWorkflow(
        Mono<WorkflowModel> workflowModelMono, ServerWebExchange exchange) {

        return workflowModelMono
            .map(this::getWorkflowModel)
            .map(ResponseEntity::ok);
    }

    @Override
    @SuppressFBWarnings("NP")
    public Mono<ResponseEntity<WorkflowResponseModel>> testWorkflow(
        String id, Mono<Map<String, Object>> inputsMono, ServerWebExchange exchange) {

        return inputsMono
            .map(inputs -> testWorkflowExecutor.execute(id, inputs))
            .map(
                workflowResponse -> conversionService.convert(
                    workflowResponse, WorkflowResponseModel.class))
            .map(ResponseEntity::ok);
    }

    @Override
    @SuppressFBWarnings("NP")
    public Mono<ResponseEntity<WorkflowModel>> updateWorkflow(
        String id, Mono<WorkflowModel> workflowModelMono, ServerWebExchange exchange) {

        return workflowModelMono
            .map(
                workflowModel -> conversionService.convert(
                    workflowService.update(id, workflowModel.getDefinition()), WorkflowModel.class))
            .map(ResponseEntity::ok);
    }

    private WorkflowModel getWorkflowModel(WorkflowModel workflowModel) {
        WorkflowFormatModel workflowFormatModel = workflowModel.getFormat();
        WorkflowModel.SourceTypeEnum sourceTypeEnum = workflowModel.getSourceType();

        return conversionService.convert(
            workflowService.create(
                workflowModel.getDefinition(),
                Workflow.Format.valueOf(workflowFormatModel.name()),
                Workflow.SourceType.valueOf(sourceTypeEnum.name())),
            WorkflowModel.class);
    }
}
