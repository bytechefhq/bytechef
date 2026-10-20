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

package com.bytechef.ee.embedded.configuration.config;

import static org.mockito.Mockito.mock;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import org.mockito.Answers;

/**
 * @author Ivica Cardic
 */
public final class TenantAdminOnlyPermissionService {

    private TenantAdminOnlyPermissionService() {
    }

    public static PermissionService create() {
        return mock(PermissionService.class, invocation -> {
            Class<?> returnType = invocation.getMethod()
                .getReturnType();

            if (returnType != boolean.class) {
                return Answers.RETURNS_DEFAULTS.answer(invocation);
            }

            String methodName = invocation.getMethod()
                .getName();

            if (methodName.equals("isAuthorizationSkipped")) {
                return false;
            }

            return SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN);
        });
    }
}
