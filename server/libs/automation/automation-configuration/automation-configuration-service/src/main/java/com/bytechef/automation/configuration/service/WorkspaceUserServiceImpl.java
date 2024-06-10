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

import com.bytechef.automation.configuration.domain.WorkspaceUser;
import com.bytechef.automation.configuration.repository.WorkspaceUserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WorkspaceUserServiceImpl implements WorkspaceUserService {

    private final WorkspaceUserRepository workspaceUserRepository;

    public WorkspaceUserServiceImpl(WorkspaceUserRepository workspaceUserRepository) {
        this.workspaceUserRepository = workspaceUserRepository;
    }

    @Override
    public WorkspaceUser create(long userId, long workspaceId) {
        return workspaceUserRepository.save(new WorkspaceUser(userId, workspaceId));
    }

    @Override
    public void delete(long id) {
        workspaceUserRepository.deleteById(id);
    }

    @Override
    public List<WorkspaceUser> getUserWorkspaceUsers(long userId) {
        return workspaceUserRepository.findAllByUserId(userId);
    }

    @Override
    public List<WorkspaceUser> getWorkspaceWorkspaceUsers(long workspaceId) {
        return workspaceUserRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    public void deleteWorkspaceUser(long userId) {
        workspaceUserRepository.findByUserId(userId)
            .ifPresent(workspaceUser -> workspaceUserRepository.deleteById(workspaceUser.getId()));
    }
}
