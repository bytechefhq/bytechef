/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.service;

import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwnerReference;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.service.ProjectOwnershipService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteProjectOwnershipServiceClient implements ProjectOwnershipService {

    @Override
    public String getExcludeProjectsCondition(Set<ProjectOwner> projectOwners, String projectIdColumn) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSelectProjectDeploymentsCondition(
        ProjectDeploymentOwner projectDeploymentOwner, String projectDeploymentIdColumn) {

        throw new UnsupportedOperationException();
    }

    @Override
    public String getExcludeProjectDeploymentsCondition(
        Set<ProjectDeploymentOwner> projectDeploymentOwners, String projectDeploymentIdColumn) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Long> getOwnedProjectDeploymentIds(
        Collection<Long> projectDeploymentIds, ProjectDeploymentOwner projectDeploymentOwner) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Long> getOwnedProjectIds(Collection<Long> projectIds, Set<ProjectOwner> projectOwners) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Long, ProjectDeploymentOwnerReference> getProjectDeploymentOwnerReferences(
        Collection<Long> projectDeploymentIds) {

        throw new UnsupportedOperationException();
    }
}
