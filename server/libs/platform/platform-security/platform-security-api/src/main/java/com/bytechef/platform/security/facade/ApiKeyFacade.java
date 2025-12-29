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
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ApiKeyFacade {

    ApiKey create(ApiKey apiKey, @Nullable PlatformType type);

    void delete(long id);

    List<ApiKey> getAdminApiKeys(long environmentId);

    ApiKey getApiKey(long id);

    List<ApiKey> getApiKeys(long environmentId, PlatformType type);

    ApiKey update(ApiKey apiKey);
}
