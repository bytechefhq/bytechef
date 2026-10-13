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

package com.bytechef.platform.connection.facade;

import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.Help;
import com.bytechef.component.definition.Property;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.config.ConnectionIntTestConfiguration;
import com.bytechef.platform.connection.config.ConnectionIntTestConfigurationSharedMocks;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.oauth2.domain.DiscoveredAuthorizationServer;
import com.bytechef.platform.oauth2.domain.OAuth2AuthorizationContext;
import com.bytechef.platform.oauth2.domain.RegisteredClient;
import com.bytechef.platform.oauth2.exception.OAuth2ErrorType;
import com.bytechef.platform.oauth2.service.OAuth2AuthorizationContextService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalSummary;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ConnectionIntTestConfiguration.class,
    properties = {
        "spring.application.name=server-app"
    })
@WithMockUser(username = "admin@localhost.com", authorities = AuthorityConstants.ADMIN)
@Import(PostgreSQLContainerConfiguration.class)
@ConnectionIntTestConfigurationSharedMocks
class ConnectionFacadeIntTest {

    private static final String REDIRECT_URI = "https://bytechef.example.com/callback";

    private static final OAuth2AuthorizationContext DISCOVERY_CONTEXT = OAuth2AuthorizationContext.ofDiscovery(
        "componentName", 1, "sealed-verifier", new RegisteredClient("reg-1", null, "none"),
        new DiscoveredAuthorizationServer(
            "https://auth.example.com", "https://auth.example.com/authorize", "https://auth.example.com/token",
            "https://auth.example.com/register", "https://mcp.example.com/mcp", List.of(), List.of("S256"),
            List.of("none")));

    @Autowired
    private ConnectionDefinitionService connectionDefinitionService;

    @Autowired
    private ConnectionFacade connectionFacade;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private OAuth2AuthorizationContextService oAuth2AuthorizationContextService;

    @Autowired
    private OAuth2Service oAuth2Service;

    @Autowired
    private TagRepository tagRepository;

    @AfterEach
    void afterEach() {
        connectionRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @BeforeEach
    void beforeEach() {
        when(connectionDefinitionService.getConnectionConnectionDefinition("componentName", 1))
            .thenReturn(
                new ConnectionDefinition(new MockConnectionDefinition(), "componentName", "componentTitle", null));
    }

    @Test
    void testCreate() {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .authorizationType(AuthorizationType.BASIC_AUTH)
            .componentName("componentName")
            .connectionVersion(1)
            .environmentId(Environment.STAGING.ordinal())
            .name("name1")
            .tags(List.of(new Tag("tag1")))
            .build();

        long connectionId = connectionFacade.create(connectionDTO, PlatformType.AUTOMATION);

        Assertions.assertThat(connectionRepository.findById(connectionId))
            .hasValueSatisfying(connection -> Assertions.assertThat(connection.getName())
                .isEqualTo("name1"));
    }

    @Test
    void testDelete() {
        ConnectionDTO connectionDTO1 = ConnectionDTO.builder()
            .authorizationType(AuthorizationType.BASIC_AUTH)
            .componentName("componentName")
            .connectionVersion(1)
            .environmentId(Environment.STAGING.ordinal())
            .name("name1")
            .tags(List.of(new Tag("tag1")))
            .build();

        long connectionId1 = connectionFacade.create(connectionDTO1, PlatformType.AUTOMATION);

        ConnectionDTO connectionDTO2 = ConnectionDTO.builder()
            .authorizationType(AuthorizationType.BASIC_AUTH)
            .componentName("componentName")
            .connectionVersion(1)
            .environmentId(Environment.STAGING.ordinal())
            .name("name2")
            .tags(List.of(new Tag("tag1")))
            .build();

        long connectionId2 = connectionFacade.create(connectionDTO2, PlatformType.AUTOMATION);

        Assertions.assertThat(connectionRepository.count())
            .isEqualTo(2);
        Assertions.assertThat(tagRepository.count())
            .isEqualTo(1);

        connectionFacade.delete(connectionId1);

        Assertions.assertThat(connectionRepository.count())
            .isEqualTo(1);

        connectionFacade.delete(connectionId2);

        Assertions.assertThat(connectionRepository.count())
            .isEqualTo(0);
        Assertions.assertThat(tagRepository.count())
            .isEqualTo(1);
    }

    @Test
    void testGetConnection() {
        Connection connection = new Connection();

        connection.setComponentName("componentName");
        connection.setName("name");
        connection.setType(PlatformType.AUTOMATION);

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        connection.setTags(List.of(tag1, tag2));

        connection = connectionRepository.save(connection);

        when(connectionDefinitionService.executeBaseUri(eq("componentName"), any()))
            .thenReturn(Optional.of("baseUri"));

        Assertions.assertThat(connectionFacade.getConnection(connection.getId()))
            .hasFieldOrPropertyWithValue("baseUri", "baseUri")
            .hasFieldOrPropertyWithValue("componentName", "componentName")
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));
    }

    @Test
    void testGetConnectionWhenExecuteBaseUriThrowsException() {
        Connection connection = new Connection();

        connection.setComponentName("componentName");
        connection.setName("name");
        connection.setType(PlatformType.AUTOMATION);

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        connection.setTags(List.of(tag1, tag2));

        connection = connectionRepository.save(connection);

        when(connectionDefinitionService.executeBaseUri(eq("componentName"), any()))
            .thenThrow(new IllegalStateException("Connection failed"));

        ConnectionDTO result = connectionFacade.getConnection(connection.getId());

        Assertions.assertThat(result)
            .isNotNull()
            .hasFieldOrPropertyWithValue("baseUri", null)
            .hasFieldOrPropertyWithValue("componentName", "componentName")
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));
    }

    @Test
    void testGetConnectionWhenExecuteBaseUriThrowsNullPointerException() {
        Connection connection = new Connection();

        connection.setComponentName("componentName");
        connection.setName("name");
        connection.setType(PlatformType.AUTOMATION);

        connection = connectionRepository.save(connection);

        // Reproduces the OAuth2-without-token regression: the component's baseUri lambda calls
        // getRequiredString(ACCESS_TOKEN) on a connection whose token hasn't been issued yet,
        // and Validate.notNull throws NullPointerException — which must not crash the list view.
        when(connectionDefinitionService.executeBaseUri(eq("componentName"), any()))
            .thenThrow(new NullPointerException("Unknown value for : access_token"));

        ConnectionDTO result = connectionFacade.getConnection(connection.getId());

        Assertions.assertThat(result)
            .isNotNull()
            .hasFieldOrPropertyWithValue("baseUri", null)
            .hasFieldOrPropertyWithValue("componentName", "componentName")
            .hasFieldOrPropertyWithValue("name", "name");
    }

    @Test
    void testGetConnections() {
        Connection connection = new Connection();

        connection.setComponentName("componentName");
        connection.setName("name");
        connection.setType(PlatformType.AUTOMATION);

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        connection.setTags(List.of(tag1, tag2));

        connection = connectionRepository.save(connection);

        List<ConnectionDTO> connectionDTOs = connectionFacade.getConnections(
            null, null, List.of(), null, null, PlatformType.AUTOMATION);

        Assertions.assertThat(CollectionUtils.map(connectionDTOs, ConnectionDTO::toConnection))
            .isEqualTo(List.of(connection));

        ConnectionDTO connectionDTO = connectionDTOs.getFirst();

        Assertions.assertThat(connectionFacade.getConnection(connection.getId()))
            .isEqualTo(connectionDTO)
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));

        when(connectionDefinitionService.getConnectionConnectionDefinition("componentName2", 1))
            .thenThrow(new IllegalArgumentException("componentName2 not found"));

        Connection connection2 = new Connection();

        connection2.setComponentName("componentName2");
        connection2.setName("name");
        connection2.setType(PlatformType.AUTOMATION);

        connection2 = connectionRepository.save(connection2);

        connectionDTOs = connectionFacade.getConnections(null, null, List.of(), null, null, PlatformType.AUTOMATION);

        Assertions.assertThat(connectionDTOs)
            .hasSize(2);

        ConnectionDTO healthyDTO = connectionDTOs.stream()
            .filter(dto -> "componentName".equals(dto.componentName()))
            .findFirst()
            .orElseThrow();

        Assertions.assertThat(healthyDTO)
            .hasFieldOrPropertyWithValue("componentName", "componentName")
            .hasFieldOrPropertyWithValue("name", "name");

        ConnectionDTO degradedDTO = connectionDTOs.stream()
            .filter(dto -> "componentName2".equals(dto.componentName()))
            .findFirst()
            .orElseThrow();

        Assertions.assertThat(degradedDTO)
            .hasFieldOrPropertyWithValue("active", false)
            .hasFieldOrPropertyWithValue("componentName", "componentName2")
            .hasFieldOrPropertyWithValue("id", connection2.getId())
            .hasFieldOrPropertyWithValue("name", "name [unavailable: IllegalArgumentException]");
    }

    @Test
    void testGetConnectionTags() {
        Connection connection = new Connection();

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        connection.setComponentName("componentName");
        connection.setName("name");
        connection.setTags(List.of(tag1, tag2));
        connection.setType(PlatformType.AUTOMATION);

        connectionRepository.save(connection);

        Assertions.assertThat(connectionFacade.getConnectionTags(PlatformType.AUTOMATION)
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet()))
            .contains("tag1", "tag2");

        connection = new Connection();

        connection.setComponentName("componentName");
        connection.setName("name2");
        connection.setType(PlatformType.AUTOMATION);

        tag1 = OptionalUtils.get(tagRepository.findById(Validate.notNull(tag1.getId(), "id")));

        connection.setTags(List.of(tag1, tagRepository.save(new Tag("tag3"))));

        connectionRepository.save(connection);

        Assertions.assertThat(connectionFacade.getConnectionTags(PlatformType.AUTOMATION)
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet()))
            .contains("tag1", "tag2", "tag3");

        connectionRepository.deleteById(Validate.notNull(connection.getId(), "id"));

        Assertions.assertThat(connectionFacade.getConnectionTags(PlatformType.AUTOMATION)
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet()))
            .contains("tag1", "tag2");
    }

    @Test
    void testUpdate() {
        Tag tag1 = new Tag("tag1");

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .authorizationType(AuthorizationType.BASIC_AUTH)
            .componentName("componentName")
            .connectionVersion(1)
            .environmentId(Environment.STAGING.ordinal())
            .name("name")
            .tags(List.of(tag1, tagRepository.save(new Tag("tag2"))))
            .build();

        connectionDTO = connectionFacade.getConnection(connectionFacade.create(connectionDTO, PlatformType.AUTOMATION));

        Assertions.assertThat(connectionDTO.tags())
            .hasSize(2);

        connectionFacade.update(connectionDTO.id(), "name", List.of(tag1), connectionDTO.version());

        connectionDTO = connectionFacade.getConnection(connectionDTO.id());

        Assertions.assertThat(connectionDTO.tags())
            .hasSize(1);
    }

    @Test
    void testUpdateAndDeleteByNonOwnerNonAdminAreDenied() {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .authorizationType(AuthorizationType.BASIC_AUTH)
            .componentName("componentName")
            .connectionVersion(1)
            .environmentId(Environment.STAGING.ordinal())
            .name("name")
            .build();

        long connectionId = connectionFacade.create(connectionDTO, PlatformType.AUTOMATION);

        ConnectionDTO createdConnectionDTO = connectionFacade.getConnection(connectionId);

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new TestingAuthenticationToken("user@localhost.com", null, AuthorityConstants.USER));

        Assertions.assertThatThrownBy(
            () -> connectionFacade.update(connectionId, "renamed", List.of(), createdConnectionDTO.version()))
            .isInstanceOf(AccessDeniedException.class);
        Assertions.assertThatThrownBy(() -> connectionFacade.delete(connectionId))
            .isInstanceOf(AccessDeniedException.class);
        Assertions.assertThatThrownBy(
            () -> connectionFacade.replaceAuthorizationParameters(connectionId, Map.of("key", "value")))
            .isInstanceOf(AccessDeniedException.class);

        Assertions.assertThat(connectionRepository.findById(connectionId))
            .hasValueSatisfying(connection -> Assertions.assertThat(connection.getName())
                .isEqualTo("name"));
    }

    @Test
    void testReauthorizeAndDeleteByTheReassignedCreatorAreAllowed() {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .authorizationType(AuthorizationType.BASIC_AUTH)
            .componentName("componentName")
            .connectionVersion(1)
            .environmentId(Environment.STAGING.ordinal())
            .name("name")
            .build();

        long connectionId = connectionFacade.create(connectionDTO, PlatformType.AUTOMATION);

        connectionService.updateCreatedBy(connectionId, "external-user-id");

        Assertions.assertThat(connectionRepository.findById(connectionId))
            .hasValueSatisfying(connection -> Assertions.assertThat(connection.getCreatedBy())
                .isEqualTo("external-user-id"));

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext
            .setAuthentication(new TestingAuthenticationToken("external-user-id", null, AuthorityConstants.USER));

        connectionFacade.replaceAuthorizationParameters(connectionId, Map.of("key", "value"));

        Assertions.assertThat(connectionRepository.findById(connectionId))
            .hasValueSatisfying(connection -> Assertions.assertThat(connection.getCreatedBy())
                .isEqualTo("external-user-id"));

        connectionFacade.delete(connectionId);

        Assertions.assertThat(connectionRepository.findById(connectionId))
            .isEmpty();
    }

    @Test
    void testCreateExchangesCodeWithUnsealedDiscoveryContextAndStripsTransientParameters() {
        stubOAuth2Exchange(Map.of("access_token", "at", "refresh_token", "rt", "expires_in", 3600));

        when(oAuth2AuthorizationContextService.unseal("sealed", "componentName")).thenReturn(DISCOVERY_CONTEXT);

        long connectionId = connectionFacade.create(
            oAuth2ConnectionDTO(
                Map.of(
                    "url", "https://mcp.example.com/mcp", Authorization.CODE, "the-code", "state", "s",
                    Authorization.AUTHORIZATION_CONTEXT, "sealed")),
            PlatformType.AUTOMATION);

        Assertions.assertThat(captureCallbackParameters())
            .containsEntry(Authorization.CODE, "the-code")
            .containsEntry(Authorization.CODE_VERIFIER, "sealed-verifier")
            .containsEntry(Authorization.CLIENT_ID, "reg-1")
            .containsEntry(Authorization.TOKEN_URL, "https://auth.example.com/token")
            .containsEntry(Authorization.RESOURCE, "https://mcp.example.com/mcp");

        Map<String, Object> storedParameters = getStoredParameters(connectionId);

        Assertions.assertThat(storedParameters)
            .containsEntry("url", "https://mcp.example.com/mcp")
            .containsEntry(Authorization.CLIENT_ID, "reg-1")
            .containsEntry(Authorization.TOKEN_ENDPOINT_AUTH_METHOD, "none")
            .containsEntry(Authorization.AUTHORIZATION_URL, "https://auth.example.com/authorize")
            .containsEntry(Authorization.TOKEN_URL, "https://auth.example.com/token")
            .containsEntry(Authorization.RESOURCE, "https://mcp.example.com/mcp")
            .containsEntry("access_token", "at")
            .containsEntry("refresh_token", "rt")
            .doesNotContainKeys(
                Authorization.CODE, Authorization.CODE_VERIFIER, Authorization.AUTHORIZATION_CONTEXT, "state");
    }

    @Test
    void testCreateWithDiscoveryContextIgnoresConflictingBrowserPostedParameters() {
        stubOAuth2Exchange(Map.of("access_token", "at", "refresh_token", "rt"));

        when(oAuth2AuthorizationContextService.unseal("sealed", "componentName")).thenReturn(DISCOVERY_CONTEXT);

        long connectionId = connectionFacade.create(
            oAuth2ConnectionDTO(
                Map.of(
                    "url", "https://mcp.example.com/mcp", Authorization.CODE, "the-code",
                    Authorization.AUTHORIZATION_CONTEXT, "sealed", Authorization.CLIENT_ID, "browser-client",
                    Authorization.CLIENT_SECRET, "browser-secret", Authorization.TOKEN_URL,
                    "https://evil.example.com/token",
                    Authorization.REFRESH_URL, "https://evil.example.com/refresh", Authorization.CODE_VERIFIER,
                    "browser-verifier")),
            PlatformType.AUTOMATION);

        Assertions.assertThat(captureCallbackParameters())
            .containsEntry(Authorization.CLIENT_ID, "reg-1")
            .containsEntry(Authorization.TOKEN_URL, "https://auth.example.com/token")
            .containsEntry(Authorization.CODE_VERIFIER, "sealed-verifier")
            .doesNotContainKeys(Authorization.CLIENT_SECRET, Authorization.REFRESH_URL);
        Assertions.assertThat(getStoredParameters(connectionId))
            .containsEntry(Authorization.CLIENT_ID, "reg-1")
            .containsEntry(Authorization.TOKEN_URL, "https://auth.example.com/token")
            .containsEntry(Authorization.TOKEN_ENDPOINT_AUTH_METHOD, "none")
            .containsEntry("refresh_token", "rt")
            .doesNotContainKeys(Authorization.CLIENT_SECRET, Authorization.REFRESH_URL, Authorization.CODE_VERIFIER);
    }

    @Test
    void testCreateUsesSealedVerifierForPlainPkceContext() {
        stubOAuth2Exchange(Map.of("access_token", "at"));

        when(oAuth2AuthorizationContextService.unseal("sealed", "componentName"))
            .thenReturn(OAuth2AuthorizationContext.ofPkce("componentName", 1, "sealed-verifier"));

        long connectionId = connectionFacade.create(
            oAuth2ConnectionDTO(
                Map.of(
                    Authorization.CLIENT_ID, "manual", Authorization.CODE, "c", Authorization.AUTHORIZATION_CONTEXT,
                    "sealed")),
            PlatformType.AUTOMATION);

        Assertions.assertThat(captureCallbackParameters())
            .containsEntry(Authorization.CODE_VERIFIER, "sealed-verifier")
            .containsEntry(Authorization.CLIENT_ID, "manual")
            .doesNotContainKeys(Authorization.TOKEN_URL, Authorization.RESOURCE);
        Assertions.assertThat(getStoredParameters(connectionId))
            .containsEntry(Authorization.CLIENT_ID, "manual")
            .containsEntry("access_token", "at")
            .doesNotContainKeys(Authorization.CODE, Authorization.CODE_VERIFIER, Authorization.AUTHORIZATION_CONTEXT);
    }

    @Test
    void testCreateWithoutAuthorizationContextBehavesAsBefore() {
        stubOAuth2Exchange(Map.of("access_token", "at"));

        long connectionId = connectionFacade.create(
            oAuth2ConnectionDTO(Map.of(Authorization.CLIENT_ID, "manual", Authorization.CODE, "c", "state", "s")),
            PlatformType.AUTOMATION);

        verify(oAuth2AuthorizationContextService, never()).unseal(any(), any());

        Assertions.assertThat(captureCallbackParameters())
            .containsEntry(Authorization.CODE, "c")
            .doesNotContainKey(Authorization.CODE_VERIFIER);
        Assertions.assertThat(getStoredParameters(connectionId))
            .containsEntry(Authorization.CLIENT_ID, "manual")
            .containsEntry("access_token", "at")
            .doesNotContainKeys("state", Authorization.CODE);
    }

    @Test
    void testCreateRejectsDiscoveryContextForEmbeddedConnection() {
        stubOAuth2Exchange(Map.of("access_token", "at"));

        when(oAuth2AuthorizationContextService.unseal("sealed", "componentName")).thenReturn(DISCOVERY_CONTEXT);

        ThrowingCallable createCallable = () -> connectionFacade.create(
            oAuth2ConnectionDTO(Map.of(Authorization.CODE, "c", Authorization.AUTHORIZATION_CONTEXT, "sealed")),
            PlatformType.EMBEDDED);

        assertOAuth2Error(createCallable, OAuth2ErrorType.DISCOVERY_NOT_AVAILABLE);

        Assertions.assertThat(catchThrowable(createCallable))
            .hasMessageContaining("Use OAuth2 (PKCE) with the server's Authorization URL and Token URL");

        verify(connectionDefinitionService, never()).executeAuthorizationCallback(
            any(), anyInt(), any(), anyMap(), any());

        Assertions.assertThat(connectionRepository.count())
            .isZero();
    }

    @Test
    void testCreateAcceptsPlainPkceContextForEmbeddedConnection() {
        stubOAuth2Exchange(Map.of("access_token", "at"));

        when(oAuth2AuthorizationContextService.unseal("sealed", "componentName"))
            .thenReturn(OAuth2AuthorizationContext.ofPkce("componentName", 1, "sealed-verifier"));

        long connectionId = connectionFacade.create(
            oAuth2ConnectionDTO(
                Map.of(
                    Authorization.CLIENT_ID, "manual", Authorization.CODE, "c", Authorization.AUTHORIZATION_CONTEXT,
                    "sealed")),
            PlatformType.EMBEDDED);

        Assertions.assertThat(captureCallbackParameters())
            .containsEntry(Authorization.CODE_VERIFIER, "sealed-verifier")
            .containsEntry(Authorization.CLIENT_ID, "manual");
        Assertions.assertThat(getStoredParameters(connectionId))
            .containsEntry("access_token", "at");
    }

    @Test
    void testCreateRejectsExpiredAuthorizationContext() {
        stubOAuth2Exchange(Map.of("access_token", "at"));

        when(oAuth2AuthorizationContextService.unseal("sealed", "componentName"))
            .thenThrow(
                new ConfigurationException(
                    "Sign-in took too long; try again", OAuth2ErrorType.INVALID_AUTHORIZATION_CONTEXT));

        assertOAuth2Error(
            () -> connectionFacade.create(
                oAuth2ConnectionDTO(Map.of(Authorization.CODE, "c", Authorization.AUTHORIZATION_CONTEXT, "sealed")),
                PlatformType.AUTOMATION),
            OAuth2ErrorType.INVALID_AUTHORIZATION_CONTEXT);

        verify(connectionDefinitionService, never()).executeAuthorizationCallback(
            any(), anyInt(), any(), anyMap(), any());

        Assertions.assertThat(connectionRepository.count())
            .isZero();
    }

    @Test
    void testCreateStoresTokensWhenRefreshTokenIsMissing() {
        stubOAuth2Exchange(Map.of("access_token", "at", "expires_in", "3600"));

        when(oAuth2AuthorizationContextService.unseal("sealed", "componentName")).thenReturn(DISCOVERY_CONTEXT);

        long connectionId = connectionFacade.create(
            oAuth2ConnectionDTO(Map.of(Authorization.CODE, "c", Authorization.AUTHORIZATION_CONTEXT, "sealed")),
            PlatformType.AUTOMATION);

        Assertions.assertThat(getStoredParameters(connectionId))
            .containsEntry("access_token", "at")
            .containsEntry("expires_in", "3600")
            .containsEntry(Authorization.CLIENT_ID, "reg-1")
            .doesNotContainKey("refresh_token");
    }

    @Test
    void testReplaceAuthorizationParametersExchangesCodeWithSealedContextAndStripsTransientParameters() {
        stubOAuth2Exchange(Map.of("access_token", "new-at", "refresh_token", "new-rt"));

        long connectionId = connectionFacade.create(
            oAuth2ConnectionDTO(Map.of("url", "https://mcp.example.com/mcp")), PlatformType.AUTOMATION);

        when(oAuth2AuthorizationContextService.unseal("sealed", "componentName")).thenReturn(DISCOVERY_CONTEXT);

        connectionFacade.replaceAuthorizationParameters(
            connectionId,
            Map.of(Authorization.CODE, "the-code", "state", "s", Authorization.AUTHORIZATION_CONTEXT, "sealed"));

        Assertions.assertThat(captureCallbackParameters())
            .containsEntry(Authorization.CODE, "the-code")
            .containsEntry(Authorization.CODE_VERIFIER, "sealed-verifier")
            .containsEntry(Authorization.CLIENT_ID, "reg-1")
            .containsEntry(Authorization.TOKEN_URL, "https://auth.example.com/token");
        Assertions.assertThat(getStoredParameters(connectionId))
            .containsEntry("url", "https://mcp.example.com/mcp")
            .containsEntry(Authorization.CLIENT_ID, "reg-1")
            .containsEntry("access_token", "new-at")
            .containsEntry("refresh_token", "new-rt")
            .doesNotContainKeys(
                Authorization.CODE, Authorization.CODE_VERIFIER, Authorization.AUTHORIZATION_CONTEXT, "state");
    }

    @Test
    void testGetReauthorizationParametersReturnsOnlyStoredConnectionProperties() {
        stubReauthorizationConnectionDefinition();

        long connectionId = createReauthorizationConnection();

        Assertions.assertThat(connectionFacade.getReauthorizationParameters(connectionId, "componentName", 1))
            .isEqualTo(Map.of("url", "https://mcp.example.com/mcp", "transportType", "SSE"));
    }

    @Test
    void testGetReauthorizationParametersRejectsMismatchedComponent() {
        stubReauthorizationConnectionDefinition();

        long connectionId = createReauthorizationConnection();

        Throwable throwable = catchThrowable(
            () -> connectionFacade.getReauthorizationParameters(connectionId, "otherComponent", 1));

        Assertions.assertThat(throwable)
            .isInstanceOf(ConfigurationException.class);
        Assertions.assertThat(((ConfigurationException) throwable).getErrorKey())
            .isEqualTo(ConnectionErrorType.INVALID_CONNECTION_COMPONENT_NAME.getErrorKey());
        Assertions
            .assertThatThrownBy(() -> connectionFacade.getReauthorizationParameters(connectionId, "componentName", 2))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testGetReauthorizationParametersByNonOwnerNonAdminIsDenied() {
        stubReauthorizationConnectionDefinition();

        long connectionId = createReauthorizationConnection();

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new TestingAuthenticationToken("user@localhost.com", null, AuthorityConstants.USER));

        Assertions.assertThatThrownBy(
            () -> connectionFacade.getReauthorizationParameters(connectionId, "componentName", 1))
            .isInstanceOf(AccessDeniedException.class);
    }

    private long createReauthorizationConnection() {
        return connectionFacade.create(
            oAuth2ConnectionDTO(
                Map.of(
                    "url", "https://mcp.example.com/mcp", "transportType", "SSE", Authorization.CLIENT_ID, "reg-1",
                    Authorization.CLIENT_SECRET, "secret", "access_token", "at", "refresh_token", "rt")),
            PlatformType.AUTOMATION);
    }

    private void stubReauthorizationConnectionDefinition() {
        when(connectionDefinitionService.getConnectionConnectionDefinition("componentName", 1))
            .thenReturn(
                new ConnectionDefinition(
                    connection()
                        .properties(string("url"), string("transportType"), string(Authorization.CLIENT_ID))
                        .authorizations(
                            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                                .properties(string(Authorization.CLIENT_ID), string(Authorization.CLIENT_SECRET))),
                    "componentName", "componentTitle", null));
    }

    private static void assertOAuth2Error(ThrowingCallable callable, OAuth2ErrorType expected) {
        Throwable throwable = catchThrowable(callable);

        Assertions.assertThat(throwable)
            .isInstanceOf(ConfigurationException.class);

        ConfigurationException configurationException = (ConfigurationException) throwable;

        Assertions.assertThat(configurationException.getEntityClass())
            .isEqualTo(OAuth2ErrorType.class);
        Assertions.assertThat(configurationException.getErrorKey())
            .isEqualTo(expected.getErrorKey());
    }

    private Map<String, Object> captureCallbackParameters() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> callbackParametersCaptor = ArgumentCaptor.forClass(Map.class);

        verify(connectionDefinitionService).executeAuthorizationCallback(
            eq("componentName"), eq(1), eq(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE),
            callbackParametersCaptor.capture(), eq(REDIRECT_URI));

        return new HashMap<>(callbackParametersCaptor.getValue());
    }

    private Map<String, Object> getStoredParameters(long connectionId) {
        Connection connection = connectionRepository.findById(connectionId)
            .orElseThrow();

        return new HashMap<>(connection.getParameters());
    }

    private static ConnectionDTO oAuth2ConnectionDTO(Map<String, ?> parameters) {
        return ConnectionDTO.builder()
            .authorizationType(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE)
            .componentName("componentName")
            .connectionVersion(1)
            .environmentId(Environment.STAGING.ordinal())
            .name("oauth")
            .parameters(parameters)
            .build();
    }

    private void stubOAuth2Exchange(Map<String, Object> tokenResponse) {
        when(
            connectionDefinitionService.getAuthorizationType(
                "componentName", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE))
                    .thenReturn(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE);
        when(oAuth2Service.checkPredefinedParameters(eq("componentName"), anyMap()))
            .thenAnswer(invocation -> invocation.getArgument(1));
        when(oAuth2Service.getRedirectUri()).thenReturn(REDIRECT_URI);
        when(
            connectionDefinitionService.executeAuthorizationCallback(
                eq("componentName"), eq(1), eq(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE), anyMap(),
                eq(REDIRECT_URI)))
                    .thenReturn(new AuthorizationCallbackResponse(tokenResponse));
    }

    @ComponentScan(basePackages = {
        "com.bytechef.platform.configuration.accessor", "com.bytechef.platform.tag"
    })
    @TestConfiguration
    public static class ConnectionFacadeIntTestConfiguration {

        @Bean
        JobPrincipalAccessor jobPrincipalAccessor() {
            return new JobPrincipalAccessor() {

                @Override
                public boolean isConnectionUsed(long connectionId) {
                    return false;
                }

                @Override
                public boolean isWorkflowEnabled(long jobPrincipalId, String workflowUuid) {
                    return false;
                }

                @Override
                public long getEnvironmentId(long jobPrincipalId) {
                    return 0;
                }

                @Override
                public Map<String, ?> getInputMap(long jobPrincipalId, String workflowUuid) {
                    return Map.of();
                }

                @Override
                public Map<String, ?> getMetadataMap(long jobPrincipalId) {
                    return Map.of();
                }

                @Override
                public JobPrincipalSummary getJobPrincipalSummary(long jobPrincipalId, String workflowId) {
                    return new JobPrincipalSummary(0, null, null, null);
                }

                @Override
                public PlatformType getType() {
                    return PlatformType.AUTOMATION;
                }

                @Override
                public String getWorkflowId(long jobPrincipalId, String workflowUuid) {
                    return "";
                }

                @Override
                public String getLastWorkflowId(String workflowUuid) {
                    return "";
                }

                @Override
                public String getWorkflowUuid(String workflowId) {
                    return "";
                }
            };
        }
    }

    private static class MockConnectionDefinition implements com.bytechef.component.definition.ConnectionDefinition {

        @Override
        public boolean getAuthorizationRequired() {
            return false;
        }

        @Override
        public List<? extends Authorization> getAuthorizations() {
            return List.of();
        }

        @Override
        public Optional<BaseUriFunction> getBaseUri() {
            return Optional.empty();
        }

        @Override
        public Optional<Help> getHelp() {
            return Optional.empty();
        }

        @Override
        public Optional<ProcessErrorResponseFunction> getProcessErrorResponse() {
            return Optional.empty();
        }

        @Override
        public List<? extends Property> getProperties() {
            return List.of();
        }

        @Override
        public Optional<TestConsumer> getTest() {
            return Optional.empty();
        }
    }
}
