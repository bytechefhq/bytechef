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
import java.util.List;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ApiKeyService {

    ApiKey create(ApiKey apiKey);

    void delete(long id);

    ApiKey getApiKey(String secretKey);

    ApiKey getApiKey(String secretKey, long environmentId);

    ApiKey getApiKey(long id);

    List<ApiKey> getApiKeys(long environmentId, @Nullable ModeType type);

    ApiKey update(ApiKey apiKey);
}
