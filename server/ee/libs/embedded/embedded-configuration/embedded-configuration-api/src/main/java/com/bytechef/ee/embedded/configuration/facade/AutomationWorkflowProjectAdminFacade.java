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
import com.bytechef.platform.configuration.dto.VersionWorkflowChange;
import com.bytechef.platform.configuration.dto.VersionWorkflowChanges;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
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

    String duplicateProjectWorkflow(String workflowUuid);

    List<AutomationWorkflowProjectCategoryDTO> getCategories();

    VersionWorkflowChange getProjectVersionWorkflowChange(long projectId, int projectVersion, String workflowUuid);

    VersionWorkflowChanges getProjectVersionWorkflowChanges(long projectId, int projectVersion);

    List<AutomationWorkflowProjectVersionDTO> getProjectVersions(long projectId);

    List<AutomationWorkflowProjectDTO> getProjects();

    List<AutomationWorkflowProjectTagDTO> getTags();

    int publishProject(long projectId, @Nullable String description);

    void updateProject(
        long projectId, String name, String description, String category, List<String> tags,
        String permissionExpression);

    void updateProjectWorkflow(String workflowUuid, String label, String description);

    void updateProjectWorkflowPermissionExpression(String workflowUuid, String permissionExpression);
}
