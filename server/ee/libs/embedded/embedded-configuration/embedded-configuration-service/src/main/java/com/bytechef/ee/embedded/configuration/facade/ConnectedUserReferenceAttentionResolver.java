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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * A reference's stored inputs and why it needs the connected user's attention, both read from one lookup of its
 * deployment row. The reason is derived on every read and never stored, so it can never disagree with what is actually
 * wired.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
class ConnectedUserReferenceAttentionResolver {

    static final String INPUT_REQUIRED_PREFIX = "INPUT_REQUIRED:";
    static final String MISSING_CONNECTION_PREFIX = "MISSING_CONNECTION:";
    static final String UPDATE_PENDING = "UPDATE_PENDING";

    private static final Logger log = LoggerFactory.getLogger(ConnectedUserReferenceAttentionResolver.class);

    private final ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager;
    private final WorkflowConnectionSlots workflowConnectionSlots;

    @SuppressFBWarnings("EI")
    public ConnectedUserReferenceAttentionResolver(
        ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager,
        WorkflowConnectionSlots workflowConnectionSlots) {

        this.connectedUserReferenceDeploymentManager = connectedUserReferenceDeploymentManager;
        this.workflowConnectionSlots = workflowConnectionSlots;
    }

    public ReferenceState resolve(ConnectedUserProjectWorkflow reference) {
        Long projectDeploymentId = reference.getProjectDeploymentId();

        if (reference.isDangling() || projectDeploymentId == null) {
            return new ReferenceState(Map.of(), null);
        }

        Optional<ProjectDeployment> fetchedProjectDeployment = connectedUserReferenceDeploymentManager.fetchDeployment(
            projectDeploymentId);

        if (fetchedProjectDeployment.isEmpty()) {
            return new ReferenceState(Map.of(), null);
        }

        ProjectDeployment projectDeployment = fetchedProjectDeployment.get();

        Optional<String> workflowId = connectedUserReferenceDeploymentManager.fetchWorkflowId(
            projectDeployment.getProjectId(), projectDeployment.getProjectVersion(),
            reference.getAutomationWorkflowUuid());

        Optional<ProjectDeploymentWorkflow> row = workflowId.flatMap(
            id -> connectedUserReferenceDeploymentManager.fetchWorkflowRow(projectDeploymentId, id));

        Map<String, ?> inputs = row.<Map<String, ?>>map(ProjectDeploymentWorkflow::getInputs)
            .orElse(Map.of());

        try {
            return new ReferenceState(inputs, resolveAttentionReason(projectDeployment, workflowId, row));
        } catch (RuntimeException exception) {
            log.warn(
                "Attention reason of reference id={} could not be derived: {}", reference.getId(),
                exception.getMessage());

            if (log.isDebugEnabled()) {
                log.debug("Attention reason of reference id={} could not be derived", reference.getId(), exception);
            }

            return new ReferenceState(inputs, null);
        }
    }

    @Nullable
    private String resolveAttentionReason(
        ProjectDeployment projectDeployment, Optional<String> workflowId, Optional<ProjectDeploymentWorkflow> row) {

        int lastPublishedVersion = connectedUserReferenceDeploymentManager.getLastPublishedVersion(
            projectDeployment.getProjectId());

        if (projectDeployment.getProjectVersion() < lastPublishedVersion) {
            return UPDATE_PENDING;
        }

        if (workflowId.isEmpty() || row.isEmpty()) {
            return null;
        }

        ProjectDeploymentWorkflow projectDeploymentWorkflow = row.get();

        List<ProjectDeploymentWorkflowConnection> connections = projectDeploymentWorkflow.getConnections();

        for (ComponentConnection slot : workflowConnectionSlots.getSlots(workflowId.get())) {
            boolean wired = connections.stream()
                .anyMatch(connection -> Objects.equals(connection.getWorkflowNodeName(), slot.workflowNodeName()) &&
                    Objects.equals(connection.getWorkflowConnectionKey(), slot.key()));

            if (slot.required() && !wired) {
                return MISSING_CONNECTION_PREFIX + slot.componentName();
            }
        }

        String missingInputName = connectedUserReferenceDeploymentManager.findMissingRequiredInput(
            workflowId.get(), projectDeploymentWorkflow.getInputs());

        return missingInputName == null ? null : INPUT_REQUIRED_PREFIX + missingInputName;
    }

    @SuppressFBWarnings("EI")
    public record ReferenceState(Map<String, ?> inputs, @Nullable String attentionReason) {
    }
}
