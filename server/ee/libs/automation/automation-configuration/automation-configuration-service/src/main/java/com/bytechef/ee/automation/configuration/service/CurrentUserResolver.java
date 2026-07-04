/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.exception.UserNotFoundException;
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
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class CurrentUserResolver {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserResolver.class);

    private final UserService userService;

    @SuppressFBWarnings("EI")
    public CurrentUserResolver(UserService userService) {
        this.userService = userService;
    }

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

        try {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

            if (requestAttributes != null) {
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

                return OptionalLong.of(userIdCache.resolve(login.get(), userService));
            }

            User user = userService.getUser(login.get());

            return OptionalLong.of(user.getId());
        } catch (UserNotFoundException exception) {
            if (log.isDebugEnabled()) {
                log.debug(
                    "Authenticated login '{}' has no backing platform user — fail-closed (returning empty).",
                    login.get());
            }

            return OptionalLong.empty();
        }
    }

    private static final class UserIdCache {

        static final String REQUEST_ATTRIBUTE_KEY = "currentUserResolver.userIdCache";

        private final ConcurrentHashMap<String, Long> logins = new ConcurrentHashMap<>();

        long resolve(String login, UserService userService) {
            return logins.computeIfAbsent(login, key -> {
                User user = userService.getUser(key);

                return user.getId();
            });
        }
    }
}
