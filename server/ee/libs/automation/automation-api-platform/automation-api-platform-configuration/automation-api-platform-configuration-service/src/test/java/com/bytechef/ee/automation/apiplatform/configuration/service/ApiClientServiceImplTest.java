/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiClient;
import com.bytechef.ee.automation.apiplatform.configuration.repository.ApiClientRepository;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Pins the owner-isolation filtering of {@code getApiClients()} (T19 follow-up): non-admin callers see only their own
 * clients; tenant admins see all.
 *
 * @author Ivica Cardic
 * @version ee
 */
class ApiClientServiceImplTest {

    private final ApiClientRepository apiClientRepository = mock(ApiClientRepository.class);
    private final ApiClientServiceImpl apiClientService = new ApiClientServiceImpl(apiClientRepository);

    @Test
    void testGetApiClientsFiltersToOwnerForNonAdmin() {
        ApiClient alice = apiClient("alice");
        ApiClient bob = apiClient("bob");

        when(apiClientRepository.findAll()).thenReturn(List.of(alice, bob));

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
                .thenReturn(false);
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("alice"));

            assertThat(apiClientService.getApiClients())
                .extracting(ApiClient::getCreatedBy)
                .containsExactly("alice");
        }
    }

    @Test
    void testGetApiClientsReturnsAllForAdmin() {
        ApiClient alice = apiClient("alice");
        ApiClient bob = apiClient("bob");

        when(apiClientRepository.findAll()).thenReturn(List.of(alice, bob));

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
                .thenReturn(true);

            assertThat(apiClientService.getApiClients()).hasSize(2);
        }
    }

    @Test
    void testGetApiClientsEmptyWhenNoCurrentUser() {
        ApiClient alice = apiClient("alice");

        when(apiClientRepository.findAll()).thenReturn(List.of(alice));

        try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
                .thenReturn(false);
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.empty());

            assertThat(apiClientService.getApiClients()).isEmpty();
        }
    }

    private static ApiClient apiClient(String createdBy) {
        ApiClient apiClient = mock(ApiClient.class);

        when(apiClient.getCreatedBy()).thenReturn(createdBy);

        return apiClient;
    }
}
