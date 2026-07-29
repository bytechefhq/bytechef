/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.web.rest.facade;

import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves {@link AutomationWorkflowProjectFacade} to remote callers. Only {@code getPublishedProjects()} is exposed --
 * the embedded-webhook bridge controllers are its only remote caller today, and they only invoke the no-arg overload.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/remote/automation-workflow-project-facade")
public class RemoteAutomationWorkflowProjectFacadeController {

    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @SuppressFBWarnings("EI")
    public RemoteAutomationWorkflowProjectFacadeController(
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade) {

        this.automationWorkflowProjectFacade = automationWorkflowProjectFacade;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-published-projects",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<AutomationWorkflowProjectDTO>> getPublishedProjects() {
        return ResponseEntity.ok(automationWorkflowProjectFacade.getPublishedProjects());
    }
}
