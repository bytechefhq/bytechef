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

package com.bytechef.platform.security.facade;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ApiKeyFacadeImpl implements ApiKeyFacade {

    private final ApiKeyService apiKeyService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public ApiKeyFacadeImpl(ApiKeyService apiKeyService, UserService userService) {
        this.apiKeyService = apiKeyService;
        this.userService = userService;
    }

    @Override
    public ApiKey create(ApiKey apiKey, PlatformType type) {
        User user = userService.getCurrentUser();

        apiKey.setType(type);
        apiKey.setUserId(user.getId());

        return apiKeyService.create(apiKey);
    }

    @Override
    public void delete(long id) {
        apiKeyService.delete(id);
    }

    @Override
    public java.util.List<ApiKey> getAdminApiKeys(long environmentId) {
        return apiKeyService.getApiKeys(environmentId, null);
    }

    @Override
    public ApiKey getApiKey(long id) {
        return apiKeyService.getApiKey(id);
    }

    @Override
    public java.util.List<ApiKey> getApiKeys(long environmentId, PlatformType type) {
        return apiKeyService.getApiKeys(environmentId, type);
    }

    @Override
    public ApiKey update(ApiKey apiKey) {
        return apiKeyService.update(apiKey);
    }
}
