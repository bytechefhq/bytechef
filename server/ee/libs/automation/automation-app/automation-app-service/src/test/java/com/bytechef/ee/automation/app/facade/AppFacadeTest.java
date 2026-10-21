/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.ee.automation.app.audit.AppAuditEvent;
import com.bytechef.ee.automation.app.audit.AppAuditPublisher;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppConnection;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.dto.AppBindableConnectionDTO;
import com.bytechef.ee.automation.app.dto.AppBindingDTO;
import com.bytechef.ee.automation.app.dto.AppComponentOperationsDTO;
import com.bytechef.ee.automation.app.dto.AppDTO;
import com.bytechef.ee.automation.app.dto.AppSecretKeyDTO;
import com.bytechef.ee.automation.app.dto.AppWorkspaceConnectionDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.security.AppVisibilityPolicy;
import com.bytechef.ee.automation.app.service.AppConnectionService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AppFacadeTest {

    @Mock
    private ActionDefinitionService actionDefinitionService;

    @Mock
    private AppAuditPublisher appAuditPublisher;

    @Mock
    private AppConnectionService appConnectionService;

    @Mock
    private AppService appService;

    @Mock
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @Mock
    private ComponentDefinitionService componentDefinitionService;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private ResourceGrantService resourceGrantService;

    @Mock
    private ResourceVisibilityResolver resourceVisibilityResolver;

    @Mock
    private UserService userService;

    @Mock
    private WorkspaceConnectionFacade workspaceConnectionFacade;

    private AppFacadeImpl appFacade;

    @BeforeEach
    void beforeEach() {
        appFacade = new AppFacadeImpl(
            actionDefinitionService, appAuditPublisher, appConnectionService, appService,
            clusterElementDefinitionService, componentDefinitionService, connectionService, permissionService,
            resourceGrantService, new ResourceVisibilityPolicyRegistry(List.of(new AppVisibilityPolicy())),
            resourceVisibilityResolver, userService, workspaceConnectionFacade);
    }

    @Test
    void preAuthorizeExpressionsArePinned() throws Exception {
        assertThat(
            preAuthorize(
                "createApp", long.class, Environment.class, String.class, String.class, AppHostingTarget.class,
                ResourceVisibility.class))
                    .isEqualTo("hasWorkspaceScopeInEnvironment(#workspaceId, 'APP_MANAGE', #environment)");
        assertThat(preAuthorize("getApps", long.class, long.class))
            .isEqualTo("hasWorkspaceScopeInEnvironmentId(#workspaceId, 'APP_VIEW', #environmentId)");
        assertThat(preAuthorize("getApp", long.class)).isEqualTo("hasPermission(#id, 'App', 'APP_VIEW')");
        assertThat(preAuthorize("getBindings", long.class)).isEqualTo("hasPermission(#appId, 'App', 'APP_VIEW')");
        assertThat(preAuthorize("getBindableConnections", long.class, String.class))
            .isEqualTo("hasPermission(#appId, 'App', 'APP_MANAGE')");
        assertThat(preAuthorize("getConnections", long.class, Environment.class, String.class))
            .isEqualTo("hasWorkspaceScopeInEnvironment(#workspaceId, 'CONNECTION_VIEW', #environment)");
        assertThat(
            preAuthorize(
                "updateApp", long.class, String.class, String.class, AppHostingTarget.class, String.class,
                Instant.class, ResourceVisibility.class))
                    .isEqualTo("hasPermission(#id, 'App', 'APP_MANAGE')");
        assertThat(preAuthorize("updateAppStatus", long.class, AppStatus.class))
            .isEqualTo("hasPermission(#id, 'App', 'APP_MANAGE')");
        assertThat(preAuthorize("rotateSecretKey", long.class)).isEqualTo("hasPermission(#id, 'App', 'APP_MANAGE')");
        assertThat(preAuthorize("deleteApp", long.class)).isEqualTo("hasPermission(#id, 'App', 'APP_MANAGE')");
        assertThat(preAuthorize("unbindConnection", long.class, String.class))
            .isEqualTo("hasPermission(#appId, 'App', 'APP_MANAGE')");
        assertThat(
            preAuthorize(
                "bindConnection", long.class, String.class, long.class, List.class, List.class, boolean.class,
                List.class))
                    .isEqualTo(
                        "hasPermission(#appId, 'App', 'APP_MANAGE') and " +
                            "hasPermission(#connectionId, 'Connection', 'CONNECTION_EDIT')");
        assertThat(preAuthorize("getComponentOperations", String.class, int.class))
            .isEqualTo("isAuthenticated()");
    }

    @Test
    void createSetsOwnerToCurrentUserAndPublishesAudit() {
        String plaintextKey = UUID.randomUUID()
            .toString();
        User user = new User();

        user.setId(9L);

        when(userService.getCurrentUser()).thenReturn(user);
        when(appService.create(any())).thenAnswer(invocation -> {
            App app = invocation.getArgument(0);

            return new AppService.CreatedApp(persisted(app, 1L), plaintextKey);
        });

        AppSecretKeyDTO result = appFacade.createApp(
            7L, Environment.PRODUCTION, "crm", null, AppHostingTarget.VERCEL, ResourceVisibility.PRIVATE);

        assertThat(result.secretKey()).isEqualTo(plaintextKey);
        assertThat(result.app()
            .ownerUserId()).isEqualTo(9L);
        assertThat(result.app()
            .visibility()).isEqualTo(ResourceVisibility.PRIVATE);

        ArgumentCaptor<App> appCaptor = ArgumentCaptor.forClass(App.class);

        verify(appService).create(appCaptor.capture());

        App created = appCaptor.getValue();

        assertThat(created.getWorkspaceId()).isEqualTo(7L);
        assertThat(created.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(created.getHostingTarget()).isEqualTo(AppHostingTarget.VERCEL);

        verify(appAuditPublisher).publish(eq(AppAuditEvent.APP_CREATED), eq(1L), anyMap());
    }

    @Test
    void createRejectsOrganizationVisibility() {
        assertThatThrownBy(
            () -> appFacade.createApp(
                7L, Environment.PRODUCTION, "crm", null, AppHostingTarget.VERCEL, ResourceVisibility.ORGANIZATION))
                    .isInstanceOf(ConfigurationException.class)
                    .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(AppErrorType.APP_VISIBILITY_UNSUPPORTED.getErrorKey());

        verifyNoInteractions(appService, appAuditPublisher);
    }

    @Test
    void updateRejectsOrganizationVisibility() {
        assertThatThrownBy(
            () -> appFacade.updateApp(
                1L, "crm", null, AppHostingTarget.VERCEL, null, null, ResourceVisibility.ORGANIZATION))
                    .isInstanceOf(ConfigurationException.class);

        verifyNoInteractions(appService);
    }

    @Test
    void requireAppInEnvironmentReturnsForAnAppOfTheEnvironment() {
        App app = persisted(new App(), 1L);

        app.setEnvironment(Environment.STAGING);

        when(appService.getApp(1L)).thenReturn(app);

        appFacade.requireAppInEnvironment(1L, Environment.STAGING);

        verify(appService).getApp(1L);
    }

    @Test
    void requireAppInEnvironmentRejectsAnAppOfAnotherEnvironmentAsNotFound() {
        App app = persisted(new App(), 1L);

        app.setEnvironment(Environment.PRODUCTION);

        when(appService.getApp(1L)).thenReturn(app);

        assertThatThrownBy(() -> appFacade.requireAppInEnvironment(1L, Environment.STAGING))
            .isInstanceOf(ConfigurationException.class)
            .hasMessage("App 1 not found")
            .extracting(throwable -> ((ConfigurationException) throwable).getErrorKey())
            .isEqualTo(AppErrorType.APP_NOT_FOUND.getErrorKey());
    }

    @Test
    void requireAppInEnvironmentRejectsAMissingAppAsNotFound() {
        when(appService.getApp(1L))
            .thenThrow(new ConfigurationException("App 1 not found", AppErrorType.APP_NOT_FOUND));

        assertThatThrownBy(() -> appFacade.requireAppInEnvironment(1L, Environment.STAGING))
            .isInstanceOf(ConfigurationException.class)
            .extracting(throwable -> ((ConfigurationException) throwable).getErrorKey())
            .isEqualTo(AppErrorType.APP_NOT_FOUND.getErrorKey());
    }

    @Test
    void requireAppInEnvironmentIsDeliberatelyUnguarded() throws Exception {
        Method method = AppFacadeImpl.class.getMethod("requireAppInEnvironment", long.class, Environment.class);

        assertThat(method.getAnnotation(PreAuthorize.class)).isNull();
    }

    @Test
    void updateWithNullFieldsPassesNothingToChangeAndSkipsTheVisibilityCheck() {
        when(appService.getApp(1L)).thenReturn(persisted(new App(), 1L));
        when(appService.update(1L, null, null, null, null, null, null)).thenReturn(persisted(new App(), 1L));

        appFacade.updateApp(1L, null, null, null, null, null, null);

        verify(appService).update(1L, null, null, null, null, null, null);
    }

    @Test
    void updatePassesTheEditableFieldsThroughAndReturnsTheSavedApp() {
        Instant deployed = Instant.parse("2026-10-01T00:00:00Z");
        App saved = persisted(new App(), 1L);

        when(appService.getApp(1L)).thenReturn(persisted(new App(), 1L));
        when(
            appService.update(
                1L, "crm", "desc", AppHostingTarget.VERCEL, "https://x.test", deployed, ResourceVisibility.WORKSPACE))
                    .thenReturn(saved);

        AppDTO result = appFacade.updateApp(
            1L, "crm", "desc", AppHostingTarget.VERCEL, "https://x.test", deployed, ResourceVisibility.WORKSPACE);

        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void updatePublishesTheChangedFieldsWithTheNewNameAndVisibility() {
        App current = persisted(new App(), 1L);

        current.setHostingTarget(AppHostingTarget.VERCEL);
        current.setVisibility(ResourceVisibility.PRIVATE);

        App saved = persisted(new App(), 1L);

        saved.setName("crm-2");
        saved.setDescription("New description");
        saved.setHostingTarget(AppHostingTarget.VERCEL);
        saved.setVisibility(ResourceVisibility.WORKSPACE);

        when(appService.getApp(1L)).thenReturn(current);
        when(appService.update(1L, "crm-2", "New description", null, null, null, ResourceVisibility.WORKSPACE))
            .thenReturn(saved);

        appFacade.updateApp(1L, "crm-2", "New description", null, null, null, ResourceVisibility.WORKSPACE);

        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(appAuditPublisher).publish(eq(AppAuditEvent.APP_UPDATED), eq(1L), dataCaptor.capture());

        assertThat(dataCaptor.getValue()).containsOnly(
            Map.entry("changedFields", List.of("name", "description", "visibility")),
            Map.entry("name", "crm-2"),
            Map.entry("visibility", "WORKSPACE"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void updatePublishesTheNewHostingTargetWhenOnlyItChanged() {
        App current = persisted(new App(), 1L);

        current.setHostingTarget(AppHostingTarget.VERCEL);

        App saved = persisted(new App(), 1L);

        saved.setHostingTarget(AppHostingTarget.SELF_HOSTED);

        when(appService.getApp(1L)).thenReturn(current);
        when(appService.update(1L, null, null, AppHostingTarget.SELF_HOSTED, null, null, null)).thenReturn(saved);

        appFacade.updateApp(1L, null, null, AppHostingTarget.SELF_HOSTED, null, null, null);

        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(appAuditPublisher).publish(eq(AppAuditEvent.APP_UPDATED), eq(1L), dataCaptor.capture());

        assertThat(dataCaptor.getValue()).containsOnly(
            Map.entry("changedFields", List.of("hostingTarget")), Map.entry("hostingTarget", "SELF_HOSTED"));
    }

    @Test
    void updateThatChangesNothingPublishesNoEvent() {
        when(appService.getApp(1L)).thenReturn(persisted(new App(), 1L));
        when(appService.update(1L, "crm", null, null, null, null, null)).thenReturn(persisted(new App(), 1L));

        appFacade.updateApp(1L, "crm", null, null, null, null, null);

        verify(appAuditPublisher, never()).publish(any(), anyLong(), anyMap());
    }

    @Test
    void deleteRemovesTheAppItsGrantsAndAudits() {
        appFacade.deleteApp(4L);

        verify(appService).delete(4L);
        verify(resourceGrantService).deleteGrants("App", 4L);
        verify(appAuditPublisher).publish(AppAuditEvent.APP_DELETED, 4L);
    }

    @Test
    void getAppsMapsTheEnvironmentIdToTheEnvironment() {
        when(appService.getApps(7L, Environment.STAGING)).thenReturn(List.of(persisted(new App(), 1L)));
        when(resourceVisibilityResolver.filterVisibleIds(eq("App"), eq(7L), anyCollection())).thenReturn(Set.of(1L));

        assertThat(appFacade.getApps(7L, Environment.STAGING.ordinal())).hasSize(1);
    }

    @Test
    void getAppsLeavesOutAppsTheCallerCannotSee() {
        App visible = persisted(new App(), 1L);
        App hidden = persisted(new App(), 2L);

        visible.setVisibility(ResourceVisibility.WORKSPACE);
        hidden.setVisibility(ResourceVisibility.PRIVATE);
        hidden.setCreatedBy("someone-else");

        when(appService.getApps(7L, Environment.STAGING)).thenReturn(List.of(visible, hidden));
        when(resourceVisibilityResolver.filterVisibleIds(eq("App"), eq(7L), anyCollection())).thenReturn(Set.of(1L));

        assertThat(appFacade.getApps(7L, Environment.STAGING.ordinal()))
            .extracting(AppDTO::id)
            .containsExactly(1L);

        ArgumentCaptor<Collection<VisibilityRecord>> candidates = ArgumentCaptor.forClass(Collection.class);

        verify(resourceVisibilityResolver).filterVisibleIds(eq("App"), eq(7L), candidates.capture());

        assertThat(candidates.getValue())
            .containsExactlyInAnyOrder(
                new VisibilityRecord(1L, ResourceVisibility.WORKSPACE, "admin"),
                new VisibilityRecord(2L, ResourceVisibility.PRIVATE, "someone-else"));
    }

    @Test
    void updateStatusPublishesTheMatchingAuditEvent() {
        when(appService.updateStatus(1L, AppStatus.DISABLED)).thenReturn(persisted(new App(), 1L));
        when(appService.updateStatus(2L, AppStatus.ENABLED)).thenReturn(persisted(new App(), 2L));

        appFacade.updateAppStatus(1L, AppStatus.DISABLED);
        appFacade.updateAppStatus(2L, AppStatus.ENABLED);

        verify(appAuditPublisher).publish(AppAuditEvent.APP_DISABLED, 1L);
        verify(appAuditPublisher).publish(AppAuditEvent.APP_ENABLED, 2L);
    }

    @Test
    void rotateReturnsTheNewKeyAndAudits() {
        String plaintextKey = UUID.randomUUID()
            .toString();

        when(appService.rotateSecretKey(1L))
            .thenReturn(new AppService.RotatedKey(persisted(new App(), 1L), plaintextKey));

        AppSecretKeyDTO result = appFacade.rotateSecretKey(1L);

        assertThat(result.secretKey()).isEqualTo(plaintextKey);

        verify(appAuditPublisher).publish(AppAuditEvent.APP_SECRET_KEY_ROTATED, 1L);
    }

    @Test
    void bindReturnsTheBindingWithConnectionDetails() {
        when(appConnectionService.fetchBinding(1L, "crm")).thenReturn(Optional.empty());
        when(appConnectionService.bind(anyLong(), any(), anyLong(), any(), any(), anyBoolean(), any()))
            .thenReturn(binding(10L, 1L, "crm", 5L));
        when(connectionService.getConnection(5L)).thenReturn(connection(5L, "Salesforce"));
        stubLatestComponentVersion("salesforce", 4);

        AppBindingDTO created =
            appFacade.bindConnection(1L, "crm", 5L, List.of("a"), List.of("t"), true, List.of("GET"));

        assertThat(created.connectionName()).isEqualTo("Salesforce");
        assertThat(created.componentName()).isEqualTo("salesforce");
        assertThat(created.actionNames()).containsExactly("a");
        assertThat(created.toolNames()).containsExactly("t");
        assertThat(created.proxyAllowedMethods()).containsExactly("GET");
        assertThat(created.connectionVersion()).isEqualTo(1);
        assertThat(created.componentVersion()).isEqualTo(4);
    }

    @Test
    void bindAuditsANewBindingKeyAsBound() {
        stubBind(Optional.empty(), 5L);

        appFacade.bindConnection(1L, "crm", 5L, List.of(), List.of(), false, List.of());

        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_CONNECTION_BOUND, 1L, Map.of("bindingKey", "crm", "connectionId", 5L));
    }

    @Test
    void bindAuditsAConnectionSwapAsBoundWithThePreviousConnection() {
        stubBind(Optional.of(binding(10L, 1L, "crm", 4L)), 5L);

        appFacade.bindConnection(1L, "crm", 5L, List.of(), List.of(), false, List.of());

        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_CONNECTION_BOUND, 1L,
            Map.of("bindingKey", "crm", "connectionId", 5L, "previousConnectionId", 4L));
    }

    @Test
    void bindAuditsRebindingTheSameConnectionAsOperationsChanged() {
        stubBind(Optional.of(binding(10L, 1L, "crm", 5L)), 5L);

        appFacade.bindConnection(1L, "crm", 5L, List.of(), List.of(), false, List.of());

        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_OPERATIONS_CHANGED, 1L, Map.of("bindingKey", "crm", "connectionId", 5L));
    }

    @Test
    void unbindAuditsWhenABindingWasDeleted() {
        when(appConnectionService.unbind(1L, "crm")).thenReturn(true);

        appFacade.unbindConnection(1L, "crm");

        verify(appAuditPublisher).publish(AppAuditEvent.APP_CONNECTION_UNBOUND, 1L, Map.of("bindingKey", "crm"));
    }

    @Test
    void unbindOfAMissingBindingKeyDoesNotAudit() {
        when(appConnectionService.unbind(1L, "missing")).thenReturn(false);

        appFacade.unbindConnection(1L, "missing");

        verifyNoInteractions(appAuditPublisher);
    }

    @Test
    void getBindableConnectionsLeavesOutConnectionsTheCallerCannotSee() {
        Connection visible = connection(5L, "Mine");
        Connection hidden = connection(6L, "Colleague private");

        hidden.setVisibility(ResourceVisibility.PRIVATE);
        hidden.setCreatedBy("someone-else");

        when(appService.getApp(1L)).thenReturn(persisted(new App(), 1L));
        when(appConnectionService.getBindableConnections(1L)).thenReturn(List.of(visible, hidden));
        when(resourceVisibilityResolver.filterVisibleIds(eq("Connection"), eq(7L), anyCollection()))
            .thenReturn(Set.of(5L));
        stubLatestComponentVersion("salesforce", 3);

        assertThat(appFacade.getBindableConnections(1L, null))
            .containsExactly(new AppBindableConnectionDTO(5L, "Mine", "salesforce", 1, 3, false));

        ArgumentCaptor<Collection<VisibilityRecord>> candidates = ArgumentCaptor.forClass(Collection.class);

        verify(resourceVisibilityResolver).filterVisibleIds(eq("Connection"), eq(7L), candidates.capture());

        assertThat(candidates.getValue())
            .contains(new VisibilityRecord(6L, ResourceVisibility.PRIVATE, "someone-else"));
    }

    @Test
    void getBindableConnectionsLeavesOutConnectionsWhoseComponentCannotBeResolved() {
        Connection resolvable = connection(5L, "Salesforce");
        Connection orphaned = connection(6L, "Legacy");

        orphaned.setComponentName("deleted-component");

        when(appService.getApp(1L)).thenReturn(persisted(new App(), 1L));
        when(appConnectionService.getBindableConnections(1L)).thenReturn(List.of(orphaned, resolvable));
        when(resourceVisibilityResolver.filterVisibleIds(eq("Connection"), eq(7L), anyCollection()))
            .thenReturn(Set.of(5L, 6L));
        when(componentDefinitionService.fetchComponentDefinition("deleted-component", null))
            .thenReturn(Optional.empty());
        stubLatestComponentVersion("salesforce", 3);

        assertThat(appFacade.getBindableConnections(1L, null))
            .containsExactly(new AppBindableConnectionDTO(5L, "Salesforce", "salesforce", 1, 3, false));
    }

    @Test
    void getBindingsMapsABindingToAnUnresolvableComponentWithNoComponentVersion() {
        Connection orphaned = connection(6L, "Legacy");

        orphaned.setComponentName("deleted-component");

        when(appConnectionService.getBindings(1L))
            .thenReturn(List.of(binding(10L, 1L, "crm", 5L), binding(11L, 1L, "legacy", 6L)));
        when(connectionService.getConnection(5L)).thenReturn(connection(5L, "Salesforce"));
        when(connectionService.getConnection(6L)).thenReturn(orphaned);
        when(componentDefinitionService.fetchComponentDefinition("deleted-component", null))
            .thenReturn(Optional.empty());
        stubLatestComponentVersion("salesforce", 3);

        List<AppBindingDTO> bindings = appFacade.getBindings(1L);

        assertThat(bindings).extracting(AppBindingDTO::bindingKey)
            .containsExactly("crm", "legacy");
        assertThat(bindings).extracting(AppBindingDTO::componentVersion)
            .containsExactly(3, null);
    }

    @Test
    void getComponentOperationsReturnsSortedActionAndToolNames() {
        List<ActionDefinition> actionDefinitions = List.of(mockAction("sendMessage"), mockAction("createChannel"));
        List<ClusterElementDefinition> toolDefinitions =
            List.of(mockClusterElement("sendMessage"), mockClusterElement("archive"));

        when(actionDefinitionService.getActionDefinitions("slack", 1)).thenReturn(actionDefinitions);
        when(clusterElementDefinitionService.getClusterElementDefinitions("slack", 1, BaseToolFunction.TOOLS))
            .thenReturn(toolDefinitions);

        AppComponentOperationsDTO result = appFacade.getComponentOperations("slack", 1);

        assertThat(result.actionNames()).containsExactly("createChannel", "sendMessage");
        assertThat(result.toolNames()).containsExactly("archive", "sendMessage");
    }

    private void stubBind(Optional<AppConnection> existingBinding, long connectionId) {
        when(appConnectionService.fetchBinding(1L, "crm")).thenReturn(existingBinding);
        when(appConnectionService.bind(anyLong(), any(), anyLong(), any(), any(), anyBoolean(), any()))
            .thenReturn(binding(10L, 1L, "crm", connectionId));
        when(connectionService.getConnection(connectionId)).thenReturn(connection(connectionId, "Salesforce"));
        stubLatestComponentVersion("salesforce", 1);
    }

    private void stubLatestComponentVersion(String componentName, int version) {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getVersion()).thenReturn(version);
        when(componentDefinitionService.fetchComponentDefinition(componentName, null))
            .thenReturn(Optional.of(componentDefinition));
    }

    private static ActionDefinition mockAction(String name) {
        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getName()).thenReturn(name);

        return actionDefinition;
    }

    private static ClusterElementDefinition mockClusterElement(String name) {
        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getName()).thenReturn(name);

        return clusterElementDefinition;
    }

    private static AppConnection binding(long id, long appId, String bindingKey, long connectionId) {
        AppConnection appConnection = new AppConnection();

        appConnection.setId(id);
        appConnection.setAppId(appId);
        appConnection.setBindingKey(bindingKey);
        appConnection.setConnectionId(connectionId);
        appConnection.setOperations(List.of("a"), List.of("t"));
        appConnection.setProxyEnabled(true);
        appConnection.setProxyAllowedMethodList(List.of("GET"));

        return appConnection;
    }

    @Test
    void getBindableConnectionsMarksOnlyValidConnectionsTheCallerMayUseAsUsable() {
        Connection usable = connection(5L, "Usable");
        Connection notUsable = connection(6L, "Not mine to use");
        Connection invalid = connection(8L, "Expired");

        usable.setCredentialStatus(CredentialStatus.VALID);
        notUsable.setCredentialStatus(CredentialStatus.VALID);
        invalid.setCredentialStatus(CredentialStatus.INVALID);

        when(appService.getApp(1L)).thenReturn(persisted(new App(), 1L));
        when(appConnectionService.getBindableConnections(1L)).thenReturn(List.of(usable, notUsable, invalid));
        when(resourceVisibilityResolver.filterVisibleIds(eq("Connection"), eq(7L), anyCollection()))
            .thenReturn(Set.of(5L, 6L, 8L));
        when(permissionService.hasResourceScope(5L, "Connection", "CONNECTION_EDIT")).thenReturn(true);
        when(permissionService.hasResourceScope(6L, "Connection", "CONNECTION_EDIT")).thenReturn(false);
        lenient().when(permissionService.hasResourceScope(8L, "Connection", "CONNECTION_EDIT"))
            .thenReturn(true);
        stubLatestComponentVersion("salesforce", 3);

        assertThat(appFacade.getBindableConnections(1L, null))
            .extracting(AppBindableConnectionDTO::id, AppBindableConnectionDTO::usable)
            .containsExactly(tuple(5L, true), tuple(6L, false), tuple(8L, false));
    }

    @Test
    void getBindableConnectionsFiltersByComponentName() {
        Connection salesforce = connection(5L, "Salesforce");
        Connection slack = connection(6L, "Slack");

        slack.setComponentName("slack");

        when(appService.getApp(1L)).thenReturn(persisted(new App(), 1L));
        when(appConnectionService.getBindableConnections(1L)).thenReturn(List.of(salesforce, slack));
        when(resourceVisibilityResolver.filterVisibleIds(eq("Connection"), eq(7L), anyCollection()))
            .thenReturn(Set.of(5L, 6L));
        stubLatestComponentVersion("slack", 2);

        assertThat(appFacade.getBindableConnections(1L, "slack"))
            .extracting(AppBindableConnectionDTO::id)
            .containsExactly(6L);

        ArgumentCaptor<Collection<VisibilityRecord>> candidates = ArgumentCaptor.forClass(Collection.class);

        verify(resourceVisibilityResolver).filterVisibleIds(eq("Connection"), eq(7L), candidates.capture());

        assertThat(candidates.getValue())
            .extracting(VisibilityRecord::id)
            .containsExactly(6L);
    }

    @Test
    void getConnectionsMarksAiProviderAndUsableConnectionsAndNeverCarriesParameters() {
        long aiProviderConnectionId = -3L;

        when(workspaceConnectionFacade.getConnections(7L, "slack", null, 0L, null)).thenReturn(List.of(
            connectionDTO(101L, "Slack A", CredentialStatus.VALID),
            connectionDTO(102L, "Slack B", CredentialStatus.INVALID),
            connectionDTO(aiProviderConnectionId, "OpenAI", CredentialStatus.VALID)));
        when(permissionService.hasResourceScope(101L, "Connection", "CONNECTION_EDIT")).thenReturn(true);
        lenient().when(permissionService.hasResourceScope(102L, "Connection", "CONNECTION_EDIT"))
            .thenReturn(true);

        List<AppWorkspaceConnectionDTO> connections = appFacade.getConnections(7L, Environment.DEVELOPMENT, "slack");

        assertThat(connections)
            .extracting(
                AppWorkspaceConnectionDTO::id, AppWorkspaceConnectionDTO::credentialStatus,
                AppWorkspaceConnectionDTO::aiProvider, AppWorkspaceConnectionDTO::usable)
            .containsExactly(
                tuple(101L, "VALID", false, true),
                tuple(102L, "INVALID", false, false),
                tuple(aiProviderConnectionId, "VALID", true, false));

        verify(permissionService, never()).hasResourceScope(aiProviderConnectionId, "Connection", "CONNECTION_EDIT");
    }

    private static ConnectionDTO connectionDTO(long id, String name, CredentialStatus credentialStatus) {
        return ConnectionDTO.builder()
            .componentName("slack")
            .connectionVersion(1)
            .credentialStatus(credentialStatus)
            .id(id)
            .name(name)
            .parameters(Map.of("token", "secret"))
            .visibility(ResourceVisibility.WORKSPACE)
            .build();
    }

    private static Connection connection(long id, String name) {
        Connection connection = new Connection();

        connection.setId(id);
        connection.setName(name);
        connection.setComponentName("salesforce");
        connection.setConnectionVersion(1);
        connection.setCreatedBy("admin");
        connection.setVisibility(ResourceVisibility.WORKSPACE);

        return connection;
    }

    private static App persisted(App app, long id) {
        app.setId(id);
        app.setUuid("uuid");
        app.setName("crm");
        app.setWorkspaceId(7L);
        app.setOwnerUserId(9L);
        app.setSecretKeyHint(String.valueOf(id));
        app.setCreatedBy("admin");
        app.setCreatedDate(Instant.EPOCH);
        app.setLastModifiedBy("admin");
        app.setLastModifiedDate(Instant.EPOCH);

        return app;
    }

    private static String preAuthorize(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = AppFacadeImpl.class.getMethod(methodName, parameterTypes);

        PreAuthorize preAuthorize = Objects.requireNonNull(method.getAnnotation(PreAuthorize.class));

        return preAuthorize.value();
    }
}
