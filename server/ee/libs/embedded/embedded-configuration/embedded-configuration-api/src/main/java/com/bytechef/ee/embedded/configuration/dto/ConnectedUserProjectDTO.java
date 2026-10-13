/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A connected user's project. The project is deployed only once the connected user copies a workflow, so a connected
 * user that only references automation workflows has no deployment, hence no project version.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ConnectedUserProjectDTO(
    long id, ConnectedUser connectedUser, List<ConnectedUserProjectWorkflowDTO> connectedUserProjectWorkflows,
    Environment environment, long environmentId, @Nullable Instant lastExecutionDate, long projectId,
    @Nullable Integer projectVersion,
    String createdBy, Instant createdDate, String lastModifiedBy, Instant lastModifiedDate, int version) {

    public ConnectedUserProjectDTO(
        ConnectedUserProject connectedUserProject, ConnectedUser connectedUser,
        List<ConnectedUserProjectWorkflowDTO> connectedUserProjectWorkflows, @Nullable Instant lastExecutionDate,
        @Nullable ProjectDeployment projectDeployment) {

        this(
            connectedUserProject.getId(), connectedUser, connectedUserProjectWorkflows,
            connectedUser.getEnvironment(), connectedUser.getEnvironmentId(), lastExecutionDate,
            connectedUserProject.getProjectId(),
            projectDeployment == null ? null : projectDeployment.getProjectVersion(),
            connectedUserProject.getCreatedBy(), connectedUserProject.getCreatedDate(),
            connectedUserProject.getLastModifiedBy(), connectedUserProject.getLastModifiedDate(),
            connectedUserProject.getVersion());
    }
}
