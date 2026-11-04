/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.security.WorkflowConnectionAccessResolver;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ConnectedUserProjectWorkflowConnectionAccessResolver implements WorkflowConnectionAccessResolver {

    private final ConnectedUserConnectionMembership connectedUserConnectionMembership;
    private final ConnectedUserProjectService connectedUserProjectService;
    private final ConnectedUserService connectedUserService;
    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectWorkflowConnectionAccessResolver(
        ConnectedUserConnectionMembership connectedUserConnectionMembership,
        ConnectedUserProjectService connectedUserProjectService, ConnectedUserService connectedUserService,
        ProjectWorkflowService projectWorkflowService) {

        this.connectedUserConnectionMembership = connectedUserConnectionMembership;
        this.connectedUserProjectService = connectedUserProjectService;
        this.connectedUserService = connectedUserService;
        this.projectWorkflowService = projectWorkflowService;
    }

    @Override
    public ConnectionAccess resolveConnectionAccess(String workflowId, long connectionId, long environmentId) {
        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getWorkflowProjectWorkflows(
            List.of(workflowId));

        if (projectWorkflows.isEmpty()) {
            return ConnectionAccess.ABSTAIN;
        }

        ProjectWorkflow projectWorkflow = projectWorkflows.getFirst();

        Optional<ConnectedUserProject> connectedUserProjectOptional =
            connectedUserProjectService.fetchProjectConnectedUserProject(projectWorkflow.getProjectId());

        if (connectedUserProjectOptional.isEmpty()) {
            return ConnectionAccess.ABSTAIN;
        }

        ConnectedUserProject connectedUserProject = connectedUserProjectOptional.get();

        Optional<ConnectedUser> connectedUserOptional = connectedUserService.fetchConnectedUser(
            connectedUserProject.getConnectedUserId());

        if (connectedUserOptional.isEmpty()) {
            return ConnectionAccess.DENIED;
        }

        ConnectedUser connectedUser = connectedUserOptional.get();

        Set<Long> connectionIds = connectedUserConnectionMembership.getConnectionIds(
            connectedUser.getId(), connectedUser.getEnvironment());

        return connectionIds.contains(connectionId) ? ConnectionAccess.GRANTED : ConnectionAccess.DENIED;
    }
}
