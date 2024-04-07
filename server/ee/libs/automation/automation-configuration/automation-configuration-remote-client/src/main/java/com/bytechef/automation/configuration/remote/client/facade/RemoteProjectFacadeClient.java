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
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.tag.domain.Tag;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteProjectFacadeClient implements ProjectFacade {

    @Override
    public Workflow addWorkflow(long id, String definition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectDTO createProject(ProjectDTO projectDTO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteProject(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteWorkflow(long id, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectDTO duplicateProject(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String duplicateWorkflow(long id, String workflowId) {
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
    public ProjectDTO updateProject(ProjectDTO projectDTO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateProjectTags(long id, List<Tag> tags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WorkflowDTO updateWorkflow(String id, String definition, Integer version) {
        throw new UnsupportedOperationException();
    }
}
