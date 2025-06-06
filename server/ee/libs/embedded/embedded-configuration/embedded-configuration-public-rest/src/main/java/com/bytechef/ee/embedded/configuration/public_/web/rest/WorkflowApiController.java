/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.configuration.facade.ConnectUserProjectFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.CreateFrontendProjectWorkflowRequestModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.PublishFrontendProjectWorkflowRequestModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.WorkflowModel;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
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
public class WorkflowApiController implements WorkflowApi {

    private final ConnectUserProjectFacade connectUserProjectFacade;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public WorkflowApiController(ConnectUserProjectFacade connectUserProjectFacade,
        ConversionService conversionService) {
        this.connectUserProjectFacade = connectUserProjectFacade;
        this.conversionService = conversionService;
    }

    @Override
    @CrossOrigin
    public ResponseEntity<String> createFrontendProjectWorkflow(
        CreateFrontendProjectWorkflowRequestModel createFrontendProjectWorkflowRequestModel,
        EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            connectUserProjectFacade.createProjectWorkflow(
                OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"),
                createFrontendProjectWorkflowRequestModel.getDefinition(), getEnvironment(xEnvironment)));
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> deleteFrontendProjectWorkflow(
        String workflowReferenceCode, EnvironmentModel xEnvironment) {

        connectUserProjectFacade.deleteProjectWorkflow(
            OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"), workflowReferenceCode,
            getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    @CrossOrigin
    public ResponseEntity<WorkflowModel> getFrontendProjectWorkflow(
        String workflowReferenceCode, EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            conversionService.convert(
                connectUserProjectFacade.getProjectWorkflow(
                    OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"), workflowReferenceCode,
                    getEnvironment(xEnvironment)),
                WorkflowModel.class));
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> enableFrontendProjectWorkflow(
        String workflowReferenceCode, Boolean enable, EnvironmentModel xEnvironment) {

        connectUserProjectFacade.enableProjectWorkflow(
            OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"), workflowReferenceCode, enable,
            getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    @CrossOrigin
    public ResponseEntity<List<WorkflowModel>> getFrontendProjectWorkflows(EnvironmentModel xEnvironment) {
        return ResponseEntity.ok(
            connectUserProjectFacade.getProjectWorkflows(
                OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"),
                getEnvironment(xEnvironment))
                .stream()
                .map(workflow -> conversionService.convert(workflow, WorkflowModel.class))
                .toList());
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> updateFrontendProjectWorkflow(
        String workflowReferenceCode,
        CreateFrontendProjectWorkflowRequestModel createFrontendProjectWorkflowRequestModel,
        EnvironmentModel xEnvironment) {

        connectUserProjectFacade.updateProjectWorkflow(
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

        connectUserProjectFacade.publishProjectWorkflow(
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
            connectUserProjectFacade.createProjectWorkflow(
                externalUserId,
                createFrontendProjectWorkflowRequestModel.getDefinition(), getEnvironment(xEnvironment)));
    }

    @Override
    public ResponseEntity<Void> deleteProjectWorkflow(
        String externalUserId, String workflowReferenceCode, EnvironmentModel xEnvironment) {

        connectUserProjectFacade.deleteProjectWorkflow(
            externalUserId, workflowReferenceCode, getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> enableProjectWorkflow(
        String externalUserId, String workflowReferenceCode, Boolean enable, EnvironmentModel xEnvironment) {

        connectUserProjectFacade.enableProjectWorkflow(
            OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"), workflowReferenceCode, enable,
            getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<WorkflowModel> getProjectWorkflow(
        String externalUserId, String workflowReferenceCode, EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            conversionService.convert(
                connectUserProjectFacade.getProjectWorkflow(
                    externalUserId, workflowReferenceCode, getEnvironment(xEnvironment)),
                WorkflowModel.class));
    }

    @Override
    public ResponseEntity<List<WorkflowModel>> getProjectWorkflows(
        String externalUserId, EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            connectUserProjectFacade.getProjectWorkflows(externalUserId, getEnvironment(xEnvironment))
                .stream()
                .map(workflow -> conversionService.convert(workflow, WorkflowModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<Void> publishProjectWorkflow(
        String externalUserId, String workflowReferenceCode, EnvironmentModel xEnvironment,
        PublishFrontendProjectWorkflowRequestModel publishFrontendProjectWorkflowRequestModel) {

        connectUserProjectFacade.publishProjectWorkflow(
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

        connectUserProjectFacade.updateProjectWorkflow(
            externalUserId, workflowReferenceCode,
            createFrontendProjectWorkflowRequestModel.getDefinition(), getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private static Environment getEnvironment(EnvironmentModel xEnvironment) {
        return xEnvironment == null
            ? Environment.PRODUCTION : Environment.valueOf(StringUtils.upperCase(xEnvironment.name()));
    }
}
