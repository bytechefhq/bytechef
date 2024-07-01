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

package com.bytechef.platform.user.facade;

import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.user.domain.ApiKey;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.ApiKeyService;
import com.bytechef.platform.user.service.UserService;
import org.springframework.lang.NonNull;
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

    public ApiKeyFacadeImpl(ApiKeyService apiKeyService, UserService userService) {
        this.apiKeyService = apiKeyService;
        this.userService = userService;
    }

    @Override
    public String create(@NonNull ApiKey apiKey, @NonNull AppType type) {
        User user = userService.getCurrentUser();

        apiKey.setType(type);
        apiKey.setUserId(user.getId());

        return apiKeyService.create(apiKey);
    }
}
