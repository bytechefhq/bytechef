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

package com.bytechef.platform.component.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.Authorization.RefreshTokenResponse;
import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.connection.service.ConnectionServiceImpl;
import com.bytechef.platform.constant.PlatformType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class TokenRefreshHandlerTest {

    private static final long CONNECTION_ID = 7L;

    private final ConnectionDefinitionService connectionDefinitionService = mock(ConnectionDefinitionService.class);
    private final ConnectionRepository connectionRepository = mock(ConnectionRepository.class);
    private ListAppender<ILoggingEvent> logAppender;
    private TokenRefreshHandler tokenRefreshHandler;

    @BeforeEach
    void beforeEach() {
        SecurityContextHolder.clearContext();

        tokenRefreshHandler = new TokenRefreshHandler(
            new ConcurrentMapCacheManager(), connectionDefinitionService,
            new ConnectionServiceImpl(connectionRepository));

        Logger logger = (Logger) LoggerFactory.getLogger(TokenRefreshHandler.class);

        logAppender = new ListAppender<>();

        logAppender.start();

        logger.addAppender(logAppender);

        when(connectionRepository.findById(CONNECTION_ID)).thenReturn(Optional.of(createStoredConnection()));
    }

    @AfterEach
    void afterEach() {
        Logger logger = (Logger) LoggerFactory.getLogger(TokenRefreshHandler.class);

        logger.detachAppender(logAppender);
    }

    @Test
    void testRefreshCredentialsWithoutAuthenticationPersistsRefreshedParameters() {
        when(connectionDefinitionService.executeRefresh(anyString(), anyInt(), any(), any(), any()))
            .thenReturn(new RefreshTokenResponse("new_access_token", "new_refresh_token", 3600));
        when(connectionRepository.save(any(Connection.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ComponentConnection refreshedComponentConnection = tokenRefreshHandler.refreshCredentials(
            createComponentConnection(), mock(Context.class));

        ArgumentCaptor<Connection> connectionArgumentCaptor = ArgumentCaptor.forClass(Connection.class);

        verify(connectionRepository).save(connectionArgumentCaptor.capture());

        Connection savedConnection = connectionArgumentCaptor.getValue();

        Map<String, Object> savedParameters = Map.copyOf(savedConnection.getParameters());
        Map<String, Object> refreshedParameters = Map.copyOf(refreshedComponentConnection.getParameters());

        assertThat(savedParameters)
            .containsEntry("access_token", "new_access_token")
            .containsEntry("refresh_token", "new_refresh_token");
        assertThat(refreshedParameters).containsEntry("access_token", "new_access_token");
    }

    @Test
    void testRefreshCredentialsFailureMarksConnectionInvalidAndLogs() {
        when(connectionDefinitionService.executeRefresh(anyString(), anyInt(), any(), any(), any()))
            .thenThrow(new IllegalStateException("Refresh failed"));
        when(connectionRepository.save(any(Connection.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ComponentConnection componentConnection = createComponentConnection();
        Context context = mock(Context.class);

        assertThatThrownBy(() -> tokenRefreshHandler.refreshCredentials(componentConnection, context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Refresh failed");

        ArgumentCaptor<Connection> connectionArgumentCaptor = ArgumentCaptor.forClass(Connection.class);

        verify(connectionRepository).save(connectionArgumentCaptor.capture());

        Connection savedConnection = connectionArgumentCaptor.getValue();

        assertThat(savedConnection.getCredentialStatus()).isEqualTo(Connection.CredentialStatus.INVALID);
        assertThat(getErrorMessages()).anyMatch(message -> message.contains("Unable to complete refresh token"));
    }

    @Test
    void testRefreshCredentialsFailureIsLoggedAndRethrownWhenMarkingInvalidFails() {
        when(connectionDefinitionService.executeRefresh(anyString(), anyInt(), any(), any(), any()))
            .thenThrow(new IllegalStateException("Refresh failed"));
        when(connectionRepository.save(any(Connection.class)))
            .thenThrow(new IllegalStateException("Database unavailable"));

        ComponentConnection componentConnection = createComponentConnection();
        Context context = mock(Context.class);

        assertThatThrownBy(() -> tokenRefreshHandler.refreshCredentials(componentConnection, context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Refresh failed");

        assertThat(getErrorMessages())
            .anyMatch(message -> message.contains("Unable to complete refresh token"))
            .anyMatch(message -> message.contains("Unable to mark credentials of connection 7 as invalid"));
    }

    private static ComponentConnection createComponentConnection() {
        return new ComponentConnection(
            "testComponent", 1, CONNECTION_ID,
            Map.of("access_token", "old_access_token", "refresh_token", "old_refresh_token"),
            AuthorizationType.OAUTH2_AUTHORIZATION_CODE);
    }

    private static Connection createStoredConnection() {
        Connection connection = Connection.builder()
            .authorizationType(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
            .componentName("testComponent")
            .connectionVersion(1)
            .id(CONNECTION_ID)
            .name("shared connection")
            .parameters(new HashMap<>(Map.of("access_token", "old_access_token")))
            .type(PlatformType.EMBEDDED)
            .build();

        connection.setCreatedBy("admin@localhost.com");

        return connection;
    }

    private List<String> getErrorMessages() {
        return logAppender.list.stream()
            .filter(loggingEvent -> loggingEvent.getLevel() == Level.ERROR)
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
    }
}
