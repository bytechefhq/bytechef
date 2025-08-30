/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.StringUtils;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceConfigurationFacade;
import com.bytechef.ee.embedded.configuration.web.rest.model.CreateIntegrationInstanceConfigurationWorkflowJob200ResponseModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationWorkflowModel;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
@ConditionalOnCoordinator
public class IntegrationInstanceConfigurationApiController implements IntegrationInstanceConfigurationApi {

    private final ConversionService conversionService;
    private final IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceConfigurationApiController(
        ConversionService conversionService,
        IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade,
        EnvironmentService environmentService) {

        this.conversionService = conversionService;
        this.integrationInstanceConfigurationFacade = integrationInstanceConfigurationFacade;
        this.environmentService = environmentService;
    }

    @Override
    public ResponseEntity<Long> createIntegrationInstanceConfiguration(
        IntegrationInstanceConfigurationModel integrationInstanceConfigurationModel) {

        return ResponseEntity.ok(
            integrationInstanceConfigurationFacade.createIntegrationInstanceConfiguration(
                conversionService.convert(
                    integrationInstanceConfigurationModel, IntegrationInstanceConfigurationDTO.class)));
    }

    @Override
    public ResponseEntity<CreateIntegrationInstanceConfigurationWorkflowJob200ResponseModel>
        createIntegrationInstanceConfigurationWorkflowJob(Long id, String workflowId) {

        return ResponseEntity.ok(
            new CreateIntegrationInstanceConfigurationWorkflowJob200ResponseModel().jobId(
                integrationInstanceConfigurationFacade.createIntegrationInstanceConfigurationWorkflowJob(
                    id, workflowId)));
    }

    @Override
    public ResponseEntity<Void> deleteIntegrationInstanceConfiguration(Long id) {
        integrationInstanceConfigurationFacade.deleteIntegrationInstanceConfiguration(id);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<Void> enableIntegrationInstanceConfiguration(Long id, Boolean enable) {
        integrationInstanceConfigurationFacade.enableIntegrationInstanceConfiguration(id, enable);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<Void> enableIntegrationInstanceConfigurationWorkflow(
        Long id, String workflowId, Boolean enable) {

        integrationInstanceConfigurationFacade.enableIntegrationInstanceConfigurationWorkflow(id, workflowId, enable);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<IntegrationInstanceConfigurationModel> getIntegrationInstanceConfiguration(Long id) {
        return ResponseEntity.ok(
            toIntegrationInstanceConfigurationModel(
                integrationInstanceConfigurationFacade.getIntegrationInstanceConfiguration(id)));
    }

    @Override
    public ResponseEntity<List<IntegrationInstanceConfigurationModel>> getIntegrationInstanceConfigurations(
        EnvironmentModel environment, Long integrationId, Long tagId, Boolean includeAllFields) {

        return ResponseEntity.ok(
            integrationInstanceConfigurationFacade
                .getIntegrationInstanceConfigurations(
                    environment == null ? null : environmentService.getEnvironment(environment.getValue()), integrationId, tagId,
                    includeAllFields)
                .stream()
                .map(this::toIntegrationInstanceConfigurationModel)
                .toList());
    }

    @Override
    public ResponseEntity<Void> updateIntegrationInstanceConfiguration(
        Long id, IntegrationInstanceConfigurationModel integrationInstanceConfigurationModel) {

        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfiguration(
            conversionService.convert(
                integrationInstanceConfigurationModel.id(id), IntegrationInstanceConfigurationDTO.class));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> updateIntegrationInstanceConfigurationWorkflow(
        Long id, Long integrationInstanceConfigurationWorkflowId,
        IntegrationInstanceConfigurationWorkflowModel integrationInstanceConfigurationWorkflowModel) {

        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationWorkflow(
            conversionService.convert(
                integrationInstanceConfigurationWorkflowModel.id(integrationInstanceConfigurationWorkflowId)
                    .integrationInstanceConfigurationId(id),
                IntegrationInstanceConfigurationWorkflow.class));

        return ResponseEntity.noContent()
            .build();
    }

    @SuppressFBWarnings("NP")
    private IntegrationInstanceConfigurationModel toIntegrationInstanceConfigurationModel(
        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO) {

        IntegrationInstanceConfigurationModel integrationInstanceConfigurationModel = conversionService.convert(
            integrationInstanceConfigurationDTO, IntegrationInstanceConfigurationModel.class);

        integrationInstanceConfigurationModel.connectionAuthorizationParameters(
            MapUtils.toMap(
                integrationInstanceConfigurationModel.getConnectionAuthorizationParameters(),
                Map.Entry::getKey,
                entry -> StringUtils.obfuscate(String.valueOf(entry.getValue()), 28, 8)));

        return Validate.notNull(integrationInstanceConfigurationModel, "integrationInstanceConfigurationModel")
            .connectionParameters(null);
    }
}
