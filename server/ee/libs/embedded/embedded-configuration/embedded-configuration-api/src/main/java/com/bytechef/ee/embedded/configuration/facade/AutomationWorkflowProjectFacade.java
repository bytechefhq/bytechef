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
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AutomationWorkflowProjectFacade {

    long createProject(
        String name, String description, String category, List<String> tags, String permissionExpression);

    String duplicateProjectWorkflow(String workflowId);

    long duplicateProject(long projectId);

    List<AutomationWorkflowProjectVersionDTO> getProjectVersions(long projectId);

    List<AutomationWorkflowProjectCategoryDTO> getCategories();

    List<AutomationWorkflowProjectTagDTO> getTags();

    String createProjectWorkflow(long projectId, String definition, String permissionExpression);

    void deleteProject(long projectId);

    void deleteProjectWorkflow(String workflowUuid);

    AutomationWorkflowProjectDTO getProject(long projectId);

    List<AutomationWorkflowProjectDTO> getProjects();

    List<AutomationWorkflowProjectDTO> getPublishedProjects();

    List<AutomationWorkflowProjectDTO> getPublishedProjects(String externalUserId, Environment environment);

    void publishProject(long projectId);

    void updateProject(
        long projectId, String name, String description, String category, List<String> tags,
        String permissionExpression);

    void updateProjectWorkflow(String workflowId, String label, String description);

    void updateProjectWorkflowPermissionExpression(String workflowId, String permissionExpression);
}
