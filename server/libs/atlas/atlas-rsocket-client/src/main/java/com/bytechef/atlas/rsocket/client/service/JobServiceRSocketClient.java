
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

package com.bytechef.atlas.rsocket.client.service;

import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.dto.JobParameters;
import com.bytechef.atlas.service.JobService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class JobServiceRSocketClient implements JobService {

    private final RSocketRequester rSocketRequester;

    public JobServiceRSocketClient(RSocketRequester rSocketRequester) {
        this.rSocketRequester = rSocketRequester;
    }

    @Override
    public Job create(JobParameters jobParameters) {
        return rSocketRequester
            .route("createJob")
            .data(jobParameters)
            .retrieveMono(Job.class)
            .block();
    }

    @Override
    public Job getJob(long id) {
        return rSocketRequester.route("getJob")
            .data(id)
            .retrieveMono(Job.class)
            .block();
    }

    @Override
    public List<Job> getJobs() {
        return rSocketRequester
            .route("getJobs")
            .retrieveMono(new ParameterizedTypeReference<List<Job>>() {})
            .block();
    }

    @Override
    public Page<Job> getJobs(int pageNumber) {
        return rSocketRequester
            .route("getJobsPage")
            .data(pageNumber)
            .retrieveMono(new ParameterizedTypeReference<Page<Job>>() {})
            .block();
    }

    @Override
    public Optional<Job> fetchLatestJob() {
        return Optional.ofNullable(
            rSocketRequester.route("fetchLatestJob")
                .retrieveMono(Job.class)
                .block());
    }

    @Override
    public Job getTaskExecutionJob(long taskExecutionId) {
        return rSocketRequester
            .route("getTaskExecutionJob")
            .data(taskExecutionId)
            .retrieveMono(Job.class)
            .block();
    }

    @Override
    public Job resume(long jobId) {
        return rSocketRequester
            .route("resumeJob")
            .data(jobId)
            .retrieveMono(Job.class)
            .block();
    }

    @Override
    public Page<Job> searchJobs(
        String status, LocalDateTime startTime, LocalDateTime endTime, Long workflowId, Integer pageNumber) {

        return null;
    }

    @Override
    public Job start(long jobId) {
        return rSocketRequester
            .route("startJob")
            .data(jobId)
            .retrieveMono(Job.class)
            .block();
    }

    @Override
    public Job stop(long jobId) {
        return rSocketRequester
            .route("stopJob")
            .data(jobId)
            .retrieveMono(Job.class)
            .block();
    }

    @Override
    public Job update(Job job) {
        return rSocketRequester
            .route("updateJob")
            .data(job)
            .retrieveMono(Job.class)
            .block();
    }
}
