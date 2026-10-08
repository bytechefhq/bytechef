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

package com.bytechef.security.web.rememberme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.domain.PersistentToken;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.PersistentTokenService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.security.config.RememberMeKey;
import com.bytechef.security.web.authentication.TenantUserDetails;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * @author Ivica Cardic
 */
class PersistentTokenRememberMeServicesTest {

    @Test
    void testLoginSuccessStoresTheTokenInTheAuthenticatedTenantWhenTheLoginExistsInTwoTenants() {
        RememberMeKey rememberMeKey = mock(RememberMeKey.class);
        PersistentTokenService persistentTokenService = mock(PersistentTokenService.class);
        TenantService tenantService = mock(TenantService.class);
        UserService userService = mock(UserService.class);
        ApplicationContext applicationContext = mock(ApplicationContext.class);

        User tenantOneUser = createUser(1L);
        User tenantTwoUser = createUser(2L);

        List<String> savedTenantIds = new ArrayList<>();
        List<Long> savedUserIds = new ArrayList<>();

        when(rememberMeKey.getKey()).thenReturn("remember-me-key");
        when(applicationContext.getBean(UserService.class)).thenReturn(userService);
        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIdsByUserLogin("shared")).thenReturn(List.of("000002", "000001"));
        when(userService.fetchUserByLogin("shared"))
            .thenAnswer(invocation -> "000001".equals(TenantContext.getCurrentTenantId())
                ? Optional.of(tenantOneUser) : Optional.of(tenantTwoUser));
        when(persistentTokenService.save(any(PersistentToken.class)))
            .thenAnswer(invocation -> {
                PersistentToken persistentToken = invocation.getArgument(0);

                savedTenantIds.add(TenantContext.getCurrentTenantId());
                savedUserIds.add(persistentToken.getUserId());

                return persistentToken;
            });

        PersistentTokenRememberMeServices persistentTokenRememberMeServices = new PersistentTokenRememberMeServices(
            rememberMeKey, mock(UserDetailsService.class), persistentTokenService, tenantService);

        persistentTokenRememberMeServices.setApplicationContext(applicationContext);

        TenantUserDetails principal = new TenantUserDetails("shared", "password", List.of(), "000001");
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.addHeader("User-Agent", "test-agent");

        persistentTokenRememberMeServices.onLoginSuccess(
            request, new MockHttpServletResponse(),
            new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        assertThat(savedTenantIds).containsExactly("000001");
        assertThat(savedUserIds).containsExactly(1L);
    }

    private static User createUser(Long id) {
        User user = new User();

        user.setId(id);
        user.setLogin("shared");

        return user;
    }
}
