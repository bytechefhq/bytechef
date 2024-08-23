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

package com.bytechef.platform.user.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.user.domain.ApiKey;
import com.bytechef.platform.user.repository.ApiKeyRepository;
import com.bytechef.tenant.TenantKey;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public String create(@NonNull ApiKey apiKey) {
        Validate.notNull(apiKey, "'apiKey' must not be null");
        Validate.isTrue(apiKey.getId() == null, "'id' must be null");
        Validate.notNull(apiKey.getName(), "'name' must not be null");

        apiKey.setSecretKey(String.valueOf(TenantKey.of()));

        apiKey = apiKeyRepository.save(apiKey);

        return apiKey.getSecretKey();
    }

    @Override
    public void delete(long id) {
        apiKeyRepository.deleteById(id);
    }

    @Override
    public Optional<ApiKey> fetchApiKey(@NonNull String secretKey, Environment environment) {
        if (environment == null) {
            return apiKeyRepository.findBySecretKeyAndEnvironmentIsNull(secretKey);
        } else {
            return apiKeyRepository.findByEnvironmentAndSecretKey(environment.ordinal(), secretKey);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ApiKey getApiKey(long id) {
        return OptionalUtils.get(apiKeyRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiKey> getApiKeys(AppType type) {
        if (type == null) {
            return apiKeyRepository.findAllByTypeIsNull();
        } else {
            return apiKeyRepository.findAllByType(type.ordinal());
        }
    }

    @Override
    public ApiKey update(@NonNull ApiKey apiKey) {
        Validate.notNull(apiKey, "'apiKey' must not be null");

        ApiKey curApiKey = getApiKey(Validate.notNull(apiKey.getId(), "id"));

        curApiKey.setName(Validate.notNull(apiKey.getName(), "name"));

        return apiKeyRepository.save(curApiKey);
    }
}
