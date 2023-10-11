
/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.helios.configuration.remote.client.service;

import com.bytechef.commons.webclient.LoadBalancedWebClient;
import com.bytechef.helios.configuration.domain.Project;
import com.bytechef.helios.configuration.service.ProjectService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Component
public class RemoteProjectServiceClient implements ProjectService {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String PROJECT_SERVICE = "/remote/project-service";

    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public RemoteProjectServiceClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public Project addWorkflow(long id, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long countProjects() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Project create(Project project) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Project> fetchProject(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isProjectEnabled(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Project getProjectInstanceProject(long projectInstanceId) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(PROJECT_SERVICE + "/get-project-instance-project/{projectInstanceId}")
                .build(projectInstanceId),
            Project.class);
    }

    @Override
    public Project getProject(long id) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(PROJECT_SERVICE + "/get-project/{id}")
                .build(id),
            Project.class);
    }

    @Override
    public Project getProject(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Project> getProjects() {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(PROJECT_SERVICE + "/get-projects")
                .build(),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<Project> getProjects(List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Project> getProjects(Long categoryId, List<Long> ids, Long tagId, Boolean published) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Project getWorkflowProject(String workflowId) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(PROJECT_SERVICE + "/get-workflow-project/{workflowId}")
                .build(workflowId),
            Project.class);
    }

    @Override
    public void removeWorkflow(long id, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Project update(long id, List<Long> tagIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Project update(Project project) {
        throw new UnsupportedOperationException();
    }
}
