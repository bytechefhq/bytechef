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

package com.bytechef.platform.security.web.authentication;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Marks an authenticated caller that is not a platform user. A module above the platform authenticates such callers;
 * the platform only needs to know that they own no platform-user resources, so a platform-user check never grants them
 * access, even when their identifier happens to equal a platform login.
 *
 * @author Ivica Cardic
 */
public interface NonPlatformUserAuthentication {

    /**
     * @return whether the current authenticated caller is not a platform user
     */
    static boolean isCurrent() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        Authentication authentication = securityContext.getAuthentication();

        return authentication instanceof NonPlatformUserAuthentication && authentication.isAuthenticated();
    }
}
