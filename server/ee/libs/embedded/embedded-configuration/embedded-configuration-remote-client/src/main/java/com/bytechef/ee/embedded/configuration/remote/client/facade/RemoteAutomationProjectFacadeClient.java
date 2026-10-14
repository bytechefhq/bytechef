/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.facade;

import com.bytechef.ee.embedded.configuration.dto.AutomationProjectCategoryDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationReleaseDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectTagDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectVersionDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationProjectFacade;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.dto.VersionWorkflowChange;
import com.bytechef.platform.configuration.dto.VersionWorkflowChanges;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteAutomationProjectFacadeClient implements AutomationProjectFacade {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String AUTOMATION_PROJECT_FACADE = "/remote/automation-project-facade";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteAutomationProjectFacadeClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public long createProject(
        String name, String description, String category, List<String> tags, String permissionExpression) {

        throw new UnsupportedOperationException();
    }

    @Override
    public String duplicateProjectWorkflow(String workflowUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long duplicateProject(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Long> fetchProjectIdByName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VersionWorkflowChange getProjectVersionWorkflowChange(
        long projectId, int projectVersion, String workflowUuid) {

        throw new UnsupportedOperationException();
    }

    @Override
    public VersionWorkflowChanges getProjectVersionWorkflowChanges(long projectId, int projectVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationProjectVersionDTO> getProjectVersions(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationProjectCategoryDTO> getCategories() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationProjectTagDTO> getTags() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String createProjectWorkflow(long projectId, String definition, String permissionExpression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteProject(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteProjectWorkflow(String workflowUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AutomationProjectDTO getProject(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationProjectDTO> getProjects() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationProjectDTO> getProjects(long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationProjectDTO> getPublishedProjects() {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(AUTOMATION_PROJECT_FACADE + "/get-published-projects")
                .build(),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<AutomationProjectDTO> getPublishedProjects(String externalUserId, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationProjectConfigurationReleaseDTO> getPublishedProjectConfigurations(
        String externalUserId, Environment environment) {

        throw new UnsupportedOperationException();
    }

    @Override
    public int publishProject(long projectId, @Nullable String description) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateProject(
        long projectId, String name, String description, String category, List<String> tags,
        String permissionExpression) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateProjectWorkflow(String workflowUuid, String label, String description) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateProjectWorkflowPermissionExpression(String workflowUuid, String permissionExpression) {
        throw new UnsupportedOperationException();
    }
}
