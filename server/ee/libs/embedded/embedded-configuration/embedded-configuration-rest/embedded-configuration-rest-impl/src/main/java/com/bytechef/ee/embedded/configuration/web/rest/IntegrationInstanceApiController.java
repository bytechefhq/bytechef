/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceModel;
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
public class IntegrationInstanceApiController implements IntegrationInstanceApi {

    private final ConversionService conversionService;
    private final IntegrationInstanceFacade integrationInstanceFacade;

    public IntegrationInstanceApiController(
        ConversionService conversionService, IntegrationInstanceFacade integrationInstanceFacade) {

        this.conversionService = conversionService;
        this.integrationInstanceFacade = integrationInstanceFacade;
    }

    @Override
    public ResponseEntity<Void> enableIntegrationInstance(Long id, Boolean enable) {
        integrationInstanceFacade.enableIntegrationInstance(id, enable);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> enableIntegrationInstanceWorkflow(Long id, String workflowId, Boolean enable) {
        integrationInstanceFacade.enableIntegrationInstanceWorkflow(id, workflowId, enable);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<IntegrationInstanceModel> getIntegrationInstance(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(
                integrationInstanceFacade.getIntegrationInstance(id), IntegrationInstanceModel.class));
    }
}
