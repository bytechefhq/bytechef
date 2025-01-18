/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.service;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.constant.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteProjectDeploymentServiceClient implements ProjectDeploymentService {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String PROJECT_DEPLOYMENT_SERVICE = "/remote/project/deployment-service";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteProjectDeploymentServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public ProjectDeployment create(ProjectDeployment projectDeployment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isProjectDeploymentEnabled(long projectDeploymentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectDeployment getProjectDeployment(long id) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(PROJECT_DEPLOYMENT_SERVICE + "/get-project/deployment/{id}")
                .build(id),
            ProjectDeployment.class);
    }

    @Override
    public List<Long> getProjectDeploymentProjectIds() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectDeployment> getProjectDeployments() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectDeployment> getProjectDeployments(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectDeployment>
        getProjectDeployments(Long workspaceId, Environment environment, Long projectId, Long tagId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectDeployment update(long id, List<Long> tagIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectDeployment update(ProjectDeployment projectDeployment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEnabled(long id, boolean enabled) {
        throw new UnsupportedOperationException();
    }
}
