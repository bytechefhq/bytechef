/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserIntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.UpdateFrontendIntegrationInstanceWorkflowRequestModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.util.SecurityUtils;
import org.jspecify.annotations.NonNull;
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
@RestController("com.bytechef.ee.embedded.configuration.public_.web.rest.IntegrationInstanceWorkflowApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class IntegrationInstanceWorkflowApiController implements IntegrationInstanceWorkflowApi {

    private final ConnectedUserIntegrationInstanceFacade connectedUserIntegrationInstanceFacade;

    public IntegrationInstanceWorkflowApiController(
        ConnectedUserIntegrationInstanceFacade connectedUserIntegrationInstanceFacade) {

        this.connectedUserIntegrationInstanceFacade = connectedUserIntegrationInstanceFacade;
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> disableFrontendIntegrationInstanceWorkflow(Long id, String workflowUuid) {
        String externalUserId = SecurityUtils.fetchCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));

        connectedUserIntegrationInstanceFacade.disableIntegrationInstanceWorkflow(
            externalUserId, id, workflowUuid);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> disableIntegrationInstanceWorkflow(
        String externalUserId, Long id, String workflowUuid) {

        connectedUserIntegrationInstanceFacade.disableIntegrationInstanceWorkflow(
            externalUserId, id, workflowUuid);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> enableFrontendIntegrationInstanceWorkflow(Long id, String workflowUuid) {
        String externalUserId = SecurityUtils.fetchCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));

        connectedUserIntegrationInstanceFacade.enableIntegrationInstanceWorkflow(
            externalUserId, id, workflowUuid);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> enableIntegrationInstanceWorkflow(
        String externalUserId, Long id, String workflowUuid) {

        connectedUserIntegrationInstanceFacade.enableIntegrationInstanceWorkflow(
            externalUserId, id, workflowUuid);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> updateFrontendIntegrationInstanceWorkflow(
        Long id, String workflowUuid,
        UpdateFrontendIntegrationInstanceWorkflowRequestModel updateFrontendIntegrationInstanceWorkflowRequestModel) {

        String externalUserId = SecurityUtils.fetchCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));

        connectedUserIntegrationInstanceFacade.updateIntegrationInstanceWorkflow(
            externalUserId, id, workflowUuid,
            updateFrontendIntegrationInstanceWorkflowRequestModel.getInputs());

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> updateIntegrationInstanceWorkflow(
        String externalUserId, Long id, String workflowUuid,
        @NonNull UpdateFrontendIntegrationInstanceWorkflowRequestModel updateFrontendIntegrationInstanceWorkflowRequestModel) {

        connectedUserIntegrationInstanceFacade.updateIntegrationInstanceWorkflow(
            externalUserId, id, workflowUuid,
            updateFrontendIntegrationInstanceWorkflowRequestModel.getInputs());

        return ResponseEntity.noContent()
            .build();
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }
}
