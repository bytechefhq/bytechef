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

import com.bytechef.automation.configuration.domain.WorkspaceApiKey;
import com.bytechef.automation.configuration.repository.WorkspaceApiKeyRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WorkspaceApiKeyServiceImpl implements WorkspaceApiKeyService {

    private final WorkspaceApiKeyRepository workspaceApiKeyRepository;

    public WorkspaceApiKeyServiceImpl(WorkspaceApiKeyRepository workspaceApiKeyRepository) {
        this.workspaceApiKeyRepository = workspaceApiKeyRepository;
    }

    @Override
    public WorkspaceApiKey create(long apiKeyId, long workspaceId) {
        return workspaceApiKeyRepository.save(new WorkspaceApiKey(apiKeyId, workspaceId));
    }

    @Override
    public void delete(long id) {
        workspaceApiKeyRepository.deleteById(id);
    }

    @Override
    public List<WorkspaceApiKey> getWorkspaceApiKeys(long workspaceId) {
        return workspaceApiKeyRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    public void deleteWorkspaceApiKey(long apiKeyId) {
        workspaceApiKeyRepository.findByApiKeyId(apiKeyId)
            .ifPresent(workspaceApiKey -> workspaceApiKeyRepository.deleteById(workspaceApiKey.getId()));
    }
}
