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
import com.bytechef.platform.configuration.service.EnvironmentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.convert.ConversionService;
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

    private List<AutomationWorkflowProjectModel> toAutomationWorkflowProjectModels() {
        return automationWorkflowProjectFacade.getPublishedProjects()
            .stream()
            .map(project -> conversionService.convert(project, AutomationWorkflowProjectModel.class))
            .toList();
    }
}
