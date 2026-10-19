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

package com.bytechef.platform.user.teardown;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.repository.UserRepository;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class RegistrationUserTenantTeardownContributorTest {

    private final TenantService tenantService = mock(TenantService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserService userService = mock(UserService.class);
    private final RegistrationUserTenantTeardownContributor contributor =
        new RegistrationUserTenantTeardownContributor(tenantService, userRepository, userService);

    @Test
    void testRegistrationRowDeletedWhenLoginInNoOtherTenant() {
        AtomicReference<String> tenantIdAtDelete = new AtomicReference<>();

        when(userRepository.findAll()).thenReturn(List.of(user("a@x")));
        when(tenantService.getTenantIdsByUserLogin("a@x")).thenReturn(List.of());
        doAnswer(invocation -> {
            tenantIdAtDelete.set(TenantContext.getCurrentTenantId());

            return null;
        })
            .when(userService)
            .delete("a@x");

        TenantContext.runWithTenantId("000001", () -> contributor.beforeSchemaDrop("000001"));

        verify(userService).delete("a@x");

        assertThat(tenantIdAtDelete.get()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testRegistrationRowKeptWhenLoginInAnotherTenant() {
        when(userRepository.findAll()).thenReturn(List.of(user("a@x")));
        when(tenantService.getTenantIdsByUserLogin("a@x")).thenReturn(List.of("000005"));

        contributor.beforeSchemaDrop("000001");

        verify(userService, never()).delete(anyString());
    }

    @Test
    void testRejectsInvalidTenantId() {
        assertThatThrownBy(() -> contributor.beforeSchemaDrop("x y"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static User user(String login) {
        User user = new User();

        user.setLogin(login);

        return user;
    }
}
