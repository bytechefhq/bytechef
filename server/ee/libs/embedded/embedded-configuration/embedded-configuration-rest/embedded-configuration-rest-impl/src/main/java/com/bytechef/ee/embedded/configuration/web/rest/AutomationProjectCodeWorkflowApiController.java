/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectCodeWorkflowFacade;
import com.bytechef.ee.embedded.configuration.web.rest.model.AutomationProjectCodeWorkflowDeployResultModel;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Deploying through this internal endpoint (as opposed to the plain automation deploy endpoint) is what creates the
 * embedded relation: the resulting catalog {@code Project} is resolved/created through
 * {@code AutomationWorkflowProjectFacade}'s marker convention so it stays hidden behind the embedded
 * automation-workflow-project entity. Never expose {@code Project} ids on this surface -- only embedded-entity
 * identifiers are meant to reach embedded callers.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.embedded.configuration.web.rest.AutomationProjectCodeWorkflowApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AutomationProjectCodeWorkflowApiController implements AutomationProjectCodeWorkflowApi {

    private final AutomationWorkflowProjectCodeWorkflowFacade automationWorkflowProjectCodeWorkflowFacade;

    @SuppressFBWarnings("EI")
    public AutomationProjectCodeWorkflowApiController(
        AutomationWorkflowProjectCodeWorkflowFacade automationWorkflowProjectCodeWorkflowFacade) {

        this.automationWorkflowProjectCodeWorkflowFacade = automationWorkflowProjectCodeWorkflowFacade;
    }

    /**
     * Authorization note: the {@code ROLE_ADMIN} guard lives on
     * {@code AutomationWorkflowProjectCodeWorkflowFacadeImpl#save}, exactly mirroring
     * {@code IntegrationCodeWorkflowApiController}, so a connected-user bearer token (which carries no authorities, see
     * {@code EmbeddedApiKeyAuthenticationProvider}) is denied at the facade regardless of what reaches this controller.
     */
    @Override
    public ResponseEntity<AutomationProjectCodeWorkflowDeployResultModel> deployAutomationProjectCodeWorkflow(
        MultipartFile projectFile) {

        List<String> warnings;

        try {
            warnings = automationWorkflowProjectCodeWorkflowFacade.save(
                projectFile.getBytes(),
                Language.of(Objects.requireNonNull(projectFile.getOriginalFilename())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(new AutomationProjectCodeWorkflowDeployResultModel().warnings(warnings));
    }
}
