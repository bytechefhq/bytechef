/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.web.rest.facade;

import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves {@link ConnectedUserProjectFacade} to remote callers. Only {@code copyWorkflowTemplate} is exposed -- the
 * embedded-webhook bridge's implicit visual-template provisioning path is its sole remote caller today.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/remote/connected-user-project-facade")
public class RemoteConnectedUserProjectFacadeController {

    private final ConnectedUserProjectFacade connectedUserProjectFacade;

    @SuppressFBWarnings("EI")
    public RemoteConnectedUserProjectFacadeController(ConnectedUserProjectFacade connectedUserProjectFacade) {
        this.connectedUserProjectFacade = connectedUserProjectFacade;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/copy-workflow-template",
        produces = {
            "application/json"
        })
    public ResponseEntity<String> copyWorkflowTemplate(
        @RequestParam String externalUserId, @RequestParam String workflowUuid,
        @RequestParam Environment environment) {

        return ResponseEntity.ok(
            connectedUserProjectFacade.copyWorkflowTemplate(externalUserId, workflowUuid, environment));
    }
}
