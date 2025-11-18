/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.ConnectedUserProjectWorkflowModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.CreateFrontendProjectWorkflowRequestModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.PublishFrontendProjectWorkflowRequestModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.UpdateFrontendWorkflowConfigurationConnectionRequestModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
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
@RestController(" com.bytechef.ee.embedded.configuration.public_.web.rest.WorkflowApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ConnectedUserProjectWorkflowApiController implements ConnectedUserProjectWorkflowApi {

    private final ConnectedUserProjectFacade connectedUserProjectFacade;
    private final ConversionService conversionService;
    private final EnvironmentService environmentService;
    private final WorkflowTestConfigurationFacade workflowTestConfigurationFacade;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectWorkflowApiController(
        ConnectedUserProjectFacade connectedUserProjectFacade, ConversionService conversionService,
        EnvironmentService environmentService, WorkflowTestConfigurationFacade workflowTestConfigurationFacade) {

        this.connectedUserProjectFacade = connectedUserProjectFacade;
        this.conversionService = conversionService;
        this.environmentService = environmentService;
        this.workflowTestConfigurationFacade = workflowTestConfigurationFacade;
    }

    @Override
    @CrossOrigin
    public ResponseEntity<String> createFrontendProjectWorkflow(
        CreateFrontendProjectWorkflowRequestModel createFrontendProjectWorkflowRequestModel,
        EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            connectedUserProjectFacade.createProjectWorkflow(
                OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"),
                createFrontendProjectWorkflowRequestModel.getDefinition(), getEnvironment(xEnvironment)));
    }

    @Override
    public ResponseEntity<String> createProjectWorkflow(
        String externalUserId, CreateFrontendProjectWorkflowRequestModel createFrontendProjectWorkflowRequestModel,
        EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            connectedUserProjectFacade.createProjectWorkflow(
                externalUserId,
                createFrontendProjectWorkflowRequestModel.getDefinition(), getEnvironment(xEnvironment)));
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> deleteFrontendProjectWorkflow(
        String workflowUuid, EnvironmentModel xEnvironment) {

        connectedUserProjectFacade.deleteProjectWorkflow(
            OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), workflowUuid,
            getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> deleteProjectWorkflow(
        String externalUserId, String workflowUuid, EnvironmentModel xEnvironment) {

        connectedUserProjectFacade.deleteProjectWorkflow(
            externalUserId, workflowUuid, getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> disableFrontendProjectWorkflow(
        String workflowUuid, EnvironmentModel xEnvironment) {

        connectedUserProjectFacade.enableProjectWorkflow(
            OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), workflowUuid, false,
            (long) getEnvironment(xEnvironment).ordinal());

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> disableProjectWorkflow(
        String externalUserId, String workflowUuid, EnvironmentModel xEnvironment) {

        connectedUserProjectFacade.enableProjectWorkflow(
            OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), workflowUuid, false,
            (long) getEnvironment(xEnvironment).ordinal());

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> enableFrontendProjectWorkflow(
        String workflowUuid, EnvironmentModel xEnvironment) {

        connectedUserProjectFacade.enableProjectWorkflow(
            OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), workflowUuid, true,
            (long) getEnvironment(xEnvironment).ordinal());

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> enableProjectWorkflow(
        String externalUserId, String workflowUuid, EnvironmentModel xEnvironment) {

        connectedUserProjectFacade.enableProjectWorkflow(
            OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), workflowUuid, true,
            (long) getEnvironment(xEnvironment).ordinal());

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    @CrossOrigin
    public ResponseEntity<ConnectedUserProjectWorkflowModel> getFrontendProjectWorkflow(
        String workflowUuid, EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            conversionService.convert(
                connectedUserProjectFacade.getConnectedUserProjectWorkflow(
                    OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), workflowUuid,
                    (long) getEnvironment(xEnvironment).ordinal()),
                ConnectedUserProjectWorkflowModel.class));
    }

    @Override
    @CrossOrigin
    public ResponseEntity<List<ConnectedUserProjectWorkflowModel>> getFrontendProjectWorkflows(
        EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            connectedUserProjectFacade
                .getConnectedUserProjectWorkflows(
                    OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"),
                    getEnvironment(xEnvironment))
                .stream()
                .map(workflow -> conversionService.convert(workflow, ConnectedUserProjectWorkflowModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<ConnectedUserProjectWorkflowModel> getProjectWorkflow(
        String externalUserId, String workflowUuid, EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            conversionService.convert(
                connectedUserProjectFacade.getConnectedUserProjectWorkflow(
                    externalUserId, workflowUuid, (long) getEnvironment(xEnvironment).ordinal()),
                ConnectedUserProjectWorkflowModel.class));
    }

    @Override
    public ResponseEntity<List<ConnectedUserProjectWorkflowModel>> getProjectWorkflows(
        String externalUserId, EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, getEnvironment(xEnvironment))
                .stream()
                .map(workflow -> conversionService.convert(workflow, ConnectedUserProjectWorkflowModel.class))
                .toList());
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> publishFrontendProjectWorkflow(
        String workflowUuid,
        PublishFrontendProjectWorkflowRequestModel publishFrontendProjectWorkflowRequestModel,
        EnvironmentModel xEnvironment) {

        connectedUserProjectFacade.publishProjectWorkflow(
            OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), workflowUuid,
            publishFrontendProjectWorkflowRequestModel.getDescription(), (long) getEnvironment(xEnvironment).ordinal());

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> publishProjectWorkflow(
        String externalUserId, String workflowUuid,
        PublishFrontendProjectWorkflowRequestModel publishFrontendProjectWorkflowRequestModel,
        EnvironmentModel xEnvironment) {

        connectedUserProjectFacade.publishProjectWorkflow(
            externalUserId, workflowUuid,
            publishFrontendProjectWorkflowRequestModel.getDescription(), (long) getEnvironment(xEnvironment).ordinal());

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> updateFrontendProjectWorkflow(
        String workflowUuid,
        CreateFrontendProjectWorkflowRequestModel createFrontendProjectWorkflowRequestModel,
        EnvironmentModel xEnvironment) {

        connectedUserProjectFacade.updateProjectWorkflow(
            OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), workflowUuid,
            createFrontendProjectWorkflowRequestModel.getDefinition(), getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> updateFrontendWorkflowConfigurationConnection(
        String workflowUuid, String workflowNodeName, String componentName,
        UpdateFrontendWorkflowConfigurationConnectionRequestModel updateFrontendWorkflowConfigurationConnectionRequestModel,
        EnvironmentModel xEnvironment) {

        String externalUserId = SecurityUtils.fetchCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        Environment environment = xEnvironment == null
            ? Environment.PRODUCTION : environmentService.getEnvironment(xEnvironment.name());

        connectedUserProjectFacade.updateWorkflowConfigurationConnection(
            externalUserId, workflowUuid, workflowNodeName, componentName,
            Objects.requireNonNull(updateFrontendWorkflowConfigurationConnectionRequestModel.getConnectionId()),
            environment);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> updateProjectWorkflow(
        String externalUserId, String workflowUuid,
        CreateFrontendProjectWorkflowRequestModel createFrontendProjectWorkflowRequestModel,
        EnvironmentModel xEnvironment) {

        connectedUserProjectFacade.updateProjectWorkflow(
            externalUserId, workflowUuid,
            createFrontendProjectWorkflowRequestModel.getDefinition(), getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> updateWorkflowConfigurationConnection(
        String externalUserId, String workflowUuid, String workflowNodeName, String componentName,
        UpdateFrontendWorkflowConfigurationConnectionRequestModel updateFrontendWorkflowConfigurationConnectionRequestModel,
        EnvironmentModel xEnvironment) {

        Environment environment = xEnvironment == null
            ? Environment.PRODUCTION : environmentService.getEnvironment(xEnvironment.name());

        connectedUserProjectFacade.updateWorkflowConfigurationConnection(
            externalUserId, workflowUuid, workflowNodeName, componentName,
            Objects.requireNonNull(updateFrontendWorkflowConfigurationConnectionRequestModel.getConnectionId()),
            environment);

        return ResponseEntity.noContent()
            .build();
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private Environment getEnvironment(EnvironmentModel xEnvironment) {
        return environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());
    }
}
