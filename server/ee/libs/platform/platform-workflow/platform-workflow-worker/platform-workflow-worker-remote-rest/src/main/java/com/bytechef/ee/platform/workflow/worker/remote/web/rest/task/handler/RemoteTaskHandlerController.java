/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.worker.remote.web.rest.task.handler;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
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
@RequestMapping("/remote/task-handler")
public class RemoteTaskHandlerController {

    private final TaskHandlerRegistry taskHandlerRegistry;

    public RemoteTaskHandlerController(TaskHandlerRegistry taskHandlerRegistry) {
        this.taskHandlerRegistry = taskHandlerRegistry;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/handle/{type}",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Object> handler(
        @PathVariable("type") String type, @Valid @RequestBody TaskExecution taskExecution)
        throws TaskExecutionException {

        return ResponseEntity.ok(
            taskHandlerRegistry.getTaskHandler(type)
                .handle(taskExecution));
    }
}
