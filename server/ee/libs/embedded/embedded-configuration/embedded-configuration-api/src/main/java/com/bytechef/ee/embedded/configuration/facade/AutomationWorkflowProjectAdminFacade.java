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
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AutomationWorkflowProjectAdminFacade {
    long createProject(
        String name, String description, String category, List<String> tags, String permissionExpression,
        @Nullable Boolean automationHubVisible);

    String createProjectWorkflow(long projectId, String definition, String permissionExpression);

    void deleteProject(long projectId);

    void deleteProjectWorkflow(String workflowUuid);

    long duplicateProject(long projectId);

    String duplicateProjectWorkflow(String workflowUuid);

    List<AutomationWorkflowProjectCategoryDTO> getCategories();

    List<AutomationWorkflowProjectVersionDTO> getProjectVersions(long projectId);

    List<AutomationWorkflowProjectDTO> getProjects();

    List<AutomationWorkflowProjectTagDTO> getTags();

    void publishProject(long projectId);

    void updateProject(
        long projectId, String name, String description, String category, List<String> tags,
        String permissionExpression, @Nullable Boolean automationHubVisible);

    void updateProjectWorkflow(String workflowUuid, String label, String description);

    void updateProjectWorkflowPermissionExpression(String workflowUuid, String permissionExpression);
}
