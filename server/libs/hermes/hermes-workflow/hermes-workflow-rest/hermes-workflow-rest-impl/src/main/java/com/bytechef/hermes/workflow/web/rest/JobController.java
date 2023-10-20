
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

import com.bytechef.atlas.facade.TaskExecutionFacade;
import com.bytechef.atlas.job.JobParameters;
import com.bytechef.atlas.job.JobFactory;
import com.bytechef.atlas.message.broker.TaskMessageRoute;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.atlas.service.JobService;
import com.bytechef.hermes.workflow.web.rest.model.CreateJob200ResponseModel;
import com.bytechef.hermes.workflow.web.rest.model.JobModel;
import com.bytechef.hermes.workflow.web.rest.model.JobParametersModel;
import com.bytechef.hermes.workflow.web.rest.model.TaskExecutionModel;
import com.bytechef.autoconfigure.annotation.ConditionalOnApi;
import com.bytechef.commons.util.OptionalUtils;
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
    private final JobFactory jobFactory;
    private final JobService jobService;
    private final MessageBroker messageBroker;
    private final TaskExecutionFacade taskExecutionFacade;

    @SuppressFBWarnings("EI2")
    public JobController(
        ConversionService conversionService, JobFactory jobFactory, JobService jobService,
        MessageBroker messageBroker, TaskExecutionFacade taskExecutionFacade) {

        this.conversionService = conversionService;
        this.jobFactory = jobFactory;
        this.jobService = jobService;
        this.messageBroker = messageBroker;
        this.taskExecutionFacade = taskExecutionFacade;
    }

    @Override
    public Mono<ResponseEntity<CreateJob200ResponseModel>> createJob(
        Mono<JobParametersModel> jobParametersModelMono, ServerWebExchange exchange) {

        return jobParametersModelMono
            .map(jobParametersModel -> new CreateJob200ResponseModel()
                .jobId(
                    jobFactory.create(
                        conversionService.convert(jobParametersModel, JobParameters.class))))
            .map(ResponseEntity::ok);
    }

    @Override
    @SuppressFBWarnings("NP")
    public Mono<ResponseEntity<JobModel>> getJob(Long id, ServerWebExchange exchange) {
        return Mono.just(conversionService.convert(jobService.getJob(id), JobModel.class))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Flux<TaskExecutionModel>>> getJobTaskExecutions(
        Long jobId, ServerWebExchange exchange) {
        return Mono.just(Flux.fromIterable(taskExecutionFacade.getJobTaskExecutions(jobId)
            .stream()
            .map(taskExecution -> conversionService.convert(taskExecution, TaskExecutionModel.class))
            .toList()))
            .map(ResponseEntity::ok);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<ResponseEntity<Page>> getJobs(Integer pageNumber, ServerWebExchange exchange) {
        return Mono.just(
            jobService
                .getJobs(pageNumber)
                .map(job -> conversionService.convert(job, JobModel.class)))
            .map(ResponseEntity::ok);
    }

    @Override
    @SuppressFBWarnings("NP")
    public Mono<ResponseEntity<JobModel>> getLatestJob(ServerWebExchange exchange) {
        return Mono.just(
            conversionService.convert(OptionalUtils.orElse(jobService.fetchLatestJob(), null), JobModel.class))
            .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> restartJob(Long id, ServerWebExchange exchange) {
        messageBroker.send(TaskMessageRoute.TASKS_RESTARTS, id);

        return Mono.empty();
    }

    @Override
    public Mono<ResponseEntity<Void>> stopJob(Long id, ServerWebExchange exchange) {
        messageBroker.send(TaskMessageRoute.TASKS_STOPS, id);

        return Mono.empty();
    }
}
