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

package com.bytechef.platform.oauth2.authorizationserver.web.graphql;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.oauth2.authorizationserver.domain.OAuth2AuthorizationGrant;
import com.bytechef.platform.oauth2.authorizationserver.domain.OAuth2ConsentClient;
import com.bytechef.platform.oauth2.authorizationserver.domain.RegisteredClientInfo;
import com.bytechef.platform.oauth2.authorizationserver.facade.RegisteredClientFacade;
import com.bytechef.platform.oauth2.authorizationserver.web.graphql.config.RegisteredClientGraphQlTestConfiguration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    RegisteredClientGraphQlTestConfiguration.class, RegisteredClientGraphQlController.class
})
@GraphQlTest(
    controllers = RegisteredClientGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true", "bytechef.oauth2.authorization-server.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
class RegisteredClientGraphQlControllerIntTest {

    private static final Instant ISSUED_AT = Instant.parse("2026-10-01T00:00:00Z");

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private RegisteredClientFacade registeredClientFacade;

    @Test
    void testRegisteredClients() {
        when(registeredClientFacade.getRegisteredClients()).thenReturn(List.of(
            new RegisteredClientInfo(
                "client-1", "client-id", "Claude", ISSUED_AT, List.of("mcp:automation"),
                List.of("authorization_code"), List.of("http://127.0.0.1/callback"), 3, false)));

        graphQlTester
            .document("""
                query {
                    registeredClients {
                        id
                        clientId
                        clientName
                        clientIdIssuedAt
                        scopes
                        authorizationGrantTypes
                        redirectUris
                        grantCount
                        deletable
                    }
                }
                """)
            .execute()
            .path("registeredClients[0].id")
            .entity(String.class)
            .isEqualTo("client-1")
            .path("registeredClients[0].clientName")
            .entity(String.class)
            .isEqualTo("Claude")
            .path("registeredClients[0].clientIdIssuedAt")
            .entity(Long.class)
            .isEqualTo(ISSUED_AT.toEpochMilli())
            .path("registeredClients[0].scopes")
            .entityList(String.class)
            .containsExactly("mcp:automation")
            .path("registeredClients[0].grantCount")
            .entity(Integer.class)
            .isEqualTo(3)
            .path("registeredClients[0].deletable")
            .entity(Boolean.class)
            .isEqualTo(false);
    }

    @Test
    void testOAuth2ConsentClient() {
        when(registeredClientFacade.fetchConsentClient("client-id")).thenReturn(
            Optional.of(new OAuth2ConsentClient("client-id", "Claude", List.of("localhost", "claude.ai"))));

        graphQlTester
            .document("""
                query {
                    oauth2ConsentClient(clientId: "client-id") {
                        clientId
                        clientName
                        redirectHosts
                    }
                }
                """)
            .execute()
            .path("oauth2ConsentClient.clientName")
            .entity(String.class)
            .isEqualTo("Claude")
            .path("oauth2ConsentClient.redirectHosts")
            .entityList(String.class)
            .containsExactly("localhost", "claude.ai");
    }

    @Test
    void testOAuth2ConsentClientOfAnUnknownClientIsNull() {
        when(registeredClientFacade.fetchConsentClient("unknown")).thenReturn(Optional.empty());

        graphQlTester
            .document("""
                query {
                    oauth2ConsentClient(clientId: "unknown") {
                        clientId
                    }
                }
                """)
            .execute()
            .path("oauth2ConsentClient")
            .valueIsNull();
    }

    @Test
    void testRegisteredClientGrants() {
        when(registeredClientFacade.getRegisteredClientGrants("client-1")).thenReturn(List.of(
            new OAuth2AuthorizationGrant(
                "alice", List.of("mcp:automation"), List.of("Management", "Sales Automation"), ISSUED_AT,
                ISSUED_AT.plusSeconds(60), null)));

        graphQlTester
            .document("""
                query {
                    registeredClientGrants(registeredClientId: "client-1") {
                        principalName
                        scopes
                        mcpServers
                        authorizedAt
                        lastIssuedAt
                        expiresAt
                    }
                }
                """)
            .execute()
            .path("registeredClientGrants[0].principalName")
            .entity(String.class)
            .isEqualTo("alice")
            .path("registeredClientGrants[0].mcpServers")
            .entityList(String.class)
            .containsExactly("Management", "Sales Automation")
            .path("registeredClientGrants[0].lastIssuedAt")
            .entity(Long.class)
            .isEqualTo(ISSUED_AT.plusSeconds(60)
                .toEpochMilli())
            .path("registeredClientGrants[0].expiresAt")
            .valueIsNull();
    }

    @Test
    void testRevokeRegisteredClientGrant() {
        graphQlTester
            .document("""
                mutation {
                    revokeRegisteredClientGrant(registeredClientId: "client-1", principalName: "alice")
                }
                """)
            .execute()
            .path("revokeRegisteredClientGrant")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(registeredClientFacade).revokeRegisteredClientGrant("client-1", "alice");
    }

    @Test
    void testRevokeRegisteredClientGrants() {
        graphQlTester
            .document("""
                mutation {
                    revokeRegisteredClientGrants(registeredClientId: "client-1")
                }
                """)
            .execute()
            .path("revokeRegisteredClientGrants")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(registeredClientFacade).revokeRegisteredClientGrants("client-1");
    }

    @Test
    void testDeleteRegisteredClient() {
        graphQlTester
            .document("""
                mutation {
                    deleteRegisteredClient(id: "client-1")
                }
                """)
            .execute()
            .path("deleteRegisteredClient")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(registeredClientFacade).deleteRegisteredClient("client-1");
    }
}
