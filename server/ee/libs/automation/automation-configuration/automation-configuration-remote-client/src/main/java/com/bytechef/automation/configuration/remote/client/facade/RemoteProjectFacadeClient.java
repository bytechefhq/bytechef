/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.automation.configuration.remote.client.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.dto.ProjectDTO;
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
    public Workflow addWorkflow(long id, @NonNull String definition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkProjectStatus(long id, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectDTO createProject(@NonNull ProjectDTO projectDTO) {
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
    public List<Workflow> getProjectWorkflows() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Workflow> getProjectWorkflows(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Workflow> getProjectVersionWorkflows(long id, int projectVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectDTO> getProjects(Long categoryId, boolean projectInstances, Long tagId, Status status) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectDTO updateProject(@NonNull ProjectDTO projectDTO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateProjectTags(long id, @NonNull List<Tag> tags) {
        throw new UnsupportedOperationException();
    }
}
