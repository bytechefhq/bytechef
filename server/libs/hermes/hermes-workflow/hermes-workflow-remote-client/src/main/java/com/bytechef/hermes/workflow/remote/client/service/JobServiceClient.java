
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

package com.bytechef.hermes.workflow.remote.client.service;

import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.dto.JobParameters;
import com.bytechef.atlas.service.JobService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Component
public class JobServiceClient implements JobService {

    @Override
    public Job create(JobParameters jobParameters) {
        return null;
    }

    @Override
    public Optional<Job> fetchLatestJob() {
        return Optional.empty();
    }

    @Override
    public List<Job> getJobs() {
        return null;
    }

    @Override
    public Page<Job> getJobs(int pageNumber) {
        return null;
    }

    @Override
    public Job getJob(long id) {
        return null;
    }

    @Override
    public Job getTaskExecutionJob(long taskExecutionId) {
        return null;
    }

    @Override
    public Job resumeToStatusStarted(long id) {
        return null;
    }

    @Override
    public Page<Job> searchJobs(
        String status, LocalDateTime startDate, LocalDateTime endDate, String workflowId, List<String> workflowIds,
        Integer pageNumber) {

        return null;
    }

    @Override
    public Job setStatusToStarted(long id) {
        return null;
    }

    @Override
    public Job setStatusToStopped(long id) {
        return null;
    }

    @Override
    public Job update(Job job) {
        return null;
    }
}
