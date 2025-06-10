/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserIntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.UpdateFrontendIntegrationInstanceWorkflowRequestModel;
import com.bytechef.platform.security.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.embedded.configuration.public_.web.rest.IntegrationInstanceWorkflowApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
public class IntegrationInstanceWorkflowApiController implements IntegrationInstanceWorkflowApi {

    private final ConnectedUserIntegrationInstanceFacade connectedUserIntegrationInstanceFacade;

    public IntegrationInstanceWorkflowApiController(
        ConnectedUserIntegrationInstanceFacade connectedUserIntegrationInstanceFacade) {

        this.connectedUserIntegrationInstanceFacade = connectedUserIntegrationInstanceFacade;
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> disableFrontendIntegrationInstanceWorkflow(Long id, String workflowReferenceCode) {
        String externalUserId = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));

        connectedUserIntegrationInstanceFacade.disableIntegrationInstanceWorkflow(
            externalUserId, id, workflowReferenceCode);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> enableFrontendIntegrationInstanceWorkflow(Long id, String workflowReferenceCode) {
        String externalUserId = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));

        connectedUserIntegrationInstanceFacade.enableIntegrationInstanceWorkflow(
            externalUserId, id, workflowReferenceCode);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> updateFrontendIntegrationInstanceWorkflow(
        Long id, String workflowReferenceCode,
        UpdateFrontendIntegrationInstanceWorkflowRequestModel updateFrontendIntegrationInstanceWorkflowRequestModel) {

        String externalUserId = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));

        connectedUserIntegrationInstanceFacade.updateIntegrationInstanceWorkflow(
            externalUserId, id, workflowReferenceCode,
            updateFrontendIntegrationInstanceWorkflowRequestModel.getInputs());

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> disableIntegrationInstanceWorkflow(
        String externalUserId, Long id, String workflowReferenceCode) {

        connectedUserIntegrationInstanceFacade.disableIntegrationInstanceWorkflow(
            externalUserId, id, workflowReferenceCode);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> enableIntegrationInstanceWorkflow(
        String externalUserId, Long id, String workflowReferenceCode) {

        connectedUserIntegrationInstanceFacade.enableIntegrationInstanceWorkflow(
            externalUserId, id, workflowReferenceCode);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> updateIntegrationInstanceWorkflow(
        String externalUserId, Long id, String workflowReferenceCode,
        UpdateFrontendIntegrationInstanceWorkflowRequestModel updateFrontendIntegrationInstanceWorkflowRequestModel) {

        connectedUserIntegrationInstanceFacade.updateIntegrationInstanceWorkflow(
            externalUserId, id, workflowReferenceCode,
            updateFrontendIntegrationInstanceWorkflowRequestModel.getInputs());

        return ResponseEntity.noContent()
            .build();
    }
}
