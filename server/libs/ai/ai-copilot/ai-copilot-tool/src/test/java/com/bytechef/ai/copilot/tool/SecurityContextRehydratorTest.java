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
