/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.webhook.public_.web.rest;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserWorkflowReferenceFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.webhook.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.webhook.public_.web.rest.model.EnvironmentModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.webhook.rest.AbstractWebhookTriggerController;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class RequestTriggerApiController extends AbstractWebhookTriggerController implements RequestTriggerApi {

    private static final Logger log = LoggerFactory.getLogger(RequestTriggerApiController.class);

    private final AtomicBoolean automationBridgeUnsupportedLogged = new AtomicBoolean(false);
    private final ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade;
    private final ConnectedUserService connectedUserService;
    private final ConnectedUserCopyModeWorkflowResolver copyModeWorkflowResolver;
    private final HttpServletRequest httpServletRequest;
    private final HttpServletResponse httpServletResponse;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WebhookWorkflowExecutor webhookWorkflowExecutor;
    private final WorkflowService workflowService;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    public RequestTriggerApiController(
        ApplicationProperties applicationProperties,
        ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade,
        ConnectedUserService connectedUserService,
        EnvironmentService environmentService,
        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
        ProjectDeploymentService projectDeploymentService, TempFileStorage tempFileStorage,
        WebhookWorkflowExecutor webhookWorkflowExecutor, IntegrationInstanceService integrationInstanceService,
        IntegrationWorkflowService integrationWorkflowService, ProjectWorkflowService projectWorkflowService,
        WorkflowService workflowService) {

        super(applicationProperties.getPublicUrl(), tempFileStorage, webhookWorkflowExecutor);

        this.connectedUserWorkflowReferenceFacade = connectedUserWorkflowReferenceFacade;
        this.connectedUserService = connectedUserService;
        this.copyModeWorkflowResolver = new ConnectedUserCopyModeWorkflowResolver(
            projectDeploymentService, projectWorkflowService);
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.projectWorkflowService = projectWorkflowService;
        this.webhookWorkflowExecutor = webhookWorkflowExecutor;
        this.workflowService = workflowService;
        this.environmentService = environmentService;
    }

    @CrossOrigin
    @Override
    public ResponseEntity<Object> executeFrontendWorkflow(String workflowUuid, EnvironmentModel xEnvironment) {
        String externalUserId = OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found");

        return executeWorkflow(externalUserId, workflowUuid, xEnvironment);
    }

    @Override
    public ResponseEntity<Object> executeWorkflow(
        String externalUserId, String workflowUuid, EnvironmentModel xEnvironment) {

        SecurityUtils.checkCurrentUserLogin(externalUserId);

        Environment environment = environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        Optional<String> integrationWorkflowId = integrationWorkflowService.fetchLastWorkflowId(
            workflowUuid, environment);

        if (integrationWorkflowId.isPresent()) {
            return executeIntegrationWorkflow(connectedUser, workflowUuid, integrationWorkflowId.get(), environment);
        }

        Optional<List<ConnectedUserProjectWorkflow>> connectedUserProjectWorkflowsOptional =
            fetchConnectedUserProjectWorkflows(connectedUser.getId());

        if (connectedUserProjectWorkflowsOptional.isEmpty()) {
            return ResponseEntity.notFound()
                .build();
        }

        return executeAutomationBridgeWorkflow(connectedUserProjectWorkflowsOptional.get(), workflowUuid, environment);
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private ResponseEntity<Object> executeIntegrationWorkflow(
        ConnectedUser connectedUser, String workflowUuid, String workflowId, Environment environment) {

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            connectedUser.getId(), workflowId, environment);

        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.EMBEDDED, integrationInstance.getId(), workflowUuid, findRequestTriggerName(workflow));

        return dispatch(workflowExecutionId);
    }

    private Optional<List<ConnectedUserProjectWorkflow>> fetchConnectedUserProjectWorkflows(long connectedUserId) {
        try {
            return Optional.of(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUserId));
        } catch (UnsupportedOperationException unsupportedOperationException) {
            if (automationBridgeUnsupportedLogged.compareAndSet(false, true)) {
                log.warn(
                    "The embedded automation-bridge is not supported in this deployment topology; "
                        + "the embedded-configuration facades are remote stubs");
            }

            return Optional.empty();
        }
    }

    private ResponseEntity<Object> executeAutomationBridgeWorkflow(
        List<ConnectedUserProjectWorkflow> connectedUserProjectWorkflows, String workflowUuid,
        Environment environment) {

        Optional<ConnectedUserProjectWorkflow> ownCopy = findOwnCopyModeWorkflow(
            connectedUserProjectWorkflows, workflowUuid);

        if (ownCopy.isPresent()) {
            return dispatchCopyModeWorkflow(ownCopy.get(), environment);
        }

        Optional<ConnectedUserProjectWorkflow> reference = connectedUserProjectWorkflows.stream()
            .filter(row -> Objects.equals(row.getAutomationWorkflowUuid(), workflowUuid))
            .findFirst();

        if (reference.isPresent()) {
            return dispatchReferenceModeWorkflow(reference.get());
        }

        return findExistingCopyOfTemplate(connectedUserProjectWorkflows, workflowUuid)
            .map(copy -> dispatchCopyModeWorkflow(copy, environment))
            .orElseGet(() -> ResponseEntity.notFound()
                .build());
    }

    private ResponseEntity<Object> dispatchReferenceModeWorkflow(ConnectedUserProjectWorkflow reference) {
        Long projectDeploymentId = reference.getProjectDeploymentId();

        if (!reference.isEnabled() || reference.isDangling() || projectDeploymentId == null) {
            return ResponseEntity.notFound()
                .build();
        }

        String automationWorkflowUuid = reference.getAutomationWorkflowUuid();

        Workflow workflow = workflowService.getWorkflow(
            projectWorkflowService.getLastPublishedWorkflowId(automationWorkflowUuid));

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, projectDeploymentId, automationWorkflowUuid, findRequestTriggerName(workflow));

        return dispatch(workflowExecutionId);
    }

    private ResponseEntity<Object>
        dispatchCopyModeWorkflow(ConnectedUserProjectWorkflow copy, Environment environment) {
        if (!copy.isEnabled() || copy.isDangling()) {
            return ResponseEntity.notFound()
                .build();
        }

        Optional<ConnectedUserCopyModeWorkflowResolver.Resolved> resolvedOptional = copyModeWorkflowResolver.resolve(
            copy, environment);

        if (resolvedOptional.isEmpty()) {
            return ResponseEntity.notFound()
                .build();
        }

        ConnectedUserCopyModeWorkflowResolver.Resolved resolved = resolvedOptional.get();

        Workflow workflow = workflowService.getWorkflow(resolved.workflowId());

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, resolved.projectDeploymentId(), resolved.workflowUuid(),
            findRequestTriggerName(workflow));

        return dispatch(workflowExecutionId);
    }

    private Optional<ConnectedUserProjectWorkflow> findOwnCopyModeWorkflow(
        List<ConnectedUserProjectWorkflow> connectedUserProjectWorkflows, String workflowUuid) {

        return connectedUserProjectWorkflows.stream()
            .filter(row -> row.getAutomationWorkflowUuid() == null)
            .filter(row -> {
                ProjectWorkflow projectWorkflow = projectWorkflowService.getProjectWorkflow(
                    row.getProjectWorkflowId());

                return Objects.equals(projectWorkflow.getUuidAsString(), workflowUuid);
            })
            .findFirst();
    }

    private static Optional<ConnectedUserProjectWorkflow> findExistingCopyOfTemplate(
        List<ConnectedUserProjectWorkflow> connectedUserProjectWorkflows, String automationWorkflowUuid) {

        return connectedUserProjectWorkflows.stream()
            .filter(row -> row.getAutomationWorkflowUuid() == null)
            .filter(row -> Objects.equals(row.getCopiedFromWorkflowUuid(), automationWorkflowUuid))
            .findFirst();
    }

    private ResponseEntity<Object> dispatch(WorkflowExecutionId workflowExecutionId) {
        if (webhookWorkflowExecutor.isWorkflowDisabled(workflowExecutionId)) {
            return ResponseEntity.ok()
                .build();
        }

        try {
            return doProcessTrigger(workflowExecutionId, null, httpServletRequest, httpServletResponse);
        } catch (IOException | ServletException e) {
            throw new RuntimeException(e);
        }
    }

    private static String findRequestTriggerName(Workflow workflow) {
        return WorkflowTrigger.of(workflow)
            .stream()
            .map(workflowTrigger -> {
                WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

                if (Objects.equals(workflowNodeType.name(), "request")) {
                    return workflowTrigger.getName();
                }

                return null;
            })
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }
}
