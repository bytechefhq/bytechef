/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.automation.configuration.exception.WorkspaceErrorType;
import com.bytechef.ee.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
@SuppressFBWarnings("NM")
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    public WorkspaceServiceImpl(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public Workspace create(Workspace workspace) {
        Assert.notNull(workspace, "'workspace' must not be null");
        Assert.isTrue(workspace.getId() == null, "'workspace.id' must be null");

        return workspaceRepository.save(workspace);
    }

    @Override
    public void delete(long id) {
        if (id == Workspace.DEFAULT_WORKSPACE_ID) {
            throw new ConfigurationException(
                "Default workspace cannot be deleted", WorkspaceErrorType.DEFAULT_WORKSPACE_NOT_DELETABLE);
        }

        workspaceRepository.deleteById(id);
    }

    @Override
    public Workspace getProjectWorkspace(long projectId) {
        return workspaceRepository.findByProjectId(projectId);
    }

    @Override
    public List<Workspace> getWorkspaces() {
        return workspaceRepository.findAll();
    }

    @Override
    public Workspace getWorkspace(long id) {
        return OptionalUtils.get(workspaceRepository.findById(id));
    }

    @Override
    public Workspace update(Workspace workspace) {
        Assert.notNull(workspace, "'workspace' must not be null");
        Assert.isTrue(workspace.getId() != null, "'workspace.id' must not be null");

        if (workspace.getId() == Workspace.DEFAULT_WORKSPACE_ID) {
            throw new ConfigurationException(
                "Default workspace cannot be updated", WorkspaceErrorType.DEFAULT_WORKSPACE_NOT_CHANGEABLE);
        }

        Workspace curWorkspace = OptionalUtils.get(workspaceRepository.findById(workspace.getId()));

        curWorkspace.setDescription(workspace.getDescription());
        curWorkspace.setName(workspace.getName());
        curWorkspace.setVersion(workspace.getVersion());

        return workspaceRepository.save(curWorkspace);
    }
}
