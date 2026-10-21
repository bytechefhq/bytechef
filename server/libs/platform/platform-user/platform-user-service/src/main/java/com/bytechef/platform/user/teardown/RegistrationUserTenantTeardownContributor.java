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

package com.bytechef.platform.user.teardown;

import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.repository.UserRepository;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.tenant.teardown.TenantTeardownContributor;
import com.bytechef.tenant.util.TenantIdUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@Order(900)
class RegistrationUserTenantTeardownContributor implements TenantTeardownContributor {

    private final TenantService tenantService;
    private final UserRepository userRepository;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    RegistrationUserTenantTeardownContributor(
        TenantService tenantService, UserRepository userRepository, UserService userService) {

        this.tenantService = tenantService;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public void beforeSchemaDrop(String tenantId) {
        TenantIdUtils.requireValid(tenantId);

        List<String> logins = userRepository.findAll()
            .stream()
            .map(User::getLogin)
            .toList();

        for (String login : logins) {
            if (!tenantService.getTenantIdsByUserLogin(login)
                .isEmpty()) {

                continue;
            }

            TenantContext.runWithTenantId(TenantContext.DEFAULT_TENANT_ID, () -> userService.delete(login));
        }
    }
}
