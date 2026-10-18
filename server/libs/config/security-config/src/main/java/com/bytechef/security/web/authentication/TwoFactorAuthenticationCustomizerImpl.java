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

package com.bytechef.security.web.authentication;

import com.bytechef.platform.security.web.config.TwoFactorAuthenticationCustomizer;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Decides whether a just-authenticated user still owes a TOTP code. The user is looked up by the authentication's name,
 * which is the ByteChef login for every login path (form, social, SSO), and in multi-tenant mode inside the tenant that
 * holds the login: these checks run before the session is bound to a tenant, in the default one.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.security.two-factor-authentication", name = "enabled", havingValue = "true")
class TwoFactorAuthenticationCustomizerImpl implements TwoFactorAuthenticationCustomizer {

    private final TenantService tenantService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    TwoFactorAuthenticationCustomizerImpl(TenantService tenantService, UserService userService) {
        this.tenantService = tenantService;
        this.userService = userService;
    }

    @Override
    public boolean isTotpEnabled(Authentication authentication) {
        String login = authentication.getName();

        if (login == null) {
            return false;
        }

        if (!tenantService.isMultiTenantEnabled()) {
            return isTotpEnabled(login);
        }

        List<String> tenantIds = tenantService.getTenantIdsByUserLogin(login);

        if (tenantIds.isEmpty()) {
            return false;
        }

        String previousTenantId = TenantContext.getCurrentTenantId();

        TenantContext.setCurrentTenantId(tenantIds.getFirst());

        try {
            return isTotpEnabled(login);
        } finally {
            TenantContext.setCurrentTenantId(previousTenantId);
        }
    }

    private boolean isTotpEnabled(String login) {
        Optional<User> user = userService.fetchUserByLogin(login);

        return user.map(User::isTotpEnabled)
            .orElse(false);
    }
}
