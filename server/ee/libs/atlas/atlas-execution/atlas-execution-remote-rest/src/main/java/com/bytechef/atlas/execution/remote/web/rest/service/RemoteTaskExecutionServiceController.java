
/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.atlas.execution.remote.web.rest.service;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/remote/task-execution-service")
public class RemoteTaskExecutionServiceController {

    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI")
    public RemoteTaskExecutionServiceController(TaskExecutionService taskExecutionService) {
        this.taskExecutionService = taskExecutionService;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/create",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<TaskExecution> create(@RequestBody TaskExecution taskExecution) {
        return ResponseEntity.ok(taskExecutionService.create(taskExecution));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-task-execution/{id}",
        produces = {
            "application/json"
        })
    public ResponseEntity<TaskExecution> getTaskExecution(@PathVariable long id) {
        return ResponseEntity.ok(taskExecutionService.getTaskExecution(id));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-job-task-executions/{jobId}",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<TaskExecution>> getJobTaskExecutions(@PathVariable long jobId) {
        return ResponseEntity.ok(taskExecutionService.getJobTaskExecutions(jobId));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-parent-task-executions/{parentId}",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<TaskExecution>> getParentTaskExecutions(@PathVariable long parentId) {
        return ResponseEntity.ok(taskExecutionService.getParentTaskExecutions(parentId));
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
    public ResponseEntity<TaskExecution> update(@RequestBody TaskExecution taskExecution) {
        return ResponseEntity.ok(taskExecutionService.update(taskExecution));
    }
}
