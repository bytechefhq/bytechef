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
@PreAuthorize("isTenantAdmin()")
public class AutomationWorkflowProjectAdminFacadeImpl implements AutomationWorkflowProjectAdminFacade {

    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectAdminFacadeImpl(AutomationWorkflowProjectFacade automationWorkflowProjectFacade) {
        this.automationWorkflowProjectFacade = automationWorkflowProjectFacade;
    }

    @Override
    public long createProject(
        String name, String description, String category, List<String> tags, String permissionExpression,
        @Nullable Boolean automationHubVisible) {

        return automationWorkflowProjectFacade.createProject(
            name, description, category, tags, permissionExpression, automationHubVisible);
    }

    @Override
    public String createProjectWorkflow(long projectId, String definition, String permissionExpression) {
        return automationWorkflowProjectFacade.createProjectWorkflow(projectId, definition, permissionExpression);
    }

    @Override
    public void deleteProject(long projectId) {
        automationWorkflowProjectFacade.deleteProject(projectId);
    }

    @Override
    public void deleteProjectWorkflow(String workflowUuid) {
        automationWorkflowProjectFacade.deleteProjectWorkflow(workflowUuid);
    }

    @Override
    public long duplicateProject(long projectId) {
        return automationWorkflowProjectFacade.duplicateProject(projectId);
    }

    @Override
    public String duplicateProjectWorkflow(String workflowUuid) {
        return automationWorkflowProjectFacade.duplicateProjectWorkflow(workflowUuid);
    }

    @Override
    public List<AutomationWorkflowProjectCategoryDTO> getCategories() {
        return automationWorkflowProjectFacade.getCategories();
    }

    @Override
    public List<AutomationWorkflowProjectVersionDTO> getProjectVersions(long projectId) {
        return automationWorkflowProjectFacade.getProjectVersions(projectId);
    }

    @Override
    public List<AutomationWorkflowProjectDTO> getProjects() {
        return automationWorkflowProjectFacade.getProjects();
    }

    @Override
    public List<AutomationWorkflowProjectTagDTO> getTags() {
        return automationWorkflowProjectFacade.getTags();
    }

    @Override
    public void publishProject(long projectId) {
        automationWorkflowProjectFacade.publishProject(projectId);
    }

    @Override
    public void updateProject(
        long projectId, String name, String description, String category, List<String> tags,
        String permissionExpression, @Nullable Boolean automationHubVisible) {

        automationWorkflowProjectFacade.updateProject(
            projectId, name, description, category, tags, permissionExpression, automationHubVisible);
    }

    @Override
    public void updateProjectWorkflow(String workflowUuid, String label, String description) {
        automationWorkflowProjectFacade.updateProjectWorkflow(workflowUuid, label, description);
    }

    @Override
    public void updateProjectWorkflowPermissionExpression(String workflowUuid, String permissionExpression) {
        automationWorkflowProjectFacade.updateProjectWorkflowPermissionExpression(workflowUuid, permissionExpression);
    }
}
