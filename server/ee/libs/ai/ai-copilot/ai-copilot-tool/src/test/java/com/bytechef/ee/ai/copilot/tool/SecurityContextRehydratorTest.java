/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class SecurityContextRehydratorTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthorityService authorityService;

    @Test
    void testRunsAsUserWhenUserIdPresent() {
        User user = new User();

        user.setId(7L);
        user.setLogin("user@localhost.com");

        when(userService.fetchUser(7L)).thenReturn(Optional.of(user));

        SecurityContextRehydrator rehydrator = new SecurityContextRehydrator(userService, authorityService);

        String principal = rehydrator.withUserSecurityContext(
            7L, () -> (String) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal());

        assertThat(principal).isEqualTo("user@localhost.com");
    }

    @Test
    void testNoRehydrationWhenUserIdNull() {
        SecurityContextRehydrator rehydrator = new SecurityContextRehydrator(userService, authorityService);

        boolean noAuth = rehydrator.withUserSecurityContext(
            null, () -> SecurityContextHolder.getContext()
                .getAuthentication() == null);

        assertThat(noAuth).isTrue();
    }
}
