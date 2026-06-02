/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserIntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserIntegrationFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationBasicModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationWorkflowModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
@RestController("com.bytechef.ee.embedded.configuration.public_.web.rest.IntegrationApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class IntegrationApiController implements IntegrationApi {

    private final ConversionService conversionService;
    private final ConnectedUserIntegrationFacade connectedUserIntegrationFacade;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    public IntegrationApiController(
        ConversionService conversionService, ConnectedUserIntegrationFacade connectedUserIntegrationFacade,
        EnvironmentService environmentService) {

        this.conversionService = conversionService;
        this.connectedUserIntegrationFacade = connectedUserIntegrationFacade;
        this.environmentService = environmentService;
    }

    @CrossOrigin
    @Override
    public ResponseEntity<IntegrationModel> getFrontendIntegration(Long id, EnvironmentModel xEnvironment) {
        String externalId = SecurityUtils.fetchCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));

        ConnectedUserIntegrationDTO connectedUserIntegrationDTO;

        try {
            connectedUserIntegrationDTO = connectedUserIntegrationFacade.getConnectedUserIntegration(
                externalId, id, true, getEnvironment(xEnvironment));
        } catch (EmbeddedIntegrationNotVisibleException exception) {
            return ResponseEntity.notFound()
                .build();
        }

        IntegrationModel integrationModel = conversionService.convert(
            connectedUserIntegrationDTO, IntegrationModel.class);

        filterDisabledWorkflows(connectedUserIntegrationDTO, integrationModel);

        return ResponseEntity.ok(integrationModel);
    }

    @CrossOrigin
    @Override
    public ResponseEntity<List<IntegrationBasicModel>> getFrontendIntegrations(EnvironmentModel xEnvironment) {
        String externalId = SecurityUtils.fetchCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));

        return ResponseEntity.ok(
            connectedUserIntegrationFacade
                .getConnectedUserIntegrations(externalId, true, getEnvironment(xEnvironment))
                .stream()
                .map(integrationDTO -> conversionService.convert(integrationDTO, IntegrationBasicModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<IntegrationModel> getIntegration(
        String externalUserId, Long id, EnvironmentModel xEnvironment) {

        ConnectedUserIntegrationDTO connectedUserIntegrationDTO;

        try {
            connectedUserIntegrationDTO = connectedUserIntegrationFacade.getConnectedUserIntegration(
                externalUserId, id, true, getEnvironment(xEnvironment));
        } catch (EmbeddedIntegrationNotVisibleException exception) {
            return ResponseEntity.notFound()
                .build();
        }

        IntegrationModel integrationModel = conversionService.convert(
            connectedUserIntegrationDTO, IntegrationModel.class);

        filterDisabledWorkflows(connectedUserIntegrationDTO, integrationModel);

        return ResponseEntity.ok(integrationModel);
    }

    @Override
    public ResponseEntity<List<IntegrationBasicModel>> getIntegrations(
        String externalUserId, EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            connectedUserIntegrationFacade
                .getConnectedUserIntegrations(externalUserId, true, getEnvironment(xEnvironment))
                .stream()
                .map(integrationInstanceConfigurationDTO -> conversionService.convert(
                    integrationInstanceConfigurationDTO, IntegrationBasicModel.class))
                .toList());
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private void filterDisabledWorkflows(
        ConnectedUserIntegrationDTO connectedUserIntegrationDTO, IntegrationModel integrationModel) {

        if (integrationModel == null || integrationModel.getWorkflows() == null) {
            return;
        }

        if (connectedUserIntegrationDTO.integrationInstanceConfiguration() == null ||
            connectedUserIntegrationDTO.integrationInstanceConfiguration()
                .integrationInstanceConfigurationWorkflows() == null) {

            return;
        }

        Set<String> mcpWorkflowUuids = integrationModel.getMcpWorkflows() == null
            ? Set.of()
            : integrationModel.getMcpWorkflows()
                .stream()
                .map(IntegrationWorkflowModel::getWorkflowUuid)
                .collect(Collectors.toSet());

        Set<String> enabledWorkflowUuids = connectedUserIntegrationDTO.integrationInstanceConfiguration()
            .integrationInstanceConfigurationWorkflows()
            .stream()
            .filter(IntegrationInstanceConfigurationWorkflowDTO::enabled)
            .map(IntegrationInstanceConfigurationWorkflowDTO::workflowUuid)
            .filter(workflowUuid -> !mcpWorkflowUuids.contains(workflowUuid))
            .collect(Collectors.toSet());

        integrationModel.setWorkflows(
            integrationModel.getWorkflows()
                .stream()
                .filter(workflowModel -> enabledWorkflowUuids.contains(workflowModel.getWorkflowUuid()))
                .toList());
    }

    private Environment getEnvironment(EnvironmentModel xEnvironment) {
        return environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());
    }
}
