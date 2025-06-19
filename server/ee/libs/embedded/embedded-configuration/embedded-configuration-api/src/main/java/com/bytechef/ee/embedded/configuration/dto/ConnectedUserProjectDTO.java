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
import com.bytechef.platform.constant.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ConnectedUserProjectDTO(
    long id, ConnectedUser connectedUser, List<ConnectedUserProjectWorkflowDTO> connectedUserProjectWorkflows,
    Environment environment, Instant lastExecutionDate, long projectId, int projectVersion,
    String createdBy, Instant createdDate, String lastModifiedBy, Instant lastModifiedDate, int version) {

    public ConnectedUserProjectDTO(
        ConnectedUserProject connectedUserProject, ConnectedUser connectedUser,
        List<ConnectedUserProjectWorkflowDTO> connectedUserProjectWorkflows, Instant lastExecutionDate,
        ProjectDeployment projectDeployment) {

        this(
            connectedUserProject.getId(), connectedUser, connectedUserProjectWorkflows,
            projectDeployment.getEnvironment(), lastExecutionDate, connectedUserProject.getProjectId(),
            projectDeployment.getProjectVersion(), connectedUserProject.getCreatedBy(),
            connectedUserProject.getCreatedDate(), connectedUserProject.getLastModifiedBy(),
            connectedUserProject.getLastModifiedDate(), connectedUserProject.getVersion());
    }
}
