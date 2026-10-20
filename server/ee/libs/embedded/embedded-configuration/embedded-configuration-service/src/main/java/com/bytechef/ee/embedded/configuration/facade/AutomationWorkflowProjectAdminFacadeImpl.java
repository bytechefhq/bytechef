/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectCategoryDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectTagDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectVersionDTO;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class AutomationWorkflowProjectAdminFacadeImpl implements AutomationWorkflowProjectAdminFacade {

    private static final String DEVELOPMENT = "T(com.bytechef.platform.configuration.domain.Environment).DEVELOPMENT";
    private static final String PERMISSION_EXPRESSION_UNSET =
        "#permissionExpression == null or #permissionExpression.isBlank()";

    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectAdminFacadeImpl(AutomationWorkflowProjectFacade automationWorkflowProjectFacade) {
        this.automationWorkflowProjectFacade = automationWorkflowProjectFacade;
    }

    @Override
    @PreAuthorize("hasEmbeddedScope('INTEGRATION_CREATE') and (" + PERMISSION_EXPRESSION_UNSET
        + " or hasEmbeddedScope('INTEGRATION_SETTINGS'))")
    public long createProject(
        String name, String description, String category, List<String> tags, String permissionExpression,
        @Nullable Boolean automationHubVisible) {

        return automationWorkflowProjectFacade.createProject(
            name, description, category, tags, permissionExpression, automationHubVisible);
    }

    @Override
    @PreAuthorize("hasResourceScopeInEnvironment(#projectId, 'AutomationWorkflowProject', 'WORKFLOW_CREATE', "
        + DEVELOPMENT + ") and (" + PERMISSION_EXPRESSION_UNSET
        + " or hasPermission(#projectId, 'AutomationWorkflowProject', 'INTEGRATION_SETTINGS'))")
    public String createProjectWorkflow(long projectId, String definition, String permissionExpression) {
        return automationWorkflowProjectFacade.createProjectWorkflow(projectId, definition, permissionExpression);
    }

    @Override
    @PreAuthorize("hasPermission(#projectId, 'AutomationWorkflowProject', 'INTEGRATION_DELETE')")
    public void deleteProject(long projectId) {
        automationWorkflowProjectFacade.deleteProject(projectId);
    }

    @Override
    @PreAuthorize("hasResourceScopeInEnvironment(#workflowUuid, 'AutomationWorkflowProjectWorkflow', "
        + "'WORKFLOW_DELETE', " + DEVELOPMENT + ")")
    public void deleteProjectWorkflow(String workflowUuid) {
        automationWorkflowProjectFacade.deleteProjectWorkflow(workflowUuid);
    }

    @Override
    @PreAuthorize("hasPermission(#projectId, 'AutomationWorkflowProject', 'WORKFLOW_VIEW') "
        + "and hasEmbeddedScope('INTEGRATION_CREATE')")
    public long duplicateProject(long projectId) {
        return automationWorkflowProjectFacade.duplicateProject(projectId);
    }

    @Override
    @PreAuthorize("hasResourceScopeInEnvironment(#workflowUuid, 'AutomationWorkflowProjectWorkflow', "
        + "'WORKFLOW_CREATE', " + DEVELOPMENT + ")")
    public String duplicateProjectWorkflow(String workflowUuid) {
        return automationWorkflowProjectFacade.duplicateProjectWorkflow(workflowUuid);
    }

    @Override
    @PreAuthorize("hasEmbeddedScope('WORKFLOW_VIEW')")
    public List<AutomationWorkflowProjectCategoryDTO> getCategories() {
        return automationWorkflowProjectFacade.getCategories();
    }

    @Override
    @PreAuthorize("hasPermission(#projectId, 'AutomationWorkflowProject', 'WORKFLOW_VIEW')")
    public List<AutomationWorkflowProjectVersionDTO> getProjectVersions(long projectId) {
        return automationWorkflowProjectFacade.getProjectVersions(projectId);
    }

    @Override
    @PreAuthorize("hasEmbeddedScope('WORKFLOW_VIEW')")
    public List<AutomationWorkflowProjectDTO> getProjects() {
        return automationWorkflowProjectFacade.getProjects();
    }

    @Override
    @PreAuthorize("hasEmbeddedScope('WORKFLOW_VIEW')")
    public List<AutomationWorkflowProjectTagDTO> getTags() {
        return automationWorkflowProjectFacade.getTags();
    }

    @Override
    @PreAuthorize("hasPermission(#projectId, 'AutomationWorkflowProject', 'WORKFLOW_EDIT') "
        + "and hasPermission(#projectId, 'AutomationWorkflowProject', 'INTEGRATION_PUBLISH')")
    public void publishProject(long projectId, @Nullable String description) {
        automationWorkflowProjectFacade.publishProject(projectId, description);
    }

    @Override
    @PreAuthorize("hasPermission(#projectId, 'AutomationWorkflowProject', 'INTEGRATION_SETTINGS')")
    public void updateProject(
        long projectId, String name, String description, String category, List<String> tags,
        String permissionExpression, @Nullable Boolean automationHubVisible) {

        automationWorkflowProjectFacade.updateProject(
            projectId, name, description, category, tags, permissionExpression, automationHubVisible);
    }

    @Override
    @PreAuthorize("hasResourceScopeInEnvironment(#workflowUuid, 'AutomationWorkflowProjectWorkflow', "
        + "'WORKFLOW_EDIT', " + DEVELOPMENT + ")")
    public void updateProjectWorkflow(String workflowUuid, String label, String description) {
        automationWorkflowProjectFacade.updateProjectWorkflow(workflowUuid, label, description);
    }

    @Override
    @PreAuthorize("hasPermission(#workflowUuid, 'AutomationWorkflowProjectWorkflow', 'INTEGRATION_SETTINGS')")
    public void updateProjectWorkflowPermissionExpression(String workflowUuid, String permissionExpression) {
        automationWorkflowProjectFacade.updateProjectWorkflowPermissionExpression(workflowUuid, permissionExpression);
    }
}
