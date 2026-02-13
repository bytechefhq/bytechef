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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.security.two-factor-authentication", name = "enabled", havingValue = "true")
class TwoFactorAuthenticationCustomizerImpl implements TwoFactorAuthenticationCustomizer {

    private final UserService userService;

    TwoFactorAuthenticationCustomizerImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean isTotpEnabled(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            return userService.fetchUserByLogin(userDetails.getUsername())
                .map(User::isTotpEnabled)
                .orElse(false);
        }

        return false;
    }
}
