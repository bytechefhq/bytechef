/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.automation.configuration.remote.client.service;

import com.bytechef.automation.configuration.domain.ProjectInstance;
import com.bytechef.automation.configuration.service.ProjectInstanceService;
import com.bytechef.commons.rest.client.LoadBalancedRestClient;
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
public class RemoteProjectInstanceServiceClient implements ProjectInstanceService {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String PROJECT_INSTANCE_SERVICE = "/remote/project-instance-service";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteProjectInstanceServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public ProjectInstance create(ProjectInstance projectInstance) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isProjectInstanceEnabled(long projectInstanceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectInstance getProjectInstance(long id) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(PROJECT_INSTANCE_SERVICE + "/get-project-instance/{id}")
                .build(id),
            ProjectInstance.class);
    }

    @Override
    public List<Long> getProjectIds() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectInstance> getProjectInstances() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectInstance> getProjectInstances(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectInstance>
        getProjectInstances(Long workspaceId, Environment environment, Long projectId, Long tagId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectInstance update(long id, List<Long> tagIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectInstance update(ProjectInstance projectInstance) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEnabled(long id, boolean enabled) {
        throw new UnsupportedOperationException();
    }
}
