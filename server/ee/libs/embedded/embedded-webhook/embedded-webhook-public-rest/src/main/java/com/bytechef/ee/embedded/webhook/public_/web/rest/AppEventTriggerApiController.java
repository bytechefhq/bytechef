/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.webhook.public_.web.rest;

import static com.bytechef.platform.component.definition.AppEventComponentDefinition.APP_EVENT;
import static com.bytechef.platform.component.definition.AppEventComponentDefinition.NEW_EVENT;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserWorkflowReferenceFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.webhook.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.webhook.public_.web.rest.model.EnvironmentModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.WebhookRequest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.WebDataBinder;
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
public class AppEventTriggerApiController extends AbstractWebhookTriggerController implements AppEventTriggerApi {

    private static final Logger log = LoggerFactory.getLogger(AppEventTriggerApiController.class);

    private final AtomicBoolean automationBridgeUnsupportedLogged = new AtomicBoolean(false);
    private final ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade;
    private final ConnectedUserCopyModeWorkflowResolver copyModeWorkflowResolver;
    private final ConnectedUserService connectedUserService;
    private final HttpServletRequest httpServletRequest;
    private final HttpServletResponse httpServletResponse;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WebhookWorkflowExecutor webhookWorkflowExecutor;
    private final WorkflowService workflowService;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    public AppEventTriggerApiController(
        ApplicationProperties applicationProperties,
        ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade,
        ConnectedUserService connectedUserService, EnvironmentService environmentService,
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceService integrationInstanceService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService,
        IntegrationWorkflowService integrationWorkflowService, ProjectDeploymentService projectDeploymentService,
        ProjectWorkflowService projectWorkflowService, TempFileStorage tempFileStorage,
        WebhookWorkflowExecutor webhookWorkflowExecutor, WorkflowService workflowService) {

        super(applicationProperties.getPublicUrl(), tempFileStorage, webhookWorkflowExecutor);

        this.connectedUserWorkflowReferenceFacade = connectedUserWorkflowReferenceFacade;
        this.copyModeWorkflowResolver = new ConnectedUserCopyModeWorkflowResolver(
            projectDeploymentService, projectWorkflowService);
        this.connectedUserService = connectedUserService;
        this.environmentService = environmentService;
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.projectWorkflowService = projectWorkflowService;
        this.webhookWorkflowExecutor = webhookWorkflowExecutor;
        this.workflowService = workflowService;
    }

    @Override
    public ResponseEntity<Void> executeFrontendWorkflows(EnvironmentModel xEnvironment) {
        String externalUserId = OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found");

        return executeWorkflows(externalUserId, xEnvironment);
    }

    @Override
    public ResponseEntity<Void> executeWorkflows(String externalUserId, EnvironmentModel xEnvironment) {
        SecurityUtils.checkCurrentUserLogin(externalUserId);

        Environment environment = environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        List<String> failedWorkflows = new ArrayList<>();
        List<WorkflowExecutionId> workflowExecutionIds = new ArrayList<>(
            resolveIntegrationWorkflowExecutionIds(connectedUser.getId()));

        workflowExecutionIds.addAll(
            resolveAutomationBridgeWorkflowExecutionIds(connectedUser.getId(), environment, failedWorkflows));

        failedWorkflows.addAll(dispatch(workflowExecutionIds));

        if (!failedWorkflows.isEmpty()) {
            throw createDispatchFailureException(failedWorkflows);
        }

        return ResponseEntity.noContent()
            .build();
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private List<WorkflowExecutionId> resolveIntegrationWorkflowExecutionIds(long connectedUserId) {
        List<WorkflowExecutionId> workflowExecutionIds = new ArrayList<>();

        List<IntegrationInstance> integrationInstances =
            integrationInstanceService.getConnectedUserIntegrationInstances(connectedUserId, true);

        for (IntegrationInstance integrationInstance : integrationInstances) {
            List<IntegrationInstanceWorkflow> integrationInstanceWorkflows =
                integrationInstanceWorkflowService.getIntegrationInstanceWorkflows(integrationInstance.getId());

            List<String> workflowIds = integrationInstanceWorkflows.stream()
                .filter(IntegrationInstanceWorkflow::isEnabled)
                .map(integrationInstanceWorkflow -> integrationInstanceConfigurationWorkflowService
                    .getIntegrationInstanceConfigurationWorkflow(
                        integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId()))
                .map(IntegrationInstanceConfigurationWorkflow::getWorkflowId)
                .toList();

            for (String workflowId : workflowIds) {
                Workflow workflow = workflowService.getWorkflow(workflowId);

                String appEventTriggerName = findAppEventTriggerName(workflow);

                if (appEventTriggerName == null) {
                    continue;
                }

                IntegrationWorkflow integrationWorkflow = integrationWorkflowService.getWorkflowIntegrationWorkflow(
                    workflowId);

                workflowExecutionIds.add(
                    WorkflowExecutionId.of(
                        PlatformType.EMBEDDED, integrationInstance.getId(), integrationWorkflow.getUuidAsString(),
                        appEventTriggerName));
            }
        }

        return workflowExecutionIds;
    }

    private List<WorkflowExecutionId> resolveAutomationBridgeWorkflowExecutionIds(
        long connectedUserId, Environment environment, List<String> failedWorkflows) {

        List<WorkflowExecutionId> workflowExecutionIds = new ArrayList<>();

        for (ConnectedUserProjectWorkflow connectedUserProjectWorkflow : fetchConnectedUserProjectWorkflows(
            connectedUserId)) {

            if (!connectedUserProjectWorkflow.isEnabled() || connectedUserProjectWorkflow.isDangling()) {
                continue;
            }

            try {
                resolveAutomationBridgeWorkflowExecutionId(connectedUserProjectWorkflow, environment)
                    .ifPresent(workflowExecutionIds::add);
            } catch (RuntimeException exception) {
                log.error(
                    "Failed to resolve automation-bridge workflow {} for an app event",
                    connectedUserProjectWorkflow.getId(), exception);

                failedWorkflows.add(getWorkflowLabel(connectedUserProjectWorkflow));
            }
        }

        return workflowExecutionIds;
    }

    private List<ConnectedUserProjectWorkflow> fetchConnectedUserProjectWorkflows(long connectedUserId) {
        try {
            return connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUserId);
        } catch (UnsupportedOperationException unsupportedOperationException) {
            if (automationBridgeUnsupportedLogged.compareAndSet(false, true)) {
                log.warn(
                    "The embedded automation-bridge is not supported in this deployment topology; "
                        + "the embedded-configuration facades are remote stubs");
            }

            return List.of();
        }
    }

    private Optional<WorkflowExecutionId> resolveAutomationBridgeWorkflowExecutionId(
        ConnectedUserProjectWorkflow connectedUserProjectWorkflow, Environment environment) {

        if (connectedUserProjectWorkflow.getAutomationWorkflowUuid() == null) {
            return resolveCopyModeWorkflowExecutionId(connectedUserProjectWorkflow, environment);
        }

        return resolveReferenceModeWorkflowExecutionId(connectedUserProjectWorkflow);
    }

    private Optional<WorkflowExecutionId> resolveReferenceModeWorkflowExecutionId(
        ConnectedUserProjectWorkflow reference) {

        String automationWorkflowId = projectWorkflowService.getLastPublishedWorkflowId(
            reference.getAutomationWorkflowUuid());

        Workflow workflow = workflowService.getWorkflow(automationWorkflowId);

        String appEventTriggerName = findAppEventTriggerName(workflow);

        if (appEventTriggerName == null) {
            return Optional.empty();
        }

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, reference.getProjectDeploymentId(), reference.getAutomationWorkflowUuid(),
            appEventTriggerName);

        if (webhookWorkflowExecutor.isWorkflowDisabled(workflowExecutionId)) {
            return Optional.empty();
        }

        return Optional.of(workflowExecutionId);
    }

    private Optional<WorkflowExecutionId> resolveCopyModeWorkflowExecutionId(
        ConnectedUserProjectWorkflow connectedUserProjectWorkflow, Environment environment) {

        Optional<ConnectedUserCopyModeWorkflowResolver.Resolved> resolvedOptional = copyModeWorkflowResolver.resolve(
            connectedUserProjectWorkflow, environment);

        if (resolvedOptional.isEmpty()) {
            return Optional.empty();
        }

        ConnectedUserCopyModeWorkflowResolver.Resolved resolved = resolvedOptional.get();

        Workflow workflow = workflowService.getWorkflow(resolved.workflowId());

        String appEventTriggerName = findAppEventTriggerName(workflow);

        if (appEventTriggerName == null) {
            return Optional.empty();
        }

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, resolved.projectDeploymentId(), resolved.workflowUuid(), appEventTriggerName);

        if (webhookWorkflowExecutor.isWorkflowDisabled(workflowExecutionId)) {
            return Optional.empty();
        }

        return Optional.of(workflowExecutionId);
    }

    private List<String> dispatch(List<WorkflowExecutionId> workflowExecutionIds) {
        if (workflowExecutionIds.isEmpty()) {
            return List.of();
        }

        WebhookRequest webhookRequest = readWebhookRequest(workflowExecutionIds.getFirst());

        List<String> failedWorkflows = new ArrayList<>();

        for (WorkflowExecutionId workflowExecutionId : workflowExecutionIds) {
            try {
                doProcessTrigger(workflowExecutionId, webhookRequest, httpServletRequest, httpServletResponse);
            } catch (IOException | ServletException | RuntimeException exception) {
                log.error("Failed to dispatch an app event to workflow {}", workflowExecutionId, exception);

                failedWorkflows.add(workflowExecutionId.getWorkflowUuid());
            }
        }

        return failedWorkflows;
    }

    private WebhookRequest readWebhookRequest(WorkflowExecutionId workflowExecutionId) {
        WebhookTriggerFlags webhookTriggerFlags = webhookWorkflowExecutor.getWebhookTriggerFlags(workflowExecutionId);

        try {
            return getWebhookRequest(httpServletRequest, webhookTriggerFlags);
        } catch (IOException | ServletException exception) {
            throw new IllegalStateException("Failed to read the app event request", exception);
        }
    }

    private static ErrorResponseException createDispatchFailureException(List<String> failedWorkflows) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "The app event could not be dispatched to workflows: " + String.join(", ", failedWorkflows));

        problemDetail.setTitle("App event dispatch failed");
        problemDetail.setProperty("failedWorkflows", failedWorkflows);

        return new ErrorResponseException(HttpStatus.INTERNAL_SERVER_ERROR, problemDetail, null);
    }

    private static String getWorkflowLabel(ConnectedUserProjectWorkflow connectedUserProjectWorkflow) {
        if (connectedUserProjectWorkflow.getAutomationWorkflowUuid() != null) {
            return connectedUserProjectWorkflow.getAutomationWorkflowUuid();
        }

        if (connectedUserProjectWorkflow.getCopiedFromWorkflowUuid() != null) {
            return connectedUserProjectWorkflow.getCopiedFromWorkflowUuid();
        }

        return "connectedUserProjectWorkflow:" + connectedUserProjectWorkflow.getId();
    }

    private static String findAppEventTriggerName(Workflow workflow) {
        return WorkflowTrigger.of(workflow)
            .stream()
            .map(workflowTrigger -> {
                WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

                if (Objects.equals(workflowNodeType.name(), APP_EVENT) &&
                    Objects.equals(workflowNodeType.operation(), NEW_EVENT)) {

                    return workflowTrigger.getName();
                }

                return null;
            })
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }
}
