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

package com.bytechef.platform.tenant.service;

import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */

@Service
@ConditionalOnProperty(value = "bytechef.tenant.mode", havingValue = "single")
public class SingleTenantService implements TenantService {

    private final UserService userService;

    @SuppressFBWarnings("EI")
    public SingleTenantService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean isMultipleTenantsAllowed() {
        return userService.countActiveUsers() <= 0;
    }
}
