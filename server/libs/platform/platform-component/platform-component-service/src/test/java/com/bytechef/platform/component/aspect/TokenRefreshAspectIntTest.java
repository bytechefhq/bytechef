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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.Authorization.RefreshTokenResponse;
import com.bytechef.component.definition.Context;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = TokenRefreshAspectIntTest.TestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class TokenRefreshAspectIntTest {

    @Configuration
    @ComponentScan(
        basePackages = {
            "com.bytechef.platform.component.aspect"
        })
    @EnableAutoConfiguration
    @EnableAspectJAutoProxy
    @EnableCaching
    @Import({
        JacksonConfiguration.class, LiquibaseConfiguration.class
    })
    static class TestConfiguration {
    }

    @Autowired
    private TokenRefreshTestService tokenRefreshTestService;

    @MockitoBean
    private ConnectionDefinitionService connectionDefinitionService;

    @MockitoBean
    private ConnectionService connectionService;

    @MockitoBean
    private ContextFactory contextFactory;

    @BeforeEach
    void setUp() {
        tokenRefreshTestService.reset();

        when(contextFactory.createContext(anyString(), any())).thenReturn(mock(Context.class));
    }

    @AfterEach
    void tearDown() {
        tokenRefreshTestService.reset();
    }

    @Test
    void testExecuteNoRefreshNeeded() {
        ComponentConnection connection = createOAuth2Connection();

        tokenRefreshTestService.setBehavior(() -> "success");

        Object result = tokenRefreshTestService.executeWithConnection("testComponent", 1, connection);

        assertThat(result).isEqualTo("success");
        assertThat(tokenRefreshTestService.getCallCount()).isEqualTo(1);

        verify(connectionDefinitionService, never()).executeRefresh(
            anyString(), anyInt(), any(), any(), any());
    }

    @Test
    void testExecute401TriggersRefreshRetrySucceeds() {
        ComponentConnection connection = createOAuth2Connection();

        setupRefreshOn401();
        setupSuccessfulRefresh();

        AtomicInteger callCount = new AtomicInteger(0);

        tokenRefreshTestService.setBehavior(() -> {
            if (callCount.incrementAndGet() == 1) {
                throw new ProviderException(401, "Unauthorized");
            }

            return "success_after_refresh";
        });

        Object result = tokenRefreshTestService.executeWithConnection("testComponent", 1, connection);

        assertThat(result).isEqualTo("success_after_refresh");
        assertThat(tokenRefreshTestService.getCallCount()).isEqualTo(2);

        verify(connectionDefinitionService).executeRefresh(
            eq("testComponent"), eq(1), eq(AuthorizationType.OAUTH2_AUTHORIZATION_CODE), any(), any());

        verify(connectionService).updateConnectionParameters(eq(1L), any());
    }

    @Test
    void testExecuteRefreshFailsConnectionMarkedInvalid() {
        ComponentConnection connection = createOAuth2Connection();

        setupRefreshOn401();

        when(connectionDefinitionService.executeRefresh(
            anyString(), anyInt(), any(), any(), any()))
                .thenThrow(new RuntimeException("Refresh failed"));

        tokenRefreshTestService.setBehavior(() -> {
            throw new ProviderException(401, "Unauthorized");
        });

        assertThatThrownBy(() -> tokenRefreshTestService.executeWithConnection("testComponent", 1, connection))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Refresh failed");

        verify(connectionService).updateConnectionCredentialStatus(eq(1L), eq(Connection.CredentialStatus.INVALID));
    }

    @Test
    void testExecuteConcurrentRefreshOnlyOneRefreshHappens() throws InterruptedException {
        ComponentConnection connection = createOAuth2Connection();

        setupRefreshOn401();
        setupSuccessfulRefresh();

        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger failureCount = new AtomicInteger(0);
        ConcurrentHashMap<Long, Boolean> threadFirstCallMap = new ConcurrentHashMap<>();

        tokenRefreshTestService.setBehavior(() -> {
            long threadId = Thread.currentThread()
                .getId();

            if (threadFirstCallMap.putIfAbsent(threadId, true) == null) {
                throw new ProviderException(401, "Unauthorized");
            }

            return "success";
        });

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    tokenRefreshTestService.executeWithConnection("testComponent", 1, connection);
                } catch (Exception exception) {
                    failureCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        boolean completed = endLatch.await(10, TimeUnit.SECONDS);

        executor.shutdown();

        assertThat(completed).isTrue();

        verify(connectionService, times(threadCount)).updateConnectionParameters(eq(1L), any());
    }

    @Test
    void testExecuteApiKeyAuthNoRefreshAttempted() {
        ComponentConnection connection = createApiKeyConnection();

        tokenRefreshTestService.setBehavior(() -> {
            throw new ProviderException(401, "Unauthorized");
        });

        assertThatThrownBy(() -> tokenRefreshTestService.executeWithConnection("testComponent", 1, connection))
            .isInstanceOf(ConfigurationException.class);

        verify(connectionDefinitionService, never()).executeRefresh(
            anyString(), anyInt(), any(), any(), any());
    }

    @Test
    void testExecuteCustomAuthAcquireNewCredentials() {
        ComponentConnection connection = createCustomConnection();

        setupRefreshOn401ForCustomAuth();

        Map<String, ?> newParams = Map.of("access_token", "new_custom_token");

        doReturn(newParams).when(connectionDefinitionService)
            .executeAcquire(
                anyString(), anyInt(), any(), any(), any());

        Connection updatedConnection = createMockConnection();

        when(connectionService.updateConnectionParameters(anyLong(), any()))
            .thenReturn(updatedConnection);

        AtomicInteger callCount = new AtomicInteger(0);

        tokenRefreshTestService.setBehavior(() -> {
            if (callCount.incrementAndGet() == 1) {
                throw new ProviderException(401, "Unauthorized");
            }

            return "success_with_custom_auth";
        });

        Object result = tokenRefreshTestService.executeWithConnection("testComponent", 1, connection);

        assertThat(result).isEqualTo("success_with_custom_auth");

        verify(connectionDefinitionService).executeAcquire(
            eq("testComponent"), eq(1), eq(AuthorizationType.CUSTOM), any(), any());

        verify(connectionDefinitionService, never()).executeRefresh(
            anyString(), anyInt(), any(), any(), any());
    }

    @Test
    void testExecuteNestedExceptionMatches() {
        ComponentConnection connection = createOAuth2Connection();

        setupRefreshOn401();
        setupSuccessfulRefresh();

        AtomicInteger callCount = new AtomicInteger(0);

        tokenRefreshTestService.setBehavior(() -> {
            if (callCount.incrementAndGet() == 1) {
                throw new RuntimeException("Outer exception", new ProviderException(401, "Unauthorized"));
            }

            return "success_after_nested_exception";
        });

        Object result = tokenRefreshTestService.executeWithConnection("testComponent", 1, connection);

        assertThat(result).isEqualTo("success_after_nested_exception");
        assertThat(tokenRefreshTestService.getCallCount()).isEqualTo(2);
    }

    @Test
    void testExecuteRetryAlsoFailsExceptionThrown() {
        ComponentConnection connection = createOAuth2Connection();

        setupRefreshOn401();
        setupSuccessfulRefresh();

        tokenRefreshTestService.setBehavior(() -> {
            throw new ProviderException(401, "Unauthorized");
        });

        assertThatThrownBy(() -> tokenRefreshTestService.executeWithConnection("testComponent", 1, connection))
            .isInstanceOf(ConfigurationException.class);

        assertThat(tokenRefreshTestService.getCallCount()).isEqualTo(2);

        verify(connectionDefinitionService, times(1)).executeRefresh(
            anyString(), anyInt(), any(), any(), any());
    }

    @Test
    void testExecuteNullConnectionNoRefreshAttempted() {
        tokenRefreshTestService.setBehavior(() -> {
            throw new ProviderException(401, "Unauthorized");
        });

        assertThatThrownBy(() -> tokenRefreshTestService.executeWithConnection("testComponent", 1, null))
            .isInstanceOf(ProviderException.class);

        verify(connectionDefinitionService, never()).executeRefresh(
            anyString(), anyInt(), any(), any(), any());
    }

    @Test
    void testExecuteNullAuthTypeNoRefreshAttempted() {
        ComponentConnection connection = createConnectionWithNullAuthType();

        tokenRefreshTestService.setBehavior(() -> {
            throw new ProviderException(401, "Unauthorized");
        });

        assertThatThrownBy(() -> tokenRefreshTestService.executeWithConnection("testComponent", 1, connection))
            .isInstanceOf(ProviderException.class);

        verify(connectionDefinitionService, never()).executeRefresh(
            anyString(), anyInt(), any(), any(), any());
    }

    @Test
    void testExecuteConnectionMapRefreshWorks() {
        ComponentConnection connection = createOAuth2Connection();
        Map<String, ComponentConnection> connectionMap = new HashMap<>();

        connectionMap.put("default", connection);

        setupRefreshOn401();
        setupSuccessfulRefresh();

        AtomicInteger callCount = new AtomicInteger(0);

        tokenRefreshTestService.setBehavior(() -> {
            if (callCount.incrementAndGet() == 1) {
                throw new ProviderException(401, "Unauthorized");
            }

            return "success_with_map";
        });

        Object result = tokenRefreshTestService.executeWithConnectionMap("testComponent", 1, connectionMap);

        assertThat(result).isEqualTo("success_with_map");
        assertThat(tokenRefreshTestService.getCallCount()).isEqualTo(2);
    }

    @Test
    void testExecuteEmptyConnectionMapNoRefreshAttempted() {
        Map<String, ComponentConnection> emptyMap = new HashMap<>();

        tokenRefreshTestService.setBehavior(() -> {
            throw new ProviderException(401, "Unauthorized");
        });

        assertThatThrownBy(() -> tokenRefreshTestService.executeWithConnectionMap("testComponent", 1, emptyMap))
            .isInstanceOf(ProviderException.class);

        verify(connectionDefinitionService, never()).executeRefresh(
            anyString(), anyInt(), any(), any(), any());
    }

    private ComponentConnection createOAuth2Connection() {
        Map<String, Object> params = new HashMap<>();

        params.put("access_token", "old_token");
        params.put("refresh_token", "refresh_token");

        return new ComponentConnection("testComponent", 1, 1L, params, AuthorizationType.OAUTH2_AUTHORIZATION_CODE);
    }

    private ComponentConnection createApiKeyConnection() {
        Map<String, Object> params = new HashMap<>();

        params.put("api_key", "test_api_key");

        return new ComponentConnection("testComponent", 1, 1L, params, AuthorizationType.API_KEY);
    }

    private ComponentConnection createCustomConnection() {
        Map<String, Object> params = new HashMap<>();

        params.put("custom_token", "old_custom_token");

        return new ComponentConnection("testComponent", 1, 1L, params, AuthorizationType.CUSTOM);
    }

    private ComponentConnection createConnectionWithNullAuthType() {
        Map<String, Object> params = new HashMap<>();

        params.put("access_token", "token");

        return new ComponentConnection("testComponent", 1, 1L, params, null);
    }

    private void setupRefreshOn401() {
        when(connectionDefinitionService.getAuthorizationRefreshOn(
            anyString(), anyInt(), eq(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)))
                .thenReturn(List.of(401));
    }

    private void setupRefreshOn401ForCustomAuth() {
        when(connectionDefinitionService.getAuthorizationRefreshOn(
            anyString(), anyInt(), eq(AuthorizationType.CUSTOM)))
                .thenReturn(List.of(401));
    }

    private void setupSuccessfulRefresh() {
        RefreshTokenResponse refreshResponse = new RefreshTokenResponse("new_access_token", "new_refresh_token", 3600L);

        when(connectionDefinitionService.executeRefresh(
            anyString(), anyInt(), any(), any(), any(Context.class)))
                .thenReturn(refreshResponse);

        Connection updatedConnection = createMockConnection();

        when(connectionService.updateConnectionParameters(anyLong(), any()))
            .thenReturn(updatedConnection);
    }

    private Connection createMockConnection() {
        Connection connection = new Connection();

        connection.setConnectionVersion(1);
        connection.setParameters(Map.of("access_token", "new_access_token"));
        connection.setAuthorizationType(AuthorizationType.OAUTH2_AUTHORIZATION_CODE);

        return connection;
    }
}
