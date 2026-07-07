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

package com.bytechef.ai.copilot.tool;

import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Rehydrates Spring Security's SecurityContext from a user id on Reactor scheduler / worker threads that did not
 * inherit the request thread's thread-local SecurityContext. Single source of truth for the
 * {@code userId -> login + authorities -> SecurityUtils.runAs} sequence shared by the property-options pickers and the
 * {@link RehydrateContextToolCallback}.
 *
 * @author Ivica Cardic
 */
@Component
public class SecurityContextRehydrator {

    private static final Logger log = LoggerFactory.getLogger(SecurityContextRehydrator.class);

    private final UserService userService;
    private final AuthorityService authorityService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SecurityContextRehydrator(UserService userService, AuthorityService authorityService) {
        this.userService = userService;
        this.authorityService = authorityService;
    }

    /**
     * Resolves the user's login + authorities and runs {@code action} inside a temporary SecurityContext. When
     * {@code userId} is null or the user no longer exists, runs the action without rehydration. Authority-resolution
     * failures are logged and the user is treated as having no authorities rather than aborting.
     */
    public <T> T withUserSecurityContext(@Nullable Long userId, Supplier<T> action) {
        if (userId == null) {
            return action.get();
        }

        Optional<User> userOptional = userService.fetchUser(userId);

        if (userOptional.isEmpty()) {
            log.warn(
                "Skipping SecurityContext rehydration: user id {} not found — downstream authorization will fail" +
                    " closed",
                userId);

            return action.get();
        }

        User user = userOptional.get();

        List<GrantedAuthority> authorities = new ArrayList<>();

        for (Long authorityId : user.getAuthorityIds()) {
            Optional<Authority> authorityOptional = authorityService.fetchAuthority(authorityId);

            if (authorityOptional.isEmpty()) {
                log.debug(
                    "Authority id {} for user id {} did not resolve; dropping it from the rehydrated context",
                    authorityId, userId);

                continue;
            }

            Authority authority = authorityOptional.get();

            authorities.add(new SimpleGrantedAuthority(authority.getName()));
        }

        return SecurityUtils.runAs(user.getLogin(), authorities, action);
    }
}
