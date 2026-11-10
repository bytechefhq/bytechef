/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationProjectFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.configuration.public_.web.rest.mapper.AutomationProjectMapper;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.AutomationProjectModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.AutomationProjectWorkflowTemplateModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.util.SecurityUtils;
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
@RestController("com.bytechef.ee.embedded.configuration.public_.web.rest.AutomationProjectApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AutomationProjectApiController implements AutomationProjectApi {

    private final AutomationProjectFacade automationProjectFacade;
    private final AutomationProjectMapper automationProjectMapper;
    private final ConversionService conversionService;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    public AutomationProjectApiController(
        AutomationProjectFacade automationProjectFacade,
        AutomationProjectMapper automationProjectMapper, ConversionService conversionService,
        EnvironmentService environmentService) {

        this.automationProjectFacade = automationProjectFacade;
        this.automationProjectMapper = automationProjectMapper;
        this.conversionService = conversionService;
        this.environmentService = environmentService;
    }

    @CrossOrigin
    @Override
    public ResponseEntity<List<AutomationProjectModel>> getFrontendProjects(EnvironmentModel xEnvironment) {
        String externalUserId = SecurityUtils.fetchCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not found"));

        List<AutomationProjectModel> models = automationProjectFacade
            .getPublishedProjects(externalUserId, getEnvironment(xEnvironment))
            .stream()
            .filter(AutomationProjectDTO::automationHubVisible)
            .map(project -> conversionService.convert(project, AutomationProjectModel.class))
            .toList();

        return ResponseEntity.ok(models);
    }

    @CrossOrigin
    @Override
    public ResponseEntity<AutomationProjectWorkflowTemplateModel> getFrontendWorkflowTemplate(
        String workflowUuid, EnvironmentModel xEnvironment) {

        String externalUserId = SecurityUtils.fetchCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not found"));

        return getWorkflowTemplate(externalUserId, workflowUuid, xEnvironment);
    }

    @Override
    public ResponseEntity<List<AutomationProjectModel>> getProjects(
        String externalUserId, EnvironmentModel xEnvironment) {

        List<AutomationProjectModel> models = automationProjectFacade
            .getPublishedProjects(externalUserId, getEnvironment(xEnvironment))
            .stream()
            .filter(AutomationProjectDTO::automationHubVisible)
            .map(project -> conversionService.convert(project, AutomationProjectModel.class))
            .toList();

        return ResponseEntity.ok(models);
    }

    @Override
    public ResponseEntity<AutomationProjectWorkflowTemplateModel> getWorkflowTemplate(
        String externalUserId, String workflowUuid, EnvironmentModel xEnvironment) {

        return automationProjectFacade.getPublishedProjects(externalUserId, getEnvironment(xEnvironment))
            .stream()
            .flatMap(project -> project.workflowTemplates()
                .stream())
            .filter(workflowTemplate -> Objects.equals(workflowTemplate.workflowUuid(), workflowUuid))
            .findFirst()
            .map(automationProjectMapper::toWorkflowTemplateModel)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound()
                .build());
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private Environment getEnvironment(EnvironmentModel xEnvironment) {
        return environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());
    }
}
