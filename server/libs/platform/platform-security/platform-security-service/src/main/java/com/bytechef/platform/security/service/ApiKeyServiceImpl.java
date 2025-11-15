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

package com.bytechef.platform.security.service;

import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.repository.ApiKeyRepository;
import com.bytechef.tenant.domain.TenantKey;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyServiceImpl(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    public ApiKey create(ApiKey apiKey) {
        Assert.notNull(apiKey, "'apiKey' must not be null");
        Assert.isTrue(apiKey.getId() == null, "'id' must be null");
        Assert.notNull(apiKey.getName(), "'name' must not be null");

        apiKey.setSecretKey(String.valueOf(TenantKey.of()));

        return apiKeyRepository.save(apiKey);
    }

    @Override
    public void delete(long id) {
        apiKeyRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiKey getApiKey(String secretKey) {
        return apiKeyRepository.findBySecretKey(secretKey)
            .orElseThrow(() -> new IllegalArgumentException("Api key not found."));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiKey getApiKey(String secretKey, long environmentId) {
        return apiKeyRepository.findBySecretKeyAndEnvironment(secretKey, (int) environmentId)
            .orElseThrow(() -> new IllegalArgumentException("Api key not found for the specified environment."));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiKey getApiKey(long id) {
        return apiKeyRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Api key not found for id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiKey> getApiKeys(long environmentId, ModeType type) {
        if (type == null) {
            return apiKeyRepository.findAllByEnvironmentAndTypeIsNull((int) environmentId);
        } else {
            return apiKeyRepository.findAllByEnvironmentAndType((int) environmentId, type.ordinal());
        }
    }

    @Override
    public ApiKey update(ApiKey apiKey) {
        Assert.notNull(apiKey, "'apiKey' must not be null");

        ApiKey curApiKey = getApiKey(Validate.notNull(apiKey.getId(), "id"));

        curApiKey.setName(Validate.notNull(apiKey.getName(), "name"));

        return apiKeyRepository.save(curApiKey);
    }
}
