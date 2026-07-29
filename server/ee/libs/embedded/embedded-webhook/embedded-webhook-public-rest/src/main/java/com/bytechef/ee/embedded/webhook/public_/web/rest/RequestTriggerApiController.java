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
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserCodeWorkflowReferenceFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.webhook.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.webhook.public_.web.rest.model.EnvironmentModel;
import com.bytechef.file.storage.token.FileEntryTokens;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade;
    private final ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade;
    private final ConnectedUserProjectFacade connectedUserProjectFacade;
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
        ApplicationProperties applicationProperties, AutomationWorkflowProjectFacade automationWorkflowProjectFacade,
        ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade,
        ConnectedUserProjectFacade connectedUserProjectFacade, ConnectedUserService connectedUserService,
        EnvironmentService environmentService, FileEntryTokens fileEntryTokens,
        HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
        ProjectDeploymentService projectDeploymentService, TempFileStorage tempFileStorage,
        WebhookWorkflowExecutor webhookWorkflowExecutor, IntegrationInstanceService integrationInstanceService,
        IntegrationWorkflowService integrationWorkflowService, ProjectWorkflowService projectWorkflowService,
        WorkflowService workflowService) {

        super(fileEntryTokens, applicationProperties.getPublicUrl(), tempFileStorage, webhookWorkflowExecutor);

        this.automationWorkflowProjectFacade = automationWorkflowProjectFacade;
        this.connectedUserCodeWorkflowReferenceFacade = connectedUserCodeWorkflowReferenceFacade;
        this.connectedUserProjectFacade = connectedUserProjectFacade;
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
    public ResponseEntity<Object> executeWorkflow(String workflowUuid, EnvironmentModel xEnvironment) {
        Environment environment = environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(
            OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found"), environment);

        Optional<String> integrationWorkflowId = integrationWorkflowService.fetchLastWorkflowId(
            workflowUuid, environment);

        if (integrationWorkflowId.isPresent()) {
            return executeIntegrationWorkflow(connectedUser, workflowUuid, integrationWorkflowId.get(), environment);
        }

        // The automation-bridge facades (AutomationWorkflowProjectFacade, ConnectedUserProjectFacade,
        // ConnectedUserCodeWorkflowReferenceFacade) are backed by remote-client stubs in deployment topologies that
        // do not carry embedded-configuration-service (e.g. a distributed webhook-app). Treat that the same as
        // "bridge absent" -- the same 404 an unknown workflowUuid always returns -- rather than letting the
        // UnsupportedOperationException propagate as a 500.
        try {
            return executeAutomationBridgeWorkflow(connectedUser, workflowUuid, environment);
        } catch (UnsupportedOperationException unsupportedOperationException) {
            if (automationBridgeUnsupportedLogged.compareAndSet(false, true)) {
                log.warn(
                    "The embedded automation-bridge is not supported in this deployment topology; "
                        + "the embedded-configuration facades are remote stubs");
            }

            return ResponseEntity.notFound()
                .build();
        }
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

    /**
     * The automation-bridge branch: workflowUuid is not an integration workflow. Three shapes resolve here, tried in
     * this order:
     * <ol>
     * <li>The caller's own copy uuid (a connected user's private copy of a visual template) -- dispatched directly, the
     * same resolution {@link AppEventTriggerApiController} uses for its fan-out.</li>
     * <li>A published catalog uuid whose project is a code-workflow catalog ({@code kind = REFERENCE}) -- the
     * pre-existing shared-reference resolution, unchanged.</li>
     * <li>A published catalog uuid whose project is a visual catalog ({@code kind = COPY}) -- implicit copy-then-run:
     * reuse the caller's existing copy if one was already provisioned from this template, otherwise provision one via
     * {@link ConnectedUserProjectFacade#copyWorkflowTemplate} (the same copy the explicit
     * {@code POST /automation/workflow-templates/{uuid}/copy} endpoint performs) and dispatch it.</li>
     * </ol>
     * No published catalog workflow with this uuid, and no enabled reference/copy to it, all resolve to the SAME 404 an
     * unknown workflowUuid always returned -- an existence leak would tell a caller something about the catalog they
     * otherwise couldn't see.
     */
    private ResponseEntity<Object> executeAutomationBridgeWorkflow(
        ConnectedUser connectedUser, String workflowUuid, Environment environment) {

        Optional<ConnectedUserProjectWorkflow> ownCopy = findOwnCopyModeWorkflow(connectedUser.getId(), workflowUuid);

        if (ownCopy.isPresent()) {
            return dispatchCopyModeWorkflow(ownCopy.get(), environment);
        }

        Optional<AutomationWorkflowProjectDTO> catalogProject = automationWorkflowProjectFacade.getPublishedProjects()
            .stream()
            .filter(
                project -> CollectionUtils.stream(project.workflowTemplates())
                    .anyMatch(workflowTemplate -> Objects.equals(workflowTemplate.workflowUuid(), workflowUuid)))
            .findFirst();

        if (catalogProject.isEmpty()) {
            return ResponseEntity.notFound()
                .build();
        }

        if (catalogProject.get()
            .codeWorkflowProject()) {

            return executeCodeReferenceWorkflow(connectedUser, workflowUuid, environment);
        }

        return executeVisualTemplateWorkflow(connectedUser, workflowUuid, environment);
    }

    private ResponseEntity<Object> executeCodeReferenceWorkflow(
        ConnectedUser connectedUser, String workflowUuid, Environment environment) {

        ConnectedUserProjectWorkflow reference;

        try {
            reference = connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
                connectedUser.getExternalId(), workflowUuid, environment);
        } catch (MissingConnectionException missingConnectionException) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("missingConnectionComponentName", missingConnectionException.getComponentName()));
        }

        if (!reference.isEnabled() || reference.isDangling()) {
            return ResponseEntity.notFound()
                .build();
        }

        Long projectDeploymentId = reference.getProjectDeploymentId();

        if (projectDeploymentId == null) {
            return ResponseEntity.notFound()
                .build();
        }

        String catalogWorkflowId = projectWorkflowService.getLastPublishedWorkflowId(workflowUuid);
        Workflow workflow = workflowService.getWorkflow(catalogWorkflowId);

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, projectDeploymentId, workflowUuid,
            findRequestTriggerName(workflow));

        return dispatch(workflowExecutionId);
    }

    /**
     * Implicit provisioning: the first sync call against a visual template's catalog uuid performs the same copy the
     * explicit {@code POST /automation/workflow-templates/{uuid}/copy} endpoint performs, then dispatches the new copy.
     * A repeat call with the same template uuid resolves the copy already provisioned for this connected user instead
     * of creating a second one.
     */
    private ResponseEntity<Object> executeVisualTemplateWorkflow(
        ConnectedUser connectedUser, String catalogWorkflowUuid, Environment environment) {

        ConnectedUserProjectWorkflow copy = findExistingCopyOfTemplate(connectedUser.getId(), catalogWorkflowUuid)
            .orElseGet(() -> provisionVisualTemplateCopy(connectedUser, catalogWorkflowUuid, environment));

        return dispatchCopyModeWorkflow(copy, environment);
    }

    private ConnectedUserProjectWorkflow provisionVisualTemplateCopy(
        ConnectedUser connectedUser, String catalogWorkflowUuid, Environment environment) {

        String copyWorkflowUuid = connectedUserProjectFacade.copyWorkflowTemplate(
            connectedUser.getExternalId(), catalogWorkflowUuid, environment);

        return findOwnCopyModeWorkflow(connectedUser.getId(), copyWorkflowUuid)
            .orElseThrow(
                () -> new IllegalStateException(
                    "Copy of catalog workflow " + catalogWorkflowUuid + " was not found immediately after "
                        + "provisioning"));
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

    /**
     * Resolves {@code workflowUuid} against the caller's own copy-mode rows (never reference-mode ones), matching on
     * the copy's own {@link ProjectWorkflow} uuid rather than the catalog uuid it may have been copied from.
     */
    private Optional<ConnectedUserProjectWorkflow> findOwnCopyModeWorkflow(long connectedUserId, String workflowUuid) {
        return connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUserId)
            .stream()
            .filter(row -> row.getCatalogWorkflowUuid() == null)
            .filter(row -> {
                ProjectWorkflow projectWorkflow = projectWorkflowService.getProjectWorkflow(
                    row.getProjectWorkflowId());

                return Objects.equals(projectWorkflow.getUuidAsString(), workflowUuid);
            })
            .findFirst();
    }

    /**
     * Resolves an existing copy-mode row that was implicitly provisioned from {@code catalogWorkflowUuid}, so a repeat
     * sync call against the same template uuid reuses it instead of provisioning a second copy.
     */
    private Optional<ConnectedUserProjectWorkflow> findExistingCopyOfTemplate(
        long connectedUserId, String catalogWorkflowUuid) {

        return connectedUserCodeWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUserId)
            .stream()
            .filter(row -> row.getCatalogWorkflowUuid() == null)
            .filter(row -> Objects.equals(row.getCopiedFromWorkflowUuid(), catalogWorkflowUuid))
            .findFirst();
    }

    private ResponseEntity<Object> dispatch(WorkflowExecutionId workflowExecutionId) {
        if (webhookWorkflowExecutor.isWorkflowDisabled(workflowExecutionId)) {
            return ResponseEntity.ok()
                .build();
        }

        try {
            return doProcessTrigger(workflowExecutionId, null, httpServletRequest, httpServletResponse)
                .join();
        } catch (IOException | ServletException e) {
            throw new RuntimeException(e);
        }
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
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
