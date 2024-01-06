/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.execution.remote.web.rest.service;

import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.execution.service.TriggerStateService;
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
@RequestMapping("/remote/trigger-state-service")
public class RemoteTriggerStateServiceController {

    private final TriggerStateService triggerStateService;

    @SuppressFBWarnings("EI")
    public RemoteTriggerStateServiceController(TriggerStateService triggerStateService) {
        this.triggerStateService = triggerStateService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/fetch-value/{workflowExecutionId}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Object> fetchValue(@PathVariable String workflowExecutionId) {
        return triggerStateService.fetchValue(WorkflowExecutionId.parse(workflowExecutionId))
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity
                .noContent()
                .build());
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/save/{workflowExecutionId}",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Void> save(
        @PathVariable String workflowExecutionId, @RequestBody DynamicWebhookEnableOutput value) {

        triggerStateService.save(WorkflowExecutionId.parse(workflowExecutionId), value);

        return ResponseEntity.noContent()
            .build();
    }
}
