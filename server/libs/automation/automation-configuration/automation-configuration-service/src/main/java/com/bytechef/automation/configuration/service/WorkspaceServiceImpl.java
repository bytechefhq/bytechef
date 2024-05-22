/*
 * Copyright 2023-present ByteChef Inc.
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
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.commons.util.OptionalUtils;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Validate.notNull(workspace, "'workspace' must not be null");
        Validate.isTrue(workspace.getId() == null, "'workspace.id' must be null");

        return workspaceRepository.save(workspace);
    }

    @Override
    public void delete(long id) {
        workspaceRepository.deleteById(id);
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
        Validate.notNull(workspace, "'workspace' must not be null");
        Validate.isTrue(workspace.getId() != null, "'workspace.id' must not be null");

        Workspace curWorkspace = OptionalUtils.get(workspaceRepository.findById(workspace.getId()));

        curWorkspace.setDescription(workspace.getDescription());
        curWorkspace.setName(workspace.getName());
        curWorkspace.setVersion(workspace.getVersion());

        return workspaceRepository.save(curWorkspace);
    }
}
