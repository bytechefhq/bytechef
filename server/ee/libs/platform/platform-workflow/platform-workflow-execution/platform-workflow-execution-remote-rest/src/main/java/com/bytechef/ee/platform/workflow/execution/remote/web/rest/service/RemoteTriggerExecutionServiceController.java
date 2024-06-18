/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.web.rest.service;

import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
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
@RequestMapping("/remote/trigger-execution-service")
public class RemoteTriggerExecutionServiceController {

    private final TriggerExecutionService triggerExecutionService;

    @SuppressFBWarnings("EI")
    public RemoteTriggerExecutionServiceController(TriggerExecutionService triggerExecutionService) {
        this.triggerExecutionService = triggerExecutionService;
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
    public ResponseEntity<TriggerExecution> create(@RequestBody TriggerExecution triggerExecution) {
        return ResponseEntity.ok(triggerExecutionService.create(triggerExecution));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-trigger-execution/{id}",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<TriggerExecution> getTriggerExecution(@PathVariable long id) {
        return ResponseEntity.ok(triggerExecutionService.getTriggerExecution(id));
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
    public ResponseEntity<TriggerExecution> update(@RequestBody TriggerExecution triggerExecution) {
        return ResponseEntity.ok(triggerExecutionService.update(triggerExecution));
    }
}
