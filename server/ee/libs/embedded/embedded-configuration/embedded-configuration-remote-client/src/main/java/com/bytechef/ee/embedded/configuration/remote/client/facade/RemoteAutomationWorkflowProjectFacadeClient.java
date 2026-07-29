/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.facade;

import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectCategoryDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectTagDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectVersionDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteAutomationWorkflowProjectFacadeClient implements AutomationWorkflowProjectFacade {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String AUTOMATION_WORKFLOW_PROJECT_FACADE = "/remote/automation-workflow-project-facade";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteAutomationWorkflowProjectFacadeClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public long createProject(
        String name, String description, String category, List<String> tags, String permissionExpression) {

        throw new UnsupportedOperationException();
    }

    @Override
    public String duplicateProjectWorkflow(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Long> fetchProjectIdByName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long duplicateProject(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationWorkflowProjectVersionDTO> getProjectVersions(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationWorkflowProjectCategoryDTO> getCategories() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationWorkflowProjectTagDTO> getTags() {
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
    public AutomationWorkflowProjectDTO getProject(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationWorkflowProjectDTO> getProjects() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AutomationWorkflowProjectDTO> getPublishedProjects() {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(AUTOMATION_WORKFLOW_PROJECT_FACADE + "/get-published-projects")
                .build(),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<AutomationWorkflowProjectDTO> getPublishedProjects(String externalUserId, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void publishProject(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateProject(
        long projectId, String name, String description, String category, List<String> tags,
        String permissionExpression) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateProjectWorkflow(String workflowId, String label, String description) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateProjectWorkflowPermissionExpression(String workflowId, String permissionExpression) {
        throw new UnsupportedOperationException();
    }
}
