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
 * Brings every reference deployment of an automation workflow project to the project's last published version, each deployment in
 * its own transaction so one connected user's failure never blocks another's. Idempotent: a deployment already at the
 * target version is skipped. {@code @SkipAutomationAuthorization} lives here, not on the listener: its aspect applies
 * on the thread that calls this bean, and the publishing thread's bypass does not follow {@code @Async}.
 *
 * <p>
 * Depends on {@link ConnectedUserReferenceDeploymentManager} only, never on the reference facade, so the facade can
 * call {@link #rollOutDeploymentIfBehind} for its lazy catch-up without a constructor cycle.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@SkipAutomationAuthorization
public class ConnectedUserReferenceRolloutManager {

    private static final Logger log = LoggerFactory.getLogger(ConnectedUserReferenceRolloutManager.class);

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

    /**
     * Rolls every reference deployment of the automation workflow project that is behind its last published version forward, each
     * in its own transaction that also loads the deployment's references. A failure is logged and leaves that
     * deployment on its old version; the rollout continues with the next one. A failure to list the deployments at all
     * is logged and ends the rollout: the deployments converge on the next publish or on their users' next enable.
     */
    public void rollOut(long automationWorkflowProjectId) {
        int lastPublishedVersion;
        List<ProjectDeployment> projectDeployments;

        try {
            lastPublishedVersion = connectedUserReferenceDeploymentManager.getLastPublishedVersion(automationWorkflowProjectId);
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

    /**
     * Rolls one deployment forward when it is behind, inside the caller's transaction. A deployment none of whose
     * references survives the new version -- or that has no reference left at all -- is deleted, so a caller that goes
     * on to write into it must look it up again.
     *
     * @return whether the deployment was behind and rolled forward, i.e. whether its references may have changed
     */
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

    /**
     * Dangling references are included on purpose: a deployment whose every reference dangles must still be emptied and
     * deleted, or its old triggers keep running.
     */
    private List<ConnectedUserProjectWorkflow> getReferences(long projectDeploymentId) {
        return connectedUserProjectWorkflowRepository.findAllByProjectDeploymentId(projectDeploymentId)
            .stream()
            .filter(reference -> reference.getAutomationWorkflowUuid() != null)
            .toList();
    }

    /**
     * A concurrent write to the same deployment or reference (the user enabling it while the rollout runs) is expected
     * and self-healing, so it is not reported as an error.
     */
    private static void logRolloutFailure(long automationWorkflowProjectId, long projectDeploymentId, RuntimeException exception) {
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
            "Rolling out automation workflow project id={} to deployment id={} failed", automationWorkflowProjectId, projectDeploymentId,
            exception);
    }

    /**
     * A reference deployment with no reference left still has to go: a reference deleted while its row was still
     * running (a dangling one, whose rows a code-workflow redeploy leaves to this rollout) may have been the last one.
     * Any other deployment of the automation workflow project is none of the rollout's business and is left alone.
     */
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

    /**
     * Rows at the new version are written as one complete list, so the rows of removed templates are dropped (their
     * triggers disabled first) and the carried rows keep their inputs; when nothing remains the deployment itself goes.
     * A reference is never enabled here: its row stays enabled only if it was enabled and still resolves.
     */
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
                if (!reference.isDangling()) {
                    reference.setDangling(true);
                    reference.setDanglingReason(REMOVED_DANGLING_REASON);
                }

                reference.setEnabled(false);

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

    /**
     * The spec carries no inputs: at a version change {@code putWorkflows} keeps each row's existing inputs.
     */
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
