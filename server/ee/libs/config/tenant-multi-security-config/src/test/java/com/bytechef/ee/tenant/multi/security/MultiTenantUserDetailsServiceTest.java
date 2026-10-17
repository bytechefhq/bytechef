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

package com.bytechef.ee.tenant.multi.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.security.web.authentication.TenantUser;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * @author Ivica Cardic
 */
class MultiTenantUserDetailsServiceTest {

    private static final String EMAIL = "jane@example.com";
    private static final String LOGIN = "jane";
    private static final String TENANT_ID = "000003";

    private final TenantService tenantService = mock(TenantService.class);
    private final UserService userService = mock(UserService.class);
    private MultiTenantUserDetailsService multiTenantUserDetailsService;

    @BeforeEach
    void beforeEach() {
        multiTenantUserDetailsService = new MultiTenantUserDetailsService(mock(AuthorityService.class), tenantService);

        ApplicationContext applicationContext = mock(ApplicationContext.class);

        when(applicationContext.getBean(UserService.class)).thenReturn(userService);

        multiTenantUserDetailsService.setApplicationContext(applicationContext);
    }

    @Test
    void testLoadUserByLoginRecordsTheTenantTheUserWasLoadedFrom() {
        when(tenantService.getTenantIdsByUserLogin(LOGIN)).thenReturn(List.of(TENANT_ID));
        when(userService.fetchUserByLogin(LOGIN)).thenAnswer(invocation -> createUser());

        UserDetails userDetails = multiTenantUserDetailsService.loadUserByUsername(LOGIN);

        assertThat(userDetails).isInstanceOf(TenantUser.class);
        assertThat(((TenantUser) userDetails).getTenantId()).isEqualTo(TENANT_ID);
        assertThat(userDetails.getUsername()).isEqualTo(LOGIN);
    }

    @Test
    void testLoadUserByEmailRecordsTheTenantTheUserWasLoadedFrom() {
        when(tenantService.getTenantIdsByUserEmail(EMAIL)).thenReturn(List.of(TENANT_ID));
        when(userService.fetchUserByEmail(EMAIL)).thenAnswer(invocation -> createUser());

        UserDetails userDetails = multiTenantUserDetailsService.loadUserByUsername(EMAIL);

        assertThat(userDetails).isInstanceOf(TenantUser.class);
        assertThat(((TenantUser) userDetails).getTenantId()).isEqualTo(TENANT_ID);
        assertThat(userDetails.getUsername()).isEqualTo(LOGIN);
    }

    private static Optional<User> createUser() {
        if (!TENANT_ID.equals(TenantContext.getCurrentTenantId())) {
            return Optional.empty();
        }

        User user = new User();

        user.setActivated(true);
        user.setLogin(LOGIN);
        user.setPassword(String.valueOf(UUID.randomUUID()));

        return Optional.of(user);
    }
}
