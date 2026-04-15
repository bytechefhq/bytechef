/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
 * avoid re-hitting {@link UserService} on every SpEL evaluation. Extracted from {@code PermissionServiceImpl} so the
 * permission service stays focused on authorization logic and so other components that need the same resolution (e.g.,
 * audit aspects, future workspace-scoped lookups) can depend on it without pulling in the rest of the permission
 * machinery.
 *
 * <p>
 * Returns an empty {@link OptionalLong} when no SecurityContext is available (anonymous request, async executor without
 * {@code DelegatingSecurityContextExecutor}). Permission callers treat the empty case as fail-closed so Spring
 * Security's normal authentication flow can respond with 401/403 instead of bubbling a bare
 * {@link IllegalStateException} as a 500.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class CurrentUserResolver {

    private static final Logger logger = LoggerFactory.getLogger(CurrentUserResolver.class);

    private final UserService userService;

    @SuppressFBWarnings("EI")
    public CurrentUserResolver(UserService userService) {
        this.userService = userService;
    }

    /**
     * Resolves the current authenticated user's id, caching within the active HTTP request so multiple {@code SpEL}
     * expressions that fire during one request share a single {@code UserService.getUser} call. Outside an HTTP request
     * the resolver still works but skips caching (no scope to attach to).
     */
    public OptionalLong fetchCurrentUserId() {
        Optional<String> login = SecurityUtils.fetchCurrentUserLogin();

        if (login.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "No SecurityContext for permission check \u2014 fail-closed (returning empty). "
                        + "Likely an async executor without DelegatingSecurityContextExecutor or an anonymous request.");
            }

            return OptionalLong.empty();
        }

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        if (requestAttributes != null) {
            // Typed holder: storing the raw Map<String, Long> under a String key would force a `@SuppressWarnings
            // ("unchecked")` cast AND risk a ClassCastException if any unrelated code ever wrote a different type to
            // the same key. Wrapping the map in a UserIdCache value type makes the attribute slot single-purpose and
            // removes the cast entirely.
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

        return OptionalLong.of(
            userService.getUser(login.get())
                .getId());
    }

    /**
     * Per-request (login \u2192 userId) cache wrapper. Holds its own key so the slot in {@link RequestAttributes} is
     * single-purpose and we can use {@code instanceof} instead of an unchecked cast.
     */
    private static final class UserIdCache {

        static final String REQUEST_ATTRIBUTE_KEY = "currentUserResolver.userIdCache";

        private final ConcurrentHashMap<String, Long> logins = new ConcurrentHashMap<>();

        long resolve(String login, UserService userService) {
            return logins.computeIfAbsent(login, key -> userService.getUser(key)
                .getId());
        }
    }
}
