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
import java.util.List;

/**
 * Admin-console-only facade over {@link AutomationWorkflowProjectFacade}. The underlying facade is shared with the
 * public {@code /v1} API and workflow-execution components (and carries {@code @SkipAutomationAuthorization}), so it
 * cannot itself carry an admin gate. This per-controller facade wraps the admin operations behind
 * {@code isTenantAdmin()} so the workflow-project GraphQL controller needs no controller-layer authorization.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AutomationWorkflowProjectAdminFacade {

    long createProject(
        String name, String description, String category, List<String> tags, String permissionExpression);

    String createProjectWorkflow(long projectId, String definition, String permissionExpression);

    void deleteProject(long projectId);

    void deleteProjectWorkflow(String workflowUuid);

    long duplicateProject(long projectId);

    String duplicateProjectWorkflow(String workflowId);

    List<AutomationWorkflowProjectCategoryDTO> getCategories();

    List<AutomationWorkflowProjectVersionDTO> getProjectVersions(long projectId);

    List<AutomationWorkflowProjectDTO> getProjects();

    List<AutomationWorkflowProjectTagDTO> getTags();

    void publishProject(long projectId);

    void updateProject(
        long projectId, String name, String description, String category, List<String> tags,
        String permissionExpression);

    void updateProjectWorkflow(String workflowId, String label, String description);

    void updateProjectWorkflowPermissionExpression(String workflowId, String permissionExpression);
}
