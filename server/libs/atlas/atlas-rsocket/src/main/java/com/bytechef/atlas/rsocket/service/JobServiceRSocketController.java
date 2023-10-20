
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

package com.bytechef.atlas.rsocket.service;

import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.dto.JobParametersDTO;
import com.bytechef.atlas.service.JobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

/**
 * @author Ivica Cardic
 */
@Controller
public class JobServiceRSocketController {

    private final JobService jobService;

    @SuppressFBWarnings("EI2")
    public JobServiceRSocketController(JobService jobService) {
        this.jobService = jobService;
    }

    @MessageMapping("createJob")
    public Mono<Job> createJob(JobParametersDTO jobParametersDTO) {
        return Mono.create(sink -> sink.success(jobService.create(jobParametersDTO)));
    }

    @MessageMapping("fetchLatestJob")
    public Mono<Job> fetchLatestJob() {
        return Mono.create(sink -> sink.success(jobService.fetchLatestJob()
            .orElse(null)));
    }

    @MessageMapping("getJob")
    public Mono<Job> getJob(Long id) {
        return Mono.create(sink -> sink.success(jobService.getJob(id)));
    }

    @MessageMapping("getJobs")
    public Mono<List<Job>> getJobs() {
        return Mono.create(sink -> sink.success(jobService.getJobs()));
    }

    @MessageMapping("getJobsPage")
    public Mono<Page<Job>> getJobsPage(int pageNumber) {
        return Mono.create(sink -> sink.success(jobService.getJobs(pageNumber)));
    }

    @MessageMapping("getTaskExecutionJob")
    public Mono<Job> getTaskExecutionJob(Long taskExecutionId) {
        return Mono.create(sink -> sink.success(jobService.getTaskExecutionJob(taskExecutionId)));
    }

    @MessageMapping("resumeJob")
    public Mono<Job> resumeJob(Long jobId) {
        return Mono.create(sink -> sink.success(jobService.resume(jobId)));
    }

    @MessageMapping("startJob")
    public Mono<Job> startJob(Long jobId) {
        return Mono.create(sink -> sink.success(jobService.start(jobId)));
    }

    @MessageMapping("stopJob")
    public Mono<Job> stopJob(Long jobId) {
        return Mono.create(sink -> sink.success(jobService.stop(jobId)));
    }

    @MessageMapping("updateJob")
    public Mono<Job> updateJob(Job job) {
        return Mono.create(sink -> sink.success(jobService.update(job)));
    }
}
