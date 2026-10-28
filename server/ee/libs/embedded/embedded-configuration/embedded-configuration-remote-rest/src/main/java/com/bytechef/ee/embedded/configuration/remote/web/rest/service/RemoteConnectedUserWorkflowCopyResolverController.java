/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.web.rest.service;

import com.bytechef.automation.configuration.service.ConnectedUserWorkflowCopyResolver;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
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
@RequestMapping("/remote/connected-user-workflow-copy-resolver")
public class RemoteConnectedUserWorkflowCopyResolverController {

    private final ConnectedUserWorkflowCopyResolver connectedUserWorkflowCopyResolver;

    @SuppressFBWarnings("EI")
    public RemoteConnectedUserWorkflowCopyResolverController(
        ConnectedUserWorkflowCopyResolver connectedUserWorkflowCopyResolver) {

        this.connectedUserWorkflowCopyResolver = connectedUserWorkflowCopyResolver;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-workflow-copy-uuids/{workflowUuid}",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<String>> getWorkflowCopyUuids(@PathVariable String workflowUuid) {
        return ResponseEntity.ok(connectedUserWorkflowCopyResolver.getWorkflowCopyUuids(workflowUuid));
    }
}
