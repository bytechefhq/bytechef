/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ConnectedUserProjectDeploymentResolver;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserReferenceDeployment;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional(readOnly = true)
@ConditionalOnEEVersion
public class ConnectedUserProjectDeploymentResolverImpl implements ConnectedUserProjectDeploymentResolver {

    private final ConnectedUserProjectService connectedUserProjectService;
    private final ConnectedUserReferenceDeploymentService connectedUserReferenceDeploymentService;
    private final ProjectDeploymentService projectDeploymentService;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectDeploymentResolverImpl(
        ConnectedUserProjectService connectedUserProjectService,
        ConnectedUserReferenceDeploymentService connectedUserReferenceDeploymentService,
        ProjectDeploymentService projectDeploymentService) {

        this.connectedUserProjectService = connectedUserProjectService;
        this.connectedUserReferenceDeploymentService = connectedUserReferenceDeploymentService;
        this.projectDeploymentService = projectDeploymentService;
    }

    @Override
    public List<Long> getProjectDeploymentIds(long connectedUserId) {
        Set<Long> projectDeploymentIds = new LinkedHashSet<>();

        for (ConnectedUserProject connectedUserProject : connectedUserProjectService
            .getConnectedUserConnectedUserProjects(connectedUserId)) {

            for (ProjectDeployment projectDeployment : projectDeploymentService.getProjectDeployments(
                connectedUserProject.getProjectId())) {

                projectDeploymentIds.add(projectDeployment.getId());
            }
        }

        for (ConnectedUserReferenceDeployment connectedUserReferenceDeployment : connectedUserReferenceDeploymentService
            .getConnectedUserReferenceDeployments(connectedUserId)) {

            projectDeploymentIds.add(connectedUserReferenceDeployment.getProjectDeploymentId());
        }

        return List.copyOf(projectDeploymentIds);
    }
}
