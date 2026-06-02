/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserIntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.OptionModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.UpdateFrontendIntegrationInstanceWorkflowRequestModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.WorkflowInputOptionsRequestModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.security.util.SecurityUtils;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
    public ResponseEntity<List<OptionModel>> getFrontendIntegrationInstanceWorkflowInputOptions(
        Long id, String workflowUuid, WorkflowInputOptionsRequestModel workflowInputOptionsRequestModel) {

        String externalUserId = SecurityUtils.fetchCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));

        return ResponseEntity.ok(
            getWorkflowInputOptions(externalUserId, id, workflowUuid, workflowInputOptionsRequestModel));
    }

    @Override
    public ResponseEntity<List<OptionModel>> getIntegrationInstanceWorkflowInputOptions(
        String externalUserId, Long id, String workflowUuid,
        WorkflowInputOptionsRequestModel workflowInputOptionsRequestModel) {

        return ResponseEntity.ok(
            getWorkflowInputOptions(externalUserId, id, workflowUuid, workflowInputOptionsRequestModel));
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

    @ExceptionHandler(EmbeddedIntegrationNotVisibleException.class)
    public ResponseEntity<Void> handleEmbeddedIntegrationNotVisibleException() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .build();
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private List<OptionModel> getWorkflowInputOptions(
        String externalUserId, Long id, String workflowUuid,
        WorkflowInputOptionsRequestModel workflowInputOptionsRequestModel) {

        Map<String, Object> lookupDependsOnValues = workflowInputOptionsRequestModel.getLookupDependsOnValues();

        List<Option> options = connectedUserIntegrationInstanceFacade.getIntegrationInstanceWorkflowInputOptions(
            externalUserId, id, workflowUuid, workflowInputOptionsRequestModel.getInputName(),
            workflowInputOptionsRequestModel.getPropertyName(),
            lookupDependsOnValues == null ? Map.of() : lookupDependsOnValues,
            workflowInputOptionsRequestModel.getSearchText());

        return options.stream()
            .map(option -> new OptionModel()
                .label(option.getLabel())
                .value(String.valueOf(option.getValue())))
            .toList();
    }
}
