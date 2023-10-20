
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

import com.bytechef.atlas.dto.JobParameters;
import com.bytechef.atlas.facade.JobFacade;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.web.rest.model.JobModel;
import com.bytechef.atlas.web.rest.model.JobParametersModel;
import com.bytechef.atlas.web.rest.model.PostJob200ResponseModel;
import com.bytechef.atlas.web.rest.model.TaskExecutionModel;
import com.bytechef.autoconfigure.annotation.ConditionalOnApi;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
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
public class JobController implements JobsApi {

    private final ConversionService conversionService;
    private final JobFacade jobFacade;
    private final JobService jobService;
    private final MessageBroker messageBroker;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI2")
    public JobController(
        ConversionService conversionService,
        JobFacade jobFacade, JobService jobService,
        MessageBroker messageBroker,
        TaskExecutionService taskExecutionService) {
        this.conversionService = conversionService;
        this.jobFacade = jobFacade;
        this.jobService = jobService;
        this.messageBroker = messageBroker;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public Mono<ResponseEntity<JobModel>> getJob(Long id, ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(conversionService.convert(jobService.getJob(id), JobModel.class)));
    }

    @Override
    public Mono<ResponseEntity<Flux<TaskExecutionModel>>> getJobTaskExecutions(
        Long jobId, ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(Flux.fromIterable(taskExecutionService.getJobTaskExecutions(jobId)
            .stream()
            .map(taskExecution -> conversionService.convert(taskExecution, TaskExecutionModel.class))
            .toList())));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<ResponseEntity<Page>> getJobs(Integer pageNumber, ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(
            jobService.getJobs(pageNumber)
                .map(job -> conversionService.convert(job, JobModel.class))));
    }

    @Override
    public Mono<ResponseEntity<JobModel>> getLatestJob(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(
            conversionService.convert(jobService.fetchLatestJob()
                .orElse(null), JobModel.class)));
    }

    @Override
    public Mono<ResponseEntity<PostJob200ResponseModel>> postJob(
        Mono<JobParametersModel> workflowParametersModelMono, ServerWebExchange exchange) {
        return workflowParametersModelMono.map(workflowParametersModel -> {
            JobParameters jobParameters = conversionService.convert(
                workflowParametersModel, JobParameters.class);

            long jobId = jobFacade.create(jobParameters);

            return ResponseEntity.ok(new PostJob200ResponseModel().jobId(jobId));
        });
    }

    @Override
    public Mono<ResponseEntity<Void>> restartJob(Long id, ServerWebExchange exchange) {
        messageBroker.send(Queues.RESTARTS, id);

        return Mono.empty();
    }

    @Override
    public Mono<ResponseEntity<Void>> stopJob(Long id, ServerWebExchange exchange) {
        messageBroker.send(Queues.STOPS, id);

        return Mono.empty();
    }
}
