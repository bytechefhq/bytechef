/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.configuration.service;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.exception.WorkspaceErrorType;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.exception.ConfigurationException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
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
