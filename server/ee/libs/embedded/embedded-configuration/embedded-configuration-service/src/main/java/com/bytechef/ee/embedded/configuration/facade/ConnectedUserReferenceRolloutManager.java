/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.security.SkipAutomationAuthorization;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserReferenceDeployment;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceDeploymentManager.ReferenceResolution;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceDeploymentManager.RowSpec;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserReferenceDeploymentService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@SkipAutomationAuthorization
public class ConnectedUserReferenceRolloutManager {

    private static final Logger log = LoggerFactory.getLogger(ConnectedUserReferenceRolloutManager.class);

    private static final String DELETED_DANGLING_REASON = "The automation project was deleted";
    private static final String NOT_IN_RELEASED_VERSION_DANGLING_REASON =
        "Not in the version released to this environment";

    private final AutomationProjectConfigurationService automationProjectConfigurationService;
    private final ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService;
    private final ConnectedUserProjectService connectedUserProjectService;
    private final ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;
    private final ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager;
    private final ConnectedUserReferenceDeploymentService connectedUserReferenceDeploymentService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectWorkflowService projectWorkflowService;
    private final TransactionTemplate requiresNewTransactionTemplate;
    private final ConcurrentMap<RolloutKey, PendingRollout> rolloutsInFlight = new ConcurrentHashMap<>();

    @SuppressFBWarnings("EI")
    public ConnectedUserReferenceRolloutManager(
        AutomationProjectConfigurationService automationProjectConfigurationService,
        ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService,
        ConnectedUserProjectService connectedUserProjectService,
        ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository,
        ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager,
        ConnectedUserReferenceDeploymentService connectedUserReferenceDeploymentService,
        PlatformTransactionManager platformTransactionManager, ProjectDeploymentService projectDeploymentService,
        ProjectWorkflowService projectWorkflowService) {

        this.automationProjectConfigurationService = automationProjectConfigurationService;
        this.connectedUserProjectConfigurationService = connectedUserProjectConfigurationService;
        this.connectedUserProjectService = connectedUserProjectService;
        this.connectedUserProjectWorkflowRepository = connectedUserProjectWorkflowRepository;
        this.connectedUserReferenceDeploymentManager = connectedUserReferenceDeploymentManager;
        this.connectedUserReferenceDeploymentService = connectedUserReferenceDeploymentService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectWorkflowService = projectWorkflowService;

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        this.requiresNewTransactionTemplate = transactionTemplate;
    }

    public void deleteReferenceDeployments(long automationProjectId) {
        for (ProjectDeployment projectDeployment : projectDeploymentService.getAllProjectDeployments(
            automationProjectId)) {

            if (!connectedUserReferenceDeploymentManager.isReferenceDeployment(projectDeployment.getId())) {
                continue;
            }

            List<ConnectedUserProjectWorkflow> references = getReferences(projectDeployment.getId());

            for (ConnectedUserProjectWorkflow reference : references) {
                markDangling(reference, DELETED_DANGLING_REASON);
            }

            connectedUserReferenceDeploymentManager.deleteDeployment(projectDeployment.getId());

            connectedUserProjectWorkflowRepository.saveAll(references);
        }
    }

    /**
     * Deletes the reference workflows, the reference deployment and the configuration assignment of each connected user
     * still assigned to the configuration. Workflow copies are left alone. The assignment is read again after the
     * user's connected user projects are locked, so a user moved to another configuration after the ids were read keeps
     * the references of that configuration. Call inside a transaction: each user's connected user project stays locked
     * until it ends.
     *
     * @param automationProjectId the automation project id
     * @param configurationId     the automation project configuration id
     * @param connectedUserIds    the ids of the connected users assigned to the configuration
     */
    public void deleteReferenceDeployments(
        long automationProjectId, long configurationId, Collection<Long> connectedUserIds) {

        Set<String> automationUuids = getAutomationUuids(automationProjectId);

        for (long connectedUserId : getSortedDistinct(connectedUserIds)) {
            lockConnectedUserProjects(connectedUserId);

            Optional<ConnectedUserProjectConfiguration> assignmentOptional =
                connectedUserProjectConfigurationService.fetchAssignment(connectedUserId, automationProjectId);

            if (assignmentOptional.isEmpty()) {
                continue;
            }

            ConnectedUserProjectConfiguration assignment = assignmentOptional.get();

            if (!Objects.equals(assignment.getConfigurationId(), configurationId)) {
                continue;
            }

            connectedUserProjectWorkflowRepository.deleteAll(getReferences(connectedUserId, automationUuids));

            Optional<ConnectedUserReferenceDeployment> referenceDeploymentOptional =
                connectedUserReferenceDeploymentService.fetchConnectedUserReferenceDeployment(
                    connectedUserId, automationProjectId);

            if (referenceDeploymentOptional.isPresent()) {
                ConnectedUserReferenceDeployment referenceDeployment = referenceDeploymentOptional.get();

                connectedUserReferenceDeploymentManager.deleteDeployment(referenceDeployment.getProjectDeploymentId());
            }

            connectedUserProjectConfigurationService.delete(Validate.notNull(assignment.getId(), "id"));
        }
    }

    /**
     * Counts the reference workflows {@link #deleteReferenceDeployments(long, long, Collection)} would delete.
     *
     * @param automationProjectId the automation project id
     * @param connectedUserIds    the connected user ids
     * @return the number of reference workflows of those users in the project
     */
    public int countReferences(long automationProjectId, Collection<Long> connectedUserIds) {
        Set<String> automationUuids = getAutomationUuids(automationProjectId);

        int referenceCount = 0;

        for (long connectedUserId : getSortedDistinct(connectedUserIds)) {
            List<ConnectedUserProjectWorkflow> references = getReferences(connectedUserId, automationUuids);

            referenceCount += references.size();
        }

        return referenceCount;
    }

    public void applyConfigurationState(long automationProjectId, long environmentId) {
        applyConfigurationState(automationProjectId, environmentId, Map.of());
    }

    private void applyConfigurationState(
        long automationProjectId, long environmentId,
        Map<Long, Map<String, Set<String>>> removedProvidedInputNamesByConfigurationId) {

        List<ProjectDeployment> projectDeployments;

        try {
            projectDeployments = projectDeploymentService.getAllProjectDeployments(automationProjectId);
        } catch (RuntimeException exception) {
            log.error(
                "Applying the configuration state of automation project id={} failed",
                automationProjectId, exception);

            return;
        }

        for (ProjectDeployment projectDeployment : projectDeployments) {
            if (projectDeployment.getEnvironmentId() != environmentId) {
                continue;
            }

            Optional<Long> connectedUserIdOptional = fetchConnectedUserId(projectDeployment);

            if (connectedUserIdOptional.isEmpty()) {
                continue;
            }

            long connectedUserId = connectedUserIdOptional.get();

            applyConfigurationState(
                projectDeployment, connectedUserId,
                getRemovedProvidedInputNames(
                    connectedUserId, automationProjectId, removedProvidedInputNamesByConfigurationId));
        }
    }

    /**
     * Applies the configuration state of the deployment's connected user to every reference of the deployment, the same
     * way {@link #applyConfigurationState(long, long)} does, but inside the caller's transaction, so it sees what the
     * caller has not committed yet, such as a new assignment. A failing reference fails the call.
     *
     * @param projectDeploymentId       the reference deployment id
     * @param removedProvidedInputNames the provided input names to take off the rows, keyed by automation workflow uuid
     * @return the references of the deployment that are not dangling, or empty when the deployment no longer exists
     */
    public List<ConnectedUserProjectWorkflow> applyDeploymentConfigurationState(
        long projectDeploymentId, Map<String, Set<String>> removedProvidedInputNames) {

        Optional<ProjectDeployment> projectDeploymentOptional = connectedUserReferenceDeploymentManager.fetchDeployment(
            projectDeploymentId);

        if (projectDeploymentOptional.isEmpty()) {
            return List.of();
        }

        ProjectDeployment projectDeployment = projectDeploymentOptional.get();

        Optional<Long> connectedUserIdOptional = fetchConnectedUserId(projectDeployment);

        if (connectedUserIdOptional.isEmpty()) {
            return List.of();
        }

        long connectedUserId = connectedUserIdOptional.get();

        List<ConnectedUserProjectWorkflow> references = getReferences(projectDeploymentId)
            .stream()
            .filter(reference -> !reference.isDangling())
            .toList();

        for (ConnectedUserProjectWorkflow reference : references) {
            applyConfigurationState(projectDeployment, connectedUserId, reference, removedProvidedInputNames);
        }

        return references;
    }

    public void rollOut(long automationProjectId, long environmentId) {
        rollOut(automationProjectId, environmentId, Map.of());
    }

    private void rollOut(
        long automationProjectId, long environmentId,
        Map<Long, Map<String, Set<String>>> removedProvidedInputNamesByConfigurationId) {

        List<ProjectDeployment> projectDeployments;

        try {
            projectDeployments = projectDeploymentService.getAllProjectDeployments(automationProjectId);
        } catch (RuntimeException exception) {
            log.error("Rolling out automation project id={} failed", automationProjectId, exception);

            return;
        }

        for (ProjectDeployment projectDeployment : projectDeployments) {
            if (projectDeployment.getEnvironmentId() != environmentId) {
                continue;
            }

            try {
                requiresNewTransactionTemplate.executeWithoutResult(
                    status -> rollOutDeploymentIfBehind(
                        projectDeployment, removedProvidedInputNamesByConfigurationId));
            } catch (RuntimeException exception) {
                logRolloutFailure(automationProjectId, projectDeployment.getId(), exception);
            }
        }
    }

    public void rollOutAndApply(long automationProjectId, long environmentId) {
        rollOutAndApply(automationProjectId, environmentId, Map.of());
    }

    /**
     * Rolls out the project in the environment and applies the configuration state, taking the inputs the configuration
     * stopped providing off the deployments of the users assigned to that configuration only.
     *
     * @param automationProjectId       the automation project id
     * @param environmentId             the environment id
     * @param configurationId           the configuration whose provided inputs were removed
     * @param removedProvidedInputNames the removed provided input names keyed by automation workflow uuid
     */
    public void rollOutAndApply(
        long automationProjectId, long environmentId, long configurationId,
        Map<String, Set<String>> removedProvidedInputNames) {

        rollOutAndApply(
            automationProjectId, environmentId,
            removedProvidedInputNames.isEmpty()
                ? Map.<Long, Map<String, Set<String>>>of() : Map.of(configurationId, removedProvidedInputNames));
    }

    private void rollOutAndApply(
        long automationProjectId, long environmentId,
        Map<Long, Map<String, Set<String>>> removedProvidedInputNamesByConfigurationId) {

        RolloutKey rolloutKey = new RolloutKey(automationProjectId, environmentId);
        AtomicBoolean startedRollout = new AtomicBoolean();

        rolloutsInFlight.compute(rolloutKey, (key, pendingRollout) -> {
            PendingRollout requestedRollout = pendingRollout;

            if (requestedRollout == null) {
                requestedRollout = new PendingRollout();

                startedRollout.set(true);
            }

            requestedRollout.request(removedProvidedInputNamesByConfigurationId);

            return requestedRollout;
        });

        if (!startedRollout.get()) {
            return;
        }

        boolean finished = false;

        try {
            Map<Long, Map<String, Set<String>>> passRemovedProvidedInputNames = takePendingPass(rolloutKey);

            while (passRemovedProvidedInputNames != null) {
                runRolloutPass(automationProjectId, environmentId, passRemovedProvidedInputNames);

                passRemovedProvidedInputNames = takePendingPass(rolloutKey);
            }

            finished = true;
        } finally {
            if (!finished) {
                rolloutsInFlight.remove(rolloutKey);
            }
        }
    }

    public boolean rollOutDeploymentIfBehind(long projectDeploymentId) {
        ProjectDeployment projectDeployment = connectedUserReferenceDeploymentManager.getDeployment(
            projectDeploymentId);

        return rollOutDeploymentIfBehind(projectDeployment, Map.of());
    }

    private boolean rollOutDeploymentIfBehind(
        ProjectDeployment projectDeployment,
        Map<Long, Map<String, Set<String>>> removedProvidedInputNamesByConfigurationId) {

        Optional<Long> connectedUserIdOptional = fetchConnectedUserId(projectDeployment);

        if (connectedUserIdOptional.isEmpty()) {
            return false;
        }

        long connectedUserId = connectedUserIdOptional.get();

        Optional<Integer> releasedVersion = connectedUserReferenceDeploymentManager.fetchReleasedVersion(
            connectedUserId, projectDeployment.getProjectId());

        if (releasedVersion.isEmpty()) {
            log.debug(
                "Automation project id={} is not released to connected user id={}; nothing to roll out to " +
                    "deployment id={}",
                projectDeployment.getProjectId(), connectedUserId, projectDeployment.getId());

            return false;
        }

        if (projectDeployment.getProjectVersion() == releasedVersion.get()) {
            return false;
        }

        return rollOutDeploymentWithReferences(
            projectDeployment, connectedUserId, releasedVersion.get(),
            getRemovedProvidedInputNames(
                connectedUserId, projectDeployment.getProjectId(), removedProvidedInputNamesByConfigurationId));
    }

    private Optional<Long> fetchConnectedUserId(ProjectDeployment projectDeployment) {
        Optional<Long> connectedUserIdOptional = connectedUserReferenceDeploymentManager.fetchConnectedUserId(
            projectDeployment.getId());

        if (connectedUserIdOptional.isEmpty()) {
            log.debug(
                "Deployment id={} of automation project id={} has no connected user; skipping it",
                projectDeployment.getId(), projectDeployment.getProjectId());
        }

        return connectedUserIdOptional;
    }

    private void runRolloutPass(
        long automationProjectId, long environmentId,
        Map<Long, Map<String, Set<String>>> removedProvidedInputNamesByConfigurationId) {

        log.debug(
            "Running a rollout pass of automation project id={} in environment id={}",
            automationProjectId, environmentId);

        try {
            requiresNewTransactionTemplate.executeWithoutResult(status -> {
                automationProjectConfigurationService.lockRollout(automationProjectId, environmentId);

                // rollOut reads the configuration only now, so it sees the newest state committed before the lock
                rollOut(automationProjectId, environmentId, removedProvidedInputNamesByConfigurationId);
                applyConfigurationState(
                    automationProjectId, environmentId, removedProvidedInputNamesByConfigurationId);
            });
        } catch (RuntimeException exception) {
            log.error(
                "Rolling out automation project id={} in environment id={} failed",
                automationProjectId, environmentId, exception);
        }
    }

    private @Nullable Map<Long, Map<String, Set<String>>> takePendingPass(RolloutKey rolloutKey) {
        AtomicReference<Map<Long, Map<String, Set<String>>>> passRemovedProvidedInputNames = new AtomicReference<>();

        rolloutsInFlight.compute(rolloutKey, (key, pendingRollout) -> {
            if (pendingRollout == null || !pendingRollout.isPending()) {
                return null;
            }

            passRemovedProvidedInputNames.set(pendingRollout.take());

            return pendingRollout;
        });

        return passRemovedProvidedInputNames.get();
    }

    private void applyConfigurationState(
        ProjectDeployment projectDeployment, long connectedUserId, Map<String, Set<String>> removedProvidedInputNames) {

        long projectDeploymentId = projectDeployment.getId();

        List<ConnectedUserProjectWorkflow> references;

        try {
            references = getReferences(projectDeploymentId);
        } catch (RuntimeException exception) {
            log.error(
                "Applying the configuration state to deployment id={} failed", projectDeploymentId, exception);

            return;
        }

        for (ConnectedUserProjectWorkflow reference : references) {
            if (reference.isDangling()) {
                continue;
            }

            try {
                requiresNewTransactionTemplate.executeWithoutResult(
                    status -> applyConfigurationState(
                        projectDeployment, connectedUserId, reference, removedProvidedInputNames));
            } catch (RuntimeException exception) {
                log.error(
                    "Applying the configuration state to reference id={} of deployment id={} failed",
                    reference.getId(), projectDeploymentId, exception);
            }
        }
    }

    private void applyConfigurationState(
        ProjectDeployment projectDeployment, long connectedUserId, ConnectedUserProjectWorkflow reference,
        Map<String, Set<String>> removedProvidedInputNames) {

        long projectDeploymentId = projectDeployment.getId();
        String automationUuid = reference.getAutomationUuid();
        Set<String> removedInputNames = removedProvidedInputNames.getOrDefault(automationUuid, Set.of());

        Optional<ProjectDeploymentWorkflow> row = connectedUserReferenceDeploymentManager.fetchRow(
            projectDeploymentId, automationUuid);

        if (row.isEmpty()) {
            return;
        }

        if (!reference.isEnabled() || !connectedUserReferenceDeploymentManager.isAdministrativelyEnabled(
            connectedUserId, projectDeployment.getProjectId(), automationUuid)) {

            connectedUserReferenceDeploymentManager.setRowEnabled(projectDeploymentId, row.get(), false);

            connectedUserReferenceDeploymentManager.removeInputs(
                projectDeploymentId, automationUuid, removedInputNames);

            return;
        }

        RowSpec rowSpec = resolveRowSpec(
            reference, projectDeployment, connectedUserId, projectDeployment.getProjectVersion(), removedInputNames);

        connectedUserReferenceDeploymentManager.putWorkflow(projectDeploymentId, automationUuid, rowSpec);
    }

    private Set<String> getAutomationUuids(long automationProjectId) {
        return projectWorkflowService.getProjectWorkflows(automationProjectId)
            .stream()
            .map(ProjectWorkflow::getUuidAsString)
            .collect(Collectors.toSet());
    }

    private Map<String, Set<String>> getRemovedProvidedInputNames(
        long connectedUserId, long automationProjectId,
        Map<Long, Map<String, Set<String>>> removedProvidedInputNamesByConfigurationId) {

        if (removedProvidedInputNamesByConfigurationId.isEmpty()) {
            return Map.of();
        }

        return connectedUserProjectConfigurationService.fetchAssignment(connectedUserId, automationProjectId)
            .map(assignment -> removedProvidedInputNamesByConfigurationId.getOrDefault(
                assignment.getConfigurationId(), Map.of()))
            .orElse(Map.of());
    }

    private List<ConnectedUserProjectWorkflow> getReferences(
        long connectedUserId, Set<String> automationUuids) {

        return connectedUserProjectWorkflowRepository.findAllByConnectedUserId(connectedUserId)
            .stream()
            .filter(reference -> automationUuids.contains(reference.getAutomationUuid()))
            .toList();
    }

    private static List<Long> getSortedDistinct(Collection<Long> connectedUserIds) {
        return connectedUserIds.stream()
            .distinct()
            .sorted()
            .toList();
    }

    private void lockConnectedUserProjects(long connectedUserId) {
        List<Long> connectedUserProjectIds = connectedUserProjectService
            .getConnectedUserConnectedUserProjects(connectedUserId)
            .stream()
            .map(ConnectedUserProject::getId)
            .sorted()
            .toList();

        for (long connectedUserProjectId : connectedUserProjectIds) {
            connectedUserProjectService.lockConnectedUserProject(connectedUserProjectId);
        }
    }

    private List<ConnectedUserProjectWorkflow> getReferences(long projectDeploymentId) {
        return connectedUserProjectWorkflowRepository.findAllByProjectDeploymentId(projectDeploymentId)
            .stream()
            .filter(reference -> reference.getAutomationUuid() != null)
            .toList();
    }

    private static void markDangling(ConnectedUserProjectWorkflow reference, String danglingReason) {
        if (!reference.isDangling()) {
            reference.setDangling(true);
            reference.setDanglingReason(danglingReason);
        }

        reference.setEnabled(false);
    }

    private static void
        logRolloutFailure(long automationProjectId, long projectDeploymentId, RuntimeException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof OptimisticLockingFailureException) {
                log.warn(
                    "Rolling out automation project id={} to deployment id={} lost a concurrent update; it will converge "
                        +
                        "on the next publish or enable",
                    automationProjectId, projectDeploymentId, exception);

                return;
            }
        }

        log.error(
            "Rolling out automation project id={} to deployment id={} failed", automationProjectId,
            projectDeploymentId,
            exception);
    }

    private boolean rollOutDeploymentWithReferences(
        ProjectDeployment projectDeployment, long connectedUserId, int releasedVersion,
        Map<String, Set<String>> removedProvidedInputNames) {

        long projectDeploymentId = projectDeployment.getId();

        List<ConnectedUserProjectWorkflow> references = getReferences(projectDeploymentId);

        if (references.isEmpty()) {
            connectedUserReferenceDeploymentManager.deleteDeployment(projectDeploymentId);

            return true;
        }

        rollOutDeployment(projectDeployment, connectedUserId, releasedVersion, references, removedProvidedInputNames);

        return true;
    }

    private void rollOutDeployment(
        ProjectDeployment projectDeployment, long connectedUserId, int releasedVersion,
        List<ConnectedUserProjectWorkflow> references, Map<String, Set<String>> removedProvidedInputNames) {

        long automationProjectId = projectDeployment.getProjectId();
        long projectDeploymentId = projectDeployment.getId();

        Set<String> publishedAutomationUuids = projectWorkflowService
            .getProjectWorkflows(automationProjectId, releasedVersion)
            .stream()
            .map(ProjectWorkflow::getUuidAsString)
            .collect(Collectors.toSet());

        Map<String, RowSpec> rowSpecsByAutomationUuid = new LinkedHashMap<>();

        for (ConnectedUserProjectWorkflow reference : references) {
            String automationUuid = reference.getAutomationUuid();

            if (reference.isDangling() || !publishedAutomationUuids.contains(automationUuid)) {
                markDangling(reference, NOT_IN_RELEASED_VERSION_DANGLING_REASON);

                continue;
            }

            RowSpec rowSpec = resolveRowSpec(
                reference, projectDeployment, connectedUserId, releasedVersion,
                removedProvidedInputNames.getOrDefault(automationUuid, Set.of()));

            boolean rowEnabled = rowSpec.enabled() && connectedUserReferenceDeploymentManager.isAdministrativelyEnabled(
                connectedUserId, automationProjectId, automationUuid);

            rowSpecsByAutomationUuid.put(
                automationUuid, new RowSpec(rowSpec.resolved(), rowEnabled, rowSpec.inputs()));
        }

        if (rowSpecsByAutomationUuid.isEmpty()) {
            connectedUserReferenceDeploymentManager.deleteDeployment(projectDeploymentId);
        } else {
            connectedUserReferenceDeploymentManager.putWorkflows(
                projectDeploymentId, releasedVersion, rowSpecsByAutomationUuid);
        }

        connectedUserProjectWorkflowRepository.saveAll(references);
    }

    private RowSpec resolveRowSpec(
        ConnectedUserProjectWorkflow reference, ProjectDeployment projectDeployment, long connectedUserId,
        int projectVersion, Set<String> removedInputNames) {

        long automationProjectId = projectDeployment.getProjectId();
        String automationUuid = reference.getAutomationUuid();

        Optional<ProjectDeploymentWorkflow> currentRow = connectedUserReferenceDeploymentManager.fetchRow(
            projectDeployment.getId(), automationUuid);

        String workflowId = connectedUserReferenceDeploymentManager.getWorkflowId(
            automationProjectId, projectVersion, automationUuid);

        Map<String, Object> userInputs = new LinkedHashMap<>(
            currentRow.<Map<String, ?>>map(ProjectDeploymentWorkflow::getInputs)
                .orElse(Map.of()));

        for (String removedInputName : removedInputNames) {
            userInputs.remove(removedInputName);
        }

        ReferenceResolution referenceResolution = connectedUserReferenceDeploymentManager.resolveReference(
            connectedUserId, automationProjectId, automationUuid, workflowId, reference.isEnabled(),
            Map.of(),
            currentRow.map(ProjectDeploymentWorkflow::getConnections)
                .orElse(List.of()),
            userInputs);

        return referenceResolution.rowSpec();
    }

    private record RolloutKey(long automationProjectId, long environmentId) {
    }

    // Read and written only inside an atomic compute on rolloutsInFlight, so a request is never lost.
    private static final class PendingRollout {

        private final Map<Long, Map<String, Set<String>>> removedProvidedInputNamesByConfigurationId =
            new HashMap<>();
        private boolean pending;

        boolean isPending() {
            return pending;
        }

        void request(Map<Long, Map<String, Set<String>>> requestedRemovedProvidedInputNamesByConfigurationId) {
            pending = true;

            for (Map.Entry<Long, Map<String, Set<String>>> configurationEntry : requestedRemovedProvidedInputNamesByConfigurationId
                .entrySet()) {

                Map<String, Set<String>> removedProvidedInputNames = removedProvidedInputNamesByConfigurationId
                    .computeIfAbsent(configurationEntry.getKey(), configurationId -> new HashMap<>());

                for (Map.Entry<String, Set<String>> workflowEntry : configurationEntry.getValue()
                    .entrySet()) {

                    Set<String> inputNames = removedProvidedInputNames.computeIfAbsent(
                        workflowEntry.getKey(), workflowUuid -> new HashSet<>());

                    inputNames.addAll(workflowEntry.getValue());
                }
            }
        }

        Map<Long, Map<String, Set<String>>> take() {
            pending = false;

            Map<Long, Map<String, Set<String>>> takenRemovedProvidedInputNamesByConfigurationId = new HashMap<>();

            for (Map.Entry<Long, Map<String, Set<String>>> configurationEntry : removedProvidedInputNamesByConfigurationId
                .entrySet()) {

                Map<String, Set<String>> takenRemovedProvidedInputNames = new HashMap<>();

                for (Map.Entry<String, Set<String>> workflowEntry : configurationEntry.getValue()
                    .entrySet()) {

                    takenRemovedProvidedInputNames.put(workflowEntry.getKey(), Set.copyOf(workflowEntry.getValue()));
                }

                takenRemovedProvidedInputNamesByConfigurationId.put(
                    configurationEntry.getKey(), Map.copyOf(takenRemovedProvidedInputNames));
            }

            removedProvidedInputNamesByConfigurationId.clear();

            return takenRemovedProvidedInputNamesByConfigurationId;
        }
    }
}
