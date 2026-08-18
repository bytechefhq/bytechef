/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserCodeWorkflowReferenceFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.ConnectedUserProjectWorkflowModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.CreateFrontendProjectWorkflowFromPromptRequestModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.CreateFrontendProjectWorkflowRequestModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.PublishFrontendProjectWorkflowRequestModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.UpdateFrontendWorkflowConfigurationConnectionRequestModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
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
@RestController(" com.bytechef.ee.embedded.configuration.public_.web.rest.WorkflowApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ConnectedUserProjectWorkflowApiController implements ConnectedUserProjectWorkflowApi {

    private static final Logger log = LoggerFactory.getLogger(ConnectedUserProjectWorkflowApiController.class);

    private final ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade;
    private final ConnectedUserProjectFacade connectedUserProjectFacade;
    private final ConversionService conversionService;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectWorkflowApiController(
        ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade,
        ConnectedUserProjectFacade connectedUserProjectFacade, ConversionService conversionService,
        EnvironmentService environmentService) {

        this.connectedUserCodeWorkflowReferenceFacade = connectedUserCodeWorkflowReferenceFacade;
        this.connectedUserProjectFacade = connectedUserProjectFacade;
        this.conversionService = conversionService;
        this.environmentService = environmentService;
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

        SecurityUtils.checkCurrentUserLogin(externalUserId);

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

        SecurityUtils.checkCurrentUserLogin(externalUserId);

        connectedUserProjectFacade.deleteProjectWorkflow(
            externalUserId, workflowUuid, getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Object> disableFrontendProjectWorkflow(
        String workflowUuid, EnvironmentModel xEnvironment) {

        return doEnableProjectWorkflow(
            OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), workflowUuid, false,
            xEnvironment);
    }

    @Override
    public ResponseEntity<Object> disableProjectWorkflow(
        String externalUserId, String workflowUuid, EnvironmentModel xEnvironment) {

        SecurityUtils.checkCurrentUserLogin(externalUserId);

        return doEnableProjectWorkflow(
            OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), workflowUuid, false,
            xEnvironment);
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Object> enableFrontendProjectWorkflow(
        String workflowUuid, EnvironmentModel xEnvironment) {

        return doEnableProjectWorkflow(
            OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), workflowUuid, true,
            xEnvironment);
    }

    @Override
    public ResponseEntity<Object> enableProjectWorkflow(
        String externalUserId, String workflowUuid, EnvironmentModel xEnvironment) {

        SecurityUtils.checkCurrentUserLogin(externalUserId);

        return doEnableProjectWorkflow(
            OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), workflowUuid, true,
            xEnvironment);
    }

    /**
     * Shared by all four enable/disable endpoints (frontend and externalUserId-scoped, enable and disable): a
     * workflowUuid that resolves to one of the caller's automation-bridge reference rows can surface
     * {@link MissingConnectionException} out of
     * {@link ConnectedUserProjectFacade#enableProjectWorkflow(String, String, boolean, Long)} the same way
     * {@link #provisionWorkflowReference} already does, so it must be mapped to the same 409 shape here instead of
     * propagating as an unhandled 500.
     */
    private ResponseEntity<Object> doEnableProjectWorkflow(
        String externalUserId, String workflowUuid, boolean enable, EnvironmentModel xEnvironment) {

        try {
            connectedUserProjectFacade.enableProjectWorkflow(
                externalUserId, workflowUuid, enable, (long) getEnvironment(xEnvironment).ordinal());
        } catch (MissingConnectionException missingConnectionException) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("missingConnectionComponentName", missingConnectionException.getComponentName()));
        }

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

        SecurityUtils.checkCurrentUserLogin(externalUserId);

        return ResponseEntity.ok(
            conversionService.convert(
                connectedUserProjectFacade.getConnectedUserProjectWorkflow(
                    externalUserId, workflowUuid, (long) getEnvironment(xEnvironment).ordinal()),
                ConnectedUserProjectWorkflowModel.class));
    }

    @Override
    public ResponseEntity<List<ConnectedUserProjectWorkflowModel>> getProjectWorkflows(
        String externalUserId, EnvironmentModel xEnvironment) {

        SecurityUtils.checkCurrentUserLogin(externalUserId);

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

        SecurityUtils.checkCurrentUserLogin(externalUserId);

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

        SecurityUtils.checkCurrentUserLogin(externalUserId);

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

        SecurityUtils.checkCurrentUserLogin(externalUserId);

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
    @CrossOrigin
    public ResponseEntity<String> copyFrontendWorkflowTemplate(String workflowUuid, EnvironmentModel xEnvironment) {
        try {
            return ResponseEntity.ok(
                connectedUserProjectFacade.copyWorkflowTemplate(
                    OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), workflowUuid,
                    getEnvironment(xEnvironment)));
        } catch (IllegalArgumentException illegalArgumentException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .build();
        }
    }

    @Override
    public ResponseEntity<String> copyWorkflowTemplate(
        String externalUserId, String workflowUuid, EnvironmentModel xEnvironment) {

        SecurityUtils.checkCurrentUserLogin(externalUserId);

        try {
            return ResponseEntity.ok(
                connectedUserProjectFacade.copyWorkflowTemplate(
                    externalUserId, workflowUuid, getEnvironment(xEnvironment)));
        } catch (IllegalArgumentException illegalArgumentException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .build();
        }
    }

    @Override
    @CrossOrigin
    public ResponseEntity<String> createFrontendProjectWorkflowFromPrompt(
        CreateFrontendProjectWorkflowFromPromptRequestModel requestModel, EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            connectedUserProjectFacade.createProjectWorkflow(
                OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), requestModel.getPrompt(),
                requestModel.getSystemPrompt(), getEnvironment(xEnvironment), true));
    }

    @Override
    public ResponseEntity<String> createProjectWorkflowFromPrompt(
        String externalUserId, CreateFrontendProjectWorkflowFromPromptRequestModel requestModel,
        EnvironmentModel xEnvironment) {

        SecurityUtils.checkCurrentUserLogin(externalUserId);

        return ResponseEntity.ok(
            connectedUserProjectFacade.createProjectWorkflow(
                externalUserId, requestModel.getPrompt(), requestModel.getSystemPrompt(), getEnvironment(xEnvironment),
                true));
    }

    @Override
    @CrossOrigin
    public ResponseEntity<String> updateFrontendProjectWorkflowFromPrompt(
        String workflowUuid, CreateFrontendProjectWorkflowFromPromptRequestModel requestModel,
        EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            connectedUserProjectFacade.updateProjectWorkflow(
                OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), workflowUuid,
                requestModel.getPrompt(), getEnvironment(xEnvironment), true));
    }

    @Override
    public ResponseEntity<String> updateProjectWorkflowFromPrompt(
        String externalUserId, String workflowUuid, CreateFrontendProjectWorkflowFromPromptRequestModel requestModel,
        EnvironmentModel xEnvironment) {

        SecurityUtils.checkCurrentUserLogin(externalUserId);

        return ResponseEntity.ok(
            connectedUserProjectFacade.updateProjectWorkflow(
                externalUserId, workflowUuid, requestModel.getPrompt(), getEnvironment(xEnvironment), true));
    }

    /**
     * A rejected {@code workflowUuid} -- one the permission-filtered catalog does not show this connected user, and one
     * that does not exist at all -- reaches this method as the same {@link IllegalArgumentException} from
     * {@code getOrCreateReference} and is mapped to the same bodyless 404 the copy endpoints use, so the response never
     * reveals whether the template exists. {@link MissingConnectionException} is a different type and still reaches
     * {@link #handleMissingConnectionException}.
     *
     * <p>
     * Unlike {@link #provisionWorkflowReference}, the generated {@code provisionFrontendWorkflowReference} signature
     * returns {@code ResponseEntity<Void>} (its OpenAPI 204 response declares no content, so the generator infers
     * {@code Void} instead of {@code Object}), which leaves no room to return a JSON body inline for the 409 case.
     * {@link #handleMissingConnectionException} covers that case at the controller level instead, the same way
     * {@code IntegrationInstanceWorkflowApiController} maps its own domain exception via a class-level
     * {@code @ExceptionHandler}.
     */
    @Override
    @CrossOrigin
    public ResponseEntity<Void> provisionFrontendWorkflowReference(String workflowUuid, EnvironmentModel xEnvironment) {
        String externalUserId = OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found");

        try {
            connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
                externalUserId, workflowUuid, getEnvironment(xEnvironment));
        } catch (IllegalArgumentException illegalArgumentException) {
            return notFoundForRejectedProvisioning(workflowUuid, illegalArgumentException);
        }

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Object> provisionWorkflowReference(
        String externalUserId, String workflowUuid, EnvironmentModel xEnvironment) {

        SecurityUtils.checkCurrentUserLogin(externalUserId);

        try {
            connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
                externalUserId, workflowUuid, getEnvironment(xEnvironment));
        } catch (MissingConnectionException missingConnectionException) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("missingConnectionComponentName", missingConnectionException.getComponentName()));
        } catch (IllegalArgumentException illegalArgumentException) {
            return notFoundForRejectedProvisioning(workflowUuid, illegalArgumentException);
        }

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> deprovisionFrontendWorkflowReference(
        String workflowUuid, EnvironmentModel xEnvironment) {

        String externalUserId = OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found");

        connectedUserCodeWorkflowReferenceFacade.deleteReference(
            externalUserId, workflowUuid, getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> deprovisionWorkflowReference(
        String externalUserId, String workflowUuid, EnvironmentModel xEnvironment) {

        SecurityUtils.checkCurrentUserLogin(externalUserId);

        connectedUserCodeWorkflowReferenceFacade.deleteReference(
            externalUserId, workflowUuid, getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    /**
     * Maps a rejected provisioning request onto a bodyless 404, the same response an unknown {@code workflowUuid}
     * produces, so neither reveals whether the template exists. The response deliberately carries nothing, so the
     * reason is logged at debug for an operator debugging a genuinely misconfigured deployment.
     */
    private <T> ResponseEntity<T> notFoundForRejectedProvisioning(
        String workflowUuid, IllegalArgumentException illegalArgumentException) {

        if (log.isDebugEnabled()) {
            log.debug(
                "Provisioning of catalog workflow {} was rejected for the connected user; returning 404: {}",
                workflowUuid, illegalArgumentException.getMessage());
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .build();
    }

    @ExceptionHandler(MissingConnectionException.class)
    public ResponseEntity<Object> handleMissingConnectionException(
        MissingConnectionException missingConnectionException) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("missingConnectionComponentName", missingConnectionException.getComponentName()));
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private Environment getEnvironment(EnvironmentModel xEnvironment) {
        return environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());
    }
}
