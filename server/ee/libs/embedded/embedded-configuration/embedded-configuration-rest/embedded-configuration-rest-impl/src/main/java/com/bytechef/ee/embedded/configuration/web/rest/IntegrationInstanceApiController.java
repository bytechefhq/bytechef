/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.ObfuscateUtils;
import com.bytechef.ee.embedded.configuration.dto.ProvisionIntegrationInstanceWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionableIntegrationDTO;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceAdminFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.ProvisionIntegrationInstanceRequestModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.ProvisionIntegrationInstanceWorkflowModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.ProvisionableIntegrationInstanceConfigurationModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.ProvisionableIntegrationModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
@ConditionalOnEEVersion
public class IntegrationInstanceApiController implements IntegrationInstanceApi {

    private final ConversionService conversionService;
    private final IntegrationInstanceAdminFacade integrationInstanceAdminFacade;
    private final IntegrationInstanceFacade integrationInstanceFacade;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceApiController(
        ConversionService conversionService, IntegrationInstanceAdminFacade integrationInstanceAdminFacade,
        IntegrationInstanceFacade integrationInstanceFacade) {

        this.conversionService = conversionService;
        this.integrationInstanceAdminFacade = integrationInstanceAdminFacade;
        this.integrationInstanceFacade = integrationInstanceFacade;
    }

    @Override
    public ResponseEntity<Void> deleteIntegrationInstance(Long id) {
        integrationInstanceFacade.deleteIntegrationInstance(id);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> enableIntegrationInstance(Long id, Boolean enable) {
        integrationInstanceFacade.enableIntegrationInstance(id, enable);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> enableIntegrationInstanceWorkflow(Long id, String workflowId, Boolean enable) {
        integrationInstanceAdminFacade.enableIntegrationInstanceWorkflow(id, workflowId, enable);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<IntegrationInstanceModel> getIntegrationInstance(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(integrationInstanceFacade.getIntegrationInstance(id),
                IntegrationInstanceModel.class));
    }

    @Override
    public ResponseEntity<List<ProvisionableIntegrationModel>> getProvisionableIntegrations(Long connectedUserId) {
        return ResponseEntity.ok(
            integrationInstanceAdminFacade.getProvisionableIntegrations(connectedUserId)
                .stream()
                .map(this::toProvisionableIntegrationModel)
                .toList());
    }

    @Override
    public ResponseEntity<Long> provisionIntegrationInstance(
        Long connectedUserId, ProvisionIntegrationInstanceRequestModel provisionIntegrationInstanceRequestModel) {

        Map<String, Object> connectionParameters = provisionIntegrationInstanceRequestModel.getConnectionParameters();

        if (connectionParameters == null) {
            connectionParameters = Map.of();
        }

        List<ProvisionIntegrationInstanceWorkflowModel> workflowModels =
            provisionIntegrationInstanceRequestModel.getWorkflows();

        List<ProvisionIntegrationInstanceWorkflowDTO> workflows = workflowModels == null
            ? List.of()
            : workflowModels.stream()
                .map(workflowModel -> new ProvisionIntegrationInstanceWorkflowDTO(
                    workflowModel.getWorkflowUuid(), workflowModel.getInputs()))
                .toList();

        return ResponseEntity.ok(
            integrationInstanceAdminFacade.provisionIntegrationInstance(
                connectedUserId, provisionIntegrationInstanceRequestModel.getIntegrationInstanceConfigurationId(),
                connectionParameters, workflows));
    }

    private ProvisionableIntegrationModel toProvisionableIntegrationModel(
        ProvisionableIntegrationDTO provisionableIntegrationDTO) {

        ProvisionableIntegrationModel provisionableIntegrationModel = Validate.notNull(
            conversionService.convert(provisionableIntegrationDTO, ProvisionableIntegrationModel.class),
            "provisionableIntegrationModel");

        for (ProvisionableIntegrationInstanceConfigurationModel configurationModel : provisionableIntegrationModel
            .getIntegrationInstanceConfigurations()) {

            IntegrationInstanceConfigurationModel integrationInstanceConfigurationModel = configurationModel
                .getIntegrationInstanceConfiguration();

            if (integrationInstanceConfigurationModel != null) {
                integrationInstanceConfigurationModel.connectionAuthorizationParameters(
                    ObfuscateUtils.toObfuscatedMap(
                        integrationInstanceConfigurationModel.getConnectionAuthorizationParameters(), 28, 8));
                integrationInstanceConfigurationModel.connectionParameters(null);
            }
        }

        return provisionableIntegrationModel;
    }
}
