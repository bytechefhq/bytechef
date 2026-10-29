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
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceDeploymentManager.ReferenceResolution;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceDeploymentManager.RowSpec;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
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

    private static final String DELETED_DANGLING_REASON = "The automation workflow project was deleted";
    private static final String NOT_IN_RELEASED_VERSION_DANGLING_REASON =
        "Not in the version released to this environment";

    private final AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService;
    private final ConnectedUserProjectService connectedUserProjectService;
    private final ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;
    private final ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectWorkflowService projectWorkflowService;
    private final TransactionTemplate requiresNewTransactionTemplate;
    private final ConcurrentMap<RolloutKey, PendingRollout> rolloutsInFlight = new ConcurrentHashMap<>();

    @SuppressFBWarnings("EI")
    public ConnectedUserReferenceRolloutManager(
        AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService,
        ConnectedUserProjectService connectedUserProjectService,
        ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository,
        ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager,
        PlatformTransactionManager platformTransactionManager, ProjectDeploymentService projectDeploymentService,
        ProjectWorkflowService projectWorkflowService) {

        this.automationWorkflowProjectConfigurationService = automationWorkflowProjectConfigurationService;
        this.connectedUserProjectService = connectedUserProjectService;
        this.connectedUserProjectWorkflowRepository = connectedUserProjectWorkflowRepository;
        this.connectedUserReferenceDeploymentManager = connectedUserReferenceDeploymentManager;
        this.projectDeploymentService = projectDeploymentService;
        this.projectWorkflowService = projectWorkflowService;

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        this.requiresNewTransactionTemplate = transactionTemplate;
    }

    public void deleteReferenceDeployments(long automationWorkflowProjectId) {
        for (ProjectDeployment projectDeployment : projectDeploymentService.getAllProjectDeployments(
            automationWorkflowProjectId)) {

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

    public void applyConfigurationState(long automationWorkflowProjectId, long environmentId) {
        applyConfigurationState(automationWorkflowProjectId, environmentId, Map.of());
    }

    public void applyConfigurationState(
        long automationWorkflowProjectId, long environmentId, Map<String, Set<String>> removedProvidedInputNames) {

        List<ProjectDeployment> projectDeployments;

        try {
            projectDeployments = projectDeploymentService.getAllProjectDeployments(automationWorkflowProjectId);
        } catch (RuntimeException exception) {
            log.error(
                "Applying the configuration state of automation workflow project id={} failed",
                automationWorkflowProjectId, exception);

            return;
        }

        for (ProjectDeployment projectDeployment : projectDeployments) {
            if (projectDeployment.getEnvironmentId() != environmentId ||
                !connectedUserReferenceDeploymentManager.isReferenceDeployment(projectDeployment.getId())) {

                continue;
            }

            applyConfigurationState(projectDeployment, removedProvidedInputNames);
        }
    }

    public void rollOut(long automationWorkflowProjectId, long environmentId) {
        rollOut(automationWorkflowProjectId, environmentId, Map.of());
    }

    private void rollOut(
        long automationWorkflowProjectId, long environmentId, Map<String, Set<String>> removedProvidedInputNames) {

        int releasedVersion;
        List<ProjectDeployment> projectDeployments;

        try {
            Environment environment = Environment.values()[(int) environmentId];

            Optional<Integer> fetchedReleasedVersion = connectedUserReferenceDeploymentManager.fetchReleasedVersion(
                automationWorkflowProjectId, environment);

            if (fetchedReleasedVersion.isEmpty()) {
                log.debug(
                    "Automation workflow project id={} is not released in environment id={}; nothing to roll out",
                    automationWorkflowProjectId, environmentId);

                return;
            }

            releasedVersion = fetchedReleasedVersion.get();
            projectDeployments = projectDeploymentService.getAllProjectDeployments(automationWorkflowProjectId);
        } catch (RuntimeException exception) {
            log.error("Rolling out automation workflow project id={} failed", automationWorkflowProjectId, exception);

            return;
        }

        for (ProjectDeployment projectDeployment : projectDeployments) {
            if (projectDeployment.getEnvironmentId() != environmentId ||
                projectDeployment.getProjectVersion() == releasedVersion) {

                continue;
            }

            try {
                requiresNewTransactionTemplate.executeWithoutResult(
                    status -> rollOutDeploymentWithReferences(
                        projectDeployment, releasedVersion, removedProvidedInputNames));
            } catch (RuntimeException exception) {
                logRolloutFailure(automationWorkflowProjectId, projectDeployment.getId(), exception);
            }
        }
    }

    public void rollOutAndApply(long automationWorkflowProjectId, long environmentId) {
        rollOutAndApply(automationWorkflowProjectId, environmentId, Map.of());
    }

    public void rollOutAndApply(
        long automationWorkflowProjectId, long environmentId, Map<String, Set<String>> removedProvidedInputNames) {

        RolloutKey rolloutKey = new RolloutKey(automationWorkflowProjectId, environmentId);
        AtomicBoolean startedRollout = new AtomicBoolean();

        rolloutsInFlight.compute(rolloutKey, (key, pendingRollout) -> {
            PendingRollout requestedRollout = pendingRollout;

            if (requestedRollout == null) {
                requestedRollout = new PendingRollout();

                startedRollout.set(true);
            }

            requestedRollout.request(removedProvidedInputNames);

            return requestedRollout;
        });

        if (!startedRollout.get()) {
            return;
        }

        boolean finished = false;

        try {
            Map<String, Set<String>> passRemovedProvidedInputNames = takePendingPass(rolloutKey);

            while (passRemovedProvidedInputNames != null) {
                runRolloutPass(automationWorkflowProjectId, environmentId, passRemovedProvidedInputNames);

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

        Optional<Integer> releasedVersion = connectedUserReferenceDeploymentManager.fetchReleasedVersion(
            projectDeployment.getProjectId(), projectDeployment.getEnvironment());

        if (releasedVersion.isEmpty() || projectDeployment.getProjectVersion() == releasedVersion.get()) {
            return false;
        }

        return rollOutDeploymentWithReferences(projectDeployment, releasedVersion.get());
    }

    private void runRolloutPass(
        long automationWorkflowProjectId, long environmentId, Map<String, Set<String>> removedProvidedInputNames) {

        log.debug(
            "Running a rollout pass of automation workflow project id={} in environment id={}",
            automationWorkflowProjectId, environmentId);

        try {
            requiresNewTransactionTemplate.executeWithoutResult(status -> {
                automationWorkflowProjectConfigurationService.lockRollout(automationWorkflowProjectId, environmentId);

                // rollOut reads the configuration only now, so it sees the newest state committed before the lock
                rollOut(automationWorkflowProjectId, environmentId, removedProvidedInputNames);
                applyConfigurationState(automationWorkflowProjectId, environmentId, removedProvidedInputNames);
            });
        } catch (RuntimeException exception) {
            log.error(
                "Rolling out automation workflow project id={} in environment id={} failed",
                automationWorkflowProjectId, environmentId, exception);
        }
    }

    private @Nullable Map<String, Set<String>> takePendingPass(RolloutKey rolloutKey) {
        AtomicReference<Map<String, Set<String>>> passRemovedProvidedInputNames = new AtomicReference<>();

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
        ProjectDeployment projectDeployment, Map<String, Set<String>> removedProvidedInputNames) {

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
                    status -> applyConfigurationState(projectDeployment, reference, removedProvidedInputNames));
            } catch (RuntimeException exception) {
                log.error(
                    "Applying the configuration state to reference id={} of deployment id={} failed",
                    reference.getId(), projectDeploymentId, exception);
            }
        }
    }

    private void applyConfigurationState(
        ProjectDeployment projectDeployment, ConnectedUserProjectWorkflow reference,
        Map<String, Set<String>> removedProvidedInputNames) {

        long projectDeploymentId = projectDeployment.getId();
        String automationWorkflowUuid = reference.getAutomationWorkflowUuid();
        Set<String> removedInputNames = removedProvidedInputNames.getOrDefault(automationWorkflowUuid, Set.of());

        Optional<ProjectDeploymentWorkflow> row = connectedUserReferenceDeploymentManager.fetchRow(
            projectDeploymentId, automationWorkflowUuid);

        if (row.isEmpty()) {
            return;
        }

        if (!reference.isEnabled() || !connectedUserReferenceDeploymentManager.isAdministrativelyEnabled(
            projectDeployment.getProjectId(), projectDeployment.getEnvironment(), automationWorkflowUuid)) {

            connectedUserReferenceDeploymentManager.setRowEnabled(projectDeploymentId, row.get(), false);

            connectedUserReferenceDeploymentManager.removeInputs(
                projectDeploymentId, automationWorkflowUuid, removedInputNames);

            return;
        }

        RowSpec rowSpec = resolveRowSpec(
            reference, projectDeployment, projectDeployment.getProjectVersion(), removedInputNames);

        connectedUserReferenceDeploymentManager.putWorkflow(projectDeploymentId, automationWorkflowUuid, rowSpec);
    }

    private List<ConnectedUserProjectWorkflow> getReferences(long projectDeploymentId) {
        return connectedUserProjectWorkflowRepository.findAllByProjectDeploymentId(projectDeploymentId)
            .stream()
            .filter(reference -> reference.getAutomationWorkflowUuid() != null)
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
        logRolloutFailure(long automationWorkflowProjectId, long projectDeploymentId, RuntimeException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof OptimisticLockingFailureException) {
                log.warn(
                    "Rolling out automation workflow project id={} to deployment id={} lost a concurrent update; it will converge "
                        +
                        "on the next publish or enable",
                    automationWorkflowProjectId, projectDeploymentId, exception);

                return;
            }
        }

        log.error(
            "Rolling out automation workflow project id={} to deployment id={} failed", automationWorkflowProjectId,
            projectDeploymentId,
            exception);
    }

    private boolean rollOutDeploymentWithReferences(ProjectDeployment projectDeployment, int releasedVersion) {
        return rollOutDeploymentWithReferences(projectDeployment, releasedVersion, Map.of());
    }

    private boolean rollOutDeploymentWithReferences(
        ProjectDeployment projectDeployment, int releasedVersion, Map<String, Set<String>> removedProvidedInputNames) {

        long projectDeploymentId = projectDeployment.getId();

        if (!connectedUserReferenceDeploymentManager.isReferenceDeployment(projectDeploymentId)) {
            return false;
        }

        List<ConnectedUserProjectWorkflow> references = getReferences(projectDeploymentId);

        if (references.isEmpty()) {
            connectedUserReferenceDeploymentManager.deleteDeployment(projectDeploymentId);

            return true;
        }

        rollOutDeployment(projectDeployment, releasedVersion, references, removedProvidedInputNames);

        return true;
    }

    private void rollOutDeployment(
        ProjectDeployment projectDeployment, int releasedVersion, List<ConnectedUserProjectWorkflow> references,
        Map<String, Set<String>> removedProvidedInputNames) {

        long automationWorkflowProjectId = projectDeployment.getProjectId();
        long projectDeploymentId = projectDeployment.getId();

        Set<String> publishedAutomationWorkflowUuids = projectWorkflowService
            .getProjectWorkflows(automationWorkflowProjectId, releasedVersion)
            .stream()
            .map(ProjectWorkflow::getUuidAsString)
            .collect(Collectors.toSet());

        Map<String, RowSpec> rowSpecsByAutomationWorkflowUuid = new LinkedHashMap<>();

        for (ConnectedUserProjectWorkflow reference : references) {
            String automationWorkflowUuid = reference.getAutomationWorkflowUuid();

            if (reference.isDangling() || !publishedAutomationWorkflowUuids.contains(automationWorkflowUuid)) {
                markDangling(reference, NOT_IN_RELEASED_VERSION_DANGLING_REASON);

                continue;
            }

            RowSpec rowSpec = resolveRowSpec(
                reference, projectDeployment, releasedVersion,
                removedProvidedInputNames.getOrDefault(automationWorkflowUuid, Set.of()));

            boolean rowEnabled = rowSpec.enabled() && connectedUserReferenceDeploymentManager.isAdministrativelyEnabled(
                automationWorkflowProjectId, projectDeployment.getEnvironment(), automationWorkflowUuid);

            rowSpecsByAutomationWorkflowUuid.put(
                automationWorkflowUuid, new RowSpec(rowSpec.resolved(), rowEnabled, rowSpec.inputs()));
        }

        if (rowSpecsByAutomationWorkflowUuid.isEmpty()) {
            connectedUserReferenceDeploymentManager.deleteDeployment(projectDeploymentId);
        } else {
            connectedUserReferenceDeploymentManager.putWorkflows(
                projectDeploymentId, releasedVersion, rowSpecsByAutomationWorkflowUuid);
        }

        connectedUserProjectWorkflowRepository.saveAll(references);
    }

    private RowSpec resolveRowSpec(
        ConnectedUserProjectWorkflow reference, ProjectDeployment projectDeployment, int projectVersion,
        Set<String> removedInputNames) {

        long automationWorkflowProjectId = projectDeployment.getProjectId();
        String automationWorkflowUuid = reference.getAutomationWorkflowUuid();

        ConnectedUserProject connectedUserProject = connectedUserProjectService.getConnectedUserProject(
            reference.getConnectedUserProjectId());
        Optional<ProjectDeploymentWorkflow> currentRow = connectedUserReferenceDeploymentManager.fetchRow(
            projectDeployment.getId(), automationWorkflowUuid);

        String workflowId = connectedUserReferenceDeploymentManager.getWorkflowId(
            automationWorkflowProjectId, projectVersion, automationWorkflowUuid);

        Map<String, Object> userInputs = new LinkedHashMap<>(
            currentRow.<Map<String, ?>>map(ProjectDeploymentWorkflow::getInputs)
                .orElse(Map.of()));

        for (String removedInputName : removedInputNames) {
            userInputs.remove(removedInputName);
        }

        ReferenceResolution referenceResolution = connectedUserReferenceDeploymentManager.resolveReference(
            connectedUserProject.getConnectedUserId(), automationWorkflowProjectId, projectDeployment.getEnvironment(),
            automationWorkflowUuid, workflowId, reference.isEnabled(), Map.of(),
            currentRow.map(ProjectDeploymentWorkflow::getConnections)
                .orElse(List.of()),
            userInputs);

        return referenceResolution.rowSpec();
    }

    private record RolloutKey(long automationWorkflowProjectId, long environmentId) {
    }

    // Read and written only inside an atomic compute on rolloutsInFlight, so a request is never lost.
    private static final class PendingRollout {

        private final Map<String, Set<String>> removedProvidedInputNames = new HashMap<>();
        private boolean pending;

        boolean isPending() {
            return pending;
        }

        void request(Map<String, Set<String>> requestedRemovedProvidedInputNames) {
            pending = true;

            for (Map.Entry<String, Set<String>> entry : requestedRemovedProvidedInputNames.entrySet()) {
                Set<String> inputNames = removedProvidedInputNames.computeIfAbsent(
                    entry.getKey(), workflowUuid -> new HashSet<>());

                inputNames.addAll(entry.getValue());
            }
        }

        Map<String, Set<String>> take() {
            pending = false;

            Map<String, Set<String>> takenRemovedProvidedInputNames = new HashMap<>();

            for (Map.Entry<String, Set<String>> entry : removedProvidedInputNames.entrySet()) {
                takenRemovedProvidedInputNames.put(entry.getKey(), Set.copyOf(entry.getValue()));
            }

            removedProvidedInputNames.clear();

            return takenRemovedProvidedInputNames;
        }
    }
}
