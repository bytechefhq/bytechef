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
import com.bytechef.platform.constant.Type;
import com.bytechef.platform.user.domain.ApiKey;
import com.bytechef.platform.user.repository.ApiKeyRepository;
import java.util.List;
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
    public ApiKey create(@NonNull ApiKey apiKey) {
        Validate.notNull(apiKey, "'apiKey' must not be null");
        Validate.isTrue(apiKey.getId() == null, "'id' must be null");
        Validate.notNull(apiKey.getSecretKey(), "'secretKey' must not be null");
        Validate.notNull(apiKey.getName(), "'name' must not be null");

        return apiKeyRepository.save(apiKey);
    }

    @Override
    public void delete(long id) {
        apiKeyRepository.deleteById(id);
    }

    @Override
    public ApiKey getApiKey(long id) {
        return OptionalUtils.get(apiKeyRepository.findById(id));
    }

    @Override
    public List<ApiKey> getApiKeys(Type type) {
        return apiKeyRepository.findAllByType(type.ordinal());
    }

    @Override
    public ApiKey update(@NonNull ApiKey apiKey) {
        Validate.notNull(apiKey, "'apiKey' must not be null");

        ApiKey curApiKey = getApiKey(Validate.notNull(apiKey.getId(), "id"));

        curApiKey.setName(Validate.notNull(apiKey.getName(), "name"));

        return apiKeyRepository.save(curApiKey);
    }
}
