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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.ee.automation.app.audit.AppAuditDataAssertions;
import com.bytechef.ee.automation.app.audit.AppAuditEvent;
import com.bytechef.ee.automation.app.audit.AppAuditPublisher;
import com.bytechef.ee.automation.app.deployment.AppDeploymentProvider;
import com.bytechef.ee.automation.app.deployment.AppDeploymentSettingsException;
import com.bytechef.ee.automation.app.deployment.AppDeploymentSynchronizer;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppConnection;
import com.bytechef.ee.automation.app.domain.AppDeployment;
import com.bytechef.ee.automation.app.domain.AppDeploymentStatus;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.dto.AppBindableConnectionDTO;
import com.bytechef.ee.automation.app.dto.AppBindingDTO;
import com.bytechef.ee.automation.app.dto.AppInstanceDTO;
import com.bytechef.ee.automation.app.dto.AppSecretKeyDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.security.AppInstanceClients;
import com.bytechef.ee.automation.app.security.AppInstanceVisibilityPolicy;
import com.bytechef.ee.automation.app.service.AppApiCollectionService;
import com.bytechef.ee.automation.app.service.AppConnectionService;
import com.bytechef.ee.automation.app.service.AppDeploymentService;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.app.service.AppWorkflowService;
import com.bytechef.ee.automation.app.util.AppSecretKeys;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.oauth2.authorizationserver.domain.ClientRegistrationRequest;
import com.bytechef.platform.oauth2.authorizationserver.exception.RegisteredClientErrorType;
import com.bytechef.platform.oauth2.authorizationserver.service.OAuth2AuthorizationGrantService;
import com.bytechef.platform.oauth2.authorizationserver.service.RegisteredClientService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class AppInstanceFacadeTest {

    private static final long APP_ID = 1L;
    private static final long APP_INSTANCE_ID = 11L;
    private static final long WORKSPACE_ID = 7L;

    private static final AppSecretKeys.GeneratedKey CREATED_KEY = AppSecretKeys.generate();
    private static final AppSecretKeys.GeneratedKey ROTATED_KEY = AppSecretKeys.generate();
    private static final String CONNECTION_CREDENTIAL = "connection-credential-sentinel";

    @Mock
    private AppApiCollectionService appApiCollectionService;

    @Mock
    private AppAuditPublisher appAuditPublisher;

    @Mock
    private AppConnectionService appConnectionService;

    @Mock
    private OAuth2AuthorizationGrantService authorizationGrantService;

    @Mock
    private ObjectProvider<OAuth2AuthorizationGrantService> authorizationGrantServiceProvider;

    @Mock
    private AppDeploymentService appDeploymentService;

    @Mock
    private AppDeploymentSynchronizer appDeploymentSynchronizer;

    @Mock
    private AppInstanceService appInstanceService;

    @Mock
    private AppService appService;

    @Mock
    private AppWorkflowService appWorkflowService;

    @Mock
    private ComponentDefinitionService componentDefinitionService;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private RegisteredClientService registeredClientService;

    @Mock
    private ObjectProvider<RegisteredClientService> registeredClientServiceProvider;

    @Mock
    private ResourceGrantService resourceGrantService;

    @Mock
    private ResourceVisibilityResolver resourceVisibilityResolver;

    @Mock
    private UserService userService;

    private AppInstanceFacadeImpl appInstanceFacade;

    @BeforeEach
    void beforeEach() {
        appInstanceFacade = new AppInstanceFacadeImpl(
            appApiCollectionService, appAuditPublisher, appConnectionService,
            new AppInstanceClients(authorizationGrantServiceProvider, registeredClientServiceProvider),
            appDeploymentService, appDeploymentSynchronizer, appInstanceService, appService,
            appWorkflowService, componentDefinitionService, connectionService, permissionService,
            resourceGrantService, new ResourceVisibilityPolicyRegistry(List.of(new AppInstanceVisibilityPolicy())),
            resourceVisibilityResolver, userService);

        lenient().when(appInstanceService.getAppInstance(APP_INSTANCE_ID))
            .thenReturn(persistedInstance(APP_INSTANCE_ID));
        lenient().when(appService.getApp(APP_ID))
            .thenReturn(persistedApp());

        enableAuthorizationServer(true);
    }

    @AfterEach
    void afterEach() {
        AppAuditDataAssertions.assertNoSecrets(
            appAuditPublisher, CREATED_KEY.key(), CREATED_KEY.hash(), CREATED_KEY.hint(), ROTATED_KEY.key(),
            ROTATED_KEY.hash(), ROTATED_KEY.hint(), CONNECTION_CREDENTIAL, "secret-value", "prj_1", "team_1", "HR",
            "DE");
    }

    @Test
    void testEveryFacadeMethodIsGated() throws Exception {
        assertThat(
            preAuthorize(
                "createAppInstance", long.class, long.class, Environment.class, String.class,
                AppHostingTarget.class, ResourceVisibility.class, AppUserAuthMode.class))
                    .isEqualTo(
                        "hasPermission(#appId, 'App', 'APP_MANAGE') and " +
                            "hasWorkspaceScopeInEnvironment(#workspaceId, 'APP_MANAGE', #environment)");
        assertThat(preAuthorize("getAppInstance", long.class))
            .isEqualTo("hasPermission(#id, 'AppInstance', 'APP_VIEW')");
        assertThat(preAuthorize("getBindings", long.class))
            .isEqualTo("hasPermission(#id, 'AppInstance', 'APP_VIEW')");
        assertThat(preAuthorize("getBindableConnections", long.class, String.class))
            .isEqualTo("hasPermission(#id, 'AppInstance', 'APP_MANAGE')");
        assertThat(
            preAuthorize("updateAppInstance", long.class, String.class, AppHostingTarget.class, String.class))
                .isEqualTo("hasPermission(#id, 'AppInstance', 'APP_MANAGE')");
        assertThat(
            preAuthorize(
                "updateAppInstance", long.class, String.class, AppHostingTarget.class, String.class, Object.class))
                    .isEqualTo("hasPermission(#id, 'AppInstance', 'APP_MANAGE')");
        assertThat(preAuthorize("updateAppInstanceConfiguration", long.class, Object.class))
            .isEqualTo("hasPermission(#id, 'AppInstance', 'APP_MANAGE')");
        assertThat(preAuthorize("updateAppInstanceStatus", long.class, AppInstanceStatus.class))
            .isEqualTo("hasPermission(#id, 'AppInstance', 'APP_MANAGE')");
        assertThat(preAuthorize("updateAppInstanceUserAuth", long.class, AppUserAuthMode.class))
            .isEqualTo("hasPermission(#id, 'AppInstance', 'APP_MANAGE')");
        assertThat(preAuthorize("rotateSecretKey", long.class))
            .isEqualTo("hasPermission(#id, 'AppInstance', 'APP_MANAGE')");
        assertThat(preAuthorize("deleteAppInstance", long.class))
            .isEqualTo("hasPermission(#id, 'AppInstance', 'APP_MANAGE')");
        assertThat(preAuthorize("getHostedAppInstances", long.class, Environment.class))
            .isEqualTo("hasWorkspaceScopeInEnvironment(#workspaceId, 'APP_VIEW', #environment)");
        assertThat(preAuthorize("unbindConnection", long.class, String.class))
            .isEqualTo("hasPermission(#id, 'AppInstance', 'APP_MANAGE')");
        assertThat(
            preAuthorize(
                "bindConnection", long.class, String.class, long.class, List.class, List.class, boolean.class,
                List.class))
                    .isEqualTo(
                        "hasPermission(#id, 'AppInstance', 'APP_MANAGE') and " +
                            "hasPermission(#connectionId, 'Connection', 'CONNECTION_EDIT')");
        assertThat(preAuthorize("updateAppInstanceDeploymentSettings", long.class, Long.class, Map.class))
            .isEqualTo(
                "hasPermission(#id, 'AppInstance', 'APP_MANAGE') and (#deploymentConnectionId == null or " +
                    "(hasPermission(#deploymentConnectionId, 'Connection', 'CONNECTION_EDIT') && " +
                    "(isResourceOwner(#deploymentConnectionId, 'Connection') || isTenantAdmin())))");

        for (Method method : AppInstanceFacade.class.getMethods()) {
            if (method.getName()
                .equals("requireAppInstanceInEnvironment")) {

                continue;
            }

            Method implementation = AppInstanceFacadeImpl.class.getMethod(
                method.getName(), method.getParameterTypes());

            assertThat(implementation.getAnnotation(PreAuthorize.class))
                .as(method.getName())
                .isNotNull();
        }
    }

    @Test
    void testRequireAppInstanceInEnvironmentIsDeliberatelyUnguarded() throws Exception {
        Method method = AppInstanceFacadeImpl.class.getMethod(
            "requireAppInstanceInEnvironment", long.class, Environment.class);

        assertThat(method.getAnnotation(PreAuthorize.class)).isNull();
    }

    @Test
    void testRequireAppInstanceInEnvironmentPassesForAnInstanceOfThatEnvironment() {
        when(appInstanceService.fetchAppInstance(APP_INSTANCE_ID))
            .thenReturn(Optional.of(persistedInstance(APP_INSTANCE_ID)));
        when(permissionService.hasResourceScope(APP_INSTANCE_ID, "AppInstance", "APP_VIEW")).thenReturn(true);

        appInstanceFacade.requireAppInstanceInEnvironment(APP_INSTANCE_ID, Environment.PRODUCTION);

        verify(appInstanceService).fetchAppInstance(APP_INSTANCE_ID);
    }

    @Test
    void testRequireAppInstanceInEnvironmentRejectsAnInstanceOfAnotherEnvironmentAsNotFound() {
        when(appInstanceService.fetchAppInstance(APP_INSTANCE_ID))
            .thenReturn(Optional.of(persistedInstance(APP_INSTANCE_ID)));

        assertThatThrownBy(
            () -> appInstanceFacade.requireAppInstanceInEnvironment(APP_INSTANCE_ID, Environment.DEVELOPMENT))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("App instance 11 not found")
                .extracting(throwable -> ((ConfigurationException) throwable).getErrorKey())
                .isEqualTo(AppErrorType.APP_INSTANCE_NOT_FOUND.getErrorKey());
    }

    @Test
    void testRequireAppInstanceInEnvironmentRejectsAnInstanceTheCallerMayNotViewAsNotFound() {
        when(appInstanceService.fetchAppInstance(APP_INSTANCE_ID))
            .thenReturn(Optional.of(persistedInstance(APP_INSTANCE_ID)));
        when(permissionService.hasResourceScope(APP_INSTANCE_ID, "AppInstance", "APP_VIEW")).thenReturn(false);

        assertThatThrownBy(
            () -> appInstanceFacade.requireAppInstanceInEnvironment(APP_INSTANCE_ID, Environment.PRODUCTION))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("App instance 11 not found")
                .extracting(throwable -> ((ConfigurationException) throwable).getErrorKey())
                .isEqualTo(AppErrorType.APP_INSTANCE_NOT_FOUND.getErrorKey());
    }

    @Test
    void testRequireAppInstanceInEnvironmentRejectsAMissingInstanceAsNotFound() {
        when(appInstanceService.fetchAppInstance(APP_INSTANCE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> appInstanceFacade.requireAppInstanceInEnvironment(APP_INSTANCE_ID, Environment.PRODUCTION))
                .isInstanceOf(ConfigurationException.class)
                .extracting(throwable -> ((ConfigurationException) throwable).getErrorKey())
                .isEqualTo(AppErrorType.APP_INSTANCE_NOT_FOUND.getErrorKey());
    }

    @Test
    void testGetHostedAppInstancesListsVisibleInstancesOfAppsOwnedElsewhere() {
        App foreignApp = persistedApp();

        foreignApp.setId(2L);
        foreignApp.setWorkspaceId(99L);

        AppInstance ownAppInstance = persistedInstance(APP_INSTANCE_ID);
        AppInstance hostedAppInstance = persistedInstance(12L);
        AppInstance hiddenAppInstance = persistedInstance(13L);

        hostedAppInstance.setAppId(2L);
        hiddenAppInstance.setAppId(2L);

        when(appInstanceService.getWorkspaceAppInstances(WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(List.of(ownAppInstance, hostedAppInstance, hiddenAppInstance));
        when(resourceVisibilityResolver.filterVisibleIds(eq("AppInstance"), eq(WORKSPACE_ID), anyCollection()))
            .thenReturn(Set.of(APP_INSTANCE_ID, 12L));
        when(appService.getApp(2L)).thenReturn(foreignApp);

        List<AppInstanceDTO> hostedAppInstances = appInstanceFacade.getHostedAppInstances(
            WORKSPACE_ID, Environment.PRODUCTION);

        assertThat(hostedAppInstances).extracting(AppInstanceDTO::id)
            .containsExactly(12L);
        assertThat(hostedAppInstances.getFirst()
            .app()
            .id()).isEqualTo(2L);

        verify(appService).getApp(2L);
    }

    @Test
    void testGetHostedAppInstancesOfAnEmptyWorkspaceReadsNothingElse() {
        when(appInstanceService.getWorkspaceAppInstances(WORKSPACE_ID, Environment.PRODUCTION)).thenReturn(List.of());

        assertThat(appInstanceFacade.getHostedAppInstances(WORKSPACE_ID, Environment.PRODUCTION)).isEmpty();

        verifyNoInteractions(resourceVisibilityResolver);
    }

    @Test
    void testCreatorBecomesInstanceOwner() {
        User user = new User();

        user.setId(42L);

        when(userService.getCurrentUser()).thenReturn(user);
        when(appInstanceService.create(any())).thenAnswer(invocation -> {
            AppInstance appInstance = invocation.getArgument(0);

            appInstance.setId(12L);
            appInstance.setSecretKeyHash(CREATED_KEY.hash());
            appInstance.setSecretKeyHint(CREATED_KEY.hint());

            return new AppInstanceService.CreatedAppInstance(appInstance, CREATED_KEY.key());
        });

        AppSecretKeyDTO result = appInstanceFacade.createAppInstance(
            APP_ID, 8L, Environment.STAGING, "Croatia", AppHostingTarget.SELF_HOSTED, ResourceVisibility.PRIVATE,
            AppUserAuthMode.NONE);

        ArgumentCaptor<AppInstance> appInstanceCaptor = ArgumentCaptor.forClass(AppInstance.class);

        verify(appInstanceService).create(appInstanceCaptor.capture());

        AppInstance createdAppInstance = appInstanceCaptor.getValue();

        assertThat(createdAppInstance.getOwnerUserId()).isEqualTo(42L);
        assertThat(createdAppInstance.getOwnerUserId()).isNotEqualTo(persistedApp().getOwnerUserId());
        assertThat(createdAppInstance.getAppId()).isEqualTo(APP_ID);
        assertThat(createdAppInstance.getWorkspaceId()).isEqualTo(8L);
        assertThat(createdAppInstance.getEnvironment()).isEqualTo(Environment.STAGING);
        assertThat(createdAppInstance.getName()).isEqualTo("Croatia");
        assertThat(createdAppInstance.getHostingTarget()).isEqualTo(AppHostingTarget.SELF_HOSTED);
        assertThat(createdAppInstance.getVisibility()).isEqualTo(ResourceVisibility.PRIVATE);

        AppInstanceDTO appInstanceDTO = result.appInstance();

        assertThat(result.secretKey()).isEqualTo(CREATED_KEY.key());
        assertThat(appInstanceDTO.ownerUserId()).isEqualTo(42L);
        assertThat(appInstanceDTO.secretKeyHint()).isEqualTo(CREATED_KEY.hint());
        assertThat(appInstanceDTO.app()
            .name()).isEqualTo("crm");

        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_INSTANCE_CREATED, APP_ID,
            Map.of("appInstanceId", 12L, "name", "Croatia", "workspaceId", 8L, "environment", "STAGING"));
    }

    @Test
    void testCreateRejectsOrganizationVisibility() {
        assertThatThrownBy(
            () -> appInstanceFacade.createAppInstance(
                APP_ID, WORKSPACE_ID, Environment.STAGING, "Croatia", AppHostingTarget.VERCEL,
                ResourceVisibility.ORGANIZATION, AppUserAuthMode.NONE))
                    .isInstanceOf(ConfigurationException.class)
                    .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(AppErrorType.APP_VISIBILITY_UNSUPPORTED.getErrorKey());

        verify(appInstanceService, never()).create(any());
    }

    @Test
    void testGetAppInstanceCarriesTheAppSummary() {
        App app = persistedApp();

        app.setDescription("Customer data");
        app.setCategoryId(3L);
        app.setIcon(new FileEntry("icon.png", "base64:///app_icons/icon.png"));
        app.setStatus(AppStatus.ACTIVE);

        when(appService.getApp(APP_ID)).thenReturn(app);

        AppInstanceDTO appInstanceDTO = appInstanceFacade.getAppInstance(APP_INSTANCE_ID);

        assertThat(appInstanceDTO.app())
            .isEqualTo(
                new AppInstanceDTO.AppSummary(
                    APP_ID, "crm", "Customer data", 3L, true, AppStatus.ACTIVE));
        assertThat(appInstanceDTO.secretKeyHint()).isEqualTo(String.valueOf(APP_INSTANCE_ID));
        assertThat(appInstanceDTO.workspaceId()).isEqualTo(WORKSPACE_ID);
    }

    @Test
    void testTerminateRemovesBindingsAndKeepsRow() {
        AppInstance terminated = persistedInstance(APP_INSTANCE_ID);

        terminated.setStatus(AppInstanceStatus.TERMINATED);

        when(appInstanceService.updateStatus(APP_INSTANCE_ID, AppInstanceStatus.TERMINATED)).thenReturn(terminated);

        AppInstanceDTO result = appInstanceFacade.updateAppInstanceStatus(
            APP_INSTANCE_ID, AppInstanceStatus.TERMINATED);

        assertThat(result.id()).isEqualTo(APP_INSTANCE_ID);
        assertThat(result.status()).isEqualTo(AppInstanceStatus.TERMINATED);

        verify(appConnectionService).deleteBindings(APP_INSTANCE_ID);
        verify(appWorkflowService).deleteBindings(APP_INSTANCE_ID);
        verify(appApiCollectionService).deleteBindings(APP_INSTANCE_ID);
        verify(appInstanceService, never()).delete(anyLong());
        verify(resourceGrantService, never()).deleteGrants(any(), anyLong());
        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_INSTANCE_TERMINATED, APP_ID, Map.of("appInstanceId", APP_INSTANCE_ID));
    }

    @Test
    void testBindingAConnectionToATerminatedInstanceIsRefused() {
        AppInstance appInstance = persistedInstance(APP_INSTANCE_ID);

        appInstance.setStatus(AppInstanceStatus.TERMINATED);

        when(appInstanceService.getAppInstance(APP_INSTANCE_ID)).thenReturn(appInstance);

        assertThatThrownBy(
            () -> appInstanceFacade.bindConnection(APP_INSTANCE_ID, "crm", 5L, List.of(), List.of(), false, List.of()))
                .isInstanceOf(ConfigurationException.class)
                .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AppErrorType.APP_INSTANCE_TERMINATED.getErrorKey());

        verify(appConnectionService, never()).bind(any(), any(), anyLong(), any(), any(), anyBoolean(), any());
    }

    @Test
    void testTerminateCancelsADeploymentInProgressAtTheProviderThenLocallyBeforeClearingTheSettings() {
        AppInstance terminated = persistedInstance(APP_INSTANCE_ID);

        terminated.setStatus(AppInstanceStatus.TERMINATED);

        AppDeployment building = new AppDeployment();

        building.setId(90L);
        building.setAppInstanceId(APP_INSTANCE_ID);
        building.setStatus(AppDeploymentStatus.BUILDING);
        building.setProviderDeploymentId("dpl_1");

        AppDeployment ready = new AppDeployment();

        ready.setId(89L);
        ready.setAppInstanceId(APP_INSTANCE_ID);
        ready.setStatus(AppDeploymentStatus.READY);
        ready.setProviderDeploymentId("dpl_0");

        when(appDeploymentService.getAppDeployments(eq(APP_INSTANCE_ID), anyInt()))
            .thenReturn(List.of(building, ready));
        when(appInstanceService.updateStatus(APP_INSTANCE_ID, AppInstanceStatus.TERMINATED)).thenReturn(terminated);

        appInstanceFacade.updateAppInstanceStatus(APP_INSTANCE_ID, AppInstanceStatus.TERMINATED);

        InOrder terminateOrder = inOrder(appDeploymentSynchronizer, appInstanceService);

        terminateOrder.verify(appDeploymentSynchronizer)
            .cancelAtProvider(building, "dpl_1");
        terminateOrder.verify(appDeploymentSynchronizer)
            .cancel(90L, "dpl_1");
        terminateOrder.verify(appInstanceService)
            .updateStatus(APP_INSTANCE_ID, AppInstanceStatus.TERMINATED);

        verify(appDeploymentSynchronizer, never()).cancelAtProvider(ready, "dpl_0");
        verify(appDeploymentSynchronizer, never()).cancel(89L, "dpl_0");
    }

    @Test
    void testSuspendCancelsNoDeployment() {
        when(appInstanceService.updateStatus(APP_INSTANCE_ID, AppInstanceStatus.SUSPENDED))
            .thenReturn(persistedInstance(APP_INSTANCE_ID));

        appInstanceFacade.updateAppInstanceStatus(APP_INSTANCE_ID, AppInstanceStatus.SUSPENDED);

        verifyNoInteractions(appDeploymentSynchronizer);
    }

    @Test
    void testSuspendAndActivateKeepTheBindings() {
        when(appInstanceService.updateStatus(APP_INSTANCE_ID, AppInstanceStatus.SUSPENDED))
            .thenReturn(persistedInstance(APP_INSTANCE_ID));
        when(appInstanceService.updateStatus(APP_INSTANCE_ID, AppInstanceStatus.ACTIVE))
            .thenReturn(persistedInstance(APP_INSTANCE_ID));

        appInstanceFacade.updateAppInstanceStatus(APP_INSTANCE_ID, AppInstanceStatus.SUSPENDED);
        appInstanceFacade.updateAppInstanceStatus(APP_INSTANCE_ID, AppInstanceStatus.ACTIVE);

        verifyNoInteractions(appConnectionService, appWorkflowService, appApiCollectionService);
        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_INSTANCE_SUSPENDED, APP_ID, Map.of("appInstanceId", APP_INSTANCE_ID));
        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_INSTANCE_ACTIVATED, APP_ID, Map.of("appInstanceId", APP_INSTANCE_ID));
    }

    @Test
    void testDeleteRemovesTheInstanceAndItsGrantsAndAudits() {
        appInstanceFacade.deleteAppInstance(APP_INSTANCE_ID);

        verify(appInstanceService).delete(APP_INSTANCE_ID);
        verify(resourceGrantService).deleteGrants("AppInstance", APP_INSTANCE_ID);
        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_INSTANCE_DELETED, APP_ID, Map.of("appInstanceId", APP_INSTANCE_ID));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUpdatePublishesTheChangedFields() {
        AppInstance saved = persistedInstance(APP_INSTANCE_ID);

        saved.setHostingTarget(AppHostingTarget.SELF_HOSTED);
        saved.setUrl("https://crm.test");

        when(appInstanceService.update(APP_INSTANCE_ID, null, AppHostingTarget.SELF_HOSTED, "https://crm.test"))
            .thenReturn(saved);

        AppInstanceDTO result = appInstanceFacade.updateAppInstance(
            APP_INSTANCE_ID, null, AppHostingTarget.SELF_HOSTED, "https://crm.test");

        assertThat(result.url()).isEqualTo("https://crm.test");

        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(appAuditPublisher).publish(eq(AppAuditEvent.APP_INSTANCE_UPDATED), eq(APP_ID), dataCaptor.capture());

        assertThat(dataCaptor.getValue()).containsOnly(
            Map.entry("appInstanceId", APP_INSTANCE_ID),
            Map.entry("changedFields", List.of("hostingTarget", "url")),
            Map.entry("hostingTarget", "SELF_HOSTED"));
    }

    @Test
    void testUpdateThatChangesNothingPublishesNoEvent() {
        when(appInstanceService.update(APP_INSTANCE_ID, "Production", null, null))
            .thenReturn(persistedInstance(APP_INSTANCE_ID));

        appInstanceFacade.updateAppInstance(APP_INSTANCE_ID, "Production", null, null);

        verify(appAuditPublisher, never()).publish(any(), anyLong(), anyMap());
    }

    @Test
    void testUpdateAppInstanceDoesNotAcceptVisibility() {
        for (Method method : AppInstanceFacade.class.getDeclaredMethods()) {
            if (method.getName()
                .equals("updateAppInstance")) {

                assertThat(method.getParameterTypes()).doesNotContain(ResourceVisibility.class);
            }
        }

        for (Method method : AppInstanceService.class.getDeclaredMethods()) {
            if (method.getName()
                .equals("update")) {

                assertThat(method.getParameterTypes()).doesNotContain(ResourceVisibility.class);
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUpdateConfigurationStoresItAndAuditsOnlyThatItChanged() {
        Map<String, Object> configuration = Map.of("apiToken", "secret-value", "country", "HR");
        AppInstance saved = persistedInstance(APP_INSTANCE_ID);

        saved.setConfiguration(configuration);

        when(appInstanceService.updateConfiguration(APP_INSTANCE_ID, configuration)).thenReturn(saved);

        AppInstanceDTO result = appInstanceFacade.updateAppInstanceConfiguration(APP_INSTANCE_ID, configuration);

        assertThat(result.configuration()).isEqualTo(configuration);

        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(appAuditPublisher).publish(eq(AppAuditEvent.APP_INSTANCE_UPDATED), eq(APP_ID), dataCaptor.capture());

        assertThat(dataCaptor.getValue()).containsOnly(
            Map.entry("appInstanceId", APP_INSTANCE_ID),
            Map.entry("changedFields", List.of("configuration")));
        assertThat(String.valueOf(dataCaptor.getValue())).doesNotContain("secret-value", "HR");
    }

    @Test
    void testUpdateConfigurationThatChangesNothingPublishesNoEvent() {
        AppInstance saved = persistedInstance(APP_INSTANCE_ID);

        saved.setConfiguration(Map.of("country", "HR"));

        when(appInstanceService.getAppInstance(APP_INSTANCE_ID)).thenReturn(saved);
        when(appInstanceService.updateConfiguration(APP_INSTANCE_ID, Map.of("country", "HR"))).thenReturn(saved);

        appInstanceFacade.updateAppInstanceConfiguration(APP_INSTANCE_ID, Map.of("country", "HR"));

        verify(appAuditPublisher, never()).publish(any(), anyLong(), anyMap());
    }

    @Test
    void testUpdateWhoseFieldUpdateFailsDoesNotWriteTheConfiguration() {
        when(appInstanceService.update(APP_INSTANCE_ID, "Taken", null, null))
            .thenThrow(new ConfigurationException("name taken", AppErrorType.APP_INSTANCE_NAME_ALREADY_EXISTS));

        assertThatThrownBy(
            () -> appInstanceFacade.updateAppInstance(APP_INSTANCE_ID, "Taken", null, null, Map.of("country", "DE")))
                .isInstanceOf(ConfigurationException.class);

        verify(appInstanceService, never()).updateConfiguration(anyLong(), any());
        verify(appAuditPublisher, never()).publish(any(), anyLong(), any());
    }

    @Test
    void testUpdateWithAnInvalidConfigurationWritesNothing() {
        assertThatThrownBy(
            () -> appInstanceFacade.updateAppInstance(APP_INSTANCE_ID, "Renamed", null, null, List.of("a")))
                .isInstanceOf(ConfigurationException.class);

        verify(appInstanceService, never()).update(anyLong(), any(), any(), any());
        verify(appInstanceService, never()).updateConfiguration(anyLong(), any());
        verify(appAuditPublisher, never()).publish(any(), anyLong(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUpdateWithFieldsAndAConfigurationAuditsOnceWithoutValues() {
        Map<String, Object> configuration = Map.of("apiToken", "secret-value");
        AppInstance renamed = persistedInstance(APP_INSTANCE_ID);

        renamed.setName("Renamed");

        AppInstance saved = persistedInstance(APP_INSTANCE_ID);

        saved.setName("Renamed");
        saved.setConfiguration(configuration);

        when(appInstanceService.update(APP_INSTANCE_ID, "Renamed", null, null)).thenReturn(renamed);
        when(appInstanceService.updateConfiguration(APP_INSTANCE_ID, configuration)).thenReturn(saved);

        AppInstanceDTO result = appInstanceFacade.updateAppInstance(
            APP_INSTANCE_ID, "Renamed", null, null, configuration);

        assertThat(result.configuration()).isEqualTo(configuration);

        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(appAuditPublisher).publish(eq(AppAuditEvent.APP_INSTANCE_UPDATED), eq(APP_ID), dataCaptor.capture());

        assertThat(dataCaptor.getValue()).isEqualTo(
            Map.of(
                "appInstanceId", APP_INSTANCE_ID, "changedFields", List.of("name", "configuration"), "name",
                "Renamed"));
        assertThat(String.valueOf(dataCaptor.getValue())).doesNotContain("secret-value");
    }

    @Test
    void testUpdateWithANullConfigurationKeepsIt() {
        when(appInstanceService.update(APP_INSTANCE_ID, "Renamed", null, null))
            .thenReturn(persistedInstance(APP_INSTANCE_ID));

        appInstanceFacade.updateAppInstance(APP_INSTANCE_ID, "Renamed", null, null, null);

        verify(appInstanceService, never()).updateConfiguration(anyLong(), any());
    }

    @Test
    void testConfigurationOverLimitIsRejected() {
        Map<String, Object> configuration = Map.of("notes", "a".repeat(64 * 1024));

        assertThatThrownBy(() -> appInstanceFacade.updateAppInstanceConfiguration(APP_INSTANCE_ID, configuration))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_INSTANCE_CONFIGURATION_TOO_LARGE.getErrorKey()));

        verify(appInstanceService, never()).updateConfiguration(anyLong(), anyMap());
        verifyNoInteractions(appAuditPublisher);
    }

    @Test
    void testConfigurationAtTheLimitIsAccepted() {
        String filler = "a".repeat(64 * 1024 - "{\"notes\":\"\"}".length());
        Map<String, Object> configuration = Map.of("notes", filler);

        when(appInstanceService.updateConfiguration(APP_INSTANCE_ID, configuration))
            .thenReturn(persistedInstance(APP_INSTANCE_ID));

        appInstanceFacade.updateAppInstanceConfiguration(APP_INSTANCE_ID, configuration);

        verify(appInstanceService).updateConfiguration(APP_INSTANCE_ID, configuration);
    }

    @Test
    void testConfigurationMustBeAnObject() {
        for (Object configuration : new Object[] {
            List.of("a", "b"), "text", 42, true, null
        }) {
            assertThatThrownBy(() -> appInstanceFacade.updateAppInstanceConfiguration(APP_INSTANCE_ID, configuration))
                .as(String.valueOf(configuration))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertThat(exception.getErrorKey())
                        .isEqualTo(AppErrorType.APP_INSTANCE_CONFIGURATION_INVALID.getErrorKey()));
        }

        verify(appInstanceService, never()).updateConfiguration(anyLong(), anyMap());
        verifyNoInteractions(appAuditPublisher);
    }

    @Test
    void testRotateReturnsTheNewKeyAndAudits() {
        when(appInstanceService.rotateSecretKey(APP_INSTANCE_ID))
            .thenReturn(new AppInstanceService.RotatedKey(rotatedInstance(), ROTATED_KEY.key()));

        AppSecretKeyDTO result = appInstanceFacade.rotateSecretKey(APP_INSTANCE_ID);

        AppInstanceDTO appInstanceDTO = result.appInstance();

        assertThat(result.secretKey()).isEqualTo(ROTATED_KEY.key());
        assertThat(appInstanceDTO.id()).isEqualTo(APP_INSTANCE_ID);

        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_SECRET_KEY_ROTATED, APP_ID, Map.of("appInstanceId", APP_INSTANCE_ID));
    }

    @Test
    void testBindReturnsTheBindingWithConnectionDetails() {
        when(appConnectionService.fetchBinding(APP_INSTANCE_ID, "crm")).thenReturn(Optional.empty());
        when(appConnectionService.bind(any(), any(), anyLong(), any(), any(), anyBoolean(), any()))
            .thenReturn(binding(10L, "crm", 5L));
        when(connectionService.getConnection(5L)).thenReturn(connection(5L, "Salesforce"));
        stubLatestComponentVersion("salesforce", 4);

        AppBindingDTO created = appInstanceFacade.bindConnection(
            APP_INSTANCE_ID, "crm", 5L, List.of("a"), List.of("t"), true, List.of("GET"));

        assertThat(created.appInstanceId()).isEqualTo(APP_INSTANCE_ID);
        assertThat(created.connectionName()).isEqualTo("Salesforce");
        assertThat(created.componentName()).isEqualTo("salesforce");
        assertThat(created.actionNames()).containsExactly("a");
        assertThat(created.toolNames()).containsExactly("t");
        assertThat(created.proxyAllowedMethods()).containsExactly("GET");
        assertThat(created.connectionVersion()).isEqualTo(1);
        assertThat(created.componentVersion()).isEqualTo(4);

        ArgumentCaptor<AppInstance> appInstanceCaptor = ArgumentCaptor.forClass(AppInstance.class);

        verify(appConnectionService).bind(
            appInstanceCaptor.capture(), eq("crm"), eq(5L), eq(List.of("a")), eq(List.of("t")), eq(true),
            eq(List.of("GET")));

        AppInstance boundAppInstance = appInstanceCaptor.getValue();

        assertThat(boundAppInstance.getId()).isEqualTo(APP_INSTANCE_ID);
    }

    @Test
    void testBindAuditsANewBindingKeyAsBound() {
        stubBind(Optional.empty(), 5L);

        appInstanceFacade.bindConnection(APP_INSTANCE_ID, "crm", 5L, List.of(), List.of(), false, List.of());

        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_CONNECTION_BOUND, APP_ID,
            Map.of("appInstanceId", APP_INSTANCE_ID, "bindingKey", "crm", "connectionId", 5L));
    }

    @Test
    void testBindAuditsAConnectionSwapAsBoundWithThePreviousConnection() {
        stubBind(Optional.of(binding(10L, "crm", 4L)), 5L);

        appInstanceFacade.bindConnection(APP_INSTANCE_ID, "crm", 5L, List.of(), List.of(), false, List.of());

        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_CONNECTION_BOUND, APP_ID,
            Map.of(
                "appInstanceId", APP_INSTANCE_ID, "bindingKey", "crm", "connectionId", 5L, "previousConnectionId",
                4L));
    }

    @Test
    void testBindAuditsRebindingTheSameConnectionAsOperationsChanged() {
        stubBind(Optional.of(binding(10L, "crm", 5L)), 5L);

        appInstanceFacade.bindConnection(APP_INSTANCE_ID, "crm", 5L, List.of(), List.of(), false, List.of());

        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_OPERATIONS_CHANGED, APP_ID,
            Map.of("appInstanceId", APP_INSTANCE_ID, "bindingKey", "crm", "connectionId", 5L));
    }

    @Test
    void testUpdateDeploymentSettingsStoresTheConnectionAndTheSettings() {
        AppInstance updatedAppInstance = persistedInstance(APP_INSTANCE_ID);

        updatedAppInstance.setDeploymentConnectionId(5L);
        updatedAppInstance.setDeploymentSettings(Map.of("projectId", "prj_1"));

        when(appConnectionService.isBindable(any(), eq(5L))).thenReturn(true);
        when(appInstanceService.updateDeploymentSettings(APP_INSTANCE_ID, 5L, Map.of("projectId", "prj_1")))
            .thenReturn(updatedAppInstance);

        appInstanceFacade.updateAppInstanceDeploymentSettings(APP_INSTANCE_ID, 5L, Map.of("projectId", "prj_1"));

        verify(appInstanceService).updateDeploymentSettings(APP_INSTANCE_ID, 5L, Map.of("projectId", "prj_1"));
        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_INSTANCE_UPDATED, APP_ID,
            Map.of(
                "appInstanceId", APP_INSTANCE_ID, "changedFields",
                List.of("deploymentConnection", "deploymentSettings"), "deploymentConnectionId", 5L));
    }

    @Test
    void testUpdateDeploymentSettingsReturnsTheStoredConnectionAndSettings() {
        AppInstance updatedAppInstance = persistedInstance(APP_INSTANCE_ID);

        updatedAppInstance.setDeploymentConnectionId(5L);
        updatedAppInstance.setDeploymentSettings(Map.of("projectId", "prj_1"));

        when(appConnectionService.isBindable(any(), eq(5L))).thenReturn(true);
        when(appInstanceService.updateDeploymentSettings(APP_INSTANCE_ID, 5L, Map.of("projectId", "prj_1")))
            .thenReturn(updatedAppInstance);

        AppInstanceDTO appInstanceDTO = appInstanceFacade.updateAppInstanceDeploymentSettings(
            APP_INSTANCE_ID, 5L, Map.of("projectId", "prj_1"));

        assertThat(appInstanceDTO.deploymentConnectionId()).isEqualTo(5L);
        assertThat(appInstanceDTO.deploymentSettings()).isEqualTo(Map.of("projectId", "prj_1"));
    }

    @Test
    void testUpdateDeploymentSettingsIsRefusedWhenTheProviderRejectsTheSettings() {
        AppDeploymentProvider appDeploymentProvider = mock(AppDeploymentProvider.class);

        when(appConnectionService.isBindable(any(), eq(5L))).thenReturn(true);
        when(appConnectionService.isConnectionOfComponent(5L, "vercel")).thenReturn(true);
        when(appDeploymentProvider.getDeploymentConnectionComponentName()).thenReturn("vercel");
        when(appDeploymentSynchronizer.fetchProvider(AppHostingTarget.VERCEL))
            .thenReturn(Optional.of(appDeploymentProvider));
        doThrow(new AppDeploymentSettingsException("The Vercel deployment setting 'projectId' is required"))
            .when(appDeploymentProvider)
            .validateDeploymentSettings(Map.of("teamId", "team_1"), Environment.PRODUCTION);

        assertThatThrownBy(() -> appInstanceFacade.updateAppInstanceDeploymentSettings(
            APP_INSTANCE_ID, 5L, Map.of("teamId", "team_1")))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> {
                        assertThat(exception.getErrorKey())
                            .isEqualTo(AppErrorType.APP_DEPLOYMENT_SETTINGS_INVALID.getErrorKey());
                        assertThat(exception.getMessage())
                            .isEqualTo("The Vercel deployment setting 'projectId' is required");
                    });

        verify(appInstanceService, never()).updateDeploymentSettings(anyLong(), any(), anyMap());
        verifyNoInteractions(appAuditPublisher);
    }

    @Test
    void testUpdateDeploymentSettingsRefusesAConnectionOfAnotherComponentThanTheProvider() {
        AppDeploymentProvider appDeploymentProvider = mock(AppDeploymentProvider.class);

        when(appConnectionService.isBindable(any(), eq(5L))).thenReturn(true);
        when(appConnectionService.isConnectionOfComponent(5L, "httpClient")).thenReturn(false);
        when(appDeploymentProvider.getDeploymentConnectionComponentName()).thenReturn("httpClient");
        when(appDeploymentSynchronizer.fetchProvider(AppHostingTarget.VERCEL))
            .thenReturn(Optional.of(appDeploymentProvider));

        assertThatThrownBy(() -> appInstanceFacade.updateAppInstanceDeploymentSettings(
            APP_INSTANCE_ID, 5L, Map.of("projectId", "prj_1")))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> {
                        assertThat(exception.getErrorKey())
                            .isEqualTo(AppErrorType.APP_DEPLOYMENT_SETTINGS_INVALID.getErrorKey());
                        assertThat(exception.getMessage()).contains("httpClient")
                            .doesNotContain("5");
                    });

        verify(appDeploymentProvider, never()).validateDeploymentSettings(any(), any());
        verify(appInstanceService, never()).updateDeploymentSettings(anyLong(), any(), anyMap());
    }

    @Test
    void testUpdateDeploymentSettingsValidatesWithTheProviderOfTheHostingTarget() {
        AppDeploymentProvider appDeploymentProvider = mock(AppDeploymentProvider.class);

        when(appConnectionService.isBindable(any(), eq(5L))).thenReturn(true);
        when(appConnectionService.isConnectionOfComponent(5L, "vercel")).thenReturn(true);
        when(appDeploymentProvider.getDeploymentConnectionComponentName()).thenReturn("vercel");
        when(appDeploymentSynchronizer.fetchProvider(AppHostingTarget.VERCEL))
            .thenReturn(Optional.of(appDeploymentProvider));
        when(appInstanceService.updateDeploymentSettings(APP_INSTANCE_ID, 5L, Map.of("projectId", "prj_1")))
            .thenReturn(persistedInstance(APP_INSTANCE_ID));

        appInstanceFacade.updateAppInstanceDeploymentSettings(APP_INSTANCE_ID, 5L, Map.of("projectId", "prj_1"));

        verify(appDeploymentProvider).validateDeploymentSettings(Map.of("projectId", "prj_1"), Environment.PRODUCTION);
        verify(appInstanceService).updateDeploymentSettings(APP_INSTANCE_ID, 5L, Map.of("projectId", "prj_1"));
    }

    @Test
    void testUpdateDeploymentSettingsWithoutAProviderForTheHostingTargetIsNotValidated() {
        when(appConnectionService.isBindable(any(), eq(5L))).thenReturn(true);
        when(appDeploymentSynchronizer.fetchProvider(AppHostingTarget.VERCEL)).thenReturn(Optional.empty());
        when(appInstanceService.updateDeploymentSettings(APP_INSTANCE_ID, 5L, Map.of("anything", "goes")))
            .thenReturn(persistedInstance(APP_INSTANCE_ID));

        appInstanceFacade.updateAppInstanceDeploymentSettings(APP_INSTANCE_ID, 5L, Map.of("anything", "goes"));

        verify(appInstanceService).updateDeploymentSettings(APP_INSTANCE_ID, 5L, Map.of("anything", "goes"));
    }

    @Test
    void testUpdateDeploymentSettingsWithNoConnectionAndNoSettingsClearsBoth() {
        AppInstance configuredAppInstance = persistedInstance(APP_INSTANCE_ID);

        configuredAppInstance.setDeploymentConnectionId(5L);
        configuredAppInstance.setDeploymentSettings(Map.of("projectId", "prj_1"));

        when(appInstanceService.getAppInstance(APP_INSTANCE_ID)).thenReturn(configuredAppInstance);
        when(appInstanceService.updateDeploymentSettings(APP_INSTANCE_ID, null, Map.of()))
            .thenReturn(persistedInstance(APP_INSTANCE_ID));

        appInstanceFacade.updateAppInstanceDeploymentSettings(APP_INSTANCE_ID, null, null);

        verify(appConnectionService, never()).isBindable(any(), anyLong());
        verifyNoInteractions(appDeploymentSynchronizer);
        verify(appDeploymentService).checkNoDeploymentInProgress(APP_INSTANCE_ID);
        verify(appInstanceService).updateDeploymentSettings(APP_INSTANCE_ID, null, Map.of());
        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_INSTANCE_UPDATED, APP_ID,
            Map.of(
                "appInstanceId", APP_INSTANCE_ID, "changedFields",
                List.of("deploymentConnection", "deploymentSettings")));
    }

    @Test
    void testUpdateDeploymentSettingsWithNoConnectionAndEmptySettingsClearsBoth() {
        when(appInstanceService.updateDeploymentSettings(APP_INSTANCE_ID, null, Map.of()))
            .thenReturn(persistedInstance(APP_INSTANCE_ID));

        appInstanceFacade.updateAppInstanceDeploymentSettings(APP_INSTANCE_ID, null, Map.of());

        verify(appInstanceService).updateDeploymentSettings(APP_INSTANCE_ID, null, Map.of());
        verifyNoInteractions(appDeploymentSynchronizer);
    }

    @Test
    void testUpdateDeploymentSettingsClearingIsRefusedWhileADeploymentIsInProgress() {
        doThrow(new ConfigurationException("in progress", AppErrorType.APP_DEPLOYMENT_IN_PROGRESS))
            .when(appDeploymentService)
            .checkNoDeploymentInProgress(APP_INSTANCE_ID);

        assertThatThrownBy(() -> appInstanceFacade.updateAppInstanceDeploymentSettings(APP_INSTANCE_ID, null, null))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_DEPLOYMENT_IN_PROGRESS.getErrorKey()));

        verify(appInstanceService, never()).updateDeploymentSettings(anyLong(), any(), anyMap());
    }

    @Test
    void testUpdateDeploymentSettingsWithNoConnectionButSettingsIsRefused() {
        assertThatThrownBy(() -> appInstanceFacade.updateAppInstanceDeploymentSettings(
            APP_INSTANCE_ID, null, Map.of("projectId", "prj_1")))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertThat(exception.getErrorKey())
                        .isEqualTo(AppErrorType.APP_DEPLOYMENT_SETTINGS_INVALID.getErrorKey()));

        verify(appInstanceService, never()).updateDeploymentSettings(anyLong(), any(), anyMap());
        verifyNoInteractions(appAuditPublisher);
    }

    @Test
    void testUpdateDeploymentSettingsRefusesAConnectionThatCannotBeBound() {
        when(appConnectionService.isBindable(any(), eq(5L))).thenReturn(false);

        assertThatThrownBy(() -> appInstanceFacade.updateAppInstanceDeploymentSettings(
            APP_INSTANCE_ID, 5L, Map.of("projectId", "prj_1")))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertThat(exception.getErrorKey())
                        .isEqualTo(AppErrorType.APP_CONNECTION_NOT_BINDABLE.getErrorKey()));

        verify(appInstanceService, never()).updateDeploymentSettings(anyLong(), any(), anyMap());
        verifyNoInteractions(appAuditPublisher);
    }

    @Test
    void testUpdateDeploymentSettingsIsRefusedWhileADeploymentIsInProgress() {
        when(appConnectionService.isBindable(any(), eq(5L))).thenReturn(true);
        doThrow(new ConfigurationException("in progress", AppErrorType.APP_DEPLOYMENT_IN_PROGRESS))
            .when(appDeploymentService)
            .checkNoDeploymentInProgress(APP_INSTANCE_ID);

        assertThatThrownBy(() -> appInstanceFacade.updateAppInstanceDeploymentSettings(
            APP_INSTANCE_ID, 5L, Map.of("projectId", "prj_1")))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertThat(exception.getErrorKey())
                        .isEqualTo(AppErrorType.APP_DEPLOYMENT_IN_PROGRESS.getErrorKey()));

        verify(appInstanceService, never()).updateDeploymentSettings(anyLong(), any(), anyMap());
        verifyNoInteractions(appAuditPublisher);
    }

    @Test
    void testUpdateDeploymentSettingsChecksForADeploymentInProgressBeforeWriting() {
        when(appConnectionService.isBindable(any(), eq(5L))).thenReturn(true);
        when(appInstanceService.updateDeploymentSettings(APP_INSTANCE_ID, 5L, Map.of("projectId", "prj_1")))
            .thenReturn(persistedInstance(APP_INSTANCE_ID));

        appInstanceFacade.updateAppInstanceDeploymentSettings(APP_INSTANCE_ID, 5L, Map.of("projectId", "prj_1"));

        InOrder inOrder = inOrder(appDeploymentService, appInstanceService);

        inOrder.verify(appDeploymentService)
            .checkNoDeploymentInProgress(APP_INSTANCE_ID);
        inOrder.verify(appInstanceService)
            .updateDeploymentSettings(APP_INSTANCE_ID, 5L, Map.of("projectId", "prj_1"));
    }

    @Test
    void testUpdateDeploymentSettingsRefusesTooLargeSettings() {
        Map<String, ?> tooLargeSettings = Map.of("projectId", "x".repeat(70 * 1024));

        assertThatThrownBy(() -> appInstanceFacade.updateAppInstanceDeploymentSettings(
            APP_INSTANCE_ID, 5L, tooLargeSettings))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertThat(exception.getErrorKey())
                        .isEqualTo(AppErrorType.APP_INSTANCE_CONFIGURATION_TOO_LARGE.getErrorKey()));

        verify(appInstanceService, never()).updateDeploymentSettings(anyLong(), any(), anyMap());
    }

    @Test
    void testUnbindAuditsWhenABindingWasDeleted() {
        when(appConnectionService.unbind(APP_INSTANCE_ID, "crm")).thenReturn(true);

        appInstanceFacade.unbindConnection(APP_INSTANCE_ID, "crm");

        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_CONNECTION_UNBOUND, APP_ID,
            Map.of("appInstanceId", APP_INSTANCE_ID, "bindingKey", "crm"));
    }

    @Test
    void testUnbindOfAMissingBindingKeyDoesNotAudit() {
        when(appConnectionService.unbind(APP_INSTANCE_ID, "missing")).thenReturn(false);

        appInstanceFacade.unbindConnection(APP_INSTANCE_ID, "missing");

        verifyNoInteractions(appAuditPublisher);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetBindableConnectionsFiltersByVisibilityInTheInstanceWorkspace() {
        AppInstance foreignAppInstance = persistedInstance(APP_INSTANCE_ID);

        foreignAppInstance.setWorkspaceId(8L);

        when(appInstanceService.getAppInstance(APP_INSTANCE_ID)).thenReturn(foreignAppInstance);

        Connection visible = connection(5L, "Mine");
        Connection hidden = connection(6L, "Colleague private");

        hidden.setVisibility(ResourceVisibility.PRIVATE);
        hidden.setCreatedBy("someone-else");

        when(appConnectionService.getBindableConnections(foreignAppInstance)).thenReturn(List.of(visible, hidden));
        when(resourceVisibilityResolver.filterVisibleIds(eq("Connection"), eq(8L), anyCollection()))
            .thenReturn(Set.of(5L));
        stubLatestComponentVersion("salesforce", 3);

        assertThat(appInstanceFacade.getBindableConnections(APP_INSTANCE_ID, null))
            .containsExactly(new AppBindableConnectionDTO(5L, "Mine", "salesforce", 1, 3, false));

        ArgumentCaptor<Collection<VisibilityRecord>> candidates = ArgumentCaptor.forClass(Collection.class);

        verify(resourceVisibilityResolver).filterVisibleIds(eq("Connection"), eq(8L), candidates.capture());

        assertThat(candidates.getValue())
            .contains(new VisibilityRecord(6L, ResourceVisibility.PRIVATE, "someone-else"));
    }

    @Test
    void testGetBindableConnectionsLeavesOutConnectionsWhoseComponentCannotBeResolved() {
        Connection resolvable = connection(5L, "Salesforce");
        Connection orphaned = connection(6L, "Legacy");

        orphaned.setComponentName("deleted-component");

        when(appConnectionService.getBindableConnections(any())).thenReturn(List.of(orphaned, resolvable));
        when(resourceVisibilityResolver.filterVisibleIds(eq("Connection"), eq(WORKSPACE_ID), anyCollection()))
            .thenReturn(Set.of(5L, 6L));
        when(componentDefinitionService.fetchComponentDefinition("deleted-component", null))
            .thenReturn(Optional.empty());
        stubLatestComponentVersion("salesforce", 3);

        assertThat(appInstanceFacade.getBindableConnections(APP_INSTANCE_ID, null))
            .containsExactly(new AppBindableConnectionDTO(5L, "Salesforce", "salesforce", 1, 3, false));
    }

    @Test
    void testGetBindableConnectionsMarksOnlyValidConnectionsTheCallerMayUseAsUsable() {
        Connection usable = connection(5L, "Usable");
        Connection notUsable = connection(6L, "Not mine to use");
        Connection invalid = connection(8L, "Expired");

        usable.setCredentialStatus(CredentialStatus.VALID);
        notUsable.setCredentialStatus(CredentialStatus.VALID);
        invalid.setCredentialStatus(CredentialStatus.INVALID);

        when(appConnectionService.getBindableConnections(any())).thenReturn(List.of(usable, notUsable, invalid));
        when(resourceVisibilityResolver.filterVisibleIds(eq("Connection"), eq(WORKSPACE_ID), anyCollection()))
            .thenReturn(Set.of(5L, 6L, 8L));
        when(permissionService.hasResourceScope(5L, "Connection", "CONNECTION_EDIT")).thenReturn(true);
        when(permissionService.hasResourceScope(6L, "Connection", "CONNECTION_EDIT")).thenReturn(false);
        lenient().when(permissionService.hasResourceScope(8L, "Connection", "CONNECTION_EDIT"))
            .thenReturn(true);
        stubLatestComponentVersion("salesforce", 3);

        assertThat(appInstanceFacade.getBindableConnections(APP_INSTANCE_ID, null))
            .extracting(AppBindableConnectionDTO::id, AppBindableConnectionDTO::usable)
            .containsExactly(tuple(5L, true), tuple(6L, false), tuple(8L, false));
    }

    @Test
    void testGetBindableConnectionsFiltersByComponentName() {
        Connection salesforce = connection(5L, "Salesforce");
        Connection slack = connection(6L, "Slack");

        slack.setComponentName("slack");

        when(appConnectionService.getBindableConnections(any())).thenReturn(List.of(salesforce, slack));
        when(resourceVisibilityResolver.filterVisibleIds(eq("Connection"), eq(WORKSPACE_ID), anyCollection()))
            .thenReturn(Set.of(5L, 6L));
        stubLatestComponentVersion("slack", 2);

        assertThat(appInstanceFacade.getBindableConnections(APP_INSTANCE_ID, "slack"))
            .extracting(AppBindableConnectionDTO::id)
            .containsExactly(6L);
    }

    @Test
    void testGetBindingsMapsABindingToAnUnresolvableComponentWithNoComponentVersion() {
        Connection orphaned = connection(6L, "Legacy");

        orphaned.setComponentName("deleted-component");

        when(appConnectionService.getBindings(APP_INSTANCE_ID))
            .thenReturn(List.of(binding(10L, "crm", 5L), binding(11L, "legacy", 6L)));
        when(connectionService.getConnection(5L)).thenReturn(connection(5L, "Salesforce"));
        when(connectionService.getConnection(6L)).thenReturn(orphaned);
        when(componentDefinitionService.fetchComponentDefinition("deleted-component", null))
            .thenReturn(Optional.empty());
        stubLatestComponentVersion("salesforce", 3);

        List<AppBindingDTO> bindings = appInstanceFacade.getBindings(APP_INSTANCE_ID);

        assertThat(bindings).extracting(AppBindingDTO::bindingKey)
            .containsExactly("crm", "legacy");
        assertThat(bindings).extracting(AppBindingDTO::componentVersion)
            .containsExactly(3, null);
        assertThat(bindings).extracting(AppBindingDTO::appInstanceId)
            .containsOnly(APP_INSTANCE_ID);
    }

    private void stubBind(Optional<AppConnection> existingBinding, long connectionId) {
        when(appConnectionService.fetchBinding(APP_INSTANCE_ID, "crm")).thenReturn(existingBinding);
        when(appConnectionService.bind(any(), any(), anyLong(), any(), any(), anyBoolean(), any()))
            .thenReturn(binding(10L, "crm", connectionId));
        when(connectionService.getConnection(connectionId)).thenReturn(connection(connectionId, "Salesforce"));
        stubLatestComponentVersion("salesforce", 1);
    }

    private void stubLatestComponentVersion(String componentName, int version) {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getVersion()).thenReturn(version);
        when(componentDefinitionService.fetchComponentDefinition(componentName, null))
            .thenReturn(Optional.of(componentDefinition));
    }

    private static AppConnection binding(long id, String bindingKey, long connectionId) {
        AppConnection appConnection = new AppConnection();

        appConnection.setId(id);
        appConnection.setAppInstanceId(APP_INSTANCE_ID);
        appConnection.setBindingKey(bindingKey);
        appConnection.setConnectionId(connectionId);
        appConnection.setOperations(List.of("a"), List.of("t"));
        appConnection.setProxyEnabled(true);
        appConnection.setProxyAllowedMethodList(List.of("GET"));

        return appConnection;
    }

    private static Connection connection(long id, String name) {
        Connection connection = new Connection();

        connection.setId(id);
        connection.setName(name);
        connection.setComponentName("salesforce");
        connection.setConnectionVersion(1);
        connection.setCreatedBy("admin");
        connection.setVisibility(ResourceVisibility.WORKSPACE);
        connection.setParameters(Map.of("access_token", CONNECTION_CREDENTIAL));

        return connection;
    }

    @Test
    void testNewInstanceDefaultsToRequired() {
        stubCreate();

        AppSecretKeyDTO result = appInstanceFacade.createAppInstance(
            APP_ID, WORKSPACE_ID, Environment.PRODUCTION, "Croatia", AppHostingTarget.VERCEL,
            ResourceVisibility.WORKSPACE, null);

        assertThat(captureCreatedInstance().getUserAuthMode()).isEqualTo(AppUserAuthMode.REQUIRED);
        assertThat(result.appInstance()
            .userAuthMode()).isEqualTo(AppUserAuthMode.REQUIRED);

        verify(registeredClientService, never()).registerClient(any());
    }

    @Test
    void testExplicitNoneIsKeptOnCreate() {
        stubCreate();

        appInstanceFacade.createAppInstance(
            APP_ID, WORKSPACE_ID, Environment.PRODUCTION, "Croatia", AppHostingTarget.VERCEL,
            ResourceVisibility.WORKSPACE, AppUserAuthMode.NONE);

        assertThat(captureCreatedInstance().getUserAuthMode()).isEqualTo(AppUserAuthMode.NONE);

        verifyNoInteractions(registeredClientService);
    }

    @Test
    void testNewInstanceDefaultsToNoneWithAuthorizationServerOff() {
        enableAuthorizationServer(false);
        stubCreate();

        AppSecretKeyDTO result = appInstanceFacade.createAppInstance(
            APP_ID, WORKSPACE_ID, Environment.PRODUCTION, "Croatia", AppHostingTarget.VERCEL,
            ResourceVisibility.WORKSPACE, null);

        assertThat(captureCreatedInstance().getUserAuthMode()).isEqualTo(AppUserAuthMode.NONE);
        assertThat(result.appInstance()
            .userAuthMode()).isEqualTo(AppUserAuthMode.NONE);
    }

    @Test
    void testExplicitRequiredCreateWithAuthorizationServerOffIsRefusedNamingTheProperty() {
        enableAuthorizationServer(false);

        assertThatThrownBy(
            () -> appInstanceFacade.createAppInstance(
                APP_ID, WORKSPACE_ID, Environment.PRODUCTION, "Croatia", AppHostingTarget.VERCEL,
                ResourceVisibility.WORKSPACE, AppUserAuthMode.REQUIRED))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("bytechef.oauth2.authorization-server.enabled")
                    .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(AppErrorType.APP_USER_AUTH_UNAVAILABLE.getErrorKey());

        verify(appInstanceService, never()).create(any());
    }

    @Test
    void testExplicitNoneCreateWorksWithAuthorizationServerOff() {
        enableAuthorizationServer(false);
        stubCreate();

        appInstanceFacade.createAppInstance(
            APP_ID, WORKSPACE_ID, Environment.PRODUCTION, "Croatia", AppHostingTarget.VERCEL,
            ResourceVisibility.WORKSPACE, AppUserAuthMode.NONE);

        assertThat(captureCreatedInstance().getUserAuthMode()).isEqualTo(AppUserAuthMode.NONE);
    }

    @Test
    void testRequiredInstanceWithoutUrlHasNoClientUntilUrlIsSet() {
        AppInstance appInstance = persistedInstance(APP_INSTANCE_ID);

        appInstance.setUserAuthMode(AppUserAuthMode.REQUIRED);

        when(appInstanceService.getAppInstance(APP_INSTANCE_ID)).thenReturn(appInstance);

        AppInstanceDTO withoutUrl = appInstanceFacade.getAppInstance(APP_INSTANCE_ID);

        assertThat(withoutUrl.userAuthMode()).isEqualTo(AppUserAuthMode.REQUIRED);
        assertThat(withoutUrl.url()).isNull();

        verifyNoInteractions(registeredClientService);

        AppInstance withUrl = persistedInstance(APP_INSTANCE_ID);

        withUrl.setUserAuthMode(AppUserAuthMode.REQUIRED);
        withUrl.setUrl("https://crm.example.com");

        when(appInstanceService.update(APP_INSTANCE_ID, null, null, "https://crm.example.com")).thenReturn(withUrl);
        when(appInstanceService.updateUserAuth(APP_INSTANCE_ID, AppUserAuthMode.REQUIRED, "app-instance-uuid"))
            .thenReturn(withUrl);

        appInstanceFacade.updateAppInstance(APP_INSTANCE_ID, null, null, "https://crm.example.com");

        ClientRegistrationRequest clientRegistrationRequest = captureRegisteredClient();

        assertThat(clientRegistrationRequest.clientId()).isEqualTo("app-instance-uuid");
        assertThat(clientRegistrationRequest.redirectUris()).containsExactly("https://crm.example.com/auth/callback");

        verify(appInstanceService).updateUserAuth(APP_INSTANCE_ID, AppUserAuthMode.REQUIRED, "app-instance-uuid");
    }

    @Test
    void testEnablingSignInRegistersClientWithCallbackOfInstanceUrl() {
        AppInstance appInstance = persistedInstance(APP_INSTANCE_ID);

        appInstance.setUrl("https://crm.example.com/");

        when(appInstanceService.getAppInstance(APP_INSTANCE_ID)).thenReturn(appInstance);
        when(appInstanceService.updateUserAuth(APP_INSTANCE_ID, AppUserAuthMode.REQUIRED, "app-instance-uuid"))
            .thenReturn(appInstance);

        appInstanceFacade.updateAppInstanceUserAuth(APP_INSTANCE_ID, AppUserAuthMode.REQUIRED);

        ClientRegistrationRequest clientRegistrationRequest = captureRegisteredClient();

        assertThat(clientRegistrationRequest.clientId()).isEqualTo("app-instance-uuid");
        assertThat(clientRegistrationRequest.clientName()).isEqualTo("crm (Production)");
        assertThat(clientRegistrationRequest.redirectUris()).containsExactly("https://crm.example.com/auth/callback");
        assertThat(clientRegistrationRequest.scopes()).containsExactlyInAnyOrder("openid", "profile", "app:use");
        assertThat(clientRegistrationRequest.requireAuthorizationConsent()).isFalse();

        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_USER_AUTH_CHANGED, APP_ID,
            Map.of("appInstanceId", APP_INSTANCE_ID, "userAuthMode", "REQUIRED", "previousUserAuthMode", "NONE"));
    }

    @Test
    void testLocalhostCallbackOnlyForDevelopment() {
        AppInstance developmentInstance = persistedInstance(APP_INSTANCE_ID);
        AppInstance productionInstance = persistedInstance(12L);

        developmentInstance.setEnvironment(Environment.DEVELOPMENT);
        developmentInstance.setUrl("https://dev.example.com");
        productionInstance.setUuid("production-uuid");
        productionInstance.setUrl("https://crm.example.com");

        when(appInstanceService.getAppInstance(APP_INSTANCE_ID)).thenReturn(developmentInstance);
        when(appInstanceService.getAppInstance(12L)).thenReturn(productionInstance);
        when(appInstanceService.updateUserAuth(eq(APP_INSTANCE_ID), eq(AppUserAuthMode.OPTIONAL), any()))
            .thenReturn(developmentInstance);
        when(appInstanceService.updateUserAuth(eq(12L), eq(AppUserAuthMode.OPTIONAL), any()))
            .thenReturn(productionInstance);

        appInstanceFacade.updateAppInstanceUserAuth(APP_INSTANCE_ID, AppUserAuthMode.OPTIONAL);
        appInstanceFacade.updateAppInstanceUserAuth(12L, AppUserAuthMode.OPTIONAL);

        ArgumentCaptor<ClientRegistrationRequest> clientRegistrationRequestCaptor =
            ArgumentCaptor.forClass(ClientRegistrationRequest.class);

        verify(registeredClientService, times(2))
            .registerClient(clientRegistrationRequestCaptor.capture());

        List<ClientRegistrationRequest> clientRegistrationRequests = clientRegistrationRequestCaptor.getAllValues();

        assertThat(clientRegistrationRequests.getFirst()
            .redirectUris())
                .containsExactly("https://dev.example.com/auth/callback", "http://localhost:3000/auth/callback");
        assertThat(clientRegistrationRequests.getLast()
            .redirectUris()).containsExactly("https://crm.example.com/auth/callback");
    }

    @Test
    void testEnablingWithoutAuthorizationServerIsRefused() {
        enableAuthorizationServer(false);

        for (AppUserAuthMode userAuthMode : List.of(AppUserAuthMode.OPTIONAL, AppUserAuthMode.REQUIRED)) {
            assertThatThrownBy(() -> appInstanceFacade.updateAppInstanceUserAuth(APP_INSTANCE_ID, userAuthMode))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("bytechef.oauth2.authorization-server.enabled")
                .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AppErrorType.APP_USER_AUTH_UNAVAILABLE.getErrorKey());
        }

        verify(appInstanceService, never()).updateUserAuth(anyLong(), any(), any());
    }

    @Test
    void testDisablingSignInDeletesTheClient() {
        AppInstance appInstance = registeredInstance();

        when(appInstanceService.getAppInstance(APP_INSTANCE_ID)).thenReturn(appInstance);
        when(appInstanceService.updateUserAuth(APP_INSTANCE_ID, AppUserAuthMode.NONE, null)).thenReturn(appInstance);

        appInstanceFacade.updateAppInstanceUserAuth(APP_INSTANCE_ID, AppUserAuthMode.NONE);

        verify(registeredClientService).deleteClient("app-instance-uuid");
        verify(appInstanceService).updateUserAuth(APP_INSTANCE_ID, AppUserAuthMode.NONE, null);
        verify(registeredClientService, never()).registerClient(any());
    }

    @Test
    void testSettingTheSameModeChangesNothing() {
        AppInstance appInstance = registeredInstance();

        when(appInstanceService.getAppInstance(APP_INSTANCE_ID)).thenReturn(appInstance);

        appInstanceFacade.updateAppInstanceUserAuth(APP_INSTANCE_ID, AppUserAuthMode.REQUIRED);

        verifyNoInteractions(registeredClientService);
        verify(appInstanceService, never()).updateUserAuth(anyLong(), any(), any());
        verify(appAuditPublisher, never()).publish(any(), anyLong(), anyMap());
    }

    @Test
    void testUserAuthOfATerminatedInstanceIsRefused() {
        AppInstance appInstance = persistedInstance(APP_INSTANCE_ID);

        appInstance.setStatus(AppInstanceStatus.TERMINATED);

        when(appInstanceService.getAppInstance(APP_INSTANCE_ID)).thenReturn(appInstance);

        assertThatThrownBy(
            () -> appInstanceFacade.updateAppInstanceUserAuth(APP_INSTANCE_ID, AppUserAuthMode.REQUIRED))
                .isInstanceOf(ConfigurationException.class)
                .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AppErrorType.APP_INSTANCE_TERMINATED.getErrorKey());

        verifyNoInteractions(registeredClientService);
    }

    @Test
    void testChangingTheUrlUpdatesTheRegisteredClient() {
        AppInstance appInstance = registeredInstance();
        AppInstance updatedAppInstance = registeredInstance();

        updatedAppInstance.setUrl("https://new.example.com");

        when(appInstanceService.getAppInstance(APP_INSTANCE_ID)).thenReturn(appInstance);
        when(appInstanceService.update(APP_INSTANCE_ID, null, null, "https://new.example.com"))
            .thenReturn(updatedAppInstance);

        appInstanceFacade.updateAppInstance(APP_INSTANCE_ID, null, null, "https://new.example.com");

        ArgumentCaptor<ClientRegistrationRequest> clientRegistrationRequestCaptor =
            ArgumentCaptor.forClass(ClientRegistrationRequest.class);

        verify(registeredClientService).updateClient(clientRegistrationRequestCaptor.capture());

        ClientRegistrationRequest clientRegistrationRequest = clientRegistrationRequestCaptor.getValue();

        assertThat(clientRegistrationRequest.clientId()).isEqualTo("app-instance-uuid");
        assertThat(clientRegistrationRequest.redirectUris()).containsExactly("https://new.example.com/auth/callback");
        assertThat(clientRegistrationRequest.scopes()).containsExactlyInAnyOrder("openid", "profile", "app:use");

        verify(registeredClientService, never()).registerClient(any());
    }

    @Test
    void testAPlainHttpUrlOutsideDevelopmentIsRefusedWhileSignInIsOn() {
        AppInstance appInstance = registeredInstance();
        AppInstance updatedAppInstance = registeredInstance();

        updatedAppInstance.setUrl("http://crm.example.com");

        when(appInstanceService.getAppInstance(APP_INSTANCE_ID)).thenReturn(appInstance);
        when(appInstanceService.update(APP_INSTANCE_ID, null, null, "http://crm.example.com"))
            .thenReturn(updatedAppInstance);

        assertThatThrownBy(
            () -> appInstanceFacade.updateAppInstance(APP_INSTANCE_ID, null, null, "http://crm.example.com"))
                .isInstanceOf(ConfigurationException.class)
                .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AppErrorType.APP_INSTANCE_URL_INVALID.getErrorKey());

        verify(registeredClientService, never()).updateClient(any());
    }

    @Test
    void testAPlainHttpUrlInDevelopmentRegistersTheClient() {
        AppInstance appInstance = registeredInstance();
        AppInstance updatedAppInstance = registeredInstance();

        appInstance.setEnvironment(Environment.DEVELOPMENT);
        updatedAppInstance.setEnvironment(Environment.DEVELOPMENT);
        updatedAppInstance.setUrl("http://localhost:3000");

        when(appInstanceService.getAppInstance(APP_INSTANCE_ID)).thenReturn(appInstance);
        when(appInstanceService.update(APP_INSTANCE_ID, null, null, "http://localhost:3000"))
            .thenReturn(updatedAppInstance);

        appInstanceFacade.updateAppInstance(APP_INSTANCE_ID, null, null, "http://localhost:3000");

        verify(registeredClientService).updateClient(any());
    }

    @Test
    void testClearingTheUrlDeletesTheRegisteredClient() {
        AppInstance appInstance = registeredInstance();
        AppInstance updatedAppInstance = registeredInstance();

        updatedAppInstance.setUrl(null);

        when(appInstanceService.getAppInstance(APP_INSTANCE_ID)).thenReturn(appInstance);
        when(appInstanceService.update(APP_INSTANCE_ID, null, null, "")).thenReturn(updatedAppInstance);
        when(appInstanceService.updateUserAuth(APP_INSTANCE_ID, AppUserAuthMode.REQUIRED, null))
            .thenReturn(updatedAppInstance);

        appInstanceFacade.updateAppInstance(APP_INSTANCE_ID, null, null, "");

        verify(registeredClientService).deleteClient("app-instance-uuid");
        verify(appInstanceService).updateUserAuth(APP_INSTANCE_ID, AppUserAuthMode.REQUIRED, null);
    }

    @Test
    void testUrlChangeOfANoneInstanceRegistersNothing() {
        AppInstance updatedAppInstance = persistedInstance(APP_INSTANCE_ID);

        updatedAppInstance.setUrl("https://crm.example.com");

        when(appInstanceService.update(APP_INSTANCE_ID, null, null, "https://crm.example.com"))
            .thenReturn(updatedAppInstance);

        appInstanceFacade.updateAppInstance(APP_INSTANCE_ID, null, null, "https://crm.example.com");

        verifyNoInteractions(registeredClientService);
    }

    @Test
    void testSuspendRevokesGrants() {
        AppInstance appInstance = registeredInstance();

        when(appInstanceService.updateStatus(APP_INSTANCE_ID, AppInstanceStatus.SUSPENDED)).thenReturn(appInstance);
        when(registeredClientService.fetchRegisteredClientId("app-instance-uuid"))
            .thenReturn(Optional.of("registered-id"));

        appInstanceFacade.updateAppInstanceStatus(APP_INSTANCE_ID, AppInstanceStatus.SUSPENDED);

        verify(authorizationGrantService).revokeGrants("registered-id");
        verify(registeredClientService, never()).deleteClient(any());
    }

    @Test
    void testSuspendOfAnInstanceWithoutGrantsSucceeds() {
        AppInstance appInstance = registeredInstance();

        when(appInstanceService.updateStatus(APP_INSTANCE_ID, AppInstanceStatus.SUSPENDED)).thenReturn(appInstance);
        when(registeredClientService.fetchRegisteredClientId("app-instance-uuid"))
            .thenReturn(Optional.of("registered-id"));
        doThrow(
            new ConfigurationException("none", RegisteredClientErrorType.GRANT_NOT_FOUND))
                .when(authorizationGrantService)
                .revokeGrants("registered-id");

        AppInstanceDTO result = appInstanceFacade.updateAppInstanceStatus(
            APP_INSTANCE_ID, AppInstanceStatus.SUSPENDED);

        assertThat(result.id()).isEqualTo(APP_INSTANCE_ID);
    }

    @Test
    void testTerminateDeletesTheClient() {
        AppInstance appInstance = registeredInstance();

        appInstance.setStatus(AppInstanceStatus.TERMINATED);

        when(appInstanceService.updateStatus(APP_INSTANCE_ID, AppInstanceStatus.TERMINATED)).thenReturn(appInstance);
        when(appInstanceService.updateUserAuth(APP_INSTANCE_ID, AppUserAuthMode.REQUIRED, null))
            .thenReturn(appInstance);

        appInstanceFacade.updateAppInstanceStatus(APP_INSTANCE_ID, AppInstanceStatus.TERMINATED);

        verify(registeredClientService).deleteClient("app-instance-uuid");
        verify(appInstanceService).updateUserAuth(APP_INSTANCE_ID, AppUserAuthMode.REQUIRED, null);
    }

    @Test
    void testDeleteDeletesTheClient() {
        when(appInstanceService.getAppInstance(APP_INSTANCE_ID)).thenReturn(registeredInstance());

        appInstanceFacade.deleteAppInstance(APP_INSTANCE_ID);

        verify(registeredClientService).deleteClient("app-instance-uuid");
        verify(appInstanceService).delete(APP_INSTANCE_ID);
    }

    @Test
    void testRotateDoesNotRevokeUserTokens() {
        when(appInstanceService.rotateSecretKey(APP_INSTANCE_ID))
            .thenReturn(new AppInstanceService.RotatedKey(registeredInstance(), ROTATED_KEY.key()));

        appInstanceFacade.rotateSecretKey(APP_INSTANCE_ID);

        verifyNoInteractions(registeredClientService, authorizationGrantService);
    }

    private ClientRegistrationRequest captureRegisteredClient() {
        ArgumentCaptor<ClientRegistrationRequest> clientRegistrationRequestCaptor =
            ArgumentCaptor.forClass(ClientRegistrationRequest.class);

        verify(registeredClientService).registerClient(clientRegistrationRequestCaptor.capture());

        return clientRegistrationRequestCaptor.getValue();
    }

    private AppInstance captureCreatedInstance() {
        ArgumentCaptor<AppInstance> appInstanceCaptor = ArgumentCaptor.forClass(AppInstance.class);

        verify(appInstanceService).create(appInstanceCaptor.capture());

        return appInstanceCaptor.getValue();
    }

    private void enableAuthorizationServer(boolean enabled) {
        lenient().when(registeredClientServiceProvider.getIfAvailable())
            .thenReturn(enabled ? registeredClientService : null);
        lenient().when(registeredClientServiceProvider.getObject())
            .thenReturn(registeredClientService);
        lenient().when(authorizationGrantServiceProvider.getIfAvailable())
            .thenReturn(enabled ? authorizationGrantService : null);
    }

    private static AppInstance registeredInstance() {
        AppInstance appInstance = persistedInstance(APP_INSTANCE_ID);

        appInstance.setUserAuthMode(AppUserAuthMode.REQUIRED);
        appInstance.setUrl("https://crm.example.com");
        appInstance.setRegisteredClientId("app-instance-uuid");

        return appInstance;
    }

    private void stubCreate() {
        User user = new User();

        user.setId(42L);

        when(userService.getCurrentUser()).thenReturn(user);
        when(appInstanceService.create(any())).thenAnswer(invocation -> {
            AppInstance appInstance = invocation.getArgument(0);

            appInstance.setId(12L);
            appInstance.setUuid("created-uuid");

            return new AppInstanceService.CreatedAppInstance(appInstance, CREATED_KEY.key());
        });
    }

    private static App persistedApp() {
        App app = new App();

        app.setId(APP_ID);
        app.setUuid("uuid");
        app.setName("crm");
        app.setWorkspaceId(WORKSPACE_ID);
        app.setOwnerUserId(9L);
        app.setStatus(AppStatus.DRAFT);
        app.setCreatedBy("admin");
        app.setCreatedDate(Instant.EPOCH);

        return app;
    }

    private static AppInstance rotatedInstance() {
        AppInstance appInstance = persistedInstance(APP_INSTANCE_ID);

        appInstance.setSecretKeyHash(ROTATED_KEY.hash());
        appInstance.setSecretKeyHint(ROTATED_KEY.hint());

        return appInstance;
    }

    private static AppInstance persistedInstance(long id) {
        AppInstance appInstance = new AppInstance();

        appInstance.setId(id);
        appInstance.setAppId(APP_ID);
        appInstance.setUuid("instance-uuid");
        appInstance.setName("Production");
        appInstance.setWorkspaceId(WORKSPACE_ID);
        appInstance.setOwnerUserId(9L);
        appInstance.setEnvironment(Environment.PRODUCTION);
        appInstance.setHostingTarget(AppHostingTarget.VERCEL);
        appInstance.setSecretKeyHint(String.valueOf(id));

        return appInstance;
    }

    private static String preAuthorize(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = AppInstanceFacadeImpl.class.getMethod(methodName, parameterTypes);

        PreAuthorize preAuthorize = Objects.requireNonNull(method.getAnnotation(PreAuthorize.class));

        return preAuthorize.value();
    }
}
