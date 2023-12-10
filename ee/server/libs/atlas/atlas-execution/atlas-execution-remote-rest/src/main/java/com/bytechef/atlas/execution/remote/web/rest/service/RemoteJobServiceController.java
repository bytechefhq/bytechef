/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.atlas.execution.remote.web.rest.service;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.OptionalUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/remote/job-service")
public class RemoteJobServiceController {

    private final JobService jobService;

    @SuppressFBWarnings("EI")
    public RemoteJobServiceController(JobService jobService) {
        this.jobService = jobService;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/create",
        produces = {
            "application/json"
        })
    public ResponseEntity<Job> create(@RequestBody JobCreateRequest jobCreateRequest) {
        return ResponseEntity.ok(jobService.create(jobCreateRequest.jobParameters, jobCreateRequest.workflow));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/fetch-last-workflow-job/{workflowId}")
    public ResponseEntity<Job> create(@PathVariable String workflowId) {
        return OptionalUtils.mapOrElse(
            jobService.fetchLastWorkflowJob(workflowId),
            ResponseEntity::ok,
            ResponseEntity
                .noContent()
                .build());
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-job/{id}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Job> getJob(@PathVariable long id) {
        return ResponseEntity.ok(jobService.getJob(id));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-task-execution-job/{taskExecutionId}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Job> getTaskExecutionJob(@PathVariable long taskExecutionId) {
        return ResponseEntity.ok(jobService.getTaskExecutionJob(taskExecutionId));
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/resume-to-status-started/{id}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Job> resumeToStatusStarted(@PathVariable long id) {
        return ResponseEntity.ok(jobService.resumeToStatusStarted(id));
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/set-status-to-started/{id}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Job> setStatusToStarted(@PathVariable long id) {
        return ResponseEntity.ok(jobService.setStatusToStarted(id));
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/set-status-to-stopped/{id}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Job> setStatusToStopped(@PathVariable long id) {
        return ResponseEntity.ok(jobService.setStatusToStopped(id));
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/update",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<Job> update(@RequestBody Job job) {
        return ResponseEntity.ok(jobService.update(job));
    }

    @SuppressFBWarnings("EI")
    public record JobCreateRequest(JobParameters jobParameters, Workflow workflow) {
    }
}
