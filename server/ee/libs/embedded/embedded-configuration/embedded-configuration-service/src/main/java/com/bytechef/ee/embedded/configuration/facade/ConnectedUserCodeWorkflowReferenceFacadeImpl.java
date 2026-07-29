/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.exception.WorkflowErrorType;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.security.SkipAutomationAuthorization;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflowConnection;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowConnectionRepository;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
@SkipAutomationAuthorization
public class ConnectedUserCodeWorkflowReferenceFacadeImpl implements ConnectedUserCodeWorkflowReferenceFacade {

    private static final String MARKER = "__EMBEDDED__";

    private final ConnectedUserProjectWorkflowConnectionRepository connectedUserProjectWorkflowConnectionRepository;
    private final ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;
    private final ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager;
    private final ConnectedUserWorkflowConnectionResolver connectedUserWorkflowConnectionResolver;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ConnectedUserCodeWorkflowReferenceFacadeImpl(
        ConnectedUserProjectWorkflowConnectionRepository connectedUserProjectWorkflowConnectionRepository,
        ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository,
        ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager,
        ConnectedUserWorkflowConnectionResolver connectedUserWorkflowConnectionResolver,
        ProjectDeploymentFacade projectDeploymentFacade, ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {

        this.connectedUserProjectWorkflowConnectionRepository = connectedUserProjectWorkflowConnectionRepository;
        this.connectedUserProjectWorkflowRepository = connectedUserProjectWorkflowRepository;
        this.connectedUserProjectWorkflowManager = connectedUserProjectWorkflowManager;
        this.connectedUserWorkflowConnectionResolver = connectedUserWorkflowConnectionResolver;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
    }

    /**
     * Creates the reference on first use, provisioning a dedicated {@link ProjectDeployment} for this (catalog project,
     * connected user) pair and auto-wiring per-node connections through
     * {@link ConnectedUserWorkflowConnectionResolver}.
     *
     * <p>
     * A component with no matching connection for the connected user does not abort provisioning: the reference row is
     * still created (and any successfully resolved connections up to that point are still wired), just left
     * {@code enabled = false}, and {@link MissingConnectionException} is rethrown afterward so the caller can surface
     * which connection is missing.
     *
     * <p>
     * {@code noRollbackFor} is required for that "still create the row, just disabled" contract to actually hold:
     * without it, Spring's default rollback rule for a {@code @Transactional} method rolls back everything this method
     * wrote (the {@code ConnectedUserProject}, the disabled reference row, any partially-resolved connection rows) the
     * instant {@link MissingConnectionException} propagates out -- silently contradicting this method's own documented
     * behavior. This only surfaces against a real transactional datasource; mocked unit tests never exercise the real
     * proxy chain, so they never catch it.
     */
    @Override
    @Transactional(noRollbackFor = MissingConnectionException.class)
    public ConnectedUserProjectWorkflow getOrCreateReference(
        String externalUserId, String catalogWorkflowUuid, Environment environment) {

        ConnectedUserProject connectedUserProject = connectedUserProjectWorkflowManager
            .getOrCreateConnectedUserProject(externalUserId, environment);

        Optional<ConnectedUserProjectWorkflow> existing = connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndCatalogWorkflowUuid(connectedUserProject.getId(), catalogWorkflowUuid);

        if (existing.isPresent()) {
            return existing.get();
        }

        String catalogWorkflowId = projectWorkflowService.getLastPublishedWorkflowId(catalogWorkflowUuid);
        ProjectWorkflow catalogProjectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(
            catalogWorkflowId);

        long catalogProjectId = catalogProjectWorkflow.getProjectId();

        Workflow catalogWorkflow = workflowService.getWorkflow(catalogWorkflowId);

        Map<String, Long> resolvedConnections;
        MissingConnectionException missingConnectionException = null;

        try {
            resolvedConnections = connectedUserWorkflowConnectionResolver.resolve(catalogWorkflow.getDefinition());
        } catch (MissingConnectionException e) {
            resolvedConnections = Map.of();
            missingConnectionException = e;
        }

        long projectDeploymentId = getOrCreateProjectDeployment(
            catalogProjectId, externalUserId, environment, catalogWorkflowId, resolvedConnections);

        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = new ConnectedUserProjectWorkflow();

        connectedUserProjectWorkflow.setConnectedUserProjectId(connectedUserProject.getId());
        connectedUserProjectWorkflow.setCatalogWorkflowUuid(catalogWorkflowUuid);
        connectedUserProjectWorkflow.setProjectDeploymentId(projectDeploymentId);
        connectedUserProjectWorkflow.setEnabled(missingConnectionException == null);

        ConnectedUserProjectWorkflow saved = connectedUserProjectWorkflowRepository.save(
            connectedUserProjectWorkflow);

        for (Map.Entry<String, Long> entry : resolvedConnections.entrySet()) {
            ConnectedUserProjectWorkflowConnection connection = new ConnectedUserProjectWorkflowConnection();

            connection.setConnectedUserProjectWorkflowId(saved.getId());
            connection.setWorkflowNodeName(entry.getKey());
            connection.setConnectionId(entry.getValue());

            connectedUserProjectWorkflowConnectionRepository.save(connection);
        }

        if (missingConnectionException != null) {
            throw missingConnectionException;
        }

        return saved;
    }

    private long getOrCreateProjectDeployment(
        long catalogProjectId, String externalUserId, Environment environment, String catalogWorkflowId,
        Map<String, Long> resolvedConnections) {

        // The environment must be part of the name: the same external user can be connected in more than one
        // Environment (e.g. PRODUCTION and STAGING), and the ProjectDeployment lookup below is scoped to
        // (catalogProjectId, name) only -- without the environment suffix, the two environments would collide onto
        // the single deployment created by whichever environment provisioned first.
        String name = MARKER + externalUserId + "__" + environment.name();

        List<ProjectDeploymentWorkflowConnection> connections = resolvedConnections.entrySet()
            .stream()
            .map(entry -> new ProjectDeploymentWorkflowConnection(entry.getValue(), entry.getKey(), entry.getKey()))
            .toList();

        return projectDeploymentService.fetchProjectDeploymentByName(catalogProjectId, name)
            .map(ProjectDeployment::getId)
            .orElseGet(() -> {
                ProjectDeployment projectDeployment = new ProjectDeployment();

                projectDeployment.setEnabled(true);
                projectDeployment.setEnvironment(environment);
                projectDeployment.setName(name);
                projectDeployment.setProjectId(catalogProjectId);
                projectDeployment.setProjectVersion(1);

                return projectDeploymentFacade.createProjectDeployment(
                    projectDeployment, catalogWorkflowId, connections);
            });
    }

    /**
     * Enabling a reference re-runs {@link ConnectedUserWorkflowConnectionResolver#resolve} and re-populates the
     * connection wiring before flipping the flag, so that a reference whose provisioning (or a previous enable) failed
     * with {@link MissingConnectionException} does not silently start running once re-enabled: if the connected user
     * has since created the missing connection, the wiring is refreshed and enabling proceeds; if the connection is
     * still missing, the same {@link MissingConnectionException} propagates and the reference is left unchanged --
     * enabling must never succeed while wiring is missing or stale.
     */
    @Override
    public void enableReference(
        String externalUserId, String catalogWorkflowUuid, boolean enable, Environment environment) {

        ConnectedUserProjectWorkflow reference = requireReference(externalUserId, catalogWorkflowUuid, environment);

        String catalogWorkflowId = projectWorkflowService.getLastPublishedWorkflowId(catalogWorkflowUuid);

        if (enable) {
            rewireConnections(reference, catalogWorkflowId);
        }

        reference.setEnabled(enable);

        connectedUserProjectWorkflowRepository.save(reference);

        projectDeploymentFacade.enableProjectDeploymentWorkflow(
            reference.getProjectDeploymentId(), catalogWorkflowId, enable);
    }

    /**
     * Replaces the reference's {@link ConnectedUserProjectWorkflowConnection} bookkeeping rows and the underlying
     * {@link ProjectDeploymentWorkflow}'s real execution-time connections with a freshly resolved set, mirroring the
     * wiring performed in {@link #getOrCreateReference}. Left to propagate, {@link MissingConnectionException} aborts
     * {@link #enableReference} before the reference is flipped to enabled.
     */
    private void rewireConnections(ConnectedUserProjectWorkflow reference, String catalogWorkflowId) {
        Workflow catalogWorkflow = workflowService.getWorkflow(catalogWorkflowId);

        Map<String, Long> resolvedConnections = connectedUserWorkflowConnectionResolver.resolve(
            catalogWorkflow.getDefinition());

        for (ConnectedUserProjectWorkflowConnection connection : connectedUserProjectWorkflowConnectionRepository
            .findAllByConnectedUserProjectWorkflowId(reference.getId())) {

            connectedUserProjectWorkflowConnectionRepository.deleteById(connection.getId());
        }

        for (Map.Entry<String, Long> entry : resolvedConnections.entrySet()) {
            ConnectedUserProjectWorkflowConnection connection = new ConnectedUserProjectWorkflowConnection();

            connection.setConnectedUserProjectWorkflowId(reference.getId());
            connection.setWorkflowNodeName(entry.getKey());
            connection.setConnectionId(entry.getValue());

            connectedUserProjectWorkflowConnectionRepository.save(connection);
        }

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowService
            .getProjectDeploymentWorkflow(reference.getProjectDeploymentId(), catalogWorkflowId);

        projectDeploymentWorkflow.setConnections(
            resolvedConnections.entrySet()
                .stream()
                .map(entry -> new ProjectDeploymentWorkflowConnection(
                    entry.getValue(), entry.getKey(), entry.getKey()))
                .toList());

        projectDeploymentWorkflowService.update(projectDeploymentWorkflow);
    }

    @Override
    public List<ConnectedUserProjectWorkflow> getConnectedUserWorkflows(long connectedUserId) {
        return connectedUserProjectWorkflowRepository.findAllByConnectedUserId(connectedUserId);
    }

    @Override
    public void deleteReference(String externalUserId, String catalogWorkflowUuid, Environment environment) {
        ConnectedUserProjectWorkflow reference = requireReference(externalUserId, catalogWorkflowUuid, environment);

        for (ConnectedUserProjectWorkflowConnection connection : connectedUserProjectWorkflowConnectionRepository
            .findAllByConnectedUserProjectWorkflowId(reference.getId())) {

            connectedUserProjectWorkflowConnectionRepository.deleteById(connection.getId());
        }

        connectedUserProjectWorkflowRepository.deleteById(reference.getId());
    }

    @Override
    public void markDanglingReferences(
        long catalogProjectId, Set<String> previousCatalogWorkflowUuids, Set<String> currentCatalogWorkflowUuids) {

        for (ConnectedUserProjectWorkflow reference : connectedUserProjectWorkflowRepository.findAll()) {
            String catalogWorkflowUuid = reference.getCatalogWorkflowUuid();

            // A reference dangles only if its uuid was served by THIS catalog project's previous deploy and is not
            // served by the current one -- a uuid never previously served by this project (i.e. one belonging to a
            // different catalog project entirely) is never touched, regardless of what the current set contains.
            if (catalogWorkflowUuid == null || !previousCatalogWorkflowUuids.contains(catalogWorkflowUuid) ||
                currentCatalogWorkflowUuids.contains(catalogWorkflowUuid)) {

                continue;
            }

            reference.setDangling(true);
            reference.setDanglingReason("Removed from the catalog project on redeploy");

            connectedUserProjectWorkflowRepository.save(reference);
        }
    }

    private ConnectedUserProjectWorkflow requireReference(
        String externalUserId, String catalogWorkflowUuid, Environment environment) {

        ConnectedUserProject connectedUserProject = connectedUserProjectWorkflowManager
            .getOrCreateConnectedUserProject(externalUserId, environment);

        return connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndCatalogWorkflowUuid(connectedUserProject.getId(), catalogWorkflowUuid)
            .orElseThrow(() -> new ConfigurationException(
                "No reference to catalog workflow: %s".formatted(catalogWorkflowUuid),
                WorkflowErrorType.WORKFLOW_NOT_FOUND));
    }
}
