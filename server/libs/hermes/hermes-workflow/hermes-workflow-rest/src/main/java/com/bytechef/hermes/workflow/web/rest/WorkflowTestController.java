
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

package com.bytechef.hermes.workflow.web.rest;

import com.bytechef.autoconfigure.annotation.ConditionalOnApi;
import com.bytechef.hermes.workflow.test.executor.WorkflowTestExecutor;
import com.bytechef.hermwes.workflow.web.rest.WorkflowTestsApi;
import com.bytechef.hermwes.workflow.web.rest.model.WorkflowTestResponseModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnApi
@RequestMapping("${openapi.openAPIDefinition.base-path:}")
public class WorkflowTestController implements WorkflowTestsApi {

    private final ConversionService conversionService;
    private final WorkflowTestExecutor workflowTestExecutor;

    public WorkflowTestController(ConversionService conversionService, WorkflowTestExecutor workflowTestExecutor) {
        this.conversionService = conversionService;
        this.workflowTestExecutor = workflowTestExecutor;
    }

    @Override
    @SuppressFBWarnings("NP")
    public Mono<ResponseEntity<WorkflowTestResponseModel>> testWorkflow(
        String id, Mono<Map<String, Object>> inputsMono, ServerWebExchange exchange) {

        return inputsMono.map(inputs -> workflowTestExecutor.execute(id, inputs))
            .map(workflowTestResponse -> conversionService.convert(
                workflowTestResponse, WorkflowTestResponseModel.class))
            .map(ResponseEntity::ok);
    }
}
