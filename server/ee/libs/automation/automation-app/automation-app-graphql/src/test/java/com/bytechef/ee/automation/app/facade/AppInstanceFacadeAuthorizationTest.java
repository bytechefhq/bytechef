/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.APP;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.APP_INSTANCE;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.CONNECTION_A;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.CONNECTION_B;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.DEVELOPMENT_ADMIN;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.EDITOR;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.FOREIGN_APP_INSTANCE;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.OUTSIDER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.PRIVATE_APP_INSTANCE;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.PRODUCTION_VIEWER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.TENANT_ADMIN;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.TWO_WORKSPACE_ADMIN;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.VIEWER;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.WORKSPACE_A;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.WORKSPACE_ADMIN;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.WORKSPACE_B;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.authenticate;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.newAppInstance;
import static com.bytechef.ee.automation.app.facade.AppAuthorizationFixture.userId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppConnection;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.dto.AppBindingDTO;
import com.bytechef.ee.automation.app.dto.AppDTO;
import com.bytechef.ee.automation.app.dto.AppInstanceDTO;
import com.bytechef.ee.automation.app.dto.AppSecretKeyDTO;
import com.bytechef.ee.automation.app.dto.ForeignAppInstanceDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.service.AppConnectionService;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs the {@code @PreAuthorize} guards on {@link AppInstanceFacadeImpl} through real Spring method security, the
 * production expression root and permission evaluator, the EE {@code PermissionServiceImpl} and the production
 * AppInstance, App and Connection resolvers (see {@link AppAuthorizationTestConfiguration}). An instance is authorized
 * against its own workspace and environment, so {@link AppAuthorizationFixture#FOREIGN_APP_INSTANCE}, which lives in
 * workspace B, is managed by B's admins and not by the admins of its app's workspace A.
 *
 * <p>
 * Every denial is paired with an allowance on the same guard, so no assertion here can pass because the context denies
 * everything; and every refused mutation also checks that the facade body never ran.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppAuthorizationTestConfiguration.class, properties = "bytechef.edition=ee")
class AppInstanceFacadeAuthorizationTest {

    @Autowired
    private AppConnectionService appConnectionService;

    @Autowired
    private AppFacade appFacade;

    @Autowired
    private AppInstanceFacade appInstanceFacade;

    @Autowired
    private AppInstanceService appInstanceService;

    @Autowired
    private AppService appService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkspaceConnectionRepository workspaceConnectionRepository;

    @Autowired
    private WorkspaceUserRepository workspaceUserRepository;

    @BeforeEach
    void beforeEach() {
        AppAuthorizationFixture.stubTenant(
            appInstanceService, appService, connectionService, userService, workspaceConnectionRepository,
            workspaceUserRepository);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class Create {

        @Test
        void testAppWithoutEnvironmentRoleCannotCreateProductionInstance() {
            authenticate(PRODUCTION_VIEWER);

            assertThatThrownBy(() -> createAppInstance(WORKSPACE_A, Environment.PRODUCTION))
                .isInstanceOf(AccessDeniedException.class);

            verify(appInstanceService, never()).create(any());

            stubCreate(WORKSPACE_A, Environment.DEVELOPMENT);

            assertThat(createAppInstance(WORKSPACE_A, Environment.DEVELOPMENT).secretKey()).isEqualTo("secret");
        }

        @Test
        void testCreateInOtherWorkspaceNeedsManageOnAppAndInTargetWorkspace() {
            authenticate(TWO_WORKSPACE_ADMIN);

            stubCreate(WORKSPACE_B, Environment.PRODUCTION);

            assertThat(createAppInstance(WORKSPACE_B, Environment.PRODUCTION).appInstance()
                .workspaceId()).isEqualTo(WORKSPACE_B);
        }

        @Test
        void testManageOnAppAloneIsRefused() {
            authenticate(WORKSPACE_ADMIN);

            assertThatThrownBy(() -> createAppInstance(WORKSPACE_B, Environment.PRODUCTION))
                .isInstanceOf(AccessDeniedException.class);

            verify(appInstanceService, never()).create(any());
        }

        @Test
        void testManageInTargetWorkspaceAloneIsRefused() {
            authenticate(OUTSIDER);

            assertThatThrownBy(() -> createAppInstance(WORKSPACE_B, Environment.PRODUCTION))
                .isInstanceOf(AccessDeniedException.class);

            verify(appInstanceService, never()).create(any());
        }

        private void stubCreate(long workspaceId, Environment environment) {
            AppInstance appInstance = newAppInstance(40L, APP, workspaceId, WORKSPACE_ADMIN);

            appInstance.setEnvironment(environment);

            when(appInstanceService.create(any()))
                .thenReturn(new AppInstanceService.CreatedAppInstance(appInstance, "secret"));
        }

        private AppSecretKeyDTO createAppInstance(
            long workspaceId, Environment environment) {

            return appInstanceFacade.createAppInstance(
                APP, workspaceId, environment, "Croatia", AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE,
                AppUserAuthMode.NONE);
        }
    }

    @Nested
    class AppInstanceView {

        @Test
        void testViewerCanGetAppInstanceAndBindings() {
            authenticate(VIEWER);

            assertThat(appInstanceFacade.getAppInstance(APP_INSTANCE)
                .id()).isEqualTo(APP_INSTANCE);
            assertThat(appInstanceFacade.getBindings(APP_INSTANCE)).isEmpty();

            verify(appConnectionService).getBindings(APP_INSTANCE);
        }

        @Test
        void testMemberOfAnotherWorkspaceIsDeniedGetBindings() {
            authenticate(OUTSIDER);

            assertThatThrownBy(() -> appInstanceFacade.getBindings(APP_INSTANCE))
                .isInstanceOf(AccessDeniedException.class);

            verify(appConnectionService, never()).getBindings(anyLong());
        }

        @Test
        void testViewerIsDeniedAColleaguesPrivateInstance() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appInstanceFacade.getAppInstance(PRIVATE_APP_INSTANCE))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testUnknownInstanceIsDenied() {
            authenticate(WORKSPACE_ADMIN);

            assertThatThrownBy(() -> appInstanceFacade.getAppInstance(999L))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testOwningWorkspaceSeesForeignInstanceWithoutBindingsOrKeyHint() {
            authenticate(VIEWER);

            AppDTO appDTO = appFacade.getApp(APP);

            assertThat(appDTO.instances())
                .extracting(AppInstanceDTO::id, AppInstanceDTO::workspaceId, AppInstanceDTO::secretKeyHint)
                .containsExactly(tuple(APP_INSTANCE, WORKSPACE_A, String.valueOf(APP_INSTANCE)));
            assertThat(appDTO.foreignInstances()).containsExactly(
                new ForeignAppInstanceDTO(
                    FOREIGN_APP_INSTANCE, "Germany", WORKSPACE_B, Environment.PRODUCTION, AppInstanceStatus.ACTIVE,
                    null));
            assertThat(Arrays.stream(ForeignAppInstanceDTO.class.getRecordComponents())
                .map(RecordComponent::getName))
                    .containsExactly("id", "name", "workspaceId", "environment", "status", "currentAppDeploymentId");

            assertThatThrownBy(() -> appInstanceFacade.getBindings(FOREIGN_APP_INSTANCE))
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> appInstanceFacade.getAppInstance(FOREIGN_APP_INSTANCE))
                .isInstanceOf(AccessDeniedException.class);

            verify(appConnectionService, never()).getBindings(anyLong());
        }

        @Test
        void testPrivateInstanceOfAWorkspaceAppIsNotReturnedToAColleague() {
            AppInstance privateAppInstance = newAppInstance(33L, APP, WORKSPACE_A, WORKSPACE_ADMIN);

            privateAppInstance.setName("Croatia");
            privateAppInstance.setVisibility(ResourceVisibility.PRIVATE);

            stubInstancesOfApp(privateAppInstance);

            authenticate(VIEWER);

            assertThat(instanceIds(appFacade.getApp(APP))).containsExactly(APP_INSTANCE);
            assertThat(appFacade.getApps(WORKSPACE_A)).singleElement()
                .satisfies(appDTO -> assertThat(instanceIds(appDTO)).containsExactly(APP_INSTANCE));

            authenticate(WORKSPACE_ADMIN);

            assertThat(instanceIds(appFacade.getApp(APP))).containsExactly(APP_INSTANCE, 33L);
            assertThat(appFacade.getApps(WORKSPACE_A)).singleElement()
                .satisfies(appDTO -> assertThat(instanceIds(appDTO)).containsExactly(APP_INSTANCE, 33L));
        }

        @Test
        void testInstanceOfAnEnvironmentWithoutAppViewIsNotReturned() {
            AppInstance developmentAppInstance = newAppInstance(34L, APP, WORKSPACE_A, WORKSPACE_ADMIN);

            developmentAppInstance.setName("Development");
            developmentAppInstance.setEnvironment(Environment.DEVELOPMENT);

            stubInstancesOfApp(developmentAppInstance);

            authenticate(DEVELOPMENT_ADMIN);

            assertThat(instanceIds(appFacade.getApp(APP))).containsExactly(34L);
            assertThat(appFacade.getApps(WORKSPACE_A)).singleElement()
                .satisfies(appDTO -> assertThat(instanceIds(appDTO)).containsExactly(34L));

            authenticate(VIEWER);

            assertThat(instanceIds(appFacade.getApp(APP))).containsExactly(APP_INSTANCE, 34L);
        }

        private void stubInstancesOfApp(AppInstance additionalAppInstance) {
            List<AppInstance> appInstances = List.of(
                appInstanceService.getAppInstance(APP_INSTANCE), additionalAppInstance);

            when(appInstanceService.getAppInstances(APP)).thenReturn(appInstances);
            when(appInstanceService.getAppInstancesByAppIds(List.of(APP))).thenReturn(appInstances);
            App app = appService.getApp(APP);

            when(appService.getApps(WORKSPACE_A)).thenReturn(List.of(app));
        }

        private static List<Long> instanceIds(AppDTO appDTO) {
            return appDTO.instances()
                .stream()
                .map(AppInstanceDTO::id)
                .toList();
        }
    }

    @Nested
    class AppInstanceManage {

        @Test
        void testViewerIsDeniedRotateSecretKey() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appInstanceFacade.rotateSecretKey(APP_INSTANCE))
                .isInstanceOf(AccessDeniedException.class);

            verify(appInstanceService, never()).rotateSecretKey(anyLong());
        }

        @Test
        void testEditorIsDeniedRotateSecretKey() {
            authenticate(EDITOR);

            assertThatThrownBy(() -> appInstanceFacade.rotateSecretKey(APP_INSTANCE))
                .isInstanceOf(AccessDeniedException.class);

            verify(appInstanceService, never()).rotateSecretKey(anyLong());
        }

        @Test
        void testWorkspaceAdminCanRotateSecretKey() {
            authenticate(WORKSPACE_ADMIN);

            when(appInstanceService.rotateSecretKey(APP_INSTANCE))
                .thenReturn(new AppInstanceService.RotatedKey(
                    newAppInstance(APP_INSTANCE, APP, WORKSPACE_A, WORKSPACE_ADMIN), "new-key"));

            assertThat(appInstanceFacade.rotateSecretKey(APP_INSTANCE)
                .secretKey()).isEqualTo("new-key");
        }

        @Test
        void testAdminInDevelopmentButViewerInProductionIsDeniedRotateSecretKeyOnAProductionInstance() {
            authenticate(PRODUCTION_VIEWER);

            assertThatThrownBy(() -> appInstanceFacade.rotateSecretKey(APP_INSTANCE))
                .isInstanceOf(AccessDeniedException.class);

            verify(appInstanceService, never()).rotateSecretKey(anyLong());
        }

        @Test
        void testViewerIsDeniedDeleteAppInstance() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appInstanceFacade.deleteAppInstance(APP_INSTANCE))
                .isInstanceOf(AccessDeniedException.class);

            verify(appInstanceService, never()).delete(anyLong());
        }

        @Test
        void testWorkspaceAdminCanDeleteAppInstance() {
            authenticate(WORKSPACE_ADMIN);

            appInstanceFacade.deleteAppInstance(APP_INSTANCE);

            verify(appInstanceService).delete(APP_INSTANCE);
        }

        @Test
        void testViewerIsDeniedUpdateAppInstanceStatus() {
            authenticate(VIEWER);

            assertThatThrownBy(
                () -> appInstanceFacade.updateAppInstanceStatus(APP_INSTANCE, AppInstanceStatus.SUSPENDED))
                    .isInstanceOf(AccessDeniedException.class);

            verify(appInstanceService, never()).updateStatus(anyLong(), any());
        }

        @Test
        void testViewerAndEditorAreDeniedUpdateAppInstanceUserAuth() {
            for (String login : List.of(VIEWER, EDITOR)) {
                authenticate(login);

                assertThatThrownBy(
                    () -> appInstanceFacade.updateAppInstanceUserAuth(APP_INSTANCE, AppUserAuthMode.REQUIRED))
                        .isInstanceOf(AccessDeniedException.class);
            }

            verify(appInstanceService, never()).updateUserAuth(anyLong(), any(), any());
        }

        @Test
        void testViewerAndEditorAreDeniedUpdateAppInstanceConfiguration() {
            for (String login : List.of(VIEWER, EDITOR)) {
                authenticate(login);

                assertThatThrownBy(
                    () -> appInstanceFacade.updateAppInstanceConfiguration(APP_INSTANCE, Map.of("country", "HR")))
                        .isInstanceOf(AccessDeniedException.class);
            }

            verify(appInstanceService, never()).updateConfiguration(anyLong(), any());
        }

        @Test
        void testWorkspaceAdminPassesTheGuardOfUpdateAppInstanceConfiguration() {
            authenticate(WORKSPACE_ADMIN);

            assertThatThrownBy(() -> appInstanceFacade.updateAppInstanceConfiguration(APP_INSTANCE, List.of("a")))
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertThat(exception.getErrorKey())
                        .isEqualTo(AppErrorType.APP_INSTANCE_CONFIGURATION_INVALID.getErrorKey()));
        }

        @Test
        void testViewerIsDeniedUnbindConnection() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appInstanceFacade.unbindConnection(APP_INSTANCE, "crm"))
                .isInstanceOf(AccessDeniedException.class);

            verify(appConnectionService, never()).unbind(anyLong(), anyString());
        }

        @Test
        void testViewerIsDeniedGetBindableConnections() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> appInstanceFacade.getBindableConnections(APP_INSTANCE, null))
                .isInstanceOf(AccessDeniedException.class);

            verify(appConnectionService, never()).getBindableConnections(any());
        }

        @Test
        void testAppWorkspaceAdminCannotManageForeignInstance() {
            authenticate(WORKSPACE_ADMIN);

            assertThatThrownBy(() -> appInstanceFacade.rotateSecretKey(FOREIGN_APP_INSTANCE))
                .isInstanceOf(AccessDeniedException.class);

            verify(appInstanceService, never()).rotateSecretKey(anyLong());
        }

        @Test
        void testTargetWorkspaceAdminManagesInstanceButCannotUpdateApp() {
            authenticate(OUTSIDER);

            AppInstance renamedAppInstance = newAppInstance(FOREIGN_APP_INSTANCE, APP, WORKSPACE_B, OUTSIDER);

            renamedAppInstance.setName("Deutschland");

            when(appInstanceService.update(FOREIGN_APP_INSTANCE, "Deutschland", null, null))
                .thenReturn(renamedAppInstance);
            when(appInstanceService.rotateSecretKey(FOREIGN_APP_INSTANCE))
                .thenReturn(new AppInstanceService.RotatedKey(renamedAppInstance, "new-key"));

            assertThat(appInstanceFacade.updateAppInstance(FOREIGN_APP_INSTANCE, "Deutschland", null, null)
                .name()).isEqualTo("Deutschland");
            assertThat(appInstanceFacade.rotateSecretKey(FOREIGN_APP_INSTANCE)
                .secretKey()).isEqualTo("new-key");

            assertThatThrownBy(() -> appFacade.updateApp(APP, "renamed", null, null, null))
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> appFacade.updateAppStatus(APP, AppStatus.SUSPENDED))
                .isInstanceOf(AccessDeniedException.class);

            verify(appService, never()).update(anyLong(), any(), any());
            verify(appService, never()).updateStatus(anyLong(), any());
        }
    }

    @Nested
    class ConnectionUse {

        @Test
        void testViewerIsDeniedBindConnection() {
            authenticate(VIEWER);

            assertThatThrownBy(() -> bindConnection(APP_INSTANCE, CONNECTION_A))
                .isInstanceOf(AccessDeniedException.class);

            verifyNeverBound();
        }

        @Test
        void testEditorWithConnectionUseButNotAppManageIsDeniedBindConnection() {
            authenticate(EDITOR);

            assertThatThrownBy(() -> bindConnection(APP_INSTANCE, CONNECTION_A))
                .isInstanceOf(AccessDeniedException.class);

            verifyNeverBound();
        }

        @Test
        void testAppManagerWithoutConnectionUseIsDeniedBindConnection() {
            authenticate(WORKSPACE_ADMIN);

            assertThatThrownBy(() -> bindConnection(APP_INSTANCE, CONNECTION_B))
                .isInstanceOf(AccessDeniedException.class);

            verifyNeverBound();
        }

        @Test
        void testAppManagerWithConnectionUseCanBindConnection() {
            authenticate(WORKSPACE_ADMIN);

            stubBind(APP_INSTANCE, CONNECTION_A);

            assertThat(bindConnection(APP_INSTANCE, CONNECTION_A).bindingKey()).isEqualTo("crm");
        }

        @Test
        void testForeignInstanceBindsOnlyConnectionsOfItsOwnWorkspace() {
            authenticate(OUTSIDER);

            assertThatThrownBy(() -> bindConnection(FOREIGN_APP_INSTANCE, CONNECTION_A))
                .isInstanceOf(AccessDeniedException.class);

            verifyNeverBound();

            stubBind(FOREIGN_APP_INSTANCE, CONNECTION_B);

            AppBindingDTO appBindingDTO = bindConnection(FOREIGN_APP_INSTANCE, CONNECTION_B);

            assertThat(appBindingDTO.appInstanceId()).isEqualTo(FOREIGN_APP_INSTANCE);
            assertThat(appBindingDTO.connectionId()).isEqualTo(CONNECTION_B);
        }

        private AppBindingDTO bindConnection(long appInstanceId, long connectionId) {
            return appInstanceFacade.bindConnection(
                appInstanceId, "crm", connectionId, List.of("send_message"), List.of(), false, List.of());
        }

        private void stubBind(long appInstanceId, long connectionId) {
            AppConnection appConnection = new AppConnection();

            appConnection.setId(1L);
            appConnection.setAppInstanceId(appInstanceId);
            appConnection.setBindingKey("crm");
            appConnection.setConnectionId(connectionId);

            when(appConnectionService.bind(
                any(), eq("crm"), eq(connectionId), anyList(), anyList(), anyBoolean(), anyList()))
                    .thenReturn(appConnection);
        }

        private void verifyNeverBound() {
            verify(appConnectionService, never()).bind(
                any(), anyString(), anyLong(), anyList(), anyList(), anyBoolean(), anyList());
        }
    }

    @Nested
    @ExtendWith(ObjectMapperSetupExtension.class)
    class DeploymentConnection {

        @Test
        void testAppManagerWithConnectionEditWhoDoesNotOwnTheConnectionIsRefused() {
            authenticate(TWO_WORKSPACE_ADMIN);

            assertThatThrownBy(() -> updateDeploymentSettings(CONNECTION_A))
                .isInstanceOf(AccessDeniedException.class);

            verify(appConnectionService, never()).isBindable(any(), anyLong());
            verify(appInstanceService, never()).updateDeploymentSettings(anyLong(), any(), any());
        }

        @Test
        void testConnectionCreatorWhoLeftTheWorkspaceIsRefusedAsANonOwner() {
            when(workspaceUserRepository.existsByUserIdAndWorkspaceId(userId(WORKSPACE_ADMIN), WORKSPACE_A))
                .thenReturn(false);

            authenticate(WORKSPACE_ADMIN);

            assertThatThrownBy(() -> updateDeploymentSettings(CONNECTION_A))
                .isInstanceOf(AccessDeniedException.class);

            verify(appConnectionService, never()).isBindable(any(), anyLong());
            verify(appInstanceService, never()).updateDeploymentSettings(anyLong(), any(), any());
        }

        @Test
        void testConnectionOwnerPassesTheGuard() {
            authenticate(WORKSPACE_ADMIN);

            assertGuardPassed(() -> updateDeploymentSettings(CONNECTION_A));
        }

        @Test
        void testTenantAdminPassesTheGuard() {
            authenticate(TENANT_ADMIN);

            assertGuardPassed(() -> updateDeploymentSettings(CONNECTION_A));
        }

        @Test
        void testAppManagerWhoDoesNotOwnTheConnectionCanClearIt() {
            authenticate(TWO_WORKSPACE_ADMIN);

            AppInstance clearedAppInstance = newAppInstance(APP_INSTANCE, APP, WORKSPACE_A, WORKSPACE_ADMIN);

            when(appInstanceService.updateDeploymentSettings(eq(APP_INSTANCE), eq(null), any()))
                .thenReturn(clearedAppInstance);

            AppInstanceDTO appInstanceDTO = appInstanceFacade.updateAppInstanceDeploymentSettings(
                APP_INSTANCE, null, null);

            assertThat(appInstanceDTO.id()).isEqualTo(APP_INSTANCE);
        }

        private void assertGuardPassed(ThrowingCallable callable) {
            assertThatThrownBy(callable)
                .isInstanceOfSatisfying(
                    ConfigurationException.class,
                    exception -> assertThat(exception.getErrorKey())
                        .isEqualTo(AppErrorType.APP_CONNECTION_NOT_BINDABLE.getErrorKey()));
        }

        private AppInstanceDTO updateDeploymentSettings(long connectionId) {
            return appInstanceFacade.updateAppInstanceDeploymentSettings(
                APP_INSTANCE, connectionId, Map.of("deployUrl", "https://example.com/deploy"));
        }
    }
}
