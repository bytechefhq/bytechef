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

package com.bytechef.platform.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.configuration.domain.ApiKey;
import com.bytechef.platform.configuration.repository.ApiKeyRepository;
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

    private final ApiKeyRepository signingKeyRepository;

    public ApiKeyServiceImpl(ApiKeyRepository signingKeyRepository) {
        this.signingKeyRepository = signingKeyRepository;
    }

    @Override
    public ApiKey create(@NonNull ApiKey apiKey) {
        Validate.notNull(apiKey, "'apiKey' must not be null");
        Validate.isTrue(apiKey.getId() == null, "'id' must be null");
        Validate.notNull(apiKey.getSecretKey(), "'secretKey' must not be null");
        Validate.notNull(apiKey.getName(), "'name' must not be null");

        return signingKeyRepository.save(apiKey);
    }

    @Override
    public void delete(long id) {
        signingKeyRepository.deleteById(id);
    }

    @Override
    public ApiKey getApiKey(long id) {
        return OptionalUtils.get(signingKeyRepository.findById(id));
    }

    @Override
    public List<ApiKey> getApiKeys() {
        return signingKeyRepository.findAll();
    }

    @Override
    public ApiKey update(@NonNull ApiKey apiKey) {
        Validate.notNull(apiKey, "'apiKey' must not be null");

        ApiKey curApiKey = getApiKey(Validate.notNull(apiKey.getId(), "id"));

        curApiKey.setName(Validate.notNull(apiKey.getName(), "name"));

        return signingKeyRepository.save(curApiKey);
    }
}
