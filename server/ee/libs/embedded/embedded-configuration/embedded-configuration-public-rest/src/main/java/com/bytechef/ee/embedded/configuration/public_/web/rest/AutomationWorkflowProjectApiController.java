/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.AutomationWorkflowProjectModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.embedded.configuration.public_.web.rest.AutomationWorkflowProjectApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AutomationWorkflowProjectApiController implements AutomationWorkflowProjectApi {

    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade;
    private final ConversionService conversionService;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectApiController(
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade, ConversionService conversionService,
        EnvironmentService environmentService) {

        this.automationWorkflowProjectFacade = automationWorkflowProjectFacade;
        this.conversionService = conversionService;
        this.environmentService = environmentService;
    }

    @CrossOrigin
    @Override
    public ResponseEntity<String> copyFrontendWorkflowTemplate(String workflowUuid, EnvironmentModel xEnvironment) {
        String externalUserId = SecurityUtils.fetchCurrentUserLogin()
            .orElseThrow(
                () -> new IllegalArgumentException("User not found"));

        try {
            return ResponseEntity.ok(
                automationWorkflowProjectFacade.copyWorkflowTemplate(externalUserId, workflowUuid,
                    getEnvironment(xEnvironment)));
        } catch (IllegalArgumentException illegalArgumentException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .build();
        }
    }

    @Override
    public ResponseEntity<String> copyWorkflowTemplate(
        String externalUserId, String workflowUuid, EnvironmentModel xEnvironment) {

        try {
            return ResponseEntity.ok(
                automationWorkflowProjectFacade.copyWorkflowTemplate(
                    externalUserId, workflowUuid, getEnvironment(xEnvironment)));
        } catch (IllegalArgumentException illegalArgumentException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .build();
        }
    }

    @CrossOrigin
    @Override
    public ResponseEntity<List<AutomationWorkflowProjectModel>> getFrontendProjects(EnvironmentModel xEnvironment) {
        return ResponseEntity.ok(toAutomationWorkflowProjectModels());
    }

    @Override
    public ResponseEntity<List<AutomationWorkflowProjectModel>> getProjects(
        String externalUserId, EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(toAutomationWorkflowProjectModels());
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private Environment getEnvironment(EnvironmentModel xEnvironment) {
        return environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());
    }

    private List<AutomationWorkflowProjectModel> toAutomationWorkflowProjectModels() {
        return automationWorkflowProjectFacade.getPublishedProjects()
            .stream()
            .map(project -> conversionService.convert(project, AutomationWorkflowProjectModel.class))
            .toList();
    }
}
