/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
class ConnectedUserReferenceAttentionResolver {

    static final String INPUT_REQUIRED_PREFIX = "INPUT_REQUIRED:";
    static final String INVALID_WORKFLOW_PREFIX = "INVALID_WORKFLOW:";
    static final String MISSING_CONNECTION_PREFIX = "MISSING_CONNECTION:";
    static final String TURNED_OFF_BY_ADMINISTRATOR = "TURNED_OFF_BY_ADMINISTRATOR";
    static final String UPDATE_PENDING = "UPDATE_PENDING";

    private static final Logger log = LoggerFactory.getLogger(ConnectedUserReferenceAttentionResolver.class);

    private final ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager;
    private final ConnectedUserWorkflowValidator connectedUserWorkflowValidator;
    private final WorkflowConnectionSlot workflowConnectionSlot;

    @SuppressFBWarnings("EI")
    public ConnectedUserReferenceAttentionResolver(
        ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager,
        ConnectedUserWorkflowValidator connectedUserWorkflowValidator, WorkflowConnectionSlot workflowConnectionSlot) {

        this.connectedUserReferenceDeploymentManager = connectedUserReferenceDeploymentManager;
        this.connectedUserWorkflowValidator = connectedUserWorkflowValidator;
        this.workflowConnectionSlot = workflowConnectionSlot;
    }

    public ReferenceState resolve(ConnectedUserProjectWorkflow reference) {
        Long projectDeploymentId = reference.getProjectDeploymentId();

        if (reference.isDangling() || projectDeploymentId == null) {
            return new ReferenceState(Map.of(), Map.of(), null);
        }

        Optional<ProjectDeployment> fetchedProjectDeployment = connectedUserReferenceDeploymentManager.fetchDeployment(
            projectDeploymentId);

        if (fetchedProjectDeployment.isEmpty()) {
            return new ReferenceState(Map.of(), Map.of(), null);
        }

        ProjectDeployment projectDeployment = fetchedProjectDeployment.get();

        Optional<String> workflowId = connectedUserReferenceDeploymentManager.fetchWorkflowId(
            projectDeployment.getProjectId(), projectDeployment.getProjectVersion(),
            reference.getAutomationUuid());

        Optional<ProjectDeploymentWorkflow> row = workflowId.flatMap(
            id -> connectedUserReferenceDeploymentManager.fetchWorkflowRow(projectDeploymentId, id));

        Map<String, ?> inputs = row.<Map<String, ?>>map(ProjectDeploymentWorkflow::getInputs)
            .orElse(Map.of());

        Map<String, Long> connections = resolveConnections(reference, workflowId, row);

        try {
            return new ReferenceState(inputs, connections, resolveAttentionReason(
                projectDeployment, reference.getAutomationUuid(), workflowId, row));
        } catch (RuntimeException exception) {
            log.warn(
                "Attention reason of reference id={} could not be derived: {}", reference.getId(),
                exception.getMessage());

            if (log.isDebugEnabled()) {
                log.debug("Attention reason of reference id={} could not be derived", reference.getId(), exception);
            }

            return new ReferenceState(inputs, connections, null);
        }
    }

    private Map<String, Long> resolveConnections(
        ConnectedUserProjectWorkflow reference, Optional<String> workflowId, Optional<ProjectDeploymentWorkflow> row) {

        if (workflowId.isEmpty() || row.isEmpty()) {
            return Map.of();
        }

        ProjectDeploymentWorkflow projectDeploymentWorkflow = row.get();

        List<ProjectDeploymentWorkflowConnection> connections = projectDeploymentWorkflow.getConnections();

        Map<String, Long> connectionIds = new LinkedHashMap<>();

        try {
            for (ComponentConnection componentConnection : workflowConnectionSlot.getSlots(workflowId.get())) {
                connections.stream()
                    .filter(connection -> isWiredTo(connection, componentConnection))
                    .map(ProjectDeploymentWorkflowConnection::getConnectionId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .ifPresent(connectionId -> connectionIds.putIfAbsent(
                        componentConnection.componentName(), connectionId));
            }
        } catch (RuntimeException exception) {
            log.warn(
                "Connections of reference id={} could not be derived: {}", reference.getId(), exception.getMessage());

            if (log.isDebugEnabled()) {
                log.debug("Connections of reference id={} could not be derived", reference.getId(), exception);
            }

            return Map.of();
        }

        return connectionIds;
    }

    @Nullable
    private String resolveAttentionReason(
        ProjectDeployment projectDeployment, String automationUuid, Optional<String> workflowId,
        Optional<ProjectDeploymentWorkflow> row) {

        Optional<Long> connectedUserIdOptional = connectedUserReferenceDeploymentManager.fetchConnectedUserId(
            projectDeployment.getId());

        if (connectedUserIdOptional.isEmpty()) {
            return null;
        }

        long connectedUserId = connectedUserIdOptional.get();

        if (!connectedUserReferenceDeploymentManager.isAdministrativelyEnabled(
            connectedUserId, projectDeployment.getProjectId(), automationUuid)) {

            return TURNED_OFF_BY_ADMINISTRATOR;
        }

        Optional<Integer> releasedVersion = connectedUserReferenceDeploymentManager.fetchReleasedVersion(
            connectedUserId, projectDeployment.getProjectId());

        if (releasedVersion.isPresent() && projectDeployment.getProjectVersion() != releasedVersion.get()) {
            return UPDATE_PENDING;
        }

        if (workflowId.isEmpty() || row.isEmpty()) {
            return null;
        }

        ProjectDeploymentWorkflow projectDeploymentWorkflow = row.get();

        List<ProjectDeploymentWorkflowConnection> connections = projectDeploymentWorkflow.getConnections();

        for (ComponentConnection componentConnection : workflowConnectionSlot.getSlots(workflowId.get())) {
            boolean wired = connections.stream()
                .anyMatch(connection -> isWiredTo(connection, componentConnection));

            if (componentConnection.required() && !wired) {
                return MISSING_CONNECTION_PREFIX + componentConnection.componentName();
            }
        }

        String missingInputName = connectedUserReferenceDeploymentManager.findMissingRequiredInput(
            workflowId.get(), projectDeploymentWorkflow.getInputs());

        if (missingInputName != null) {
            return INPUT_REQUIRED_PREFIX + missingInputName;
        }

        return connectedUserWorkflowValidator.getAttentionReason(workflowId.get(), projectDeployment.getEnvironment());
    }

    private static boolean isWiredTo(
        ProjectDeploymentWorkflowConnection connection, ComponentConnection componentConnection) {

        return Objects.equals(connection.getWorkflowNodeName(), componentConnection.workflowNodeName()) &&
            Objects.equals(connection.getWorkflowConnectionKey(), componentConnection.key());
    }

    /**
     * @param inputs          the reference's stored input values, keyed by input name
     * @param connections     the id of the connection each component is wired to, keyed by component name
     * @param attentionReason why the reference needs attention, {@code null} when it is healthy
     */
    @SuppressFBWarnings("EI")
    public record ReferenceState(
        Map<String, ?> inputs, Map<String, Long> connections, @Nullable String attentionReason) {
    }
}
