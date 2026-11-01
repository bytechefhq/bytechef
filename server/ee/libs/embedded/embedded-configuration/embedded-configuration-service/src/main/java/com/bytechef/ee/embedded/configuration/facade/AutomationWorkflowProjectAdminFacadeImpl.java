/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.audit.AutomationWorkflowProjectAuditEvents;
import com.bytechef.ee.embedded.configuration.audit.WorkflowAuditEvents;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectCategoryDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectTagDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectVersionDTO;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.configuration.dto.VersionWorkflowChange;
import com.bytechef.platform.configuration.dto.VersionWorkflowChanges;
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
@PreAuthorize("isTenantAdmin()")
public class AutomationWorkflowProjectAdminFacadeImpl implements AutomationWorkflowProjectAdminFacade {

    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectAdminFacadeImpl(AutomationWorkflowProjectFacade automationWorkflowProjectFacade) {
        this.automationWorkflowProjectFacade = automationWorkflowProjectFacade;
    }

    @Override
    @Audited(event = AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_PROJECT_CREATED, data = {
        @AuditData(key = "projectId", value = "#result"),
        @AuditData(key = "name", value = "#name"),
        @AuditData(key = "category", value = "#category"),
        @AuditData(key = "permissionExpression", value = "#permissionExpression")
    })
    public long createProject(
        String name, String description, String category, List<String> tags, String permissionExpression) {

        return automationWorkflowProjectFacade.createProject(
            name, description, category, tags, permissionExpression);
    }

    @Override
    @Audited(event = AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_CREATED, data = {
        @AuditData(key = "projectId", value = "#projectId"),
        @AuditData(key = "workflowUuid", value = "#result"),
        @AuditData(key = "permissionExpression", value = "#permissionExpression")
    })
    public String createProjectWorkflow(long projectId, String definition, String permissionExpression) {
        return automationWorkflowProjectFacade.createProjectWorkflow(projectId, definition, permissionExpression);
    }

    @Override
    @Audited(event = AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_PROJECT_DELETED, data = {
        @AuditData(key = "projectId", value = "#projectId")
    })
    public void deleteProject(long projectId) {
        automationWorkflowProjectFacade.deleteProject(projectId);
    }

    @Override
    @Audited(event = AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_DELETED, data = {
        @AuditData(key = "workflowUuid", value = "#workflowUuid")
    })
    public void deleteProjectWorkflow(String workflowUuid) {
        automationWorkflowProjectFacade.deleteProjectWorkflow(workflowUuid);
    }

    @Override
    @Audited(event = AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_PROJECT_DUPLICATED, data = {
        @AuditData(key = "sourceProjectId", value = "#projectId"),
        @AuditData(key = "projectId", value = "#result")
    })
    public long duplicateProject(long projectId) {
        return automationWorkflowProjectFacade.duplicateProject(projectId);
    }

    @Override
    @Audited(event = AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_DUPLICATED, data = {
        @AuditData(key = "sourceWorkflowUuid", value = "#workflowUuid"),
        @AuditData(key = "workflowUuid", value = "#result")
    })
    public String duplicateProjectWorkflow(String workflowUuid) {
        return automationWorkflowProjectFacade.duplicateProjectWorkflow(workflowUuid);
    }

    @Override
    public List<AutomationWorkflowProjectCategoryDTO> getCategories() {
        return automationWorkflowProjectFacade.getCategories();
    }

    @Override
    public VersionWorkflowChange getProjectVersionWorkflowChange(
        long projectId, int projectVersion, String workflowUuid) {

        return automationWorkflowProjectFacade.getProjectVersionWorkflowChange(
            projectId, projectVersion, workflowUuid);
    }

    @Override
    public VersionWorkflowChanges getProjectVersionWorkflowChanges(long projectId, int projectVersion) {
        return automationWorkflowProjectFacade.getProjectVersionWorkflowChanges(projectId, projectVersion);
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
    @Audited(event = AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_PROJECT_PUBLISHED, data = {
        @AuditData(key = "projectId", value = "#projectId"),
        @AuditData(key = "projectVersion", value = "#result")
    })
    public int publishProject(long projectId, @Nullable String description) {
        return automationWorkflowProjectFacade.publishProject(projectId, description);
    }

    @Override
    @Audited(event = AutomationWorkflowProjectAuditEvents.AUTOMATION_WORKFLOW_PROJECT_UPDATED, data = {
        @AuditData(key = "projectId", value = "#projectId"),
        @AuditData(key = "name", value = "#name"),
        @AuditData(key = "category", value = "#category"),
        @AuditData(key = "permissionExpression", value = "#permissionExpression")
    })
    public void updateProject(
        long projectId, String name, String description, String category, List<String> tags,
        String permissionExpression) {

        automationWorkflowProjectFacade.updateProject(
            projectId, name, description, category, tags, permissionExpression);
    }

    @Override
    public void updateProjectWorkflow(String workflowUuid, String label, String description) {
        automationWorkflowProjectFacade.updateProjectWorkflow(workflowUuid, label, description);
    }

    @Override
    @Audited(event = WorkflowAuditEvents.AUTOMATION_WORKFLOW_PERMISSION_EXPRESSION_UPDATED, data = {
        @AuditData(key = "workflowUuid", value = "#workflowUuid"),
        @AuditData(key = "permissionExpression", value = "#permissionExpression")
    })
    public void updateProjectWorkflowPermissionExpression(String workflowUuid, String permissionExpression) {
        automationWorkflowProjectFacade.updateProjectWorkflowPermissionExpression(workflowUuid, permissionExpression);
    }
}
