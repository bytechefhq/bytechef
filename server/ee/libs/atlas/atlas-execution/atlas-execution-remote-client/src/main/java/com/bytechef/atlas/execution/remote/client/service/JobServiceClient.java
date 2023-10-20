
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

package com.bytechef.atlas.execution.remote.client.service;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.service.JobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Component
public class JobServiceClient implements JobService {

    private final WebClient.Builder loadBalancedWebClientBuilder;

    @SuppressFBWarnings("EI")
    public JobServiceClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.loadBalancedWebClientBuilder = loadBalancedWebClientBuilder;
    }

    @Override
    public Job create(JobParameters jobParameters, Workflow workflow) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Job> fetchLatestJob() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Job> getJobs() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Page<Job> getJobs(int pageNumber) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Job getJob(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Job getTaskExecutionJob(long taskExecutionId) {
        return loadBalancedWebClientBuilder
            .build()
            .get()
            .uri(uriBuilder -> uriBuilder
                .host("execution-service-app")
                .path("/api/internal/job-service/get-task-execution-job/{taskExecutionId}")
                .build(taskExecutionId))
            .retrieve()
            .bodyToMono(Job.class)
            .block();
    }

    @Override
    public Job resumeToStatusStarted(long id) {
        return loadBalancedWebClientBuilder
            .build()
            .put()
            .uri(uriBuilder -> uriBuilder
                .host("execution-service-app")
                .path("/api/internal/job-service/resume-to-status-started/{id}")
                .build(id))
            .retrieve()
            .bodyToMono(Job.class)
            .block();
    }

    @Override
    public Page<Job> getJobs(
        String status, LocalDateTime startDate, LocalDateTime endDate, String workflowId, List<String> workflowIds,
        Integer pageNumber) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Job setStatusToStarted(long id) {
        return loadBalancedWebClientBuilder
            .build()
            .put()
            .uri(uriBuilder -> uriBuilder
                .host("execution-service-app")
                .path("/api/internal/job-service/set-status-to-started/{id}")
                .build(id))
            .retrieve()
            .bodyToMono(Job.class)
            .block();
    }

    @Override
    public Job setStatusToStopped(long id) {
        return loadBalancedWebClientBuilder
            .build()
            .put()
            .uri(uriBuilder -> uriBuilder
                .host("execution-service-app")
                .path("/api/internal/job-service/set-status-to-stopped/{id}")
                .build(id))
            .retrieve()
            .bodyToMono(Job.class)
            .block();
    }

    @Override
    public Job update(Job job) {
        return loadBalancedWebClientBuilder
            .build()
            .put()
            .uri(uriBuilder -> uriBuilder
                .host("execution-service-app")
                .path("/api/internal/job-service/update")
                .build())
            .bodyValue(job)
            .retrieve()
            .bodyToMono(Job.class)
            .block();
    }
}
