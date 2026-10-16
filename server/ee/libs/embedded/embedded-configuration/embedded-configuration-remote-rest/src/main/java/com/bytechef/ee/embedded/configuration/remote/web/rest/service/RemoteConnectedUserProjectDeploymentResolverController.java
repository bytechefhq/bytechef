/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.web.rest.service;

import com.bytechef.automation.configuration.service.ConnectedUserProjectDeploymentResolver;
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
@RequestMapping("/remote/connected-user-project-deployment-resolver")
public class RemoteConnectedUserProjectDeploymentResolverController {

    private final ConnectedUserProjectDeploymentResolver connectedUserProjectDeploymentResolver;

    @SuppressFBWarnings("EI")
    public RemoteConnectedUserProjectDeploymentResolverController(
        ConnectedUserProjectDeploymentResolver connectedUserProjectDeploymentResolver) {

        this.connectedUserProjectDeploymentResolver = connectedUserProjectDeploymentResolver;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-project-deployment-ids/{connectedUserId}",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<Long>> getProjectDeploymentIds(@PathVariable long connectedUserId) {
        return ResponseEntity.ok(connectedUserProjectDeploymentResolver.getProjectDeploymentIds(connectedUserId));
    }
}
