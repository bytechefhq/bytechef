/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.job.sync.executor;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.JobService;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

public record JobServiceWrapper(JobSyncExecutor.JobFactoryFunction jobFactoryFunction) implements JobService {

    @Override
    public Job getJob(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Page<Job> getJobsPage(int pageNumber) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Job getTaskExecutionJob(long taskExecutionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Job create(JobParametersDTO jobParametersDTO, Workflow workflow) {
        return jobFactoryFunction.apply(jobParametersDTO);
    }

    @Override
    public void deleteJob(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Job> fetchJob(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Job> fetchLastJob() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Job> fetchLastWorkflowJob(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Job> fetchLastWorkflowJob(List<String> workflowIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Job resumeToStatusStarted(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Job setStatusToStarted(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Job setStatusToStopped(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Job update(Job job) {
        throw new UnsupportedOperationException();
    }
}
