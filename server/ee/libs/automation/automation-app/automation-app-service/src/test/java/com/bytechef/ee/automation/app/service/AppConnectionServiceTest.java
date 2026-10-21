/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppConnection;
import com.bytechef.ee.automation.app.domain.AppOperationType;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.repository.AppConnectionRepository;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AppConnectionServiceTest {

    @Mock
    private AppConnectionRepository appConnectionRepository;

    @Mock
    private AppService appService;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private WorkspaceConnectionService workspaceConnectionService;

    private AppConnectionServiceImpl appConnectionService;

    @BeforeEach
    void beforeEach() {
        appConnectionService = new AppConnectionServiceImpl(
            appConnectionRepository, appService, connectionService, workspaceConnectionService);
    }

    @Test
    void rejectsBindingKeyThatIsNotLowerKebab() {
        assertThatThrownBy(
            () -> appConnectionService.bind(1L, "Sales Slack", 5L, List.of(), List.of(), false, List.of()))
                .isInstanceOf(ConfigurationException.class)
                .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AppErrorType.APP_BINDING_KEY_INVALID.getErrorKey());
    }

    @Test
    void rejectsConnectionOutsideTheAppsWorkspaceOrEnvironmentOrOfAnotherType() {
        App app = app(7L, Environment.PRODUCTION);

        when(appService.getApp(1L)).thenReturn(app);
        when(workspaceConnectionService.getWorkspaceConnections(7L))
            .thenReturn(List.of(new WorkspaceConnection(5L, 7L)));

        Connection wrongEnvironment = connection(5L, Environment.STAGING, PlatformType.AUTOMATION);

        when(connectionService.getConnection(5L)).thenReturn(wrongEnvironment);

        assertNotBindable(() -> appConnectionService.bind(1L, "crm", 5L, List.of(), List.of(), false, List.of()));

        Connection embedded = connection(5L, Environment.PRODUCTION, PlatformType.EMBEDDED);

        when(connectionService.getConnection(5L)).thenReturn(embedded);

        assertNotBindable(() -> appConnectionService.bind(1L, "crm", 5L, List.of(), List.of(), false, List.of()));

        Connection otherWorkspace = connection(6L, Environment.PRODUCTION, PlatformType.AUTOMATION);

        when(connectionService.getConnection(6L)).thenReturn(otherWorkspace);

        assertNotBindable(() -> appConnectionService.bind(1L, "crm", 6L, List.of(), List.of(), false, List.of()));
    }

    @Test
    void bindReplacesAnExistingBindingInPlace() {
        App app = app(7L, Environment.PRODUCTION);

        when(appService.getApp(1L)).thenReturn(app);
        when(workspaceConnectionService.getWorkspaceConnections(7L))
            .thenReturn(List.of(new WorkspaceConnection(5L, 7L)));
        when(connectionService.getConnection(5L))
            .thenReturn(connection(5L, Environment.PRODUCTION, PlatformType.AUTOMATION));

        AppConnection existing = new AppConnection();

        existing.setId(44L);
        existing.setAppId(1L);
        existing.setBindingKey("crm");
        existing.setConnectionId(5L);

        when(appConnectionRepository.findByAppIdAndBindingKey(1L, "crm")).thenReturn(Optional.of(existing));
        when(appConnectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppConnection saved = appConnectionService.bind(
            1L, "crm", 5L, List.of("createRecord"), List.of("searchRecords"), true, List.of("GET", "POST"));

        assertThat(saved.getId()).isEqualTo(44L);
        assertThat(saved.allows(AppOperationType.ACTION, "createRecord")).isTrue();
        assertThat(saved.allows(AppOperationType.ACTION, "deleteRecord")).isFalse();
        assertThat(saved.getProxyAllowedMethodList()).containsExactly("GET", "POST");
    }

    @Test
    void rejectsBindingKeysThatNormalizeToTheSameFlatName() {
        assertFlatKeyCollision("support-crm", "support_crm");
        assertFlatKeyCollision("a--b", "a_b");
    }

    @Test
    void rebindingTheSameKeyIsNotACollision() {
        App app = app(7L, Environment.PRODUCTION);

        when(appService.getApp(1L)).thenReturn(app);
        when(workspaceConnectionService.getWorkspaceConnections(7L))
            .thenReturn(List.of(new WorkspaceConnection(5L, 7L)));
        when(connectionService.getConnection(5L))
            .thenReturn(connection(5L, Environment.PRODUCTION, PlatformType.AUTOMATION));

        AppConnection existing = new AppConnection();

        existing.setId(44L);
        existing.setAppId(1L);
        existing.setBindingKey("support-crm");
        existing.setConnectionId(5L);

        when(appConnectionRepository.findAllByAppIdOrderByBindingKey(1L)).thenReturn(List.of(existing));
        when(appConnectionRepository.findByAppIdAndBindingKey(1L, "support-crm")).thenReturn(Optional.of(existing));
        when(appConnectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppConnection saved = appConnectionService.bind(
            1L, "support-crm", 5L, List.of("createRecord"), List.of(), false, List.of());

        assertThat(saved.getId()).isEqualTo(44L);
    }

    private void assertFlatKeyCollision(String existingKey, String newKey) {
        AppConnection existing = new AppConnection();

        existing.setId(44L);
        existing.setAppId(1L);
        existing.setBindingKey(existingKey);
        existing.setConnectionId(5L);

        when(appConnectionRepository.findAllByAppIdOrderByBindingKey(1L)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> appConnectionService.bind(1L, newKey, 5L, List.of(), List.of(), false, List.of()))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(AppErrorType.APP_BINDING_KEY_ALREADY_EXISTS.getErrorKey());
    }

    @Test
    void rejectsUnknownProxyMethod() {
        App app = app(7L, Environment.PRODUCTION);

        when(appService.getApp(1L)).thenReturn(app);
        when(workspaceConnectionService.getWorkspaceConnections(7L))
            .thenReturn(List.of(new WorkspaceConnection(5L, 7L)));
        when(connectionService.getConnection(5L))
            .thenReturn(connection(5L, Environment.PRODUCTION, PlatformType.AUTOMATION));

        assertThatThrownBy(() -> appConnectionService.bind(1L, "crm", 5L, List.of(), List.of(), true, List.of("TRACE")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getBindableConnectionsReturnsOnlyWhatBindWouldAccept() {
        App app = app(7L, Environment.PRODUCTION);

        when(appService.getApp(1L)).thenReturn(app);
        when(workspaceConnectionService.getWorkspaceConnections(7L))
            .thenReturn(List.of(new WorkspaceConnection(5L, 7L), new WorkspaceConnection(6L, 7L),
                new WorkspaceConnection(8L, 7L)));
        when(connectionService.getConnections(List.of(5L, 6L, 8L)))
            .thenReturn(List.of(
                connection(5L, Environment.PRODUCTION, PlatformType.AUTOMATION),
                connection(6L, Environment.STAGING, PlatformType.AUTOMATION),
                connection(8L, Environment.PRODUCTION, PlatformType.EMBEDDED)));

        assertThat(appConnectionService.getBindableConnections(1L))
            .extracting(Connection::getId)
            .containsExactly(5L);
    }

    @Test
    void getBindableConnectionsIsEmptyWhenTheWorkspaceHasNoConnections() {
        when(appService.getApp(1L)).thenReturn(app(7L, Environment.PRODUCTION));
        when(workspaceConnectionService.getWorkspaceConnections(7L)).thenReturn(List.of());

        assertThat(appConnectionService.getBindableConnections(1L)).isEmpty();
    }

    @Test
    void unbindReportsWhetherABindingWasDeleted() {
        AppConnection existing = new AppConnection();

        when(appConnectionRepository.findByAppIdAndBindingKey(1L, "crm")).thenReturn(Optional.of(existing));
        when(appConnectionRepository.findByAppIdAndBindingKey(1L, "missing")).thenReturn(Optional.empty());

        assertThat(appConnectionService.unbind(1L, "crm")).isTrue();
        assertThat(appConnectionService.unbind(1L, "missing")).isFalse();

        verify(appConnectionRepository).delete(existing);
    }

    private static void assertNotBindable(ThrowingCallable callable) {
        assertThatThrownBy(callable)
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(AppErrorType.APP_CONNECTION_NOT_BINDABLE.getErrorKey());
    }

    private static App app(long workspaceId, Environment environment) {
        App app = new App();

        app.setId(1L);
        app.setWorkspaceId(workspaceId);
        app.setEnvironment(environment);

        return app;
    }

    private static Connection connection(long id, Environment environment, PlatformType type) {
        Connection connection = new Connection();

        connection.setId(id);
        connection.setEnvironmentId(environment.ordinal());
        connection.setType(type);
        connection.setComponentName("salesforce");
        connection.setConnectionVersion(1);

        return connection;
    }
}
