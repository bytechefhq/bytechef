/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import static com.bytechef.ee.embedded.configuration.public_.web.rest.util.EnvironmentUtils.getEnvironment;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.ConnectedUserProjectWorkflowModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.CreateFrontendProjectWorkflowRequestModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.PublishFrontendProjectWorkflowRequestModel;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
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
public class ConnectedUserProjectWorkflowApiController implements ConnectedUserProjectWorkflowApi {

    private final ConnectedUserProjectFacade connectedUserProjectFacade;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectWorkflowApiController(
        ConnectedUserProjectFacade connectedUserProjectFacade, ConversionService conversionService) {

        this.connectedUserProjectFacade = connectedUserProjectFacade;
        this.conversionService = conversionService;
    }

    @Override
    @CrossOrigin
    public ResponseEntity<String> createFrontendProjectWorkflow(
        CreateFrontendProjectWorkflowRequestModel createFrontendProjectWorkflowRequestModel,
        EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            connectedUserProjectFacade.createProjectWorkflow(
                OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"),
                createFrontendProjectWorkflowRequestModel.getDefinition(), getEnvironment(xEnvironment)));
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> deleteFrontendProjectWorkflow(
        String workflowReferenceCode, EnvironmentModel xEnvironment) {

        connectedUserProjectFacade.deleteProjectWorkflow(
            OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"), workflowReferenceCode,
            getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> disableFrontendProjectWorkflow(
        String workflowReferenceCode, EnvironmentModel xEnvironment) {

        connectedUserProjectFacade.enableProjectWorkflow(
            OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"), workflowReferenceCode, false,
            getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> enableFrontendProjectWorkflow(
        String workflowReferenceCode, EnvironmentModel xEnvironment) {

        connectedUserProjectFacade.enableProjectWorkflow(
            OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"), workflowReferenceCode, true,
            getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    @CrossOrigin
    public ResponseEntity<ConnectedUserProjectWorkflowModel> getFrontendProjectWorkflow(
        String workflowReferenceCode, EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            conversionService.convert(
                connectedUserProjectFacade.getConnectedUserProjectWorkflow(
                    OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"), workflowReferenceCode,
                    getEnvironment(xEnvironment)),
                ConnectedUserProjectWorkflowModel.class));
    }

    @Override
    @CrossOrigin
    public ResponseEntity<List<ConnectedUserProjectWorkflowModel>> getFrontendProjectWorkflows(
        EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            connectedUserProjectFacade
                .getConnectedUserProjectWorkflows(
                    OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"),
                    getEnvironment(xEnvironment))
                .stream()
                .map(workflow -> conversionService.convert(workflow, ConnectedUserProjectWorkflowModel.class))
                .toList());
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> updateFrontendProjectWorkflow(
        String workflowReferenceCode,
        CreateFrontendProjectWorkflowRequestModel createFrontendProjectWorkflowRequestModel,
        EnvironmentModel xEnvironment) {

        connectedUserProjectFacade.updateProjectWorkflow(
            OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"), workflowReferenceCode,
            createFrontendProjectWorkflowRequestModel.getDefinition(), getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> publishFrontendProjectWorkflow(
        String workflowReferenceCode, EnvironmentModel xEnvironment,
        PublishFrontendProjectWorkflowRequestModel publishFrontendProjectWorkflowRequestModel) {

        connectedUserProjectFacade.publishProjectWorkflow(
            OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"), workflowReferenceCode,
            publishFrontendProjectWorkflowRequestModel.getDescription(), getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
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
    public ResponseEntity<Void> deleteProjectWorkflow(
        String externalUserId, String workflowReferenceCode, EnvironmentModel xEnvironment) {

        connectedUserProjectFacade.deleteProjectWorkflow(
            externalUserId, workflowReferenceCode, getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> disableProjectWorkflow(
        String externalUserId, String workflowReferenceCode, EnvironmentModel xEnvironment) {

        connectedUserProjectFacade.enableProjectWorkflow(
            OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"), workflowReferenceCode, false,
            getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> enableProjectWorkflow(
        String externalUserId, String workflowReferenceCode, EnvironmentModel xEnvironment) {

        connectedUserProjectFacade.enableProjectWorkflow(
            OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"), workflowReferenceCode, true,
            getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<ConnectedUserProjectWorkflowModel> getProjectWorkflow(
        String externalUserId, String workflowReferenceCode, EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            conversionService.convert(
                connectedUserProjectFacade.getConnectedUserProjectWorkflow(
                    externalUserId, workflowReferenceCode, getEnvironment(xEnvironment)),
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
    public ResponseEntity<Void> publishProjectWorkflow(
        String externalUserId, String workflowReferenceCode, EnvironmentModel xEnvironment,
        PublishFrontendProjectWorkflowRequestModel publishFrontendProjectWorkflowRequestModel) {

        connectedUserProjectFacade.publishProjectWorkflow(
            externalUserId, workflowReferenceCode,
            publishFrontendProjectWorkflowRequestModel.getDescription(), getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> updateProjectWorkflow(
        String externalUserId, String workflowReferenceCode,
        CreateFrontendProjectWorkflowRequestModel createFrontendProjectWorkflowRequestModel,
        EnvironmentModel xEnvironment) {

        connectedUserProjectFacade.updateProjectWorkflow(
            externalUserId, workflowReferenceCode,
            createFrontendProjectWorkflowRequestModel.getDefinition(), getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }
}
