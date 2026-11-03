/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.configuration.public_.web.rest.mapper.AutomationWorkflowProjectMapper;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.AutomationWorkflowProjectModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.AutomationWorkflowProjectWorkflowTemplateModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.connected.user.security.ConnectedUserAuthentications;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
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
    private final AutomationWorkflowProjectMapper automationWorkflowProjectMapper;
    private final ConversionService conversionService;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectApiController(
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade,
        AutomationWorkflowProjectMapper automationWorkflowProjectMapper, ConversionService conversionService,
        EnvironmentService environmentService) {

        this.automationWorkflowProjectFacade = automationWorkflowProjectFacade;
        this.automationWorkflowProjectMapper = automationWorkflowProjectMapper;
        this.conversionService = conversionService;
        this.environmentService = environmentService;
    }

    @CrossOrigin
    @Override
    public ResponseEntity<List<AutomationWorkflowProjectModel>> getFrontendProjects(EnvironmentModel xEnvironment) {
        return ResponseEntity.ok(
            toAutomationWorkflowProjectModels(ConnectedUserAuthentications.getCurrentExternalUserId(),
                getEnvironment(xEnvironment)));
    }

    @CrossOrigin
    @Override
    public ResponseEntity<AutomationWorkflowProjectWorkflowTemplateModel> getFrontendWorkflowTemplate(
        String workflowUuid, EnvironmentModel xEnvironment) {

        return toWorkflowTemplate(
            ConnectedUserAuthentications.getCurrentExternalUserId(), workflowUuid, getEnvironment(xEnvironment));
    }

    @Override
    public ResponseEntity<List<AutomationWorkflowProjectModel>> getProjects(
        String externalUserId, EnvironmentModel xEnvironment) {

        ConnectedUserAuthentications.requireCurrentExternalUserId(externalUserId);

        return ResponseEntity.ok(toAutomationWorkflowProjectModels(externalUserId, getEnvironment(xEnvironment)));
    }

    @Override
    public ResponseEntity<AutomationWorkflowProjectWorkflowTemplateModel> getWorkflowTemplate(
        String externalUserId, String workflowUuid, EnvironmentModel xEnvironment) {

        ConnectedUserAuthentications.requireCurrentExternalUserId(externalUserId);

        return toWorkflowTemplate(externalUserId, workflowUuid, getEnvironment(xEnvironment));
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private Environment getEnvironment(EnvironmentModel xEnvironment) {
        return environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());
    }

    private List<AutomationWorkflowProjectModel> toAutomationWorkflowProjectModels(
        String externalUserId, Environment environment) {

        return automationWorkflowProjectFacade.getPublishedProjects(externalUserId, environment)
            .stream()
            .filter(AutomationWorkflowProjectDTO::automationHubVisible)
            .map(project -> conversionService.convert(project, AutomationWorkflowProjectModel.class))
            .toList();
    }

    private ResponseEntity<AutomationWorkflowProjectWorkflowTemplateModel> toWorkflowTemplate(
        String externalUserId, String workflowUuid, Environment environment) {

        return automationWorkflowProjectFacade.getPublishedProjects(externalUserId, environment)
            .stream()
            .flatMap(project -> project.workflowTemplates()
                .stream())
            .filter(workflowTemplate -> Objects.equals(workflowTemplate.workflowUuid(), workflowUuid))
            .findFirst()
            .map(automationWorkflowProjectMapper::toWorkflowTemplateModel)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound()
                .build());
    }
}
