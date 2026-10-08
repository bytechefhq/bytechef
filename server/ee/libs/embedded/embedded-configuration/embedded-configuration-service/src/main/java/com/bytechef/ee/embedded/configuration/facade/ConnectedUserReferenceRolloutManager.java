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
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
    private static final String REMOVED_DANGLING_REASON = "Removed from the automation workflow project on publish";

    private final ConnectedUserProjectService connectedUserProjectService;
    private final ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;
    private final ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectWorkflowService projectWorkflowService;
    private final TransactionTemplate requiresNewTransactionTemplate;

    @SuppressFBWarnings("EI")
    public ConnectedUserReferenceRolloutManager(
        ConnectedUserProjectService connectedUserProjectService,
        ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository,
        ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager,
        PlatformTransactionManager platformTransactionManager, ProjectDeploymentService projectDeploymentService,
        ProjectWorkflowService projectWorkflowService) {

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

            if (!ConnectedUserReferenceDeploymentManager.isReferenceDeployment(projectDeployment)) {
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

    public void rollOut(long automationWorkflowProjectId) {
        int lastPublishedVersion;
        List<ProjectDeployment> projectDeployments;

        try {
            lastPublishedVersion =
                connectedUserReferenceDeploymentManager.getLastPublishedVersion(automationWorkflowProjectId);
            projectDeployments = projectDeploymentService.getAllProjectDeployments(automationWorkflowProjectId);
        } catch (RuntimeException exception) {
            log.error("Rolling out automation workflow project id={} failed", automationWorkflowProjectId, exception);

            return;
        }

        for (ProjectDeployment projectDeployment : projectDeployments) {
            if (projectDeployment.getProjectVersion() >= lastPublishedVersion) {
                continue;
            }

            try {
                requiresNewTransactionTemplate.executeWithoutResult(
                    status -> rollOutDeploymentWithReferences(projectDeployment, lastPublishedVersion));
            } catch (RuntimeException exception) {
                logRolloutFailure(automationWorkflowProjectId, projectDeployment.getId(), exception);
            }
        }
    }

    public boolean rollOutDeploymentIfBehind(long projectDeploymentId) {
        ProjectDeployment projectDeployment = connectedUserReferenceDeploymentManager.getDeployment(
            projectDeploymentId);

        int lastPublishedVersion = connectedUserReferenceDeploymentManager.getLastPublishedVersion(
            projectDeployment.getProjectId());

        if (projectDeployment.getProjectVersion() >= lastPublishedVersion) {
            return false;
        }

        rollOutDeploymentWithReferences(projectDeployment, lastPublishedVersion);

        return true;
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

    private void rollOutDeploymentWithReferences(ProjectDeployment projectDeployment, int lastPublishedVersion) {
        if (!ConnectedUserReferenceDeploymentManager.isReferenceDeployment(projectDeployment)) {
            return;
        }

        List<ConnectedUserProjectWorkflow> references = getReferences(projectDeployment.getId());

        if (references.isEmpty()) {
            connectedUserReferenceDeploymentManager.deleteDeployment(projectDeployment.getId());

            return;
        }

        rollOutDeployment(projectDeployment, lastPublishedVersion, references);
    }

    private void rollOutDeployment(
        ProjectDeployment projectDeployment, int lastPublishedVersion, List<ConnectedUserProjectWorkflow> references) {
        long automationWorkflowProjectId = projectDeployment.getProjectId();
        long projectDeploymentId = projectDeployment.getId();

        Set<String> publishedAutomationWorkflowUuids = projectWorkflowService
            .getProjectWorkflows(automationWorkflowProjectId, lastPublishedVersion)
            .stream()
            .map(ProjectWorkflow::getUuidAsString)
            .collect(Collectors.toSet());

        Map<String, RowSpec> rowSpecsByAutomationWorkflowUuid = new LinkedHashMap<>();

        for (ConnectedUserProjectWorkflow reference : references) {
            String automationWorkflowUuid = reference.getAutomationWorkflowUuid();

            if (reference.isDangling() || !publishedAutomationWorkflowUuids.contains(automationWorkflowUuid)) {
                markDangling(reference, REMOVED_DANGLING_REASON);

                continue;
            }

            RowSpec rowSpec = resolveRowSpec(
                reference, automationWorkflowProjectId, projectDeploymentId, lastPublishedVersion);

            rowSpecsByAutomationWorkflowUuid.put(automationWorkflowUuid, rowSpec);

            reference.setEnabled(rowSpec.enabled());
        }

        if (rowSpecsByAutomationWorkflowUuid.isEmpty()) {
            connectedUserReferenceDeploymentManager.deleteDeployment(projectDeploymentId);
        } else {
            connectedUserReferenceDeploymentManager.putWorkflows(
                projectDeploymentId, lastPublishedVersion, rowSpecsByAutomationWorkflowUuid);
        }

        connectedUserProjectWorkflowRepository.saveAll(references);
    }

    private RowSpec resolveRowSpec(
        ConnectedUserProjectWorkflow reference, long automationWorkflowProjectId, long projectDeploymentId,
        int lastPublishedVersion) {
        String automationWorkflowUuid = reference.getAutomationWorkflowUuid();

        ConnectedUserProject connectedUserProject = connectedUserProjectService.getConnectedUserProject(
            reference.getConnectedUserProjectId());
        Optional<ProjectDeploymentWorkflow> currentRow = connectedUserReferenceDeploymentManager.fetchRow(
            projectDeploymentId, automationWorkflowUuid);

        String workflowId = connectedUserReferenceDeploymentManager.getWorkflowId(
            automationWorkflowProjectId, lastPublishedVersion, automationWorkflowUuid);

        ReferenceResolution referenceResolution = connectedUserReferenceDeploymentManager.resolveReference(
            connectedUserProject.getConnectedUserId(), workflowId, reference.isEnabled(), Map.of(),
            currentRow.map(ProjectDeploymentWorkflow::getConnections)
                .orElse(List.of()),
            currentRow.<Map<String, ?>>map(ProjectDeploymentWorkflow::getInputs)
                .orElse(Map.of()));

        return referenceResolution.rowSpec();
    }
}
