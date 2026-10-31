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
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.Authorization.RefreshTokenResponse;
import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.config.ComponentRegistryConfiguration;
import com.bytechef.platform.component.config.ComponentRegistryConfigurationSharedMocks;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.constant.PlatformType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ComponentRegistryConfiguration.class)
@ComponentRegistryConfigurationSharedMocks
class TokenRefreshHandlerIntTest {

    @Autowired
    private ConnectionRepository connectionRepository;

    @MockitoBean
    private ConnectionDefinitionService connectionDefinitionService;

    @Autowired
    private TokenRefreshHandler tokenRefreshHandler;

    private ListAppender<ILoggingEvent> logAppender;
    private Connection storedConnection;

    @BeforeEach
    void beforeEach() {
        SecurityContextHolder.clearContext();

        storedConnection = connectionRepository.save(createConnection());

        Logger logger = (Logger) LoggerFactory.getLogger(TokenRefreshHandler.class);

        logAppender = new ListAppender<>();

        logAppender.start();

        logger.addAppender(logAppender);
    }

    @AfterEach
    void afterEach() {
        Logger logger = (Logger) LoggerFactory.getLogger(TokenRefreshHandler.class);

        logger.detachAppender(logAppender);

        connectionRepository.deleteAll();
    }

    @Test
    void testRefreshCredentialsWithoutAuthenticationPersistsRefreshedParameters() {
        when(connectionDefinitionService.executeRefresh(anyString(), anyInt(), any(), any(), any()))
            .thenReturn(new RefreshTokenResponse("new_access_token", "new_refresh_token", 3600));

        ComponentConnection refreshedComponentConnection = tokenRefreshHandler.refreshCredentials(
            createComponentConnection(storedConnection.getId()), mock(Context.class));

        Connection persistedConnection = connectionRepository.findById(storedConnection.getId())
            .orElseThrow();

        Map<String, Object> persistedParameters = Map.copyOf(persistedConnection.getParameters());
        Map<String, Object> refreshedParameters = Map.copyOf(refreshedComponentConnection.getParameters());

        assertThat(persistedParameters)
            .containsEntry("access_token", "new_access_token")
            .containsEntry("refresh_token", "new_refresh_token");
        assertThat(refreshedParameters).containsEntry("access_token", "new_access_token");
    }

    @Test
    void testRefreshCredentialsFailureMarksConnectionInvalidAndLogs() {
        when(connectionDefinitionService.executeRefresh(anyString(), anyInt(), any(), any(), any()))
            .thenThrow(new IllegalStateException("Refresh failed"));

        ComponentConnection componentConnection = createComponentConnection(storedConnection.getId());
        Context context = mock(Context.class);

        assertThatThrownBy(() -> tokenRefreshHandler.refreshCredentials(componentConnection, context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Refresh failed");

        Connection persistedConnection = connectionRepository.findById(storedConnection.getId())
            .orElseThrow();

        assertThat(persistedConnection.getCredentialStatus()).isEqualTo(Connection.CredentialStatus.INVALID);
        assertThat(getErrorMessages()).anyMatch(message -> message.contains("Unable to complete refresh token"));
    }

    @Test
    void testRefreshCredentialsFailureIsLoggedAndRethrownWhenMarkingInvalidFails() {
        when(connectionDefinitionService.executeRefresh(anyString(), anyInt(), any(), any(), any()))
            .thenThrow(new IllegalStateException("Refresh failed"));

        long missingConnectionId = storedConnection.getId() + 1000;

        ComponentConnection componentConnection = createComponentConnection(missingConnectionId);
        Context context = mock(Context.class);

        assertThatThrownBy(() -> tokenRefreshHandler.refreshCredentials(componentConnection, context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Refresh failed");

        assertThat(getErrorMessages())
            .anyMatch(message -> message.contains("Unable to complete refresh token"))
            .anyMatch(message -> message.contains(
                "Unable to mark credentials of connection " + missingConnectionId + " as invalid"));
    }

    private static ComponentConnection createComponentConnection(long connectionId) {
        return new ComponentConnection(
            "testComponent", 1, connectionId,
            Map.of("access_token", "old_access_token", "refresh_token", "old_refresh_token"),
            AuthorizationType.OAUTH2_AUTHORIZATION_CODE);
    }

    private static Connection createConnection() {
        return Connection.builder()
            .authorizationType(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
            .componentName("testComponent")
            .connectionVersion(1)
            .name("shared connection")
            .parameters(new HashMap<>(Map.of("access_token", "old_access_token")))
            .type(PlatformType.EMBEDDED)
            .build();
    }

    private List<String> getErrorMessages() {
        return logAppender.list.stream()
            .filter(loggingEvent -> loggingEvent.getLevel() == Level.ERROR)
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
    }
}
