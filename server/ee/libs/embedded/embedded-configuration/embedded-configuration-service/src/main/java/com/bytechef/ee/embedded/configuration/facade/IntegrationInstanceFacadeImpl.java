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
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceWorkflowDTO;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
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

    private final ObjectProvider<ConnectedUserIntegrationInstanceFacade> connectedUserIntegrationInstanceFacadeProvider;
    private final ConnectedUserService connectedUserService;
    private final Evaluator evaluator;
    private final PrincipalJobService principalJobService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
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
        ApplicationProperties applicationProperties,
        ObjectProvider<ConnectedUserIntegrationInstanceFacade> connectedUserIntegrationInstanceFacadeProvider,
        ConnectedUserService connectedUserService, Evaluator evaluator,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService,
        IntegrationInstanceService integrationInstanceService, IntegrationService integrationService,
        IntegrationWorkflowService integrationWorkflowService, JobService jobService,
        PrincipalJobService principalJobService, TriggerLifecycleFacade triggerLifecycleFacade,
        ComponentConnectionFacade componentConnectionFacade, WorkflowService workflowService) {

        this.componentConnectionFacade = componentConnectionFacade;
        this.connectedUserIntegrationInstanceFacadeProvider = connectedUserIntegrationInstanceFacadeProvider;
        this.connectedUserService = connectedUserService;
        this.evaluator = evaluator;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
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
    @PreAuthorize("isTenantAdmin()")
    public void deleteIntegrationInstance(long integrationInstanceId) {
        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            integrationInstanceId);

        if (integrationInstance.isEnabled()) {
            enableIntegrationInstanceWorkflowTriggers(integrationInstanceId, false);
        }

        integrationInstanceWorkflowService.deleteByIntegrationInstanceId(integrationInstanceId);

        integrationInstanceService.delete(integrationInstanceId);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
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
    @PreAuthorize("isTenantAdmin() or @connectedUserSecurity.isConnectedUser()")
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
    @PreAuthorize("isTenantAdmin()")
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
    @PreAuthorize("isTenantAdmin()")
    public long enableIntegrationInstanceWorkflowTriggers(
        long integrationInstanceId, String workflowId, boolean enable) {

        return updateIntegrationInstanceWorkflowTriggers(integrationInstanceId, workflowId, enable);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isTenantAdmin()")
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
                        workflowId);
                })
                .collect(Collectors.toSet()),
            getIntegrationInstanceLastExecutionDate(integrationInstance.getId()));
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
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
    @PreAuthorize("isTenantAdmin() or @connectedUserSecurity.isConnectedUser()")
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

        integrationInstanceWorkflow.setInputs(inputs);

        integrationInstanceWorkflowService.update(integrationInstanceWorkflow);
    }

    private void disableWorkflowTriggers(IntegrationInstanceWorkflow integrationInstanceWorkflow) {
        disableWorkflowTriggers(
            integrationInstanceWorkflow,
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId()));
    }

    @SuppressWarnings("unchecked")
    private void disableWorkflowTriggers(
        IntegrationInstanceWorkflow integrationInstanceWorkflow,
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow) {

        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId());

        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.getWorkflowIntegrationWorkflow(
            integrationInstanceConfigurationWorkflow.getWorkflowId());

        Workflow workflow = workflowService.getWorkflow(integrationWorkflow.getWorkflowId());

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                PlatformType.EMBEDDED, integrationInstanceWorkflow.getIntegrationInstanceId(),
                integrationWorkflow.getUuidAsString(), workflowTrigger.getName());

            triggerLifecycleFacade.executeTriggerDisable(
                workflow.getId(), workflowExecutionId, WorkflowNodeType.ofType(workflowTrigger.getType()),
                workflowTrigger.evaluateParameters(
                    MapUtils.concat(
                        (Map<String, Object>) integrationInstanceConfigurationWorkflow.getInputs(),
                        (Map<String, Object>) integrationInstanceWorkflow.getInputs()),
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

    @SuppressWarnings("unchecked")
    private void enableWorkflowTriggers(
        IntegrationInstanceWorkflow integrationInstanceWorkflow,
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow) {

        Workflow workflow = workflowService.getWorkflow(integrationInstanceConfigurationWorkflow.getWorkflowId());

// TODO Don’t validate inputs as they are currently entered one by one in ConnectDialog, check if it is possible to
// change the logic and send the all inputs at once
//        validateInputs(integrationInstanceWorkflow.getInputs(), workflow);

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);
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
                        MapUtils.concat(
                            (Map<String, Object>) integrationInstanceConfigurationWorkflow.getInputs(),
                            (Map<String, Object>) integrationInstanceWorkflow.getInputs()),
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

    @SuppressWarnings("unchecked")
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
                        MapUtils.concat(
                            (Map<String, Object>) integrationInstanceConfigurationWorkflow.getInputs(),
                            (Map<String, Object>) integrationInstanceWorkflow.getInputs()),
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

    private void validateCurrentPrincipalIntegrationInstanceOwnership(long integrationInstanceId) {
        ConnectedUserIntegrationInstanceFacade connectedUserIntegrationInstanceFacade =
            connectedUserIntegrationInstanceFacadeProvider.getObject();

        connectedUserIntegrationInstanceFacade.validateCurrentPrincipalIntegrationInstanceOwnership(
            integrationInstanceId);
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
