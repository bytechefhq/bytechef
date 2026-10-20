/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.audit.IntegrationAuditEvents;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionIntegrationInstanceWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionableIntegrationDTO;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.configuration.util.IntegrationWorkflowInputUtils;
import com.bytechef.ee.embedded.configuration.util.InternalOnlyInputUtils;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class IntegrationInstanceFacadeImpl implements IntegrationInstanceFacade {

    private final ConnectedUserConnectionFacade connectedUserConnectionFacade;
    private final ObjectProvider<ConnectedUserIntegrationFacade> connectedUserIntegrationFacadeProvider;
    private final ObjectProvider<ConnectedUserIntegrationInstanceFacade> connectedUserIntegrationInstanceFacadeProvider;
    private final ConnectedUserService connectedUserService;
    private final ObjectProvider<ConnectedUserWorkflowWebhookUrlResolver> connectedUserWorkflowWebhookUrlResolverProvider;
    private final Evaluator evaluator;
    private final PrincipalJobService principalJobService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceConnectionCleanup integrationInstanceConnectionCleanup;
    private final IntegrationInstanceCredentialsUpdater integrationInstanceCredentialsUpdater;
    private final IntegrationInstanceDataStorageCleanup integrationInstanceDataStorageCleanup;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationService integrationService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final JobService jobService;
    private final TriggerLifecycleFacade triggerLifecycleFacade;
    private final ComponentConnectionFacade componentConnectionFacade;
    private final WorkflowService workflowService;
    private final String webhookUrl;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceFacadeImpl(
        ApplicationProperties applicationProperties, ConnectedUserConnectionFacade connectedUserConnectionFacade,
        ObjectProvider<ConnectedUserIntegrationFacade> connectedUserIntegrationFacadeProvider,
        ObjectProvider<ConnectedUserIntegrationInstanceFacade> connectedUserIntegrationInstanceFacadeProvider,
        ConnectedUserService connectedUserService,
        ObjectProvider<ConnectedUserWorkflowWebhookUrlResolver> connectedUserWorkflowWebhookUrlResolverProvider,
        Evaluator evaluator, IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceConnectionCleanup integrationInstanceConnectionCleanup,
        IntegrationInstanceCredentialsUpdater integrationInstanceCredentialsUpdater,
        IntegrationInstanceDataStorageCleanup integrationInstanceDataStorageCleanup,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService,
        IntegrationInstanceService integrationInstanceService, IntegrationService integrationService,
        IntegrationWorkflowService integrationWorkflowService, JobService jobService,
        PrincipalJobService principalJobService, TriggerLifecycleFacade triggerLifecycleFacade,
        ComponentConnectionFacade componentConnectionFacade, WorkflowService workflowService) {

        this.componentConnectionFacade = componentConnectionFacade;
        this.connectedUserConnectionFacade = connectedUserConnectionFacade;
        this.connectedUserIntegrationFacadeProvider = connectedUserIntegrationFacadeProvider;
        this.connectedUserIntegrationInstanceFacadeProvider = connectedUserIntegrationInstanceFacadeProvider;
        this.connectedUserService = connectedUserService;
        this.connectedUserWorkflowWebhookUrlResolverProvider = connectedUserWorkflowWebhookUrlResolverProvider;
        this.evaluator = evaluator;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceConnectionCleanup = integrationInstanceConnectionCleanup;
        this.integrationInstanceCredentialsUpdater = integrationInstanceCredentialsUpdater;
        this.integrationInstanceDataStorageCleanup = integrationInstanceDataStorageCleanup;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationService = integrationService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.jobService = jobService;
        this.principalJobService = principalJobService;
        this.triggerLifecycleFacade = triggerLifecycleFacade;
        this.webhookUrl = applicationProperties.getWebhookUrl();
        this.workflowService = workflowService;
    }

    @Override
    @Audited(event = IntegrationAuditEvents.INTEGRATION_INSTANCE_DELETED, data = {
        @AuditData(key = "integrationInstanceId", value = "#integrationInstanceId")
    })
    @PreAuthorize("hasPermission(#integrationInstanceId, 'IntegrationInstance', 'CONNECTED_USER_DELETE')")
    public void deleteIntegrationInstance(long integrationInstanceId) {
        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            integrationInstanceId);

        if (integrationInstance.isEnabled()) {
            enableIntegrationInstanceWorkflowTriggers(integrationInstanceId, false);
        }

        integrationInstanceDataStorageCleanup.deleteAfterCommit(integrationInstanceId);

        integrationInstanceWorkflowService.deleteByIntegrationInstanceId(integrationInstanceId);

        integrationInstanceService.delete(integrationInstanceId);

        integrationInstanceConnectionCleanup.deleteOrKeepForConnectedUser(
            integrationInstance.getConnectedUserId(), integrationInstance.getConnectionId());
    }

    @Override
    @Audited(event = IntegrationAuditEvents.INTEGRATION_INSTANCE_ENABLED_UPDATED, data = {
        @AuditData(key = "integrationInstanceId", value = "#integrationInstanceId"),
        @AuditData(key = "enabled", value = "#enable")
    })
    @PreAuthorize("hasPermission(#integrationInstanceId, 'IntegrationInstance', 'CONNECTED_USER_MANAGE')")
    public void enableIntegrationInstance(long integrationInstanceId, boolean enable) {
        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            integrationInstanceId);

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(
            integrationInstance.getConnectedUserId());

        if (connectedUser.isEnabled()) {
            enableIntegrationInstanceWorkflowTriggers(integrationInstanceId, enable);
        }

        integrationInstanceService.updateEnabled(integrationInstanceId, enable);
    }

    @Override
    @Audited(event = IntegrationAuditEvents.INTEGRATION_INSTANCE_WORKFLOW_ENABLED_UPDATED, data = {
        @AuditData(key = "integrationInstanceId", value = "#integrationInstanceId"),
        @AuditData(key = "workflowId", value = "#workflowId"),
        @AuditData(key = "enabled", value = "#enable")
    })
    @PreAuthorize("hasPermission(#integrationInstanceId, 'IntegrationInstance', 'CONNECTED_USER_MANAGE') " +
        "or isConnectedUser()")
    public void enableIntegrationInstanceWorkflow(long integrationInstanceId, String workflowId, boolean enable) {
        validateCurrentPrincipalIntegrationInstanceOwnership(integrationInstanceId);

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            integrationInstanceId);

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(integrationInstance.getConnectedUserId());

        long integrationInstanceWorkflowId;

        if (connectedUser.isEnabled()) {
            integrationInstanceWorkflowId = updateIntegrationInstanceWorkflowTriggers(
                integrationInstanceId, workflowId, enable);
        } else {
            IntegrationInstanceWorkflow integrationInstanceWorkflow =
                integrationInstanceWorkflowService
                    .fetchIntegrationInstanceWorkflow(integrationInstanceId, workflowId)
                    .orElseGet(() -> {
                        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                                integrationInstance.getIntegrationInstanceConfigurationId(), workflowId);

                        return integrationInstanceWorkflowService.createIntegrationInstanceWorkflow(
                            integrationInstanceId, integrationInstanceConfigurationWorkflow.getId());
                    });

            integrationInstanceWorkflowId = integrationInstanceWorkflow.getId();
        }

        integrationInstanceWorkflowService.updateEnabled(integrationInstanceWorkflowId, enable);
    }

    @Override
    @PreAuthorize("hasPermission(#integrationInstanceId, 'IntegrationInstance', 'CONNECTED_USER_MANAGE')")
    public void enableIntegrationInstanceWorkflowTriggers(long integrationInstanceId, boolean enable) {
        List<IntegrationInstanceWorkflow> integrationInstanceWorkflows = integrationInstanceWorkflowService
            .getIntegrationInstanceWorkflows(integrationInstanceId);

        for (IntegrationInstanceWorkflow integrationInstanceWorkflow : integrationInstanceWorkflows) {
            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                    integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId());

            if (!integrationInstanceConfigurationWorkflow.isEnabled() || !integrationInstanceWorkflow.isEnabled()) {
                continue;
            }

            if (enable) {
                enableWorkflowTriggers(integrationInstanceWorkflow, integrationInstanceConfigurationWorkflow);
            } else {
                disableWorkflowTriggers(integrationInstanceWorkflow, integrationInstanceConfigurationWorkflow);
            }
        }
    }

    @Override
    @PreAuthorize("hasPermission(#integrationInstanceId, 'IntegrationInstance', 'CONNECTED_USER_MANAGE')")
    public long enableIntegrationInstanceWorkflowTriggers(
        long integrationInstanceId, String workflowId, boolean enable) {

        return updateIntegrationInstanceWorkflowTriggers(integrationInstanceId, workflowId, enable);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#id, 'IntegrationInstance', 'CONNECTED_USER_VIEW')")
    public IntegrationInstanceDTO getIntegrationInstance(long id) {
        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(id);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(integrationInstance.getIntegrationInstanceConfigurationId());

        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                integrationInstance.getIntegrationInstanceConfigurationId());

        return new IntegrationInstanceDTO(
            integrationInstance, integrationInstanceConfiguration,
            integrationInstanceWorkflowService.getIntegrationInstanceWorkflows(id)
                .stream()
                .map(integrationInstanceWorkflow -> {
                    String workflowId = CollectionUtils.getFirstFilter(
                        integrationInstanceConfigurationWorkflows,
                        integrationInstanceConfigurationWorkflow -> Objects.equals(
                            integrationInstanceConfigurationWorkflow.getId(),
                            integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId()),
                        IntegrationInstanceConfigurationWorkflow::getWorkflowId);

                    return new IntegrationInstanceWorkflowDTO(
                        integrationInstanceWorkflow, getIntegrationInstanceWorkflowLastExecutionDate(id, workflowId),
                        workflowId, getStaticWebhookUrl(id, workflowId));
                })
                .collect(Collectors.toSet()),
            getIntegrationInstanceLastExecutionDate(integrationInstance.getId()));
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Transactional(propagation = Propagation.SUPPORTS)
    public ConnectionDTO getIntegrationInstanceConnection(long integrationInstanceId) {
        IntegrationInstance integrationInstance = requireProvisioned(integrationInstanceId);

        long connectionId = integrationInstance.getConnectionId();

        return connectedUserConnectionFacade
            .getConnectedUserConnectionsForAdmin(
                integrationInstance.getConnectedUserId(), null, List.of(connectionId))
            .stream()
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException(
                "Integration instance id=%s not found".formatted(integrationInstanceId)));
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<ProvisionableIntegrationDTO> getProvisionableIntegrations(long connectedUserId) {
        ConnectedUserIntegrationFacade connectedUserIntegrationFacade =
            connectedUserIntegrationFacadeProvider.getObject();

        return connectedUserIntegrationFacade.getProvisionableIntegrations(connectedUserId);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Transactional(propagation = Propagation.SUPPORTS)
    public long provisionIntegrationInstance(
        long connectedUserId, long integrationInstanceConfigurationId, Map<String, Object> connectionParameters,
        List<ProvisionIntegrationInstanceWorkflowDTO> workflows) {

        ConnectedUserIntegrationFacade connectedUserIntegrationFacade =
            connectedUserIntegrationFacadeProvider.getObject();

        return connectedUserIntegrationFacade.provisionIntegrationInstance(
            connectedUserId, integrationInstanceConfigurationId, connectionParameters, workflows,
            SecurityUtils.getCurrentUserLogin());
    }

    @Override
    @PreAuthorize("hasPermission(#integrationInstanceConfigurationWorkflow.integrationInstanceConfigurationId, " +
        "'IntegrationInstanceConfiguration', 'INTEGRATION_INSTANCE_CONFIGURATION_EDIT')")
    public void updateIntegrationInstanceConfigurationWorkflowTriggers(
        IntegrationInstanceConfigurationWorkflow previousIntegrationInstanceConfigurationWorkflow,
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow) {

        List<IntegrationInstanceWorkflow> updatedIntegrationInstanceWorkflows = new ArrayList<>();

        for (IntegrationInstanceWorkflow integrationInstanceWorkflow : getActiveIntegrationInstanceWorkflows(
            previousIntegrationInstanceConfigurationWorkflow)) {

            try {
                if (previousIntegrationInstanceConfigurationWorkflow.isEnabled()) {
                    disableWorkflowTriggers(
                        integrationInstanceWorkflow, previousIntegrationInstanceConfigurationWorkflow);
                }

                if (integrationInstanceConfigurationWorkflow.isEnabled()) {
                    enableWorkflowTriggers(integrationInstanceWorkflow, integrationInstanceConfigurationWorkflow);
                }
            } catch (RuntimeException exception) {
                restoreWorkflowTriggers(
                    integrationInstanceWorkflow, updatedIntegrationInstanceWorkflows,
                    previousIntegrationInstanceConfigurationWorkflow, integrationInstanceConfigurationWorkflow,
                    exception);

                throw exception;
            }

            updatedIntegrationInstanceWorkflows.add(integrationInstanceWorkflow);
        }
    }

    @Override
    @Audited(event = IntegrationAuditEvents.INTEGRATION_INSTANCE_CONNECTION_UPDATED, data = {
        @AuditData(key = "integrationInstanceId", value = "#integrationInstanceId"),
        @AuditData(key = "connectionId", value = "#connectionId")
    })
    @PreAuthorize("isTenantAdmin()")
    @Transactional(propagation = Propagation.SUPPORTS)
    public void updateIntegrationInstanceConnection(long integrationInstanceId, long connectionId) {
        IntegrationInstance integrationInstance = requireProvisioned(integrationInstanceId);

        long previousConnectionId = integrationInstance.getConnectionId();

        if (previousConnectionId == connectionId) {
            return;
        }

        validateIntegrationInstanceConnection(integrationInstance, connectionId);

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(integrationInstance.getConnectedUserId());

        if (!integrationInstance.isEnabled() || !connectedUser.isEnabled()) {
            integrationInstanceService.updateConnectionId(integrationInstanceId, connectionId);

            integrationInstanceConnectionCleanup.keepForConnectedUser(connectedUser.getId(), previousConnectionId);

            return;
        }

        List<IntegrationInstanceWorkflow> enabledIntegrationInstanceWorkflows =
            getEnabledIntegrationInstanceWorkflows(integrationInstanceId);

        disableWorkflowTriggers(enabledIntegrationInstanceWorkflows);

        integrationInstanceService.updateConnectionId(integrationInstanceId, connectionId);

        List<IntegrationInstanceWorkflow> restartedIntegrationInstanceWorkflows = new ArrayList<>();

        for (IntegrationInstanceWorkflow integrationInstanceWorkflow : enabledIntegrationInstanceWorkflows) {
            try {
                enableWorkflowTriggers(integrationInstanceWorkflow);
            } catch (RuntimeException exception) {
                restorePreviousConnection(
                    integrationInstanceId, previousConnectionId, enabledIntegrationInstanceWorkflows,
                    restartedIntegrationInstanceWorkflows, exception);

                throw exception;
            }

            restartedIntegrationInstanceWorkflows.add(integrationInstanceWorkflow);
        }

        integrationInstanceConnectionCleanup.keepForConnectedUser(connectedUser.getId(), previousConnectionId);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Transactional(propagation = Propagation.SUPPORTS)
    public void updateIntegrationInstanceCredentials(
        long integrationInstanceId, Map<String, ?> parameters, int version) {

        IntegrationInstance integrationInstance = requireProvisioned(integrationInstanceId);

        integrationInstanceCredentialsUpdater.updateCredentials(
            integrationInstanceId, integrationInstance.getConnectedUserId(), integrationInstance.getConnectionId(),
            parameters, version);
    }

    @Override
    @PreAuthorize("hasPermission(#integrationInstanceId, 'IntegrationInstance', 'CONNECTED_USER_MANAGE') " +
        "or isConnectedUser()")
    public void updateIntegrationInstanceWorkflow(
        long integrationInstanceId, String workflowId, Map<String, Object> inputs) {

        validateCurrentPrincipalIntegrationInstanceOwnership(integrationInstanceId);

        IntegrationInstanceWorkflow integrationInstanceWorkflow =
            integrationInstanceWorkflowService.fetchIntegrationInstanceWorkflow(integrationInstanceId, workflowId)
                .orElseGet(() -> {
                    IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
                        integrationInstanceId);

                    IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                        integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                            integrationInstance.getIntegrationInstanceConfigurationId(), workflowId);

                    return integrationInstanceWorkflowService.createIntegrationInstanceWorkflow(
                        integrationInstanceId, integrationInstanceConfigurationWorkflow.getId());
                });

// TODO Don’t validate inputs as they are currently entered one by one in ConnectDialog, check if it is possible to
// change the logic and send the all inputs at once
//        Workflow workflow = workflowService.getWorkflow(workflowId);
//        validateInputs(inputs, workflow);

        integrationInstanceWorkflow.setInputs(
            InternalOnlyInputUtils.removeInternalOnlyInputs(
                workflowService.getWorkflow(workflowId), inputs == null ? Map.of() : inputs));

        integrationInstanceWorkflowService.update(integrationInstanceWorkflow);
    }

    private IntegrationInstance requireProvisioned(long integrationInstanceId) {
        Optional<IntegrationInstance> integrationInstanceOptional = integrationInstanceService.fetchIntegrationInstance(
            integrationInstanceId);

        return integrationInstanceOptional
            .filter(integrationInstance -> integrationInstance.getProvisionedBy() != null)
            .orElseThrow(() -> new NoSuchElementException(
                "Integration instance id=%s not found".formatted(integrationInstanceId)));
    }

    private void disableWorkflowTriggers(List<IntegrationInstanceWorkflow> integrationInstanceWorkflows) {
        List<IntegrationInstanceWorkflow> disabledIntegrationInstanceWorkflows = new ArrayList<>();

        for (IntegrationInstanceWorkflow integrationInstanceWorkflow : integrationInstanceWorkflows) {
            try {
                disableWorkflowTriggers(integrationInstanceWorkflow);
            } catch (RuntimeException exception) {
                for (IntegrationInstanceWorkflow disabledIntegrationInstanceWorkflow : disabledIntegrationInstanceWorkflows) {

                    try {
                        enableWorkflowTriggers(disabledIntegrationInstanceWorkflow);
                    } catch (RuntimeException enableException) {
                        exception.addSuppressed(enableException);
                    }
                }

                throw exception;
            }

            disabledIntegrationInstanceWorkflows.add(integrationInstanceWorkflow);
        }
    }

    private void disableWorkflowTriggers(IntegrationInstanceWorkflow integrationInstanceWorkflow) {
        disableWorkflowTriggers(
            integrationInstanceWorkflow,
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId()));
    }

    private void disableWorkflowTriggers(
        IntegrationInstanceWorkflow integrationInstanceWorkflow,
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow) {

        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId());

        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.getWorkflowIntegrationWorkflow(
            integrationInstanceConfigurationWorkflow.getWorkflowId());

        Workflow workflow = workflowService.getWorkflow(integrationWorkflow.getWorkflowId());

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.ofEnabled(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                PlatformType.EMBEDDED, integrationInstanceWorkflow.getIntegrationInstanceId(),
                integrationWorkflow.getUuidAsString(), workflowTrigger.getName());

            triggerLifecycleFacade.executeTriggerDisable(
                workflow.getId(), workflowExecutionId, WorkflowNodeType.ofType(workflowTrigger.getType()),
                workflowTrigger.evaluateParameters(
                    IntegrationWorkflowInputUtils.mergeInputs(
                        workflow, integrationInstanceConfigurationWorkflow.getInputs(),
                        integrationInstanceWorkflow.getInputs()),
                    evaluator),
                getConnectionId(
                    integrationInstanceConfiguration.getIntegrationId(), integrationInstanceConfigurationWorkflow,
                    integrationInstanceWorkflow.getIntegrationInstanceId(), workflowTrigger));
        }
    }

    private void enableWorkflowTriggers(IntegrationInstanceWorkflow integrationInstanceWorkflow) {
        enableWorkflowTriggers(
            integrationInstanceWorkflow,
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId()));
    }

    private void enableWorkflowTriggers(
        IntegrationInstanceWorkflow integrationInstanceWorkflow,
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow) {

        Workflow workflow = workflowService.getWorkflow(integrationInstanceConfigurationWorkflow.getWorkflowId());

// TODO Don’t validate inputs as they are currently entered one by one in ConnectDialog, check if it is possible to
// change the logic and send the all inputs at once
//        validateInputs(integrationInstanceWorkflow.getInputs(), workflow);

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.ofEnabled(workflow);
        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId());
        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.getWorkflowIntegrationWorkflow(
            workflow.getId());

        List<WorkflowTrigger> enabledWorkflowTriggers = new ArrayList<>();

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                PlatformType.EMBEDDED, integrationInstanceWorkflow.getIntegrationInstanceId(),
                integrationWorkflow.getUuidAsString(), workflowTrigger.getName());

            try {
                triggerLifecycleFacade.executeTriggerEnable(
                    workflow.getId(), workflowExecutionId, WorkflowNodeType.ofType(workflowTrigger.getType()),
                    workflowTrigger.evaluateParameters(
                        IntegrationWorkflowInputUtils.mergeInputs(
                            workflow, integrationInstanceConfigurationWorkflow.getInputs(),
                            integrationInstanceWorkflow.getInputs()),
                        evaluator),
                    getConnectionId(
                        integrationInstanceConfiguration.getIntegrationId(), integrationInstanceConfigurationWorkflow,
                        integrationInstanceWorkflow.getIntegrationInstanceId(), workflowTrigger),
                    getWebhookUrl(workflowExecutionId), integrationInstanceConfiguration.getEnvironmentId());
            } catch (RuntimeException exception) {
                disableEnabledWorkflowTriggers(
                    integrationInstanceWorkflow, integrationInstanceConfigurationWorkflow,
                    integrationInstanceConfiguration, integrationWorkflow, workflow, enabledWorkflowTriggers,
                    exception);

                throw exception;
            }

            enabledWorkflowTriggers.add(workflowTrigger);
        }
    }

    private void disableEnabledWorkflowTriggers(
        IntegrationInstanceWorkflow integrationInstanceWorkflow,
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow,
        IntegrationInstanceConfiguration integrationInstanceConfiguration, IntegrationWorkflow integrationWorkflow,
        Workflow workflow, List<WorkflowTrigger> enabledWorkflowTriggers, RuntimeException enableFailure) {

        for (WorkflowTrigger enabledWorkflowTrigger : enabledWorkflowTriggers) {
            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                PlatformType.EMBEDDED, integrationInstanceWorkflow.getIntegrationInstanceId(),
                integrationWorkflow.getUuidAsString(), enabledWorkflowTrigger.getName());

            try {
                triggerLifecycleFacade.executeTriggerDisable(
                    workflow.getId(), workflowExecutionId, WorkflowNodeType.ofType(enabledWorkflowTrigger.getType()),
                    enabledWorkflowTrigger.evaluateParameters(
                        IntegrationWorkflowInputUtils.mergeInputs(
                            workflow, integrationInstanceConfigurationWorkflow.getInputs(),
                            integrationInstanceWorkflow.getInputs()),
                        evaluator),
                    getConnectionId(
                        integrationInstanceConfiguration.getIntegrationId(), integrationInstanceConfigurationWorkflow,
                        integrationInstanceWorkflow.getIntegrationInstanceId(), enabledWorkflowTrigger));
            } catch (RuntimeException exception) {
                enableFailure.addSuppressed(exception);
            }
        }
    }

    private List<IntegrationInstanceWorkflow> getActiveIntegrationInstanceWorkflows(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow) {

        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId());

        if (!integrationInstanceConfiguration.isEnabled()) {
            return List.of();
        }

        List<IntegrationInstanceWorkflow> activeIntegrationInstanceWorkflows = new ArrayList<>();

        for (IntegrationInstance integrationInstance : integrationInstanceService.getIntegrationInstances(
            integrationInstanceConfiguration.getId())) {

            if (!integrationInstance.isEnabled()) {
                continue;
            }

            ConnectedUser connectedUser = connectedUserService.getConnectedUser(
                integrationInstance.getConnectedUserId());

            if (!connectedUser.isEnabled()) {
                continue;
            }

            for (IntegrationInstanceWorkflow integrationInstanceWorkflow : integrationInstanceWorkflowService
                .getIntegrationInstanceWorkflows(integrationInstance.getId())) {

                if (integrationInstanceWorkflow.isEnabled() && Objects.equals(
                    integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId(),
                    integrationInstanceConfigurationWorkflow.getId())) {

                    activeIntegrationInstanceWorkflows.add(integrationInstanceWorkflow);
                }
            }
        }

        return activeIntegrationInstanceWorkflows;
    }

    private List<IntegrationInstanceWorkflow> getEnabledIntegrationInstanceWorkflows(long integrationInstanceId) {
        List<IntegrationInstanceWorkflow> enabledIntegrationInstanceWorkflows = new ArrayList<>();

        for (IntegrationInstanceWorkflow integrationInstanceWorkflow : integrationInstanceWorkflowService
            .getIntegrationInstanceWorkflows(integrationInstanceId)) {

            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                    integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId());

            if (integrationInstanceConfigurationWorkflow.isEnabled() && integrationInstanceWorkflow.isEnabled()) {
                enabledIntegrationInstanceWorkflows.add(integrationInstanceWorkflow);
            }
        }

        return enabledIntegrationInstanceWorkflows;
    }

    private Long getConnectionId(
        long integrationId, IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow,
        long integrationInstanceId, WorkflowTrigger workflowTrigger) {

        Integration integration = integrationService.getIntegration(integrationId);

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

        if (Objects.equals(integration.getComponentName(), workflowNodeType.name())) {
            IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
                integrationInstanceId);

            return integrationInstance.getConnectionId();
        }

        return componentConnectionFacade
            .getComponentConnections(workflowTrigger)
            .stream()
            .findFirst()
            .flatMap(workflowConnection -> getConnectionId(
                integrationInstanceConfigurationWorkflow, workflowConnection.workflowNodeName(),
                workflowConnection.key()))
            .orElse(null);
    }

    private static Optional<Long> getConnectionId(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow, String workflowNodeName,
        String workflowConnectionKey) {

        return integrationInstanceConfigurationWorkflow.getConnections()
            .stream()
            .filter(integrationInstanceConfigurationWorkflowConnection -> Objects.equals(
                integrationInstanceConfigurationWorkflowConnection.getWorkflowNodeName(), workflowNodeName) &&
                Objects.equals(
                    integrationInstanceConfigurationWorkflowConnection.getWorkflowConnectionKey(),
                    workflowConnectionKey))
            .findFirst()
            .map(IntegrationInstanceConfigurationWorkflowConnection::getConnectionId);
    }

    private Instant getIntegrationInstanceLastExecutionDate(long integrationInstanceId) {
        return OptionalUtils.mapOrElse(
            principalJobService.fetchLastJobId(integrationInstanceId, PlatformType.EMBEDDED), this::getJobEndDate,
            null);
    }

    private @Nullable Instant getIntegrationInstanceWorkflowLastExecutionDate(
        long integrationInstanceId, @Nullable String workflowId) {

        if (workflowId == null) {
            return null;
        }

        return OptionalUtils.mapOrElse(
            principalJobService.fetchLastWorkflowJobId(integrationInstanceId, List.of(workflowId),
                PlatformType.EMBEDDED),
            this::getJobEndDate, null);
    }

    private Instant getJobEndDate(Long jobId) {
        Job job = jobService.getJob(jobId);

        return job.getEndDate();
    }

    private String getWebhookUrl(WorkflowExecutionId workflowExecutionId) {
        return webhookUrl.replace("{id}", workflowExecutionId.toString());
    }

    private long updateIntegrationInstanceWorkflowTriggers(
        long integrationInstanceId, String workflowId, boolean enable) {

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            integrationInstanceId);

        IntegrationInstanceWorkflow integrationInstanceWorkflow =
            integrationInstanceWorkflowService
                .fetchIntegrationInstanceWorkflow(integrationInstanceId, workflowId)
                .orElseGet(() -> {
                    IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                        integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                            integrationInstance.getIntegrationInstanceConfigurationId(), workflowId);

                    return integrationInstanceWorkflowService.createIntegrationInstanceWorkflow(
                        integrationInstanceId, integrationInstanceConfigurationWorkflow.getId());
                });

        if (integrationInstance.isEnabled()) {
            if (enable) {
                enableWorkflowTriggers(integrationInstanceWorkflow);
            } else {
                disableWorkflowTriggers(integrationInstanceWorkflow);
            }
        }

        return integrationInstanceWorkflow.getId();
    }

    private void restoreWorkflowTriggers(
        IntegrationInstanceWorkflow failedIntegrationInstanceWorkflow,
        List<IntegrationInstanceWorkflow> updatedIntegrationInstanceWorkflows,
        IntegrationInstanceConfigurationWorkflow previousIntegrationInstanceConfigurationWorkflow,
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow, RuntimeException exception) {

        for (IntegrationInstanceWorkflow updatedIntegrationInstanceWorkflow : updatedIntegrationInstanceWorkflows) {
            if (integrationInstanceConfigurationWorkflow.isEnabled()) {
                try {
                    disableWorkflowTriggers(
                        updatedIntegrationInstanceWorkflow, integrationInstanceConfigurationWorkflow);
                } catch (RuntimeException disableException) {
                    exception.addSuppressed(disableException);
                }
            }

            restorePreviousWorkflowTriggers(
                updatedIntegrationInstanceWorkflow, previousIntegrationInstanceConfigurationWorkflow, exception);
        }

        restorePreviousWorkflowTriggers(
            failedIntegrationInstanceWorkflow, previousIntegrationInstanceConfigurationWorkflow, exception);
    }

    private void restorePreviousWorkflowTriggers(
        IntegrationInstanceWorkflow integrationInstanceWorkflow,
        IntegrationInstanceConfigurationWorkflow previousIntegrationInstanceConfigurationWorkflow,
        RuntimeException exception) {

        if (!previousIntegrationInstanceConfigurationWorkflow.isEnabled()) {
            return;
        }

        try {
            enableWorkflowTriggers(integrationInstanceWorkflow, previousIntegrationInstanceConfigurationWorkflow);
        } catch (RuntimeException enableException) {
            exception.addSuppressed(enableException);
        }
    }

    private void restorePreviousConnection(
        long integrationInstanceId, long previousConnectionId,
        List<IntegrationInstanceWorkflow> enabledIntegrationInstanceWorkflows,
        List<IntegrationInstanceWorkflow> restartedIntegrationInstanceWorkflows, RuntimeException restartFailure) {

        for (IntegrationInstanceWorkflow integrationInstanceWorkflow : restartedIntegrationInstanceWorkflows) {
            try {
                disableWorkflowTriggers(integrationInstanceWorkflow);
            } catch (RuntimeException exception) {
                restartFailure.addSuppressed(exception);
            }
        }

        integrationInstanceService.updateConnectionId(integrationInstanceId, previousConnectionId);

        for (IntegrationInstanceWorkflow integrationInstanceWorkflow : enabledIntegrationInstanceWorkflows) {
            try {
                enableWorkflowTriggers(integrationInstanceWorkflow);
            } catch (RuntimeException exception) {
                restartFailure.addSuppressed(exception);
            }
        }
    }

    private void validateCurrentPrincipalIntegrationInstanceOwnership(long integrationInstanceId) {
        ConnectedUserIntegrationInstanceFacade connectedUserIntegrationInstanceFacade =
            connectedUserIntegrationInstanceFacadeProvider.getObject();

        connectedUserIntegrationInstanceFacade.validateCurrentPrincipalIntegrationInstanceOwnership(
            integrationInstanceId);
    }

    private void validateIntegrationInstanceConnection(IntegrationInstance integrationInstance, long connectionId) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(integrationInstance.getIntegrationInstanceConfigurationId());

        Integration integration = integrationService.getIntegration(
            integrationInstanceConfiguration.getIntegrationId());

        List<ConnectionDTO> connectionDTOs = connectedUserConnectionFacade.getConnectedUserConnectionsForAdmin(
            integrationInstance.getConnectedUserId(), integration.getComponentName(), List.of(connectionId));

        Set<Long> ownedConnectionIds = connectedUserConnectionFacade.getOwnedConnectionIds(
            integrationInstance.getConnectedUserId());

        boolean connectionValid = ownedConnectionIds.contains(connectionId) && connectionDTOs.stream()
            .anyMatch(connectionDTO -> Objects.equals(connectionDTO.id(), connectionId) &&
                connectionDTO.environmentId() == integrationInstanceConfiguration.getEnvironmentId());

        if (!connectionValid) {
            throw new IllegalArgumentException(
                "Connection id=%s is not a %s connection of connected user id=%s in environment=%s".formatted(
                    connectionId, integration.getComponentName(), integrationInstance.getConnectedUserId(),
                    integrationInstanceConfiguration.getEnvironment()));
        }
    }

    private @Nullable String getStaticWebhookUrl(long integrationInstanceId, @Nullable String workflowId) {
        ConnectedUserWorkflowWebhookUrlResolver connectedUserWorkflowWebhookUrlResolver =
            connectedUserWorkflowWebhookUrlResolverProvider.getIfAvailable();

        if (connectedUserWorkflowWebhookUrlResolver == null || workflowId == null) {
            return null;
        }

        Optional<IntegrationWorkflow> integrationWorkflowOptional =
            integrationWorkflowService.fetchWorkflowIntegrationWorkflow(workflowId);

        if (integrationWorkflowOptional.isEmpty()) {
            return null;
        }

        IntegrationWorkflow integrationWorkflow = integrationWorkflowOptional.get();

        Optional<Workflow> workflowOptional = workflowService.fetchWorkflow(integrationWorkflow.getWorkflowId());

        if (workflowOptional.isEmpty()) {
            return null;
        }

        return connectedUserWorkflowWebhookUrlResolver.resolveIntegrationInstanceWorkflow(
            integrationInstanceId, integrationWorkflow.getUuidAsString(), workflowOptional.get());
    }

// TODO Don’t validate inputs as they are currently entered one by one in ConnectDialog, check if it is possible to
// change the logic and send the all inputs at once
//    private void validateInputs(Map<String, ?> inputs, Workflow workflow) {
//        for (Workflow.Input input : workflow.getInputs()) {
//            if (input.required()) {
//                Validate.isTrue(inputs.containsKey(input.name()), "Missing required param: " + input.name());
//                Validate.notEmpty((String) inputs.get(input.name()), "Missing required param: " + input.name());
//            }
//        }
//    }
}
