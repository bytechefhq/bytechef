/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.facade;

import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectTemplateDTO;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.dto.SharedProjectDTO;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteProjectFacadeClient implements ProjectFacade {

    @Override
    public long createProject(ProjectDTO projectDTO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteProject(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteSharedProject(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectDTO duplicateProject(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] exportProject(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportSharedProject(long id, String description) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectTemplateDTO getProjectTemplate(String id, boolean sharedProject) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectDTO getProject(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectTemplateDTO> getPreBuiltProjectTemplates(String query, String category) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectDTO> getProjects(Long categoryId, Boolean projectDeployments, Long tagId, Status status) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SharedProjectDTO getSharedProject(String projectUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectDTO> getWorkspaceProjects(
        Boolean apiCollections, Long categoryId, boolean includeAllFields, Boolean projectDeployments, Status status,
        Long tagId, long workspaceId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectWorkflowDTO> getWorkspaceProjectWorkflows(long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long importProject(byte[] projectData, long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long importProjectTemplate(String id, long workspaceId, boolean sharedProject) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int publishProject(long id, String description, boolean syncWithGit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateProject(ProjectDTO projectDTO) {
        throw new UnsupportedOperationException();
    }
}
