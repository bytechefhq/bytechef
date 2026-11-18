/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.facade.AutomationProjectCodeWorkflowFacade;
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
 * @version ee
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.embedded.configuration.web.rest.AutomationProjectCodeWorkflowApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AutomationProjectCodeWorkflowApiController implements AutomationProjectCodeWorkflowApi {

    private final AutomationProjectCodeWorkflowFacade automationProjectCodeWorkflowFacade;

    @SuppressFBWarnings("EI")
    public AutomationProjectCodeWorkflowApiController(
        AutomationProjectCodeWorkflowFacade automationProjectCodeWorkflowFacade) {

        this.automationProjectCodeWorkflowFacade = automationProjectCodeWorkflowFacade;
    }

    @Override
    public ResponseEntity<AutomationProjectCodeWorkflowDeployResultModel> deployAutomationProjectCodeWorkflow(
        MultipartFile projectFile) {

        List<String> warnings;

        try {
            warnings = automationProjectCodeWorkflowFacade.save(
                projectFile.getBytes(),
                Language.of(Objects.requireNonNull(projectFile.getOriginalFilename())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(new AutomationProjectCodeWorkflowDeployResultModel().warnings(warnings));
    }
}
