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

package com.bytechef.platform.oauth2.authorizationserver.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.oauth2.authorizationserver.config.RegisteredClientMethodSecurityTestConfiguration;
import com.bytechef.platform.oauth2.authorizationserver.config.RegisteredClientMethodSecurityTestConfiguration.TenantAdminCheck;
import com.bytechef.platform.oauth2.authorizationserver.domain.OAuth2AuthorizationGrant;
import com.bytechef.platform.oauth2.authorizationserver.domain.OAuth2ConsentClient;
import com.bytechef.platform.oauth2.authorizationserver.domain.RegisteredClientInfo;
import com.bytechef.platform.oauth2.authorizationserver.exception.RegisteredClientErrorType;
import com.bytechef.platform.oauth2.authorizationserver.service.OAuth2AuthorizationGrantService;
import com.bytechef.platform.oauth2.authorizationserver.service.RegisteredClientService;
import com.bytechef.tenant.service.TenantService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        RegisteredClientFacadeImpl.class, RegisteredClientMethodSecurityTestConfiguration.class
    },
    properties = "bytechef.oauth2.authorization-server.enabled=true")
class RegisteredClientFacadeIntTest {

    @MockitoBean
    private OAuth2AuthorizationGrantService oAuth2AuthorizationGrantService;

    @Autowired
    private RegisteredClientFacade registeredClientFacade;

    @MockitoBean
    private RegisteredClientService registeredClientService;

    @Autowired
    private TenantAdminCheck tenantAdminCheck;

    @MockitoBean
    private TenantService tenantService;

    @AfterEach
    void afterEach() {
        reset(tenantAdminCheck);
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void testNonAdminIsDeniedEveryOperation() {
        List<Executable> operations = List.of(
            () -> registeredClientFacade.getRegisteredClients(),
            () -> registeredClientFacade.getRegisteredClientGrants("client-1"),
            () -> registeredClientFacade.deleteRegisteredClient("client-1"),
            () -> registeredClientFacade.revokeRegisteredClientGrant("client-1", "alice"),
            () -> registeredClientFacade.revokeRegisteredClientGrants("client-1"));

        for (Executable operation : operations) {
            assertThatThrownBy(operation::execute).isInstanceOf(AccessDeniedException.class);
        }

        verifyNoInteractions(registeredClientService, oAuth2AuthorizationGrantService);
    }

    @Test
    void testConsentClientRequiresAuthentication() {
        assertThatThrownBy(() -> registeredClientFacade.fetchConsentClient("client-id"))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);

        verifyNoInteractions(registeredClientService);
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void testConsentClientIsAvailableToAnAuthenticatedNonAdmin() {
        OAuth2ConsentClient consentClient = new OAuth2ConsentClient("client-id", "Client", List.of("localhost"));

        when(tenantAdminCheck.isTenantAdmin()).thenReturn(false);
        when(registeredClientService.fetchConsentClient("client-id")).thenReturn(Optional.of(consentClient));

        assertThat(registeredClientFacade.fetchConsentClient("client-id")).contains(consentClient);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testAdminOfAnotherTenantIsDeniedEveryOperation() {
        when(tenantAdminCheck.isTenantAdmin()).thenReturn(false);

        List<Executable> operations = List.of(
            () -> registeredClientFacade.getRegisteredClients(),
            () -> registeredClientFacade.getRegisteredClientGrants("client-1"),
            () -> registeredClientFacade.deleteRegisteredClient("client-1"),
            () -> registeredClientFacade.revokeRegisteredClientGrant("client-1", "alice"),
            () -> registeredClientFacade.revokeRegisteredClientGrants("client-1"));

        for (Executable operation : operations) {
            assertThatThrownBy(operation::execute).isInstanceOf(AccessDeniedException.class);
        }

        verifyNoInteractions(registeredClientService, oAuth2AuthorizationGrantService);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testTenantAdminReachesTheServices() {
        when(tenantAdminCheck.isTenantAdmin()).thenReturn(true);

        RegisteredClientInfo registeredClientInfo = new RegisteredClientInfo(
            "client-1", "client-id", "Client", Instant.EPOCH, List.of(), List.of(), List.of(), 1, true);
        OAuth2AuthorizationGrant grant = new OAuth2AuthorizationGrant(
            "alice", List.of(), List.of("Management"), Instant.EPOCH, Instant.EPOCH, null);

        when(registeredClientService.getRegisteredClients(true)).thenReturn(List.of(registeredClientInfo));
        when(oAuth2AuthorizationGrantService.getGrants("client-1")).thenReturn(List.of(grant));

        assertThat(registeredClientFacade.getRegisteredClients()).containsExactly(registeredClientInfo);
        assertThat(registeredClientFacade.getRegisteredClientGrants("client-1")).containsExactly(grant);

        registeredClientFacade.deleteRegisteredClient("client-1");
        registeredClientFacade.revokeRegisteredClientGrant("client-1", "alice");
        registeredClientFacade.revokeRegisteredClientGrants("client-1");

        verify(registeredClientService).deleteRegisteredClient("client-1");
        verify(oAuth2AuthorizationGrantService).revokeGrant("client-1", "alice");
        verify(oAuth2AuthorizationGrantService).revokeGrants("client-1");
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testSingleTenantListsEveryClientAsDeletable() {
        when(tenantAdminCheck.isTenantAdmin()).thenReturn(true);
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);

        RegisteredClientInfo grantedClient = createRegisteredClientInfo("client-1", 1, true);
        RegisteredClientInfo ungrantedClient = createRegisteredClientInfo("client-2", 0, true);

        when(registeredClientService.getRegisteredClients(true)).thenReturn(List.of(grantedClient, ungrantedClient));

        assertThat(registeredClientFacade.getRegisteredClients()).containsExactly(grantedClient, ungrantedClient);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testMultiTenantListsOnlyClientsWithActiveGrantsAsNotDeletable() {
        when(tenantAdminCheck.isTenantAdmin()).thenReturn(true);
        when(tenantService.isMultiTenantEnabled()).thenReturn(true);

        RegisteredClientInfo grantedClient = createRegisteredClientInfo("client-1", 1, false);
        RegisteredClientInfo ungrantedClient = createRegisteredClientInfo("client-2", 0, false);

        when(registeredClientService.getRegisteredClients(false)).thenReturn(List.of(grantedClient, ungrantedClient));

        assertThat(registeredClientFacade.getRegisteredClients()).containsExactly(grantedClient);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testMultiTenantForbidsDeletingASharedClient() {
        when(tenantAdminCheck.isTenantAdmin()).thenReturn(true);
        when(tenantService.isMultiTenantEnabled()).thenReturn(true);

        assertThatThrownBy(() -> registeredClientFacade.deleteRegisteredClient("client-1"))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(RegisteredClientErrorType.DELETE_SHARED_REGISTERED_CLIENT.getErrorKey()));

        verify(registeredClientService, never()).deleteRegisteredClient("client-1");
    }

    private static RegisteredClientInfo createRegisteredClientInfo(String id, int grantCount, boolean deletable) {
        return new RegisteredClientInfo(
            id, id + "-client-id", id + " name", Instant.EPOCH, List.of(), List.of(), List.of(), grantCount,
            deletable);
    }
}
