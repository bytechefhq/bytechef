/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.apiplatform.configuration.config.ApiCollectionIntTestConfiguration;
import com.bytechef.ee.automation.apiplatform.configuration.config.ApiCollectionIntTestConfigurationSharedMocks;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiClient;
import com.bytechef.ee.automation.apiplatform.configuration.repository.ApiClientRepository;
import com.bytechef.ee.automation.apiplatform.configuration.security.ApiClientOwnershipResolver;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiClientService;
import com.bytechef.platform.security.web.expression.MethodSecurityExpressionContributor;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        ApiCollectionIntTestConfiguration.class, ApiClientFacadeIntTest.MethodSecurityConfiguration.class
    },
    properties = "bytechef.edition=ee")
@Import(PostgreSQLContainerConfiguration.class)
@ApiCollectionIntTestConfigurationSharedMocks
class ApiClientFacadeIntTest {

    private static final long CURRENT_USER_ID = 1L;
    private static final String CURRENT_USER_LOGIN = "owner";
    private static final long OTHER_USER_ID = 2L;
    private static final String OTHER_USER_LOGIN = "other";

    @Autowired
    private ApiClientFacade apiClientFacade;

    @Autowired
    private ApiClientOwnershipResolver apiClientOwnershipResolver;

    @Autowired
    private ApiClientRepository apiClientRepository;

    @Autowired
    private ApiClientService apiClientService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private UserService userService;

    private long otherApiClientId;
    private long ownApiClientId;

    @BeforeEach
    void beforeEach() {
        apiClientRepository.deleteAll();

        ownApiClientId = createApiClient("own", CURRENT_USER_LOGIN);
        otherApiClientId = createApiClient("other", OTHER_USER_LOGIN);

        when(userService.fetchUserByLogin(CURRENT_USER_LOGIN)).thenReturn(Optional.of(createUser(CURRENT_USER_ID)));
        when(userService.fetchUserByLogin(OTHER_USER_LOGIN)).thenReturn(Optional.of(createUser(OTHER_USER_ID)));
        when(permissionService.isResourceOwner(eq("ApiClient"), anyLong())).thenAnswer(invocation -> {
            ResourceOwner resourceOwner = apiClientOwnershipResolver.resolveOwner((long) invocation.getArgument(1));

            return resourceOwner.ownerUserId()
                .equals(OptionalLong.of(CURRENT_USER_ID));
        });

        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    CURRENT_USER_LOGIN, "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        apiClientRepository.deleteAll();
    }

    @Test
    void testGetApiClientsReturnsOnlyTheCallersApiClients() {
        List<ApiClient> apiClients = apiClientFacade.getApiClients();

        assertThat(apiClients)
            .extracting(ApiClient::getId)
            .containsExactly(ownApiClientId);
    }

    @Test
    void testGetApiClientOfTheCallerIsAllowed() {
        ApiClient apiClient = apiClientFacade.getApiClient(ownApiClientId);

        assertThat(apiClient.getName()).isEqualTo("own");
    }

    @Test
    void testGetApiClientOfAnotherUserIsDenied() {
        assertThatThrownBy(() -> apiClientFacade.getApiClient(otherApiClientId))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testUpdateApiClientOfAnotherUserIsDenied() {
        ApiClient apiClient = apiClientService.getApiClient(otherApiClientId);

        apiClient.setName("renamed");

        assertThatThrownBy(() -> apiClientFacade.update(apiClient))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(apiClientService.getApiClient(otherApiClientId)
            .getName()).isEqualTo("other");
    }

    @Test
    void testDeleteApiClientOfAnotherUserIsDenied() {
        assertThatThrownBy(() -> apiClientFacade.delete(otherApiClientId))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(apiClientRepository.findById(otherApiClientId)).isPresent();
    }

    @Test
    void testDeleteApiClientOfTheCallerIsAllowed() {
        apiClientFacade.delete(ownApiClientId);

        assertThat(apiClientRepository.findById(ownApiClientId)).isEmpty();
    }

    @Test
    void testResolveOwnerOfAMissingApiClientIsUnknown() {
        assertThat(apiClientOwnershipResolver.resolveOwner(Long.MAX_VALUE)).isEqualTo(ResourceOwner.unknown());
    }

    private long createApiClient(String name, String createdBy) {
        ApiClient apiClient = new ApiClient();

        apiClient.setEnvironment(0);
        apiClient.setName(name);

        String secretKey = apiClientService.create(apiClient);

        ApiClient savedApiClient = apiClientService.fetchApiClient(secretKey, 0)
            .orElseThrow();

        jdbcTemplate.update("UPDATE api_client SET created_by = ? WHERE id = ?", createdBy, savedApiClient.getId());

        return savedApiClient.getId();
    }

    private static User createUser(long id) {
        User user = new User();

        user.setId(id);

        return user;
    }

    @EnableMethodSecurity
    static class MethodSecurityConfiguration {

        @Bean
        static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            @Lazy PermissionService permissionService,
            ObjectProvider<MethodSecurityExpressionContributor> methodSecurityExpressionContributorProvider) {

            return new AutomationMethodSecurityExpressionHandler(
                permissionService, methodSecurityExpressionContributorProvider);
        }
    }
}
