/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.configuration.exception.AutomationWorkflowTemplateNotVisibleException;
import com.bytechef.ee.embedded.configuration.exception.ConnectionNotEntitledException;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.exception.MissingInputException;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserWorkflowReferenceFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.ConnectedUserProjectWorkflowModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.CreateFrontendProjectWorkflowFromPromptRequestModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.CreateFrontendProjectWorkflowRequestModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.ProvisionWorkflowReferenceRequestModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.PublishFrontendProjectWorkflowRequestModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.UpdateFrontendWorkflowConfigurationConnectionRequestModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.UpdateWorkflowInputsRequestModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
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
import org.springframework.web.server.ResponseStatusException;

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

    private final ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade;
    private final ConnectedUserProjectFacade connectedUserProjectFacade;
    private final ConversionService conversionService;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectWorkflowApiController(
        ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade,
        ConnectedUserProjectFacade connectedUserProjectFacade, ConversionService conversionService,
        EnvironmentService environmentService) {
        this.connectedUserWorkflowReferenceFacade = connectedUserWorkflowReferenceFacade;
        this.connectedUserProjectFacade = connectedUserProjectFacade;
        this.conversionService = conversionService;
        this.environmentService = environmentService;
    }

    @Override
    @CrossOrigin
    public ResponseEntity<String> createFrontendProjectWorkflow(
        CreateFrontendProjectWorkflowRequestModel createFrontendProjectWorkflowRequestModel,
        EnvironmentModel xEnvironment) {
        return workflowUuidResponse(
            connectedUserProjectFacade.createProjectWorkflow(
                OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"),
                createFrontendProjectWorkflowRequestModel.getDefinition(), getEnvironment(xEnvironment)));
    }

    @Override
    public ResponseEntity<String> createProjectWorkflow(
        String externalUserId, CreateFrontendProjectWorkflowRequestModel createFrontendProjectWorkflowRequestModel,
        EnvironmentModel xEnvironment) {
        return workflowUuidResponse(
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
        return doEnableProjectWorkflow(
            OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), workflowUuid, false,
            xEnvironment);
    }

    @Override
    public ResponseEntity<Void> disableProjectWorkflow(
        String externalUserId, String workflowUuid, EnvironmentModel xEnvironment) {
        return doEnableProjectWorkflow(
            OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), workflowUuid, false,
            xEnvironment);
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> enableFrontendProjectWorkflow(
        String workflowUuid, EnvironmentModel xEnvironment) {
        return doEnableProjectWorkflow(
            OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), workflowUuid, true,
            xEnvironment);
    }

    @Override
    public ResponseEntity<Void> enableProjectWorkflow(
        String externalUserId, String workflowUuid, EnvironmentModel xEnvironment) {
        return doEnableProjectWorkflow(
            OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), workflowUuid, true,
            xEnvironment);
    }

    private ResponseEntity<Void> doEnableProjectWorkflow(
        String externalUserId, String workflowUuid, boolean enable, EnvironmentModel xEnvironment) {
        connectedUserProjectFacade.enableProjectWorkflow(
            externalUserId, workflowUuid, enable, (long) getEnvironment(xEnvironment).ordinal());

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
    public ResponseEntity<Void> updateFrontendProjectWorkflowInputs(
        String workflowUuid, UpdateWorkflowInputsRequestModel updateWorkflowInputsRequestModel,
        EnvironmentModel xEnvironment) {
        connectedUserProjectFacade.updateProjectWorkflowInputs(
            OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), workflowUuid,
            updateWorkflowInputsRequestModel.getInputs(), (long) getEnvironment(xEnvironment).ordinal());

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> updateProjectWorkflowInputs(
        String externalUserId, String workflowUuid, UpdateWorkflowInputsRequestModel updateWorkflowInputsRequestModel,
        EnvironmentModel xEnvironment) {
        connectedUserProjectFacade.updateProjectWorkflowInputs(
            externalUserId, workflowUuid, updateWorkflowInputsRequestModel.getInputs(),
            (long) getEnvironment(xEnvironment).ordinal());

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

    @Override
    @CrossOrigin
    public ResponseEntity<String> copyFrontendWorkflowTemplate(String workflowUuid, EnvironmentModel xEnvironment) {
        try {
            return workflowUuidResponse(
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
        try {
            return workflowUuidResponse(
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
        return workflowUuidResponse(
            connectedUserProjectFacade.createProjectWorkflow(
                OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), requestModel.getPrompt(),
                requestModel.getSystemPrompt(), getEnvironment(xEnvironment), true));
    }

    @Override
    public ResponseEntity<String> createProjectWorkflowFromPrompt(
        String externalUserId, CreateFrontendProjectWorkflowFromPromptRequestModel requestModel,
        EnvironmentModel xEnvironment) {
        return workflowUuidResponse(
            connectedUserProjectFacade.createProjectWorkflow(
                externalUserId, requestModel.getPrompt(), requestModel.getSystemPrompt(), getEnvironment(xEnvironment),
                true));
    }

    @Override
    @CrossOrigin
    public ResponseEntity<String> updateFrontendProjectWorkflowFromPrompt(
        String workflowUuid, CreateFrontendProjectWorkflowFromPromptRequestModel requestModel,
        EnvironmentModel xEnvironment) {
        return workflowUuidResponse(
            connectedUserProjectFacade.updateProjectWorkflow(
                OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), workflowUuid,
                requestModel.getPrompt(), getEnvironment(xEnvironment), true));
    }

    @Override
    public ResponseEntity<String> updateProjectWorkflowFromPrompt(
        String externalUserId, String workflowUuid, CreateFrontendProjectWorkflowFromPromptRequestModel requestModel,
        EnvironmentModel xEnvironment) {
        return workflowUuidResponse(
            connectedUserProjectFacade.updateProjectWorkflow(
                externalUserId, workflowUuid, requestModel.getPrompt(), getEnvironment(xEnvironment), true));
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> provisionFrontendWorkflowReference(
        String workflowUuid, EnvironmentModel xEnvironment,
        ProvisionWorkflowReferenceRequestModel provisionWorkflowReferenceRequestModel) {
        String externalUserId = OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found");

        try {
            connectedUserWorkflowReferenceFacade.getOrCreateReference(
                externalUserId, workflowUuid, getEnvironment(xEnvironment),
                getRequestedConnectionIds(provisionWorkflowReferenceRequestModel),
                getRequestedInputs(provisionWorkflowReferenceRequestModel));
        } catch (AutomationWorkflowTemplateNotVisibleException automationWorkflowTemplateNotVisibleException) {
            return notFoundForRejectedProvisioning(workflowUuid, automationWorkflowTemplateNotVisibleException);
        }

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> provisionWorkflowReference(
        String externalUserId, String workflowUuid, EnvironmentModel xEnvironment,
        ProvisionWorkflowReferenceRequestModel provisionWorkflowReferenceRequestModel) {
        try {
            connectedUserWorkflowReferenceFacade.getOrCreateReference(
                externalUserId, workflowUuid, getEnvironment(xEnvironment),
                getRequestedConnectionIds(provisionWorkflowReferenceRequestModel),
                getRequestedInputs(provisionWorkflowReferenceRequestModel));
        } catch (AutomationWorkflowTemplateNotVisibleException automationWorkflowTemplateNotVisibleException) {
            return notFoundForRejectedProvisioning(workflowUuid, automationWorkflowTemplateNotVisibleException);
        }

        return ResponseEntity.noContent()
            .build();
    }

    private static @Nullable Map<String, ?> getRequestedInputs(
        @Nullable ProvisionWorkflowReferenceRequestModel provisionWorkflowReferenceRequestModel) {

        if (provisionWorkflowReferenceRequestModel == null) {
            return null;
        }

        Map<String, Object> inputs = provisionWorkflowReferenceRequestModel.getInputs();

        return inputs == null || inputs.isEmpty() ? null : inputs;
    }

    private static Map<String, Long> getRequestedConnectionIds(
        ProvisionWorkflowReferenceRequestModel provisionWorkflowReferenceRequestModel) {
        if (provisionWorkflowReferenceRequestModel == null ||
            provisionWorkflowReferenceRequestModel.getConnections() == null) {
            return Map.of();
        }

        Map<String, Long> connections = provisionWorkflowReferenceRequestModel.getConnections();

        if (connections.containsValue(null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Every requested connection needs an id");
        }

        return connections;
    }

    @Override
    @CrossOrigin
    public ResponseEntity<Void> deprovisionFrontendWorkflowReference(
        String workflowUuid, EnvironmentModel xEnvironment) {
        String externalUserId = OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found");

        connectedUserWorkflowReferenceFacade.deleteReference(
            externalUserId, workflowUuid, getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<Void> deprovisionWorkflowReference(
        String externalUserId, String workflowUuid, EnvironmentModel xEnvironment) {
        connectedUserWorkflowReferenceFacade.deleteReference(
            externalUserId, workflowUuid, getEnvironment(xEnvironment));

        return ResponseEntity.noContent()
            .build();
    }

    private <T> ResponseEntity<T> notFoundForRejectedProvisioning(
        String workflowUuid,
        AutomationWorkflowTemplateNotVisibleException automationWorkflowTemplateNotVisibleException) {
        if (log.isDebugEnabled()) {
            log.debug(
                "Provisioning of automation workflow {} was rejected for the connected user; returning 404: {}",
                workflowUuid, automationWorkflowTemplateNotVisibleException.getMessage());
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

    @ExceptionHandler(ConnectionNotEntitledException.class)
    public ResponseEntity<Void> handleConnectionNotEntitledException(
        ConnectionNotEntitledException connectionNotEntitledException) {
        return ResponseEntity.badRequest()
            .build();
    }

    @ExceptionHandler(MissingInputException.class)
    public ResponseEntity<Object> handleMissingInputException(MissingInputException missingInputException) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("missingInputName", missingInputException.getInputName()));
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private Environment getEnvironment(EnvironmentModel xEnvironment) {
        return environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());
    }

    private static ResponseEntity<String> workflowUuidResponse(String workflowUuid) {
        return ResponseEntity.ok("\"" + workflowUuid + "\"");
    }
}
