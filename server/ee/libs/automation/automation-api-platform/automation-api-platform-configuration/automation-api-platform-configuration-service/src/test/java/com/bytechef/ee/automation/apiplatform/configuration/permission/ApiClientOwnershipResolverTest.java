/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiClient;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiClientService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 * @version ee
 */
class ApiClientOwnershipResolverTest {

    private final ApiClientService apiClientService = Mockito.mock(ApiClientService.class);
    private final UserService userService = Mockito.mock(UserService.class);
    private final ApiClientOwnershipResolver resolver = new ApiClientOwnershipResolver(apiClientService, userService);

    @Test
    void testResourceType() {
        assertThat(resolver.resourceType()).isEqualTo("ApiClient");
    }

    @Test
    void testResolvesOwnerViaCreatedBy() {
        ApiClient apiClient = mock(ApiClient.class);

        when(apiClient.getCreatedBy()).thenReturn("alice");
        when(apiClientService.fetchApiClient(1L)).thenReturn(Optional.of(apiClient));

        User user = new User();

        user.setId(7L);

        when(userService.fetchUserByLogin("alice")).thenReturn(Optional.of(user));

        assertThat(resolver.resolveOwner(1L)
            .ownerUserId()).hasValue(7L);
    }

    @Test
    void testUnknownIsUnknown() {
        when(apiClientService.fetchApiClient(99L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(99L)
            .ownerUserId()).isEmpty();
    }
}
