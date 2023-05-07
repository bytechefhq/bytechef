
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

import com.bytechef.helios.project.facade.ProjectFacade;
import com.bytechef.helios.project.web.rest.model.WorkflowExecutionBasicModel;
import com.bytechef.helios.project.web.rest.model.WorkflowExecutionModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * @author Ivica Cardic
 */
@RestController

@RequestMapping("${openapi.openAPIDefinition.base-path:}/automation")
public class WorkflowExecutionController implements WorkflowExecutionsApi {

    private final ConversionService conversionService;
    private final ProjectFacade projectFacade;

    @SuppressFBWarnings("EI")
    public WorkflowExecutionController(ConversionService conversionService, ProjectFacade projectFacade) {
        this.conversionService = conversionService;
        this.projectFacade = projectFacade;
    }

    @Override
    @SuppressFBWarnings("NP")
    public Mono<ResponseEntity<WorkflowExecutionModel>> getWorkflowExecution(Long id, ServerWebExchange exchange) {
        return Mono.just(projectFacade.getWorkflowExecution(id))
            .map(worflowExecutionDTO -> conversionService.convert(worflowExecutionDTO, WorkflowExecutionModel.class))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Page>> getWorkflowExecutions(
        String jobStatus, LocalDateTime jobStartDate, LocalDateTime jobEndDate, Long projectId, Long projectInstanceId,
        String workflowId, Integer pageNumber, ServerWebExchange exchange) {

        return Mono.just(
            projectFacade
                .searchWorkflowExecutions(
                    jobStatus, jobStartDate, jobEndDate, projectId, projectInstanceId, workflowId, pageNumber)
                .map(workflowExecutionDTO -> conversionService.convert(
                    workflowExecutionDTO, WorkflowExecutionBasicModel.class)))
            .map(ResponseEntity::ok);
    }
}
