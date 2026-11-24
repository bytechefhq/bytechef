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

package com.bytechef.automation.configuration.service;

import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Resolves the current user id from Spring Security and caches the (login \u2192 userId) lookup per HTTP request to
 * avoid re-hitting {@link UserService} on every SpEL evaluation.
 *
 * @author Ivica Cardic
 */
@Service
public class CurrentUserResolverImpl implements CurrentUserResolver {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserResolverImpl.class);

    private final UserService userService;

    @SuppressFBWarnings("EI")
    public CurrentUserResolverImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public OptionalLong fetchCurrentUserId() {
        Optional<String> login = SecurityUtils.fetchCurrentUserLogin();

        if (login.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug(
                    "No SecurityContext for permission check \u2014 fail-closed (returning empty). " +
                        "Likely an async executor without DelegatingSecurityContextExecutor or an anonymous request.");
            }

            return OptionalLong.empty();
        }

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        OptionalLong userId;

        if (requestAttributes == null) {
            userId = fetchUserId(login.get(), userService);
        } else {
            Object cached = requestAttributes.getAttribute(
                UserIdCache.REQUEST_ATTRIBUTE_KEY, RequestAttributes.SCOPE_REQUEST);

            UserIdCache userIdCache;

            if (cached instanceof UserIdCache existing) {
                userIdCache = existing;
            } else {
                userIdCache = new UserIdCache();

                requestAttributes.setAttribute(
                    UserIdCache.REQUEST_ATTRIBUTE_KEY, userIdCache, RequestAttributes.SCOPE_REQUEST);
            }

            userId = userIdCache.resolve(login.get(), userService);
        }

        if (userId.isEmpty() && log.isDebugEnabled()) {
            log.debug(
                "Authenticated login '{}' has no backing platform user — fail-closed (returning empty).", login.get());
        }

        return userId;
    }

    private static OptionalLong fetchUserId(String login, UserService userService) {
        return userService.fetchUserByLogin(login)
            .map(user -> OptionalLong.of(user.getId()))
            .orElse(OptionalLong.empty());
    }

    private static final class UserIdCache {

        static final String REQUEST_ATTRIBUTE_KEY = "currentUserResolver.userIdCache";

        private final ConcurrentHashMap<String, OptionalLong> logins = new ConcurrentHashMap<>();

        OptionalLong resolve(String login, UserService userService) {
            return logins.computeIfAbsent(login, key -> fetchUserId(key, userService));
        }
    }
}
