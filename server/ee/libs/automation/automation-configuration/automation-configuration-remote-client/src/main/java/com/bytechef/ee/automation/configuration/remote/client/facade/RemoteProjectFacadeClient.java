/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.facade;

import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteProjectFacadeClient implements ProjectFacade {

    @Override
    public long addWorkflow(long id, @NonNull String definition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long createProject(@NonNull ProjectDTO projectDTO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteProject(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteWorkflow(@NonNull String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectDTO duplicateProject(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String duplicateWorkflow(long id, @NonNull String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectDTO getProject(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Category> getProjectCategories() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Tag> getProjectTags() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectWorkflowDTO getProjectWorkflow(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectWorkflowDTO getProjectWorkflow(long projectWorkflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectWorkflowDTO> getProjectWorkflows() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectWorkflowDTO> getProjectWorkflows(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectWorkflowDTO> getProjectVersionWorkflows(long id, int projectVersion, boolean includeAllFields) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectDTO> getProjects(Long categoryId, boolean projectDeployments, Long tagId, Status status) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectDTO> getWorkspaceProjects(
        long workspaceId, boolean projectDeployments, Long categoryId, Long tagId,
        Status status, List<Long> projectIds, boolean includeAllFields) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void publishProject(long id, String description) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateProject(@NonNull ProjectDTO projectDTO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateProjectTags(long id, @NonNull List<Tag> tags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateWorkflow(String workflowId, String definition, int version) {
        throw new UnsupportedOperationException();
    }
}
