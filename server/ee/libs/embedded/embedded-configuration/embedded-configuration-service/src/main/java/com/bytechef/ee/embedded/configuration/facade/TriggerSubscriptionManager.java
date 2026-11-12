/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.dto.ProjectDeploymentWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.security.SkipAutomationAuthorization;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionDisableReason;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionErrorCode;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionStatus;
import com.bytechef.ee.embedded.configuration.exception.TriggerSubscriptionEnableException;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.TriggerSubscriptionService;
import com.bytechef.ee.embedded.configuration.service.TriggerSubscriptionService.RegistrationLeaseExtension;
import com.bytechef.ee.embedded.configuration.trigger.subscription.TriggerSubscriptionEventSender;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.AbstractException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.tenant.concurrent.TenantTaskDecorator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@SkipAutomationAuthorization
public class TriggerSubscriptionManager implements DisposableBean {

    private static final Set<Integer> CREDENTIAL_REFRESH_FAILED_ERROR_KEYS = Set.of(101, 105, 106);
    private static final Set<Class<?>> COMPONENT_DEFINITION_CLASSES = Set.of(
        ActionDefinition.class, ClusterElementDefinition.class, ConnectionDefinition.class, TriggerDefinition.class);
    private static final Duration DEFAULT_REGISTRATION_LEASE = Duration.ofMinutes(5);
    private static final String DELETE_ROLLED_BACK_ERROR_MESSAGE = "Trigger unregistered; delete was rolled back";
    private static final int ERROR_MESSAGE_MAX_LENGTH = 1024;
    private static final String INTERNAL_ERROR_MESSAGE = "Internal error while enabling the trigger subscription";
    private static final String MISSING_CONNECTIONS_ERROR_MESSAGE =
        "Not all required connections are set for the trigger subscription";
    private static final String REGISTRATION_LEASE_PROPERTY =
        "bytechef.embedded.trigger-subscription.registration-lease";
    private static final String REGISTRATION_LEASE_RENEWAL_THREAD_NAME = "trigger-subscription-lease-renewal";
    private static final String SUBSCRIPTION_DELETED_EVENT_TYPE = "subscription.deleted";
    private static final String SUBSCRIPTION_DISABLED_EVENT_TYPE = "subscription.disabled";
    private static final Set<Class<? extends Throwable>> TIMEOUT_EXCEPTION_CLASSES = Set.of(
        HttpTimeoutException.class, SocketTimeoutException.class, TimeoutException.class);
    private static final int WORKFLOW_CONNECTIONS_NOT_FOUND_ERROR_KEY = 101;

    private static final Logger log = LoggerFactory.getLogger(TriggerSubscriptionManager.class);

    private final ConnectedUserProjectService connectedUserProjectService;
    private final ConnectedUserService connectedUserService;
    private final ConnectionService connectionService;
    private final IntegrationInstanceService integrationInstanceService;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectFacade projectFacade;
    private final ProjectService projectService;
    private final ProjectWorkflowFacade projectWorkflowFacade;
    private final ProjectWorkflowService projectWorkflowService;
    private final Duration registrationLease;
    private final ScheduledExecutorService registrationLeaseRenewalExecutor;
    private final TransactionTemplate requiresNewTransactionTemplate;
    private final TransactionTemplate transactionTemplate;
    private final TriggerDefinitionService triggerDefinitionService;
    private final TriggerSubscriptionEventSender triggerSubscriptionEventSender;
    private final TriggerSubscriptionService triggerSubscriptionService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public TriggerSubscriptionManager(
        ApplicationProperties applicationProperties, ConnectedUserProjectService connectedUserProjectService,
        ConnectedUserService connectedUserService, ConnectionService connectionService,
        IntegrationInstanceService integrationInstanceService, ProjectDeploymentFacade projectDeploymentFacade,
        ProjectDeploymentService projectDeploymentService, ProjectFacade projectFacade, ProjectService projectService,
        ProjectWorkflowFacade projectWorkflowFacade, ProjectWorkflowService projectWorkflowService,
        PlatformTransactionManager transactionManager, TriggerDefinitionService triggerDefinitionService,
        TriggerSubscriptionEventSender triggerSubscriptionEventSender,
        TriggerSubscriptionService triggerSubscriptionService, WorkflowService workflowService) {

        this.connectedUserProjectService = connectedUserProjectService;
        this.connectedUserService = connectedUserService;
        this.connectionService = connectionService;
        this.integrationInstanceService = integrationInstanceService;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectDeploymentService = projectDeploymentService;
        this.projectFacade = projectFacade;
        this.projectService = projectService;
        this.projectWorkflowFacade = projectWorkflowFacade;
        this.projectWorkflowService = projectWorkflowService;
        this.registrationLease = getRegistrationLease(applicationProperties);
        this.registrationLeaseRenewalExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, REGISTRATION_LEASE_RENEWAL_THREAD_NAME);

            thread.setDaemon(true);

            return thread;
        });

        TransactionTemplate requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);

        requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        this.requiresNewTransactionTemplate = requiresNewTransactionTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.triggerDefinitionService = triggerDefinitionService;
        this.triggerSubscriptionEventSender = triggerSubscriptionEventSender;
        this.triggerSubscriptionService = triggerSubscriptionService;
        this.workflowService = workflowService;
    }

    /**
     * Creates the subscription with its project and deployment, then registers its trigger. The connected user row is
     * locked while the subscriptions are counted against {@code maxTriggerSubscriptions} (0 for no limit) and the new
     * one is inserted, so concurrent creates of one connected user cannot exceed the limit. The lock is released when
     * that transaction commits, before the trigger is registered with the app under the registration lease, as
     * {@link #enable} does.
     */
    public TriggerSubscription create(
        ConnectedUser connectedUser, @Nullable Long integrationInstanceId, @Nullable Long connectionId,
        String componentName, int componentVersion, String triggerName, Map<String, ?> parameters,
        @Nullable String name, @Nullable String externalId, @Nullable String webhookUrl, int maxTriggerSubscriptions) {

        TriggerSubscriptionRules.checkTriggerExists(
            triggerDefinitionService, componentName, componentVersion, triggerName);

        TriggerSubscription triggerSubscription = Objects.requireNonNull(
            transactionTemplate.execute(transactionStatus -> {
                checkTriggerSubscriptionLimit(connectedUser, maxTriggerSubscriptions);

                TriggerSubscription newTriggerSubscription = new TriggerSubscription();

                newTriggerSubscription.setComponentName(componentName);
                newTriggerSubscription.setComponentVersion(componentVersion);
                newTriggerSubscription.setConnectedUserId(connectedUser.getId());
                newTriggerSubscription.setConnectionId(connectionId);
                newTriggerSubscription.setEnvironment(connectedUser.getEnvironment());
                newTriggerSubscription.setExternalId(externalId);
                newTriggerSubscription.setIntegrationInstanceId(integrationInstanceId);
                newTriggerSubscription.setName(name);
                newTriggerSubscription.setParameters(parameters);
                newTriggerSubscription.setStatus(TriggerSubscriptionStatus.FAILED);
                newTriggerSubscription.setTriggerName(triggerName);
                newTriggerSubscription.setWebhookUrl(webhookUrl);

                newTriggerSubscription = triggerSubscriptionService.create(newTriggerSubscription);

                return deploy(newTriggerSubscription, connectedUser.getExternalId());
            }));

        long id = Objects.requireNonNull(triggerSubscription.getId());

        Optional<TriggerSubscription> enabledTriggerSubscriptionOptional = enableExclusively(id);

        return enabledTriggerSubscriptionOptional
            .orElseGet(() -> triggerSubscriptionService.getTriggerSubscription(id));
    }

    /**
     * Unregisters the trigger of an enabled subscription with the app, then deletes the subscription with its project
     * and deployment. The remote call comes first because it needs the deployment the local delete removes; when the
     * local delete fails or is rolled back after it, the subscription is marked FAILED.
     */
    public void delete(long id) {
        TriggerSubscription triggerSubscription = triggerSubscriptionService.getTriggerSubscription(id);

        List<TriggerSubscription> unregisteredTriggerSubscriptions = new ArrayList<>();

        if (triggerSubscription.getStatus() == TriggerSubscriptionStatus.ENABLED) {
            unregisterTrigger(triggerSubscription);

            unregisteredTriggerSubscriptions.add(triggerSubscription);
        }

        deleteLocally(List.of(triggerSubscription), unregisteredTriggerSubscriptions, Set.of());
    }

    /**
     * Deletes the trigger subscriptions of a connected user inside the current transaction. A trigger that cannot be
     * unregistered with the app is logged and its subscription is deleted anyway. When the current transaction rolls
     * back, every subscription whose trigger was already unregistered is marked FAILED.
     */
    public void deleteByConnectedUserId(long connectedUserId) {
        deleteAll(
            triggerSubscriptionService.getConnectedUserTriggerSubscriptions(connectedUserId),
            TriggerSubscriptionDisableReason.CONNECTED_USER_DELETED);
    }

    /**
     * Deletes the trigger subscriptions of a connection inside the current transaction. A trigger that cannot be
     * unregistered with the app is logged and its subscription is deleted anyway. When the current transaction rolls
     * back, every subscription whose trigger was already unregistered is marked FAILED.
     */
    public void deleteByConnectionId(long connectionId) {
        deleteAll(
            triggerSubscriptionService.getConnectionTriggerSubscriptions(connectionId),
            TriggerSubscriptionDisableReason.CONNECTION_DELETED);
    }

    @Override
    public void destroy() {
        registrationLeaseRenewalExecutor.shutdownNow();
    }

    /**
     * Disables the subscription and unregisters its trigger in one transaction that holds the subscription row lock, so
     * it serializes with the status checks of {@link #update}.
     */
    public void disable(long id, @Nullable TriggerSubscriptionDisableReason reason) {
        transactionTemplate.executeWithoutResult(
            transactionStatus -> disable(triggerSubscriptionService.lockTriggerSubscription(id), reason));
    }

    /**
     * Disables the enabled trigger subscriptions of a connected user after the current transaction commits, or at once
     * when no transaction is active. Nothing is disabled when the current transaction rolls back.
     */
    public void disableByConnectedUserId(long connectedUserId, TriggerSubscriptionDisableReason reason) {
        runAfterCommit(
            () -> disableEnabled(
                () -> triggerSubscriptionService.getConnectedUserTriggerSubscriptions(connectedUserId), reason,
                "connected user id=" + connectedUserId));
    }

    /**
     * Disables the enabled trigger subscriptions of a connection at once. Call it only once the change that made the
     * connection unusable has committed: unlike the other bulk disables it does not wait for the current transaction.
     */
    public void disableByConnectionId(long connectionId, TriggerSubscriptionDisableReason reason) {
        disableEnabled(
            () -> triggerSubscriptionService.getConnectionTriggerSubscriptions(connectionId), reason,
            "connection id=" + connectionId);
    }

    /**
     * Disables the enabled trigger subscriptions of an integration instance after the current transaction commits, or
     * at once when no transaction is active. Nothing is disabled when the current transaction rolls back.
     */
    public void disableByIntegrationInstanceId(long integrationInstanceId, TriggerSubscriptionDisableReason reason) {
        runAfterCommit(
            () -> disableEnabled(
                () -> triggerSubscriptionService.getIntegrationInstanceTriggerSubscriptions(integrationInstanceId),
                reason, "integration instance id=" + integrationInstanceId));
    }

    /**
     * Registers the trigger of a subscription that is not ENABLED. A short transaction claims the registration lease of
     * the subscription under its row lock: when another request holds an unexpired lease, that request registers the
     * trigger and this one returns without touching it, so the trigger is never registered twice. Saving the lease
     * increments the {@code version}, and the version decides the outcome when a disable or an update changes the
     * subscription meanwhile, as described on registerTrigger. No transaction or connection is held while the trigger
     * is registered with the app. The holder renews the lease while its call to the app runs, so a call that outlasts
     * the lease keeps it. A lease whose holder died expires after
     * {@code bytechef.embedded.trigger-subscription.registration-lease}, and the next enable proceeds. A holder whose
     * lease was taken over all the same leaves the subscription to the new holder.
     */
    public void enable(long id) {
        enableExclusively(id);
    }

    @Nullable
    public String getStaticWebhookUrl(TriggerSubscription triggerSubscription) {
        Long projectDeploymentId = triggerSubscription.getProjectDeploymentId();

        if (projectDeploymentId == null || !isStaticWebhookTrigger(triggerSubscription)) {
            return null;
        }

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacade.getProjectDeployment(projectDeploymentId);

        List<ProjectDeploymentWorkflowDTO> projectDeploymentWorkflowDTOs =
            projectDeploymentDTO.projectDeploymentWorkflows();

        if (projectDeploymentWorkflowDTOs.isEmpty()) {
            return null;
        }

        ProjectDeploymentWorkflowDTO projectDeploymentWorkflowDTO = projectDeploymentWorkflowDTOs.getFirst();

        return projectDeploymentWorkflowDTO.staticWebhookUrl();
    }

    /**
     * Stores the new values of the subscription and redeploys its workflow. The trigger of an enabled subscription is
     * re-registered under the registration lease: the lease is claimed first, under the row lock, then the trigger is
     * unregistered, the values are stored and the workflow redeployed in a transaction that locks the row again, and
     * the trigger of the new values is registered. Another request may change the subscription in between, so each step
     * locks the row instead of trusting what was read at the start. The redeploy transaction keeps the current status;
     * the trigger is registered again only when the stored status is ENABLED.
     *
     * <p>
     * When another request holds the lease of an ENABLED subscription, it is registering the trigger of the previous
     * values. The update then neither unregisters nor registers the trigger: it stores the values and redeploys, which
     * changes the {@code version}. The final write of the lease holder sees the subscription still ENABLED with another
     * version while it holds the lease, so it renews the lease, unregisters the trigger and registers the trigger of
     * the new values, leaving exactly one registration, of the new values. A failed update registers the trigger of the
     * stored values again, under its lease, unless a disable or a delete took the subscription meanwhile.
     */
    public TriggerSubscription update(
        long id, Map<String, ?> parameters, @Nullable String name, @Nullable String externalId,
        @Nullable String webhookUrl) {

        TriggerSubscription triggerSubscription = triggerSubscriptionService.getTriggerSubscription(id);

        String workflowDefinition = TriggerSubscriptionWorkflowDefinitionFactory.create(
            triggerSubscription.getComponentName(), triggerSubscription.getComponentVersion(),
            triggerSubscription.getTriggerName(), parameters, id, this.registrationLease);

        RegistrationLease registrationLease = null;

        if (triggerSubscription.getStatus() == TriggerSubscriptionStatus.ENABLED) {
            registrationLease = transactionTemplate.execute(transactionStatus -> {
                TriggerSubscription lockedTriggerSubscription = triggerSubscriptionService.lockTriggerSubscription(id);

                if (lockedTriggerSubscription.getStatus() != TriggerSubscriptionStatus.ENABLED) {
                    return null;
                }

                return claimRegistrationLease(lockedTriggerSubscription);
            });
        }

        if (registrationLease != null) {
            RegistrationLease claimedRegistrationLease = registrationLease;

            try {
                runRenewingRegistrationLease(
                    claimedRegistrationLease,
                    () -> disableTrigger(claimedRegistrationLease.triggerSubscription()));
            } catch (RuntimeException exception) {
                throw failEnable(registrationLease, exception);
            }
        }

        RegistrationLease heldRegistrationLease = registrationLease;
        RegistrationResult updateResult;

        try {
            updateResult = Objects.requireNonNull(
                transactionTemplate.execute(transactionStatus -> {
                    TriggerSubscription currentTriggerSubscription = triggerSubscriptionService.lockTriggerSubscription(
                        id);

                    currentTriggerSubscription.setExternalId(externalId);
                    currentTriggerSubscription.setName(name);
                    currentTriggerSubscription.setParameters(parameters);
                    currentTriggerSubscription.setWebhookUrl(webhookUrl);

                    redeploy(currentTriggerSubscription, workflowDefinition);

                    return storeUpdated(currentTriggerSubscription, heldRegistrationLease);
                }));
        } catch (RuntimeException exception) {
            if (heldRegistrationLease != null) {
                throw restoreTrigger(heldRegistrationLease, exception);
            }

            TriggerSubscriptionErrorCode errorCode = getErrorCode(exception);

            logEnableFailure(id, errorCode, exception);

            throw new TriggerSubscriptionEnableException(
                null, errorCode, getErrorMessage(errorCode, exception), exception);
        }

        RegistrationLease reregistrationLease = updateResult.renewedRegistrationLease();

        if (reregistrationLease != null) {
            return enableTrigger(reregistrationLease);
        }

        return updateResult.triggerSubscription();
    }

    private void checkTriggerSubscriptionLimit(ConnectedUser connectedUser, int maxTriggerSubscriptions) {
        if (maxTriggerSubscriptions <= 0) {
            return;
        }

        long connectedUserId = connectedUserProjectService.lockConnectedUser(
            connectedUser.getExternalId(), connectedUser.getEnvironment());

        List<TriggerSubscription> triggerSubscriptions = triggerSubscriptionService
            .getConnectedUserTriggerSubscriptions(connectedUserId, connectedUser.getEnvironment());

        TriggerSubscriptionRules.checkTriggerSubscriptionLimit(
            connectedUserId, triggerSubscriptions.size(), maxTriggerSubscriptions);
    }

    private TriggerSubscription deploy(TriggerSubscription triggerSubscription, String externalUserId) {
        Project project = new Project();

        project.setName(
            "%s - %s/%s".formatted(
                externalUserId, triggerSubscription.getComponentName(), triggerSubscription.getTriggerName()));
        project.setWorkspaceId(Workspace.DEFAULT_WORKSPACE_ID);

        project = projectService.create(project);

        long projectId = Objects.requireNonNull(project.getId());

        ProjectWorkflow projectWorkflow = projectWorkflowFacade.addWorkflow(
            projectId, createWorkflowDefinition(triggerSubscription));

        String workflowUuid = projectWorkflow.getUuidAsString();

        int publishedProjectVersion = publishProject(projectId);

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnabled(false);
        projectDeployment.setEnvironment(triggerSubscription.getEnvironment());
        projectDeployment.setName(project.getName());
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(publishedProjectVersion);

        long projectDeploymentId = projectDeploymentFacade.createProjectDeployment(
            projectDeployment, getWorkflowId(projectId, publishedProjectVersion, workflowUuid),
            getConnections(triggerSubscription));

        triggerSubscription.setProjectDeploymentId(projectDeploymentId);
        triggerSubscription.setProjectId(projectId);
        triggerSubscription.setWorkflowUuid(workflowUuid);

        return triggerSubscriptionService.update(triggerSubscription);
    }

    private void deleteAll(
        List<TriggerSubscription> triggerSubscriptions, TriggerSubscriptionDisableReason reason) {

        List<TriggerSubscription> unregisteredTriggerSubscriptions = new ArrayList<>();
        Set<Long> unregisterFailedIds = new HashSet<>();

        for (TriggerSubscription triggerSubscription : triggerSubscriptions) {
            if (triggerSubscription.getStatus() != TriggerSubscriptionStatus.ENABLED) {
                continue;
            }

            Long id = triggerSubscription.getId();

            try {
                unregisterTrigger(triggerSubscription);

                unregisteredTriggerSubscriptions.add(triggerSubscription);
            } catch (RuntimeException exception) {
                log.error(
                    "Unable to unregister the trigger of trigger subscription id={}, project deployment id={} " +
                        "before deleting it for reason {}",
                    id, triggerSubscription.getProjectDeploymentId(), reason, exception);

                unregisterFailedIds.add(id);
            }
        }

        deleteLocally(triggerSubscriptions, unregisteredTriggerSubscriptions, unregisterFailedIds);

        for (TriggerSubscription triggerSubscription : triggerSubscriptions) {
            triggerSubscriptionEventSender.sendLifecycleEvent(
                triggerSubscription, SUBSCRIPTION_DELETED_EVENT_TYPE, reason);
        }
    }

    private void deleteLocally(
        List<TriggerSubscription> triggerSubscriptions, List<TriggerSubscription> unregisteredTriggerSubscriptions,
        Set<Long> unregisterFailedIds) {

        boolean synchronizationActive = TransactionSynchronizationManager.isSynchronizationActive();

        if (synchronizationActive && !unregisteredTriggerSubscriptions.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        markUnregisteredFailed(unregisteredTriggerSubscriptions);
                    }
                }
            });
        }

        for (TriggerSubscription triggerSubscription : triggerSubscriptions) {
            Long id = triggerSubscription.getId();
            Long projectDeploymentId = triggerSubscription.getProjectDeploymentId();
            Long projectId = triggerSubscription.getProjectId();

            try {
                transactionTemplate.executeWithoutResult(transactionStatus -> {
                    triggerSubscriptionService.delete(Objects.requireNonNull(id));

                    if (projectDeploymentId != null) {
                        if (unregisterFailedIds.contains(id)) {
                            projectDeploymentService.updateEnabled(projectDeploymentId, false);
                        }

                        projectDeploymentFacade.deleteProjectDeployment(projectDeploymentId);
                    }

                    if (projectId != null) {
                        projectFacade.deleteProject(projectId);
                    }
                });
            } catch (RuntimeException exception) {
                log.error(
                    "Unable to delete trigger subscription id={}, project deployment id={}, project id={}", id,
                    projectDeploymentId, projectId, exception);

                if (!synchronizationActive) {
                    markUnregisteredFailed(unregisteredTriggerSubscriptions);
                }

                throw exception;
            }
        }
    }

    private void disable(TriggerSubscription triggerSubscription, @Nullable TriggerSubscriptionDisableReason reason) {
        triggerSubscription.setErrorCode(null);
        triggerSubscription.setErrorMessage(null);
        triggerSubscription.setStatus(getDisabledStatus(reason));

        disableTrigger(triggerSubscriptionService.update(triggerSubscription));
    }

    private void disableTrigger(TriggerSubscription triggerSubscription) {
        Long projectDeploymentId = triggerSubscription.getProjectDeploymentId();

        if (projectDeploymentId == null) {
            return;
        }

        projectDeploymentFacade.enableProjectDeploymentWorkflow(
            projectDeploymentId, getDeployedWorkflowId(triggerSubscription), false);
    }

    private void unregisterTrigger(TriggerSubscription triggerSubscription) {
        requiresNewTransactionTemplate.executeWithoutResult(transactionStatus -> disableTrigger(triggerSubscription));
    }

    private void disableEnabled(
        Supplier<List<TriggerSubscription>> triggerSubscriptionsSupplier, TriggerSubscriptionDisableReason reason,
        String owner) {

        List<TriggerSubscription> triggerSubscriptions;

        try {
            triggerSubscriptions = Objects.requireNonNull(
                requiresNewTransactionTemplate.execute(transactionStatus -> triggerSubscriptionsSupplier.get()));
        } catch (RuntimeException exception) {
            log.error("Unable to load the trigger subscriptions of {} to disable them", owner, exception);

            return;
        }

        for (TriggerSubscription triggerSubscription : triggerSubscriptions) {
            if (triggerSubscription.getStatus() != TriggerSubscriptionStatus.ENABLED) {
                continue;
            }

            long id = Objects.requireNonNull(triggerSubscription.getId());

            try {
                requiresNewTransactionTemplate.executeWithoutResult(transactionStatus -> disableEnabled(id, reason));
            } catch (RuntimeException exception) {
                log.error(
                    "Unable to disable trigger subscription id={} of {} for reason {}", id, owner, reason, exception);

                recordDisableFailure(id, exception);
            }
        }
    }

    private void disableEnabled(long id, TriggerSubscriptionDisableReason reason) {
        Optional<TriggerSubscription> triggerSubscriptionOptional =
            triggerSubscriptionService.fetchLockedTriggerSubscription(id);

        if (triggerSubscriptionOptional.isEmpty()) {
            return;
        }

        TriggerSubscription triggerSubscription = triggerSubscriptionOptional.get();

        if (triggerSubscription.getStatus() != TriggerSubscriptionStatus.ENABLED) {
            return;
        }

        disable(triggerSubscription, reason);

        triggerSubscriptionEventSender.sendLifecycleEvent(
            triggerSubscription, SUBSCRIPTION_DISABLED_EVENT_TYPE, reason);
    }

    private void disableDeployment(
        @Nullable Long triggerSubscriptionId, long projectDeploymentId, RuntimeException failure) {

        try {
            ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);

            if (projectDeployment.isEnabled()) {
                requiresNewTransactionTemplate.executeWithoutResult(
                    transactionStatus -> projectDeploymentFacade.enableProjectDeployment(projectDeploymentId, false));
            }
        } catch (RuntimeException exception) {
            log.error(
                "Unable to disable project deployment id={} of failed trigger subscription id={}, its trigger may " +
                    "still be registered",
                projectDeploymentId, triggerSubscriptionId, exception);

            if (exception != failure) {
                failure.addSuppressed(exception);
            }
        }
    }

    /**
     * Registers the trigger and commits the subscription as ENABLED, then disables it again when its owner stopped
     * being eligible meanwhile. A bulk disable of an owner runs after the owner change commits and acts only on ENABLED
     * subscriptions, while the owner check here runs after the ENABLED write commits: either the bulk disable sees the
     * ENABLED subscription or this check sees the owner change, so no window remains.
     */
    private TriggerSubscription enableTrigger(RegistrationLease registrationLease) {
        return disableIfOwnerIneligible(registerTrigger(registrationLease));
    }

    /**
     * Enables the trigger of a subscription that is not ENABLED once its registration lease is claimed, or returns
     * empty at once when the subscription is ENABLED or another request holds the lease and is registering the trigger.
     */
    private Optional<TriggerSubscription> enableExclusively(long id) {
        RegistrationLease registrationLease = transactionTemplate.execute(transactionStatus -> {
            TriggerSubscription lockedTriggerSubscription = triggerSubscriptionService.lockTriggerSubscription(id);

            if (lockedTriggerSubscription.getStatus() == TriggerSubscriptionStatus.ENABLED) {
                return null;
            }

            return claimRegistrationLease(lockedTriggerSubscription);
        });

        if (registrationLease == null) {
            log.debug("Trigger subscription id={} is enabled or its trigger is being registered by another request",
                id);

            return Optional.empty();
        }

        return Optional.of(enableTrigger(registrationLease));
    }

    /**
     * Claims the registration lease of the locked subscription, or returns null when another request holds a lease that
     * has not expired. Saving the lease increments the {@code version} the final write of the holder checks.
     */
    @Nullable
    private RegistrationLease claimRegistrationLease(TriggerSubscription lockedTriggerSubscription) {
        if (isRegistrationLeaseActive(lockedTriggerSubscription)) {
            return null;
        }

        return renewRegistrationLease(lockedTriggerSubscription);
    }

    private RegistrationLease renewRegistrationLease(TriggerSubscription lockedTriggerSubscription) {
        Instant now = Instant.now();

        Instant registrationLeaseUntil = now.plus(registrationLease);

        registrationLeaseUntil = registrationLeaseUntil.truncatedTo(ChronoUnit.MILLIS);

        lockedTriggerSubscription.setRegistrationLeaseUntil(registrationLeaseUntil);

        TriggerSubscription leasedTriggerSubscription = triggerSubscriptionService.update(lockedTriggerSubscription);

        return new RegistrationLease(leasedTriggerSubscription, registrationLeaseUntil);
    }

    private static boolean isRegistrationLeaseActive(TriggerSubscription triggerSubscription) {
        Instant registrationLeaseUntil = triggerSubscription.getRegistrationLeaseUntil();

        return registrationLeaseUntil != null && registrationLeaseUntil.isAfter(Instant.now());
    }

    /**
     * Returns whether the request holds the registration lease of the subscription: the lease it last saved is still
     * stored and no renewal found it taken over.
     */
    private static boolean isRegistrationLeaseHolder(
        TriggerSubscription triggerSubscription, RegistrationLease registrationLease) {

        return !registrationLease.isLost() &&
            Objects.equals(triggerSubscription.getRegistrationLeaseUntil(), registrationLease.leaseUntil());
    }

    /**
     * Runs a remote register or unregister call of the trigger while renewing the registration lease every third of its
     * duration, so a call that outlasts the lease keeps it and no other request takes it over meanwhile. A renewal that
     * fails, or finds the row locked by another request, is retried at the next period; one that finds the lease taken
     * over marks it lost and stops. The renewal stops before this method returns, so it never runs alongside the final
     * write of the holder.
     */
    private void runRenewingRegistrationLease(RegistrationLease heldRegistrationLease, Runnable remoteCall) {
        RegistrationLeaseRenewal registrationLeaseRenewal = new RegistrationLeaseRenewal(
            heldRegistrationLease, registrationLease);

        registrationLeaseRenewal.start();

        try {
            remoteCall.run();
        } finally {
            registrationLeaseRenewal.stop();
        }
    }

    /**
     * Stores the updated subscription in the redeploy transaction. The registration lease is claimed, or renewed when
     * this update holds it, when the stored status is ENABLED and no other request holds an unexpired lease; a lease
     * this update holds is released otherwise. When another request holds the lease, the values are only stored: the
     * holder registers the trigger of the new values, as described on update.
     */
    private RegistrationResult storeUpdated(
        TriggerSubscription lockedTriggerSubscription, @Nullable RegistrationLease heldRegistrationLease) {

        boolean registrationLeaseHolder = heldRegistrationLease != null &&
            isRegistrationLeaseHolder(lockedTriggerSubscription, heldRegistrationLease);

        if (lockedTriggerSubscription.getStatus() == TriggerSubscriptionStatus.ENABLED &&
            (registrationLeaseHolder || !isRegistrationLeaseActive(lockedTriggerSubscription))) {

            RegistrationLease registrationLease = renewRegistrationLease(lockedTriggerSubscription);

            return new RegistrationResult(registrationLease.triggerSubscription(), registrationLease);
        }

        if (registrationLeaseHolder) {
            lockedTriggerSubscription.setRegistrationLeaseUntil(null);
        }

        return new RegistrationResult(triggerSubscriptionService.update(lockedTriggerSubscription), null);
    }

    private TriggerSubscription disableIfOwnerIneligible(TriggerSubscription triggerSubscription) {
        long id = Objects.requireNonNull(triggerSubscription.getId());
        TriggerSubscriptionDisableReason reason;

        try {
            reason = getIneligibleOwnerReason(triggerSubscription);
        } catch (RuntimeException exception) {
            log.error("Unable to check the owner of enabled trigger subscription id={}", id, exception);

            return triggerSubscription;
        }

        if (reason == null) {
            return triggerSubscription;
        }

        try {
            requiresNewTransactionTemplate.executeWithoutResult(transactionStatus -> disableEnabled(id, reason));
        } catch (RuntimeException exception) {
            log.error(
                "Unable to disable trigger subscription id={} whose owner became ineligible for reason {}", id, reason,
                exception);

            recordDisableFailure(id, exception);
        }

        return triggerSubscriptionService.getTriggerSubscription(id);
    }

    @Nullable
    private TriggerSubscriptionDisableReason getIneligibleOwnerReason(TriggerSubscription triggerSubscription) {
        Optional<ConnectedUser> connectedUserOptional = connectedUserService.fetchConnectedUser(
            triggerSubscription.getConnectedUserId());

        if (connectedUserOptional.isEmpty()) {
            return TriggerSubscriptionDisableReason.CONNECTED_USER_DELETED;
        }

        ConnectedUser connectedUser = connectedUserOptional.get();

        if (!connectedUser.isEnabled()) {
            return TriggerSubscriptionDisableReason.CONNECTED_USER_DISABLED;
        }

        Long integrationInstanceId = triggerSubscription.getIntegrationInstanceId();

        if (integrationInstanceId != null) {
            Optional<IntegrationInstance> integrationInstanceOptional =
                integrationInstanceService.fetchIntegrationInstance(integrationInstanceId);

            if (integrationInstanceOptional.isEmpty()) {
                return TriggerSubscriptionDisableReason.INTEGRATION_INSTANCE_DELETED;
            }

            IntegrationInstance integrationInstance = integrationInstanceOptional.get();

            if (!integrationInstance.isEnabled()) {
                return TriggerSubscriptionDisableReason.INTEGRATION_INSTANCE_DISABLED;
            }
        }

        Long connectionId = triggerSubscription.getConnectionId();

        if (connectionId == null) {
            return null;
        }

        Optional<Connection> connectionOptional = connectionService.fetchConnection(connectionId);

        if (connectionOptional.isPresent()) {
            Connection connection = connectionOptional.get();

            if (connection.getCredentialStatus() == CredentialStatus.INVALID) {
                return TriggerSubscriptionDisableReason.CONNECTION_INVALID;
            }
        }

        return null;
    }

    /**
     * Registers the trigger with the app, then writes the ENABLED status under the subscription row lock and releases
     * the registration lease in the same save. Every state change of a subscription saves its row and so increments its
     * {@code version}, while delivery results do not. The lease passed in carries the subscription as its claim saved
     * it; when the locked row carries another version, a concurrent request changed the subscription during the call. A
     * disable stands: the trigger is unregistered again under the lock, as a disable does, the lease is released and
     * the current subscription is returned. An update that left the subscription ENABLED stored new values while this
     * request held the lease, so the lease is renewed, the trigger unregistered and the trigger of the new values
     * registered in its place. A failure is recorded the same way, only when the version is unchanged. Only the lease
     * holder registers the trigger, so two requests never register it at the same time: the trigger key is the same for
     * both, and the unregister of the one that loses the version check would otherwise stop the trigger the other one
     * registered.
     */
    private TriggerSubscription registerTrigger(RegistrationLease registrationLease) {
        RegistrationLease currentRegistrationLease = registrationLease;

        while (true) {
            RegistrationLease attemptRegistrationLease = currentRegistrationLease;
            TriggerSubscription triggerSubscription = attemptRegistrationLease.triggerSubscription();

            long id = Objects.requireNonNull(triggerSubscription.getId());

            RegistrationResult registrationResult;

            try {
                runRenewingRegistrationLease(
                    attemptRegistrationLease, () -> registerTriggerRemotely(triggerSubscription));

                registrationResult = Objects.requireNonNull(
                    transactionTemplate.execute(
                        transactionStatus -> writeEnabledStatus(id, attemptRegistrationLease)));
            } catch (RuntimeException exception) {
                RegistrationLease supersedingRegistrationLease = fetchSupersedingRegistrationLease(
                    attemptRegistrationLease, exception);

                if (supersedingRegistrationLease == null) {
                    throw failEnable(attemptRegistrationLease, exception);
                }

                log.info(
                    "Unable to register the trigger of trigger subscription id={} whose values were updated " +
                        "meanwhile, registering the trigger of its new values",
                    id, exception);

                registrationResult = new RegistrationResult(
                    supersedingRegistrationLease.triggerSubscription(), supersedingRegistrationLease);
            }

            RegistrationLease renewedRegistrationLease = registrationResult.renewedRegistrationLease();

            if (renewedRegistrationLease == null) {
                return registrationResult.triggerSubscription();
            }

            log.info(
                "Trigger subscription id={} was updated while its trigger was being registered, registering the " +
                    "trigger of its new values",
                id);

            currentRegistrationLease = renewedRegistrationLease;

            try {
                runRenewingRegistrationLease(
                    renewedRegistrationLease, () -> disableTrigger(renewedRegistrationLease.triggerSubscription()));
            } catch (RuntimeException exception) {
                throw failEnable(renewedRegistrationLease, exception);
            }
        }
    }

    private void registerTriggerRemotely(TriggerSubscription triggerSubscription) {
        long projectDeploymentId = Objects.requireNonNull(triggerSubscription.getProjectDeploymentId());

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);
        String deployedWorkflowId = getDeployedWorkflowId(triggerSubscription);

        if (!projectDeployment.isEnabled()) {
            projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentId, deployedWorkflowId, false);
            projectDeploymentFacade.enableProjectDeployment(projectDeploymentId, true);
        }

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentId, deployedWorkflowId, true);
    }

    /**
     * Renews and returns the registration lease when the registration failed for values an update replaced while this
     * request held the lease, so the trigger of the new values is registered instead of the failure being recorded.
     */
    @Nullable
    private RegistrationLease fetchSupersedingRegistrationLease(
        RegistrationLease registrationLease, RuntimeException failure) {

        TriggerSubscription triggerSubscription = registrationLease.triggerSubscription();

        long id = Objects.requireNonNull(triggerSubscription.getId());

        try {
            return transactionTemplate.execute(transactionStatus -> {
                Optional<TriggerSubscription> lockedTriggerSubscriptionOptional =
                    triggerSubscriptionService.fetchLockedTriggerSubscription(id);

                if (lockedTriggerSubscriptionOptional.isEmpty()) {
                    return null;
                }

                TriggerSubscription lockedTriggerSubscription = lockedTriggerSubscriptionOptional.get();

                if (lockedTriggerSubscription.getVersion() == triggerSubscription.getVersion() ||
                    lockedTriggerSubscription.getStatus() != TriggerSubscriptionStatus.ENABLED ||
                    !isRegistrationLeaseHolder(lockedTriggerSubscription, registrationLease)) {

                    return null;
                }

                return renewRegistrationLease(lockedTriggerSubscription);
            });
        } catch (RuntimeException exception) {
            failure.addSuppressed(exception);

            log.error("Unable to read trigger subscription id={} after its registration failed", id, exception);

            return null;
        }
    }

    private RegistrationResult writeEnabledStatus(long id, RegistrationLease registrationLease) {
        TriggerSubscription lockedTriggerSubscription = triggerSubscriptionService.lockTriggerSubscription(id);

        TriggerSubscription triggerSubscription = registrationLease.triggerSubscription();

        boolean registrationLeaseHolder = isRegistrationLeaseHolder(lockedTriggerSubscription, registrationLease);

        if (registrationLeaseHolder && lockedTriggerSubscription.getVersion() == triggerSubscription.getVersion()) {
            lockedTriggerSubscription.setErrorCode(null);
            lockedTriggerSubscription.setErrorMessage(null);
            lockedTriggerSubscription.setRegistrationLeaseUntil(null);
            lockedTriggerSubscription.setStatus(TriggerSubscriptionStatus.ENABLED);

            return new RegistrationResult(triggerSubscriptionService.update(lockedTriggerSubscription), null);
        }

        if (registrationLeaseHolder && lockedTriggerSubscription.getStatus() == TriggerSubscriptionStatus.ENABLED) {
            RegistrationLease renewedRegistrationLease = renewRegistrationLease(lockedTriggerSubscription);

            return new RegistrationResult(renewedRegistrationLease.triggerSubscription(), renewedRegistrationLease);
        }

        if (!registrationLeaseHolder &&
            (lockedTriggerSubscription.getStatus() == TriggerSubscriptionStatus.ENABLED ||
                isRegistrationLeaseActive(lockedTriggerSubscription))) {

            logLostRegistrationLease(lockedTriggerSubscription);

            return new RegistrationResult(lockedTriggerSubscription, null);
        }

        return new RegistrationResult(unregisterChangedTrigger(lockedTriggerSubscription), null);
    }

    /**
     * Reports a registration made by a request whose registration lease expired and another request took over during
     * the call. That request registers the trigger, or already committed the subscription as ENABLED, so the
     * subscription is left to it. The registration this request made cannot be undone on its own: the trigger state of
     * the deployment workflow holds a single registration, written by whichever request registered last, and an
     * unregister would remove the registration of the lease holder instead. It may stay with the provider, so it is
     * logged as an error.
     */
    private void logLostRegistrationLease(TriggerSubscription lockedTriggerSubscription) {
        log.error(
            "Registration lease of trigger subscription id={} was taken over by another request while its trigger " +
                "was being registered, leaving the subscription to that request; the provider registration made " +
                "for project deployment id={}, workflow uuid={} may be orphaned",
            lockedTriggerSubscription.getId(), lockedTriggerSubscription.getProjectDeploymentId(),
            lockedTriggerSubscription.getWorkflowUuid());
    }

    /**
     * Unregisters the trigger of a subscription that a disable changed while its trigger was being registered and
     * releases the registration lease.
     */
    private TriggerSubscription unregisterChangedTrigger(TriggerSubscription lockedTriggerSubscription) {
        Long id = lockedTriggerSubscription.getId();

        log.info(
            "Trigger subscription id={} changed while its trigger was being registered, unregistering the trigger", id);

        lockedTriggerSubscription.setRegistrationLeaseUntil(null);

        try {
            unregisterTrigger(lockedTriggerSubscription);

            return triggerSubscriptionService.update(lockedTriggerSubscription);
        } catch (RuntimeException exception) {
            TriggerSubscriptionErrorCode errorCode = getErrorCode(exception);

            log.error(
                "Unable to unregister the trigger of trigger subscription id={} that changed while it was being " +
                    "registered",
                id, exception);

            Long projectDeploymentId = lockedTriggerSubscription.getProjectDeploymentId();

            if (projectDeploymentId != null) {
                disableDeployment(id, projectDeploymentId, exception);
            }

            lockedTriggerSubscription.setErrorCode(errorCode);
            lockedTriggerSubscription.setErrorMessage(getErrorMessage(errorCode, exception));
            lockedTriggerSubscription.setStatus(TriggerSubscriptionStatus.FAILED);

            return triggerSubscriptionService.update(lockedTriggerSubscription);
        }
    }

    private TriggerSubscriptionEnableException failEnable(
        RegistrationLease registrationLease, RuntimeException exception) {

        TriggerSubscription triggerSubscription = registrationLease.triggerSubscription();

        Long id = triggerSubscription.getId();
        TriggerSubscriptionErrorCode errorCode = getErrorCode(exception);

        logEnableFailure(id, errorCode, exception);

        try {
            markFailed(registrationLease, errorCode, exception);
        } catch (RuntimeException markFailedException) {
            exception.addSuppressed(markFailedException);

            log.error("Unable to mark trigger subscription id={} as failed", id, markFailedException);
        }

        return new TriggerSubscriptionEnableException(id, errorCode, getErrorMessage(errorCode, exception), exception);
    }

    private List<ProjectDeploymentWorkflowConnection> getConnections(TriggerSubscription triggerSubscription) {
        Long connectionId = triggerSubscription.getConnectionId();

        if (connectionId == null) {
            return List.of();
        }

        return List.of(
            new ProjectDeploymentWorkflowConnection(
                connectionId, triggerSubscription.getComponentName(),
                TriggerSubscriptionWorkflowDefinitionFactory.TRIGGER_NAME));
    }

    private String getDeployedWorkflowId(TriggerSubscription triggerSubscription) {
        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(
            Objects.requireNonNull(triggerSubscription.getProjectDeploymentId()));

        return getWorkflowId(
            Objects.requireNonNull(triggerSubscription.getProjectId()), projectDeployment.getProjectVersion(),
            Objects.requireNonNull(triggerSubscription.getWorkflowUuid()));
    }

    private static TriggerSubscriptionErrorCode getErrorCode(Throwable throwable) {
        List<Throwable> throwables = ExceptionUtils.getThrowableList(throwable);

        for (Throwable curThrowable : throwables) {
            for (Class<? extends Throwable> timeoutExceptionClass : TIMEOUT_EXCEPTION_CLASSES) {
                if (timeoutExceptionClass.isInstance(curThrowable)) {
                    return TriggerSubscriptionErrorCode.TIMEOUT_ERROR;
                }
            }
        }

        for (Throwable curThrowable : throwables) {
            if (isCredentialRefreshFailure(curThrowable)) {
                return TriggerSubscriptionErrorCode.OAUTH_REFRESH;
            }
        }

        for (Throwable curThrowable : throwables) {
            if (isComponentFailure(curThrowable)) {
                return TriggerSubscriptionErrorCode.API_ERROR;
            }
        }

        for (Throwable curThrowable : throwables) {
            if (isMissingConnectionsFailure(curThrowable)) {
                return TriggerSubscriptionErrorCode.USER_INPUT_ERROR;
            }
        }

        return TriggerSubscriptionErrorCode.INTERNAL_ERROR;
    }

    private static String getErrorMessage(TriggerSubscriptionErrorCode errorCode, Throwable throwable) {
        if (errorCode == TriggerSubscriptionErrorCode.INTERNAL_ERROR) {
            return INTERNAL_ERROR_MESSAGE;
        }

        if (errorCode == TriggerSubscriptionErrorCode.USER_INPUT_ERROR) {
            return MISSING_CONNECTIONS_ERROR_MESSAGE;
        }

        String message = throwable.getMessage();

        if (StringUtils.isBlank(message)) {
            Class<? extends Throwable> throwableClass = throwable.getClass();

            message = throwableClass.getName();
        }

        return StringUtils.abbreviate(message, ERROR_MESSAGE_MAX_LENGTH);
    }

    private static TriggerSubscriptionStatus getDisabledStatus(@Nullable TriggerSubscriptionDisableReason reason) {
        if (reason == TriggerSubscriptionDisableReason.CONNECTION_DELETED ||
            reason == TriggerSubscriptionDisableReason.CONNECTION_INVALID) {

            return TriggerSubscriptionStatus.CONNECTION_ERROR;
        }

        return TriggerSubscriptionStatus.DISABLED;
    }

    private String getWorkflowId(long projectId, int projectVersion, String workflowUuid) {
        Optional<ProjectWorkflow> projectWorkflowOptional = projectWorkflowService.fetchProjectWorkflow(
            projectId, projectVersion, workflowUuid);

        ProjectWorkflow projectWorkflow = projectWorkflowOptional.orElseThrow(
            () -> new IllegalArgumentException(
                "Workflow %s is not in version %s of project id=%s".formatted(
                    workflowUuid, projectVersion, projectId)));

        return projectWorkflow.getWorkflowId();
    }

    private static boolean isComponentFailure(Throwable throwable) {
        if (throwable instanceof ProviderException) {
            return true;
        }

        if (!(throwable instanceof AbstractException abstractException)) {
            return false;
        }

        return COMPONENT_DEFINITION_CLASSES.contains(abstractException.getEntityClass());
    }

    private static boolean isCredentialRefreshFailure(Throwable throwable) {
        if (!(throwable instanceof AbstractException abstractException)) {
            return false;
        }

        return abstractException.getEntityClass() == ConnectionDefinition.class &&
            CREDENTIAL_REFRESH_FAILED_ERROR_KEYS.contains(abstractException.getErrorKey());
    }

    private static boolean isMissingConnectionsFailure(Throwable throwable) {
        if (!(throwable instanceof AbstractException abstractException)) {
            return false;
        }

        return abstractException.getEntityClass() == ProjectDeployment.class &&
            abstractException.getErrorKey() == WORKFLOW_CONNECTIONS_NOT_FOUND_ERROR_KEY;
    }

    private boolean isStaticWebhookTrigger(TriggerSubscription triggerSubscription) {
        com.bytechef.platform.component.domain.TriggerDefinition triggerDefinition;

        try {
            triggerDefinition = triggerDefinitionService.getTriggerDefinition(
                triggerSubscription.getComponentName(), triggerSubscription.getComponentVersion(),
                triggerSubscription.getTriggerName());
        } catch (IllegalArgumentException illegalArgumentException) {
            log.warn(
                "Unable to resolve the trigger of trigger subscription id={}, its static webhook URL is omitted",
                triggerSubscription.getId(), illegalArgumentException);

            return false;
        }

        return triggerDefinition.getType() == TriggerType.STATIC_WEBHOOK;
    }

    private static void logEnableFailure(
        @Nullable Long id, TriggerSubscriptionErrorCode errorCode, RuntimeException exception) {

        if (errorCode == TriggerSubscriptionErrorCode.INTERNAL_ERROR) {
            log.error("Unable to enable trigger subscription id={}, error code {}", id, errorCode, exception);
        } else {
            log.warn("Unable to enable trigger subscription id={}, error code {}", id, errorCode, exception);
        }
    }

    /**
     * Disables the deployment and marks the subscription FAILED under the row lock, and releases the registration
     * lease. When another request changed the subscription since the lease was claimed, that request already disabled,
     * enabled or failed it, and its state stands; only the lease is released. A subscription that stayed ENABLED while
     * this request held the lease was updated, and its trigger is not registered, so it is marked FAILED all the same.
     * A subscription deleted meanwhile still has its deployment disabled, so no trigger stays registered.
     */
    private void markFailed(
        RegistrationLease registrationLease, TriggerSubscriptionErrorCode errorCode, RuntimeException exception) {

        TriggerSubscription triggerSubscription = registrationLease.triggerSubscription();

        long id = Objects.requireNonNull(triggerSubscription.getId());

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            Optional<TriggerSubscription> lockedTriggerSubscriptionOptional =
                triggerSubscriptionService.fetchLockedTriggerSubscription(id);

            if (lockedTriggerSubscriptionOptional.isPresent()) {
                TriggerSubscription lockedTriggerSubscription = lockedTriggerSubscriptionOptional.get();

                boolean registrationLeaseHolder = isRegistrationLeaseHolder(
                    lockedTriggerSubscription, registrationLease);

                if (!registrationLeaseHolder ||
                    (lockedTriggerSubscription.getVersion() != triggerSubscription.getVersion() &&
                        lockedTriggerSubscription.getStatus() != TriggerSubscriptionStatus.ENABLED)) {

                    log.info(
                        "Trigger subscription id={} changed while it was being enabled, keeping its current state",
                        id);

                    if (registrationLeaseHolder) {
                        lockedTriggerSubscription.setRegistrationLeaseUntil(null);

                        triggerSubscriptionService.update(lockedTriggerSubscription);
                    }

                    return;
                }
            }

            Long projectDeploymentId = triggerSubscription.getProjectDeploymentId();

            if (projectDeploymentId != null) {
                disableDeployment(id, projectDeploymentId, exception);
            }

            TriggerSubscription failedTriggerSubscription = lockedTriggerSubscriptionOptional.orElseThrow(
                () -> new IllegalArgumentException("Trigger subscription not found: " + id));

            failedTriggerSubscription.setErrorCode(errorCode);
            failedTriggerSubscription.setErrorMessage(getErrorMessage(errorCode, exception));
            failedTriggerSubscription.setRegistrationLeaseUntil(null);
            failedTriggerSubscription.setStatus(TriggerSubscriptionStatus.FAILED);

            triggerSubscriptionService.update(failedTriggerSubscription);
        });
    }

    /**
     * Marks FAILED, under the row lock, a subscription whose disable failed and rolled back. The failed disable wrote
     * nothing, so the subscription is overwritten only while it is still ENABLED: its trigger is then still registered
     * for an owner that is no longer eligible, whatever else changed. Any other status was set by a request that
     * committed meanwhile and stands.
     */
    private void recordDisableFailure(long id, RuntimeException failure) {
        try {
            requiresNewTransactionTemplate.executeWithoutResult(transactionStatus -> {
                Optional<TriggerSubscription> triggerSubscriptionOptional =
                    triggerSubscriptionService.fetchLockedTriggerSubscription(id);

                if (triggerSubscriptionOptional.isEmpty()) {
                    return;
                }

                TriggerSubscription triggerSubscription = triggerSubscriptionOptional.get();

                if (triggerSubscription.getStatus() != TriggerSubscriptionStatus.ENABLED) {
                    return;
                }

                TriggerSubscriptionErrorCode errorCode = getErrorCode(failure);

                triggerSubscription.setErrorCode(errorCode);
                triggerSubscription.setErrorMessage(getErrorMessage(errorCode, failure));
                triggerSubscription.setStatus(TriggerSubscriptionStatus.FAILED);

                triggerSubscriptionService.update(triggerSubscription);
            });
        } catch (RuntimeException exception) {
            log.error("Unable to record the disable failure of trigger subscription id={}", id, exception);
        }
    }

    /**
     * Marks FAILED, under the row lock, the subscriptions whose trigger was unregistered before a delete that did not
     * complete. A subscription that another request changed since the delete read it keeps the state that request set,
     * which also describes its trigger.
     */
    private void markUnregisteredFailed(List<TriggerSubscription> unregisteredTriggerSubscriptions) {
        for (TriggerSubscription unregisteredTriggerSubscription : unregisteredTriggerSubscriptions) {
            Long id = unregisteredTriggerSubscription.getId();

            try {
                requiresNewTransactionTemplate.executeWithoutResult(transactionStatus -> {
                    Optional<TriggerSubscription> triggerSubscriptionOptional =
                        triggerSubscriptionService.fetchLockedTriggerSubscription(Objects.requireNonNull(id));

                    if (triggerSubscriptionOptional.isEmpty()) {
                        return;
                    }

                    TriggerSubscription triggerSubscription = triggerSubscriptionOptional.get();

                    if (triggerSubscription.getVersion() != unregisteredTriggerSubscription.getVersion()) {
                        return;
                    }

                    triggerSubscription.setErrorCode(TriggerSubscriptionErrorCode.INTERNAL_ERROR);
                    triggerSubscription.setErrorMessage(DELETE_ROLLED_BACK_ERROR_MESSAGE);
                    triggerSubscription.setStatus(TriggerSubscriptionStatus.FAILED);

                    triggerSubscriptionService.update(triggerSubscription);

                    log.error(
                        "Trigger of trigger subscription id={} was unregistered but its delete did not complete, " +
                            "marked it failed",
                        id);
                });
            } catch (RuntimeException exception) {
                log.error(
                    "Unable to mark trigger subscription id={} failed after its trigger was unregistered and its " +
                        "delete did not complete",
                    id, exception);
            }
        }
    }

    private int publishProject(long projectId) {
        int draftProjectVersion = projectFacade.publishProject(projectId, null, false);

        return draftProjectVersion - 1;
    }

    private void redeploy(TriggerSubscription triggerSubscription, String workflowDefinition) {
        long projectId = Objects.requireNonNull(triggerSubscription.getProjectId());
        long projectDeploymentId = Objects.requireNonNull(triggerSubscription.getProjectDeploymentId());
        String workflowUuid = Objects.requireNonNull(triggerSubscription.getWorkflowUuid());

        Project project = projectService.getProject(projectId);

        String draftWorkflowId = getWorkflowId(projectId, project.getLastProjectVersion(), workflowUuid);

        Workflow draftWorkflow = workflowService.getWorkflow(draftWorkflowId);

        workflowService.update(draftWorkflowId, workflowDefinition, draftWorkflow.getVersion());

        int publishedProjectVersion = publishProject(projectId);

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);

        projectDeployment.setProjectVersion(publishedProjectVersion);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setConnections(getConnections(triggerSubscription));
        projectDeploymentWorkflow.setEnabled(false);
        projectDeploymentWorkflow.setInputs(Map.of());
        projectDeploymentWorkflow.setProjectDeploymentId(projectDeploymentId);
        projectDeploymentWorkflow.setWorkflowId(getWorkflowId(projectId, publishedProjectVersion, workflowUuid));

        projectDeploymentFacade.updateProjectDeployment(projectDeployment, List.of(projectDeploymentWorkflow),
            List.of());
    }

    /**
     * Registers the trigger of the stored values again after an update that unregistered it failed, under the
     * registration lease the update holds. Nothing is registered when a disable or a delete changed the subscription
     * meanwhile; the lease is released and the state that request set stands.
     */
    private TriggerSubscriptionEnableException restoreTrigger(
        RegistrationLease registrationLease, RuntimeException updateException) {

        TriggerSubscription triggerSubscription = registrationLease.triggerSubscription();

        Long id = triggerSubscription.getId();
        TriggerSubscriptionErrorCode errorCode = getErrorCode(updateException);

        logEnableFailure(id, errorCode, updateException);

        RegistrationLease resumedRegistrationLease;

        try {
            resumedRegistrationLease = transactionTemplate.execute(
                transactionStatus -> resumeRegistration(registrationLease));
        } catch (RuntimeException exception) {
            updateException.addSuppressed(exception);

            log.error("Unable to restore the trigger of trigger subscription id={} after its update failed", id,
                exception);

            resumedRegistrationLease = null;
        }

        if (resumedRegistrationLease == null) {
            return new TriggerSubscriptionEnableException(
                null, errorCode, getErrorMessage(errorCode, updateException), updateException);
        }

        TriggerSubscription restoredTriggerSubscription;

        try {
            restoredTriggerSubscription = enableTrigger(resumedRegistrationLease);
        } catch (TriggerSubscriptionEnableException triggerSubscriptionEnableException) {
            triggerSubscriptionEnableException.addSuppressed(updateException);

            return triggerSubscriptionEnableException;
        }

        if (restoredTriggerSubscription.getStatus() == TriggerSubscriptionStatus.ENABLED) {
            log.warn("Restored the previous trigger of trigger subscription id={} after its update failed", id);
        }

        return new TriggerSubscriptionEnableException(
            id, errorCode, getErrorMessage(errorCode, updateException), updateException);
    }

    @Nullable
    private RegistrationLease resumeRegistration(RegistrationLease registrationLease) {
        TriggerSubscription triggerSubscription = registrationLease.triggerSubscription();

        Optional<TriggerSubscription> lockedTriggerSubscriptionOptional =
            triggerSubscriptionService.fetchLockedTriggerSubscription(
                Objects.requireNonNull(triggerSubscription.getId()));

        if (lockedTriggerSubscriptionOptional.isEmpty()) {
            return null;
        }

        TriggerSubscription lockedTriggerSubscription = lockedTriggerSubscriptionOptional.get();

        if (!isRegistrationLeaseHolder(lockedTriggerSubscription, registrationLease)) {
            return null;
        }

        if (lockedTriggerSubscription.getVersion() == triggerSubscription.getVersion()) {
            return registrationLease;
        }

        if (lockedTriggerSubscription.getStatus() == TriggerSubscriptionStatus.ENABLED) {
            return renewRegistrationLease(lockedTriggerSubscription);
        }

        lockedTriggerSubscription.setRegistrationLeaseUntil(null);

        triggerSubscriptionService.update(lockedTriggerSubscription);

        return null;
    }

    private static void runAfterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });

            return;
        }

        runnable.run();
    }

    private static Duration getRegistrationLease(ApplicationProperties applicationProperties) {
        ApplicationProperties.Embedded embedded = applicationProperties.getEmbedded();

        ApplicationProperties.Embedded.TriggerSubscription triggerSubscription = embedded.getTriggerSubscription();

        Duration registrationLease = triggerSubscription.getRegistrationLease();

        if (registrationLease == null) {
            return DEFAULT_REGISTRATION_LEASE;
        }

        if (registrationLease.isNegative() || registrationLease.isZero()) {
            throw new IllegalStateException(
                REGISTRATION_LEASE_PROPERTY + " must be greater than 0, but is " + registrationLease);
        }

        return registrationLease;
    }

    private String createWorkflowDefinition(TriggerSubscription triggerSubscription) {
        return TriggerSubscriptionWorkflowDefinitionFactory.create(
            triggerSubscription.getComponentName(), triggerSubscription.getComponentVersion(),
            triggerSubscription.getTriggerName(), triggerSubscription.getParameters(),
            Objects.requireNonNull(triggerSubscription.getId()), registrationLease);
    }

    /**
     * The registration lease a request holds: the time it expires, which also identifies the claim and moves forward
     * with each renewal, and the subscription as the claim saved it, whose {@code version} the final write of the
     * request expects. A renewal changes neither the {@code version} nor the subscription. The lease is lost once a
     * renewal finds another value stored.
     */
    private static final class RegistrationLease {

        private final TriggerSubscription triggerSubscription;
        private volatile Instant leaseUntil;
        private volatile boolean lost;

        @SuppressFBWarnings("EI2")
        private RegistrationLease(TriggerSubscription triggerSubscription, Instant leaseUntil) {
            this.triggerSubscription = triggerSubscription;
            this.leaseUntil = leaseUntil;
        }

        private Instant leaseUntil() {
            return leaseUntil;
        }

        private boolean isLost() {
            return lost;
        }

        private void markLost() {
            lost = true;
        }

        private void renew(Instant renewedLeaseUntil) {
            leaseUntil = renewedLeaseUntil;
        }

        @SuppressFBWarnings("EI")
        private TriggerSubscription triggerSubscription() {
            return triggerSubscription;
        }
    }

    /**
     * Renews a registration lease periodically while its holder runs a remote call. Each renewal runs in its own short
     * transaction and moves the stored lease forward only while it is still the value the holder last saved. Renewals
     * and {@link #stop} are serialized, so no renewal runs or starts once stop returns.
     * <p>
     * A renewal never waits on the row lock of the subscription: the renewals of every subscription share one thread,
     * so a renewal blocked behind a request that holds the lock, such as a disable calling the app, would stall the
     * renewals of unrelated subscriptions until their leases expire and are taken over. A request that holds the lock
     * is changing the subscription, and the {@code version} check of the final write of the holder decides afterwards
     * whether its registration stands. So a renewal that finds the row locked while the stored lease is still the held
     * one skips the period and keeps the lease; only a different stored lease marks it lost.
     */
    private final class RegistrationLeaseRenewal {

        private final Duration duration;
        private final RegistrationLease heldRegistrationLease;
        private final Lock lock = new ReentrantLock();
        private @Nullable ScheduledFuture<?> scheduledFuture;
        private boolean stopped;

        private RegistrationLeaseRenewal(RegistrationLease heldRegistrationLease, Duration duration) {
            this.duration = duration;
            this.heldRegistrationLease = heldRegistrationLease;
        }

        private void start() {
            long periodMillis = Math.max(duration.toMillis() / 3, 1);

            Runnable renewal = new TenantTaskDecorator().decorate(this::renew);

            lock.lock();

            try {
                scheduledFuture = registrationLeaseRenewalExecutor.scheduleWithFixedDelay(
                    renewal, periodMillis, periodMillis, TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException rejectedExecutionException) {
                log.warn(
                    "Unable to schedule the registration lease renewal of trigger subscription id={}",
                    getTriggerSubscriptionId(), rejectedExecutionException);
            } finally {
                lock.unlock();
            }
        }

        private void stop() {
            lock.lock();

            try {
                stopped = true;

                cancel();
            } finally {
                lock.unlock();
            }
        }

        private void cancel() {
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
        }

        private @Nullable Long getTriggerSubscriptionId() {
            TriggerSubscription triggerSubscription = heldRegistrationLease.triggerSubscription();

            return triggerSubscription.getId();
        }

        private void renew() {
            lock.lock();

            try {
                if (stopped) {
                    return;
                }

                long id = Objects.requireNonNull(getTriggerSubscriptionId());

                Instant renewedLeaseUntil = Instant.now()
                    .plus(duration)
                    .truncatedTo(ChronoUnit.MILLIS);

                RegistrationLeaseExtension registrationLeaseExtension = transactionTemplate.execute(
                    transactionStatus -> triggerSubscriptionService.extendRegistrationLease(
                        id, heldRegistrationLease.leaseUntil(), renewedLeaseUntil));

                if (registrationLeaseExtension == RegistrationLeaseExtension.EXTENDED) {
                    heldRegistrationLease.renew(renewedLeaseUntil);

                    return;
                }

                if (registrationLeaseExtension == RegistrationLeaseExtension.LOCKED) {
                    log.debug(
                        "Skipped a registration lease renewal of trigger subscription id={} whose row another " +
                            "request holds locked",
                        id);

                    return;
                }

                heldRegistrationLease.markLost();

                stopped = true;

                cancel();

                log.warn(
                    "Registration lease of trigger subscription id={} was taken over by another request while its " +
                        "holder was registering or unregistering the trigger",
                    id);
            } catch (RuntimeException exception) {
                log.warn(
                    "Unable to renew the registration lease of trigger subscription id={}", getTriggerSubscriptionId(),
                    exception);
            } finally {
                lock.unlock();
            }
        }
    }

    @SuppressFBWarnings("EI")
    private record RegistrationResult(
        TriggerSubscription triggerSubscription, @Nullable RegistrationLease renewedRegistrationLease) {
    }
}
