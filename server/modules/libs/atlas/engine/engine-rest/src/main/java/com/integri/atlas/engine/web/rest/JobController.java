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

package com.integri.atlas.engine.web.rest;

import com.integri.atlas.engine.annotation.ConditionalOnCoordinator;
import com.integri.atlas.engine.coordinator.Coordinator;
import com.integri.atlas.engine.data.Page;
import com.integri.atlas.engine.job.Job;
import com.integri.atlas.engine.job.JobSummary;
import com.integri.atlas.engine.job.repository.JobRepository;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Arik Cohen
 */
@RestController
@ConditionalOnCoordinator
public class JobController {

    private final Coordinator coordinator;
    private final JobRepository jobRepository;

    public JobController(Coordinator coordinator, JobRepository jobRepository) {
        this.coordinator = coordinator;
        this.jobRepository = jobRepository;
    }

    @GetMapping(value = "/jobs", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<JobSummary> list(@RequestParam(value = "pageNumber", defaultValue = "1") Integer pageNumber) {
        return jobRepository.getPage(pageNumber);
    }

    @PostMapping(value = "/jobs", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Job create(@RequestBody Map<String, Object> jobRequestMap) {
        return coordinator.create(jobRequestMap);
    }

    @GetMapping(value = "/jobs/{id}")
    public Job get(@PathVariable("id") String jobId) {
        return jobRepository.getById(jobId);
    }

    @GetMapping(value = "/jobs/latest", produces = MediaType.APPLICATION_JSON_VALUE)
    public Job latest() {
        Optional<Job> job = jobRepository.getLatest();

        Assert.isTrue(job.isPresent(), "no jobs");

        return job.get();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public void handleIllegalArgumentException(HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.sendError(HttpStatus.BAD_REQUEST.value());
    }

    @PutMapping(value = "/jobs/{id}/restart")
    public Job restart(@PathVariable("id") String jobId) {
        return coordinator.resume(jobId);
    }

    @PutMapping(value = "/jobs/{id}/stop")
    public Job stop(@PathVariable("id") String jobId) {
        return coordinator.stop(jobId);
    }
}
