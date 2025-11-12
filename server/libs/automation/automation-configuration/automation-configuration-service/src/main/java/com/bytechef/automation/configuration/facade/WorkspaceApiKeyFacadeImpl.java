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

package com.bytechef.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.WorkspaceApiKey;
import com.bytechef.automation.configuration.service.WorkspaceApiKeyService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WorkspaceApiKeyFacadeImpl implements WorkspaceApiKeyFacade {

    private final ApiKeyFacade apiKeyFacade;
    private final ApiKeyService apiKeyService;
    private final WorkspaceApiKeyService workspaceApiKeyService;

    @SuppressFBWarnings("EI")
    public WorkspaceApiKeyFacadeImpl(
        ApiKeyFacade apiKeyFacade, ApiKeyService apiKeyService, WorkspaceApiKeyService workspaceApiKeyService) {

        this.apiKeyFacade = apiKeyFacade;
        this.apiKeyService = apiKeyService;
        this.workspaceApiKeyService = workspaceApiKeyService;
    }

    @Override
    public String create(long workspaceId, ApiKey apiKey) {
        apiKey = apiKeyFacade.create(apiKey, ModeType.AUTOMATION);

        workspaceApiKeyService.create(apiKey.getId(), workspaceId);

        return apiKey.getSecretKey();
    }

    @Override
    public void delete(long apiKeyId) {
        workspaceApiKeyService.deleteWorkspaceApiKey(apiKeyId);
        apiKeyService.delete(apiKeyId);
    }

    @Override
    public List<ApiKey> getApiKeys(long workspaceId, long environmentId) {
        List<Long> apiKeyIds = CollectionUtils.map(
            workspaceApiKeyService.getWorkspaceApiKeys(workspaceId), WorkspaceApiKey::getApiKeyId);

        if (apiKeyIds.isEmpty()) {
            return List.of();
        }

        return apiKeyIds.stream()
            .map(apiKeyService::getApiKey)
            .filter(apiKey -> {
                Environment environment = apiKey.getEnvironment();

                return environment.ordinal() == environmentId;
            })
            .toList();
    }
}
