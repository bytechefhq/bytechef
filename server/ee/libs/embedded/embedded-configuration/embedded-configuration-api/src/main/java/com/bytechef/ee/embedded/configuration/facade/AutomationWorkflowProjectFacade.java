/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectCategoryDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationReleaseDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectTagDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectVersionDTO;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.dto.VersionWorkflowChange;
import com.bytechef.platform.configuration.dto.VersionWorkflowChanges;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AutomationWorkflowProjectFacade {
    long createProject(
        String name, String description, String category, List<String> tags, String permissionExpression);

    String duplicateProjectWorkflow(String workflowUuid);

    long duplicateProject(long projectId);

    List<AutomationWorkflowProjectVersionDTO> getProjectVersions(long projectId);

    VersionWorkflowChange getProjectVersionWorkflowChange(long projectId, int projectVersion, String workflowUuid);

    VersionWorkflowChanges getProjectVersionWorkflowChanges(long projectId, int projectVersion);

    List<AutomationWorkflowProjectCategoryDTO> getCategories();

    List<AutomationWorkflowProjectTagDTO> getTags();

    String createProjectWorkflow(long projectId, String definition, String permissionExpression);

    void deleteProject(long projectId);

    void deleteProjectWorkflow(String workflowUuid);

    AutomationWorkflowProjectDTO getProject(long projectId);

    List<AutomationWorkflowProjectDTO> getProjects();

    /**
     * Lists every automation workflow project, each with the number of its configurations in the environment.
     *
     * @param environmentId the environment whose configurations are counted
     * @return the projects, sorted by name ignoring case
     */
    List<AutomationWorkflowProjectDTO> getProjects(long environmentId);

    List<AutomationWorkflowProjectDTO> getPublishedProjects();

    List<AutomationWorkflowProjectDTO> getPublishedProjects(String externalUserId, Environment environment);

    /**
     * Lists every enabled configuration in the connected user's environment of each published project the connected
     * user may see, whatever the configuration's permission expression says, with the project as that configuration
     * releases it to the connected user. Projects come in name order and each project's configurations in id order.
     *
     * @param externalUserId the connected user's external id
     * @param environment    the connected user's environment
     * @return the project releases, one per configuration
     */
    List<AutomationWorkflowProjectConfigurationReleaseDTO> getPublishedProjectConfigurations(
        String externalUserId, Environment environment);

    int publishProject(long projectId, @Nullable String description);

    void updateProject(
        long projectId, String name, String description, String category, List<String> tags,
        String permissionExpression);

    void updateProjectWorkflow(String workflowUuid, String label, String description);

    void updateProjectWorkflowPermissionExpression(String workflowUuid, String permissionExpression);
}
