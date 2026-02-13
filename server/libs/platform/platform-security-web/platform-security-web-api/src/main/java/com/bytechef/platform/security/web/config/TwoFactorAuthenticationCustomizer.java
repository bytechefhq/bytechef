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

package com.bytechef.platform.security.web.config;

import org.springframework.security.core.Authentication;

/**
 * Allows security-config (which doesn't depend on platform-user) to check whether TOTP-based two-factor authentication
 * is enabled for a given authenticated user.
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface TwoFactorAuthenticationCustomizer {

    boolean isTotpEnabled(Authentication authentication);
}
