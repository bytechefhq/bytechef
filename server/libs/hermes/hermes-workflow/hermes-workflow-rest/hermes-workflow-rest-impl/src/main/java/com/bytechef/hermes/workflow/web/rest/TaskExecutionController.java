
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

import com.bytechef.hermes.workflow.facade.TaskExecutionFacade;
import com.bytechef.hermes.workflow.web.rest.model.TaskExecutionModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@RestController

@RequestMapping("${openapi.openAPIDefinition.base-path:}/core")
public class TaskExecutionController implements TaskExecutionsApi {

    private final ConversionService conversionService;
    private final TaskExecutionFacade taskExecutionFacade;

    @SuppressFBWarnings("EI2")
    public TaskExecutionController(ConversionService conversionService, TaskExecutionFacade taskExecutionFacade) {
        this.conversionService = conversionService;
        this.taskExecutionFacade = taskExecutionFacade;
    }

    @Override
    @SuppressFBWarnings("NP")
    public Mono<ResponseEntity<TaskExecutionModel>> getTaskExecution(Long id, ServerWebExchange exchange) {
        return Mono.just(
            conversionService.convert(taskExecutionFacade.getTaskExecution(id), TaskExecutionModel.class))
            .map(ResponseEntity::ok);
    }
}
