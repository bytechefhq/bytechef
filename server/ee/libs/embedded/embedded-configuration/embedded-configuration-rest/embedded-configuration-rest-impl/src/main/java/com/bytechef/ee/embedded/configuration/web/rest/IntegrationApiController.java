/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion.Status;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.facade.IntegrationFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationStatusModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationVersionModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.PublishIntegrationRequestModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.WorkflowModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
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
@RestController("com.bytechef.ee.embedded.configuration.web.rest.IntegrationApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
@ConditionalOnCoordinator
public class IntegrationApiController implements IntegrationApi {

    private final ConversionService conversionService;
    private final IntegrationService integrationService;
    private final IntegrationFacade integrationFacade;

    @SuppressFBWarnings("EI2")
    public IntegrationApiController(
        ConversionService conversionService, IntegrationService integrationService,
        IntegrationFacade integrationFacade) {

        this.conversionService = conversionService;
        this.integrationService = integrationService;
        this.integrationFacade = integrationFacade;
    }

    @Override
    public ResponseEntity<Long> createIntegration(IntegrationModel integrationModel) {
        return ResponseEntity.ok(
            integrationFacade.createIntegration(
                Validate.notNull(
                    conversionService.convert(integrationModel, IntegrationDTO.class), "integrationDTO")));
    }

    @Override
    public ResponseEntity<Long> createIntegrationWorkflow(Long id, WorkflowModel workflowModel) {
        return ResponseEntity.ok(integrationFacade.addWorkflow(id, workflowModel.getDefinition()));
    }

    @Override
    public ResponseEntity<Void> deleteIntegration(Long id) {
        integrationFacade.deleteIntegration(id);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<IntegrationModel> getIntegration(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(integrationFacade.getIntegration(id), IntegrationModel.class));
    }

    @Override
    public ResponseEntity<List<IntegrationVersionModel>> getIntegrationVersions(Long id) {
        return ResponseEntity.ok(
            integrationService.getIntegrationVersions(id)
                .stream()
                .map(projectVersion -> conversionService.convert(projectVersion, IntegrationVersionModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<List<IntegrationModel>> getIntegrations(
        Long categoryId, Boolean integrationInstanceConfigurations, IntegrationStatusModel status, Long tagId,
        Boolean includeAllFields) {

        return ResponseEntity.ok(
            integrationFacade
                .getIntegrations(
                    categoryId, integrationInstanceConfigurations != null, tagId,
                    status == null ? null : Status.valueOf(status.name()), includeAllFields)
                .stream()
                .map(integration -> conversionService.convert(integration, IntegrationModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<Void> publishIntegration(
        Long id, PublishIntegrationRequestModel publishIntegrationRequestModel) {

        integrationFacade.publishIntegration(
            id, publishIntegrationRequestModel == null ? null : publishIntegrationRequestModel.getDescription());

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> updateIntegration(Long id, IntegrationModel integrationModel) {
        integrationFacade.updateIntegration(
            Validate.notNull(
                conversionService.convert(integrationModel.id(id), IntegrationDTO.class), "integrationDTO"));

        return ResponseEntity.noContent()
            .build();
    }
}
