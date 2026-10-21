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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
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
import com.bytechef.ee.automation.app.audit.AppAuditDataAssertions;
import com.bytechef.ee.automation.app.audit.AppAuditEvent;
import com.bytechef.ee.automation.app.audit.AppAuditPublisher;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.dto.AppCategoryDTO;
import com.bytechef.ee.automation.app.dto.AppComponentOperationsDTO;
import com.bytechef.ee.automation.app.dto.AppDTO;
import com.bytechef.ee.automation.app.dto.AppIconDTO;
import com.bytechef.ee.automation.app.dto.AppInstanceDTO;
import com.bytechef.ee.automation.app.dto.AppSecretKeyDTO;
import com.bytechef.ee.automation.app.dto.AppTagDTO;
import com.bytechef.ee.automation.app.dto.AppWorkspaceConnectionDTO;
import com.bytechef.ee.automation.app.dto.ForeignAppInstanceDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.file.storage.AppFileStorage;
import com.bytechef.ee.automation.app.security.AppInstanceClients;
import com.bytechef.ee.automation.app.security.AppVisibilityPolicy;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.app.util.AppSecretKeys;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.oauth2.authorizationserver.service.OAuth2AuthorizationGrantService;
import com.bytechef.platform.oauth2.authorizationserver.service.RegisteredClientService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AppFacadeTest {

    private static final int ICON_SIZE_LIMIT = 256 * 1024;
    private static final byte[] JPEG = {
        (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F'
    };
    private static final byte[] PNG = {
        (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D
    };
    private static final byte[] WEBP = {
        'R', 'I', 'F', 'F', 0x24, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '
    };

    private static final AppSecretKeys.GeneratedKey CREATED_KEY = AppSecretKeys.generate();

    @Mock
    private ActionDefinitionService actionDefinitionService;

    @Mock
    private AppAuditPublisher appAuditPublisher;

    @Mock
    private AppFileStorage appFileStorage;

    @Mock
    private OAuth2AuthorizationGrantService authorizationGrantService;

    @Mock
    private ObjectProvider<OAuth2AuthorizationGrantService> authorizationGrantServiceProvider;

    @Mock
    private AppInstanceService appInstanceService;

    @Mock
    private AppService appService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ClusterElementDefinitionService clusterElementDefinitionService;

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
    private TagService tagService;

    @Mock
    private UserService userService;

    @Mock
    private WorkspaceConnectionFacade workspaceConnectionFacade;

    private AppFacadeImpl appFacade;

    @BeforeEach
    void beforeEach() {
        appFacade = new AppFacadeImpl(
            actionDefinitionService, appAuditPublisher, appFileStorage,
            new AppInstanceClients(authorizationGrantServiceProvider, registeredClientServiceProvider),
            appInstanceService, appService, categoryService,
            clusterElementDefinitionService, permissionService, resourceGrantService,
            new ResourceVisibilityPolicyRegistry(List.of(new AppVisibilityPolicy())), resourceVisibilityResolver,
            tagService, userService, workspaceConnectionFacade);

        lenient().when(resourceVisibilityResolver.filterVisibleIds(eq("AppInstance"), anyLong(), anyCollection()))
            .thenAnswer(invocation -> {
                Collection<VisibilityRecord> visibilityRecords = invocation.getArgument(2);

                return visibilityRecords.stream()
                    .map(VisibilityRecord::id)
                    .collect(Collectors.toSet());
            });
        lenient().when(permissionService.hasWorkspaceScope(anyLong(), eq("APP_VIEW"), any(Environment.class)))
            .thenReturn(true);
        lenient().when(permissionService.hasWorkspaceScopeInEveryEnvironment(anyLong(), eq("APP_MANAGE")))
            .thenReturn(true);

        enableAuthorizationServer(true);
    }

    @AfterEach
    void afterEach() {
        AppAuditDataAssertions.assertNoSecrets(
            appAuditPublisher, CREATED_KEY.key(), CREATED_KEY.hash(), CREATED_KEY.hint());
    }

    @Test
    void testPreAuthorizeExpressionsArePinned() throws Exception {
        assertThat(
            preAuthorize(
                "createApp", long.class, Environment.class, String.class, String.class, AppHostingTarget.class,
                ResourceVisibility.class, AppUserAuthMode.class, AppCategoryDTO.class, boolean.class))
                    .isEqualTo("hasWorkspaceScopeInEnvironment(#workspaceId, 'APP_MANAGE', #environment)");
        assertThat(preAuthorize("getApps", long.class))
            .isEqualTo("hasPermission(#workspaceId, 'Workspace', 'APP_VIEW')");
        assertThat(preAuthorize("getApp", long.class)).isEqualTo("hasPermission(#id, 'App', 'APP_VIEW')");
        assertThat(preAuthorize("getConnections", long.class, Environment.class, String.class))
            .isEqualTo("hasWorkspaceScopeInEnvironment(#workspaceId, 'CONNECTION_VIEW', #environment)");
        assertThat(preAuthorize("isUserAuthAvailable", long.class))
            .isEqualTo("hasPermission(#workspaceId, 'Workspace', 'APP_VIEW')");
        assertThat(preAuthorize("getAppCategories", long.class))
            .isEqualTo("hasPermission(#workspaceId, 'Workspace', 'APP_VIEW')");
        assertThat(
            preAuthorize("updateApp", long.class, String.class, String.class, AppCategoryDTO.class, Boolean.class))
                .isEqualTo("hasPermission(#id, 'App', 'APP_MANAGE')");
        assertThat(preAuthorize("updateAppStatus", long.class, AppStatus.class))
            .isEqualTo("hasPermission(#id, 'App', 'APP_MANAGE')");
        assertThat(preAuthorize("deleteApp", long.class)).isEqualTo("hasPermission(#id, 'App', 'APP_MANAGE')");
        assertThat(preAuthorize("getComponentOperations", String.class, int.class))
            .isEqualTo("isAuthenticated()");
        assertThat(preAuthorize("updateAppIcon", long.class, InputStream.class))
            .isEqualTo("hasPermission(#id, 'App', 'APP_MANAGE')");
        assertThat(preAuthorize("deleteAppIcon", long.class)).isEqualTo("hasPermission(#id, 'App', 'APP_MANAGE')");
        assertThat(preAuthorize("getAppIcon", long.class)).isEqualTo("hasPermission(#id, 'App', 'APP_VIEW')");
        assertThat(preAuthorize("getAppTags", long.class))
            .isEqualTo("hasPermission(#workspaceId, 'Workspace', 'APP_VIEW')");
        assertThat(preAuthorize("updateAppTags", long.class, List.class))
            .isEqualTo("hasPermission(#id, 'App', 'APP_MANAGE')");
    }

    @Test
    void testCreateAppCreatesFirstInstance() {
        String plaintextKey = CREATED_KEY.key();

        stubCurrentUser(9L);

        when(appService.create(any())).thenAnswer(invocation -> persisted(invocation.getArgument(0), 1L));
        when(appInstanceService.create(any())).thenAnswer(invocation -> {
            AppInstance appInstance = invocation.getArgument(0);

            appInstance.setId(11L);
            appInstance.setUuid("instance-uuid");
            appInstance.setSecretKeyHash(CREATED_KEY.hash());
            appInstance.setSecretKeyHint(CREATED_KEY.hint());

            return new AppInstanceService.CreatedAppInstance(appInstance, plaintextKey);
        });

        AppSecretKeyDTO result = appFacade.createApp(
            7L, Environment.PRODUCTION, "crm", null, AppHostingTarget.VERCEL, ResourceVisibility.PRIVATE,
            AppUserAuthMode.NONE, null, false);

        ArgumentCaptor<App> appCaptor = ArgumentCaptor.forClass(App.class);

        verify(appService).create(appCaptor.capture());

        App createdApp = appCaptor.getValue();

        assertThat(createdApp.getWorkspaceId()).isEqualTo(7L);
        assertThat(createdApp.getName()).isEqualTo("crm");
        assertThat(createdApp.getVisibility()).isEqualTo(ResourceVisibility.PRIVATE);
        assertThat(createdApp.getCategoryId()).isNull();
        assertThat(createdApp.isPortalDiscoverable()).isFalse();

        ArgumentCaptor<AppInstance> appInstanceCaptor = ArgumentCaptor.forClass(AppInstance.class);

        verify(appInstanceService).create(appInstanceCaptor.capture());

        AppInstance createdAppInstance = appInstanceCaptor.getValue();

        assertThat(createdAppInstance.getAppId()).isEqualTo(1L);
        assertThat(createdAppInstance.getWorkspaceId()).isEqualTo(7L);
        assertThat(createdAppInstance.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(createdAppInstance.getHostingTarget()).isEqualTo(AppHostingTarget.VERCEL);
        assertThat(createdAppInstance.getName()).isEqualTo("Production");
        assertThat(createdAppInstance.getVisibility()).isEqualTo(ResourceVisibility.PRIVATE);

        AppInstanceDTO appInstanceDTO = result.appInstance();

        assertThat(result.secretKey()).isEqualTo(plaintextKey);
        assertThat(appInstanceDTO.id()).isEqualTo(11L);
        assertThat(appInstanceDTO.environment()).isEqualTo(Environment.PRODUCTION);
        assertThat(appInstanceDTO.secretKeyHint()).isEqualTo(CREATED_KEY.hint());
        assertThat(appInstanceDTO.app()
            .id()).isEqualTo(1L);
        assertThat(appInstanceDTO.app()
            .name()).isEqualTo("crm");

        InOrder auditOrder = inOrder(appAuditPublisher);

        auditOrder.verify(appAuditPublisher)
            .publish(eq(AppAuditEvent.APP_CREATED), eq(1L), anyMap());
        auditOrder.verify(appAuditPublisher)
            .publish(
                AppAuditEvent.APP_INSTANCE_CREATED, 1L,
                Map.of(
                    "appInstanceId", 11L, "name", "Production", "workspaceId", 7L, "environment", "PRODUCTION"));
    }

    @Test
    void testFirstInstanceOwnerIsAppOwner() {
        stubCurrentUser(9L);

        when(appService.create(any())).thenAnswer(invocation -> persisted(invocation.getArgument(0), 1L));
        when(appInstanceService.create(any())).thenAnswer(invocation -> {
            AppInstance appInstance = invocation.getArgument(0);

            appInstance.setId(11L);

            return new AppInstanceService.CreatedAppInstance(appInstance, "secret");
        });

        appFacade.createApp(7L, Environment.STAGING, "crm", null, AppHostingTarget.VERCEL,
            ResourceVisibility.WORKSPACE, AppUserAuthMode.NONE, null, false);

        ArgumentCaptor<App> appCaptor = ArgumentCaptor.forClass(App.class);
        ArgumentCaptor<AppInstance> appInstanceCaptor = ArgumentCaptor.forClass(AppInstance.class);

        verify(appService).create(appCaptor.capture());
        verify(appInstanceService).create(appInstanceCaptor.capture());

        App createdApp = appCaptor.getValue();
        AppInstance createdAppInstance = appInstanceCaptor.getValue();

        assertThat(createdApp.getOwnerUserId()).isEqualTo(9L);
        assertThat(createdAppInstance.getOwnerUserId()).isEqualTo(createdApp.getOwnerUserId());
    }

    @Test
    void testCreateRejectsOrganizationVisibility() {
        assertThatThrownBy(
            () -> appFacade.createApp(
                7L, Environment.PRODUCTION, "crm", null, AppHostingTarget.VERCEL, ResourceVisibility.ORGANIZATION,
                AppUserAuthMode.NONE, null, false))
                    .isInstanceOf(ConfigurationException.class)
                    .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(AppErrorType.APP_VISIBILITY_UNSUPPORTED.getErrorKey());

        verifyNoInteractions(appService, appInstanceService, appAuditPublisher);
    }

    @Test
    void testUpdateAppDoesNotAcceptVisibility() {
        for (Method method : AppFacade.class.getDeclaredMethods()) {
            if (method.getName()
                .equals("updateApp")) {

                assertThat(method.getParameterTypes()).doesNotContain(ResourceVisibility.class);
            }
        }
    }

    @Test
    void testUpdateWithNullFieldsPassesNothingToChange() {
        when(appService.getApp(1L)).thenReturn(persisted(new App(), 1L));
        when(appService.update(1L, null, null)).thenReturn(persisted(new App(), 1L));

        appFacade.updateApp(1L, null, null, null, null);

        verify(appService).update(1L, null, null);
        verify(appService, never()).updateCategory(anyLong(), any());
        verify(appService, never()).updatePortalDiscoverable(anyLong(), anyBoolean());
        verify(appInstanceService, never()).update(anyLong(), any(), any(), any());
        verifyNoInteractions(categoryService);
    }

    @Test
    void testUpdateReturnsTheSavedAppWithItsInstances() {
        App saved = persisted(new App(), 1L);

        saved.setDescription("desc");

        when(appService.getApp(1L)).thenReturn(persisted(new App(), 1L));
        when(appService.update(1L, "crm", "desc")).thenReturn(saved);
        when(appInstanceService.getAppInstances(1L)).thenReturn(List.of(persistedInstance(11L, 1L)));

        AppDTO result = appFacade.updateApp(1L, "crm", "desc", null, null);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.description()).isEqualTo("desc");
        assertThat(result.instances()).extracting(AppInstanceDTO::id)
            .containsExactly(11L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUpdatePublishesTheChangedFieldsWithTheNewName() {
        App current = persisted(new App(), 1L);

        App saved = persisted(new App(), 1L);

        saved.setName("crm-2");
        saved.setDescription("New description");

        when(appService.getApp(1L)).thenReturn(current);
        when(appService.update(1L, "crm-2", "New description")).thenReturn(saved);

        appFacade.updateApp(1L, "crm-2", "New description", null, null);

        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(appAuditPublisher).publish(eq(AppAuditEvent.APP_UPDATED), eq(1L), dataCaptor.capture());

        assertThat(dataCaptor.getValue()).containsOnly(
            Map.entry("changedFields", List.of("name", "description")),
            Map.entry("name", "crm-2"));
    }

    @Test
    void testUpdateThatChangesNothingPublishesNoEvent() {
        when(appService.getApp(1L)).thenReturn(persisted(new App(), 1L));
        when(appService.update(1L, "crm", null)).thenReturn(persisted(new App(), 1L));

        appFacade.updateApp(1L, "crm", null, null, null);

        verify(appAuditPublisher, never()).publish(any(), anyLong(), anyMap());
    }

    @Test
    void testCreateAppAttachesAnExistingCategoryByIdAndCanBeDiscoverable() {
        stubCreate();

        when(categoryService.getCategories(List.of(42L))).thenReturn(List.of(new Category(42L, "Sales")));

        appFacade.createApp(
            7L, Environment.DEVELOPMENT, "crm", null, AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE,
            AppUserAuthMode.NONE, new AppCategoryDTO(42L, "ignored"), true);

        App createdApp = captureCreatedApp();

        assertThat(createdApp.getCategoryId()).isEqualTo(42L);
        assertThat(createdApp.isPortalDiscoverable()).isTrue();

        verify(categoryService, never()).save(any());
    }

    @Test
    void testCreateAppAuditCarriesTheCategoryAndPortalVisibility() {
        stubCreate();

        when(categoryService.getCategories(List.of(42L))).thenReturn(List.of(new Category(42L, "Sales")));

        appFacade.createApp(
            7L, Environment.DEVELOPMENT, "crm", null, AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE,
            AppUserAuthMode.NONE, new AppCategoryDTO(42L, null), true);

        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_CREATED, 1L,
            Map.of("name", "crm", "workspaceId", 7L, "categoryId", 42L, "portalDiscoverable", true));
    }

    @Test
    void testCreateAppAuditOmitsTheCategoryWhenNoneIsAttached() {
        stubCreate();

        appFacade.createApp(
            7L, Environment.DEVELOPMENT, "crm", null, AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE,
            AppUserAuthMode.NONE, null, false);

        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_CREATED, 1L, Map.of("name", "crm", "workspaceId", 7L, "portalDiscoverable", false));
    }

    @Test
    void testCreateAppWithANewCategoryNameCreatesTheCategory() {
        stubCreate();

        when(categoryService.save(any(Category.class))).thenReturn(new Category(43L, "Finance"));

        appFacade.createApp(
            7L, Environment.DEVELOPMENT, "crm", null, AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE,
            AppUserAuthMode.NONE, new AppCategoryDTO(null, " Finance "), false);

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);

        verify(categoryService).save(categoryCaptor.capture());

        Category savedCategory = categoryCaptor.getValue();

        assertThat(savedCategory.getId()).isNull();
        assertThat(savedCategory.getName()).isEqualTo("Finance");
        assertThat(captureCreatedApp().getCategoryId()).isEqualTo(43L);
    }

    @Test
    void testCreateAppWithAnUnknownCategoryIdIsRejectedBeforeAnythingIsStored() {
        when(categoryService.getCategories(List.of(42L))).thenReturn(List.of());

        assertThatThrownBy(
            () -> appFacade.createApp(
                7L, Environment.DEVELOPMENT, "crm", null, AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE,
                AppUserAuthMode.NONE, new AppCategoryDTO(42L, null), false))
                    .isInstanceOf(ConfigurationException.class)
                    .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(AppErrorType.APP_CATEGORY_INVALID.getErrorKey());

        verifyNoInteractions(appService, appInstanceService, appAuditPublisher);
    }

    @Test
    void testCreateAppWithACategoryNameLongerThanTheColumnIsRejected() {
        assertThatThrownBy(
            () -> appFacade.createApp(
                7L, Environment.DEVELOPMENT, "crm", null, AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE,
                AppUserAuthMode.NONE, new AppCategoryDTO(null, "x".repeat(257)), false))
                    .isInstanceOf(ConfigurationException.class)
                    .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(AppErrorType.APP_FIELD_TOO_LONG.getErrorKey());

        verifyNoInteractions(appService, categoryService);
    }

    @Test
    void testCreateAppWithAnEmptyCategoryNameHasNoCategory() {
        stubCreate();

        appFacade.createApp(
            7L, Environment.DEVELOPMENT, "crm", null, AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE,
            AppUserAuthMode.NONE, new AppCategoryDTO(null, " "), false);

        assertThat(captureCreatedApp().getCategoryId()).isNull();

        verifyNoInteractions(categoryService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUpdateAppSetsTheCategoryAndPortalVisibilityAndAuditsBoth() {
        App current = persisted(new App(), 1L);
        App updated = persisted(new App(), 1L);

        updated.setCategoryId(42L);
        updated.setPortalDiscoverable(true);

        when(appService.getApp(1L)).thenReturn(current);
        when(appService.update(1L, null, null)).thenReturn(persisted(new App(), 1L));
        when(appService.updateCategory(1L, 42L)).thenReturn(updated);
        when(appService.updatePortalDiscoverable(1L, true)).thenReturn(updated);
        when(categoryService.getCategories(List.of(42L))).thenReturn(List.of(new Category(42L, "Sales")));

        AppDTO result = appFacade.updateApp(1L, null, null, new AppCategoryDTO(42L, null), true);

        assertThat(result.categoryId()).isEqualTo(42L);
        assertThat(result.portalDiscoverable()).isTrue();

        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(appAuditPublisher).publish(eq(AppAuditEvent.APP_UPDATED), eq(1L), dataCaptor.capture());

        assertThat(dataCaptor.getValue()).containsOnly(
            Map.entry("changedFields", List.of("categoryId", "portalDiscoverable")),
            Map.entry("categoryId", 42L),
            Map.entry("portalDiscoverable", true));
    }

    @Test
    void testUpdateAppWithAnEmptyCategoryNameClearsTheCategory() {
        App current = persisted(new App(), 1L);

        current.setCategoryId(42L);

        when(appService.getApp(1L)).thenReturn(current);
        when(appService.update(1L, null, null)).thenAnswer(invocation -> {
            App app = persisted(new App(), 1L);

            app.setCategoryId(42L);

            return app;
        });
        when(appService.updateCategory(1L, null)).thenReturn(persisted(new App(), 1L));

        AppDTO result = appFacade.updateApp(1L, null, null, new AppCategoryDTO(null, ""), null);

        assertThat(result.categoryId()).isNull();

        verify(appService).updateCategory(1L, null);
        verify(appService, never()).updatePortalDiscoverable(anyLong(), anyBoolean());
        verifyNoInteractions(categoryService);
    }

    @Test
    void testUpdateAppToTheStoredCategoryAndPortalVisibilityStoresAndAuditsNothing() {
        App current = persisted(new App(), 1L);

        current.setCategoryId(42L);
        current.setPortalDiscoverable(true);

        when(appService.getApp(1L)).thenReturn(current);
        when(appService.update(1L, null, null)).thenReturn(current);
        when(categoryService.getCategories(List.of(42L))).thenReturn(List.of(new Category(42L, "Sales")));

        appFacade.updateApp(1L, null, null, new AppCategoryDTO(42L, null), true);

        verify(appService, never()).updateCategory(anyLong(), any());
        verify(appService, never()).updatePortalDiscoverable(anyLong(), anyBoolean());
        verify(appAuditPublisher, never()).publish(any(), anyLong(), anyMap());
    }

    @Test
    void testCreateAppAlreadyPublishedToThePortalAuditsThePublication() {
        stubCreate();

        appFacade.createApp(
            7L, Environment.DEVELOPMENT, "crm", null, AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE,
            AppUserAuthMode.NONE, null, true);

        verify(appAuditPublisher).publish(AppAuditEvent.APP_PUBLISHED_TO_PORTAL, 1L);
        verify(appAuditPublisher, never()).publish(eq(AppAuditEvent.APP_UNPUBLISHED_FROM_PORTAL), anyLong());
    }

    @Test
    void testCreateAppNotPublishedToThePortalAuditsNoPublication() {
        stubCreate();

        appFacade.createApp(
            7L, Environment.DEVELOPMENT, "crm", null, AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE,
            AppUserAuthMode.NONE, null, false);

        verify(appAuditPublisher, never()).publish(eq(AppAuditEvent.APP_PUBLISHED_TO_PORTAL), anyLong());
        verify(appAuditPublisher, never()).publish(eq(AppAuditEvent.APP_UNPUBLISHED_FROM_PORTAL), anyLong());
    }

    @Test
    void testUpdateAppPublishingToThePortalAuditsThePublication() {
        App current = persisted(new App(), 1L);
        App updated = persisted(new App(), 1L);

        updated.setPortalDiscoverable(true);

        when(appService.getApp(1L)).thenReturn(current);
        when(appService.update(1L, null, null)).thenReturn(current);
        when(appService.updatePortalDiscoverable(1L, true)).thenReturn(updated);

        appFacade.updateApp(1L, null, null, null, true);

        verify(appAuditPublisher).publish(
            AppAuditEvent.APP_UPDATED, 1L, Map.of("changedFields", List.of("portalDiscoverable"),
                "portalDiscoverable", true));
        verify(appAuditPublisher).publish(AppAuditEvent.APP_PUBLISHED_TO_PORTAL, 1L);
        verify(appAuditPublisher, never()).publish(eq(AppAuditEvent.APP_UNPUBLISHED_FROM_PORTAL), anyLong());
    }

    @Test
    void testUpdateAppUnpublishingFromThePortalAuditsTheUnpublication() {
        App current = persisted(new App(), 1L);

        current.setPortalDiscoverable(true);

        App updated = persisted(new App(), 1L);

        when(appService.getApp(1L)).thenReturn(current);
        when(appService.update(1L, null, null)).thenReturn(current);
        when(appService.updatePortalDiscoverable(1L, false)).thenReturn(updated);

        appFacade.updateApp(1L, null, null, null, false);

        verify(appAuditPublisher).publish(AppAuditEvent.APP_UNPUBLISHED_FROM_PORTAL, 1L);
        verify(appAuditPublisher, never()).publish(eq(AppAuditEvent.APP_PUBLISHED_TO_PORTAL), anyLong());
    }

    @Test
    void testUpdateAppWithoutAPortalChangeAuditsNoPortalEvent() {
        App current = persisted(new App(), 1L);

        current.setPortalDiscoverable(true);

        when(appService.getApp(1L)).thenReturn(current);
        when(appService.update(1L, "renamed", null)).thenAnswer(invocation -> {
            App renamed = persisted(new App(), 1L);

            renamed.setName("renamed");
            renamed.setPortalDiscoverable(true);

            return renamed;
        });

        appFacade.updateApp(1L, "renamed", null, null, true);

        verify(appAuditPublisher, never()).publish(eq(AppAuditEvent.APP_PUBLISHED_TO_PORTAL), anyLong());
        verify(appAuditPublisher, never()).publish(eq(AppAuditEvent.APP_UNPUBLISHED_FROM_PORTAL), anyLong());
    }

    @Test
    void testUpdateAppWithAnUnknownCategoryIdIsRejectedBeforeAnythingIsStored() {
        when(categoryService.getCategories(List.of(42L))).thenReturn(List.of());

        assertThatThrownBy(() -> appFacade.updateApp(1L, "renamed", null, new AppCategoryDTO(42L, null), true))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(AppErrorType.APP_CATEGORY_INVALID.getErrorKey());

        verify(appService, never()).update(anyLong(), any(), any());
        verify(appService, never()).updateCategory(anyLong(), any());
        verify(appService, never()).updatePortalDiscoverable(anyLong(), anyBoolean());
        verifyNoInteractions(appAuditPublisher);
    }

    @Test
    void testGetAppCategoriesListsTheCategoriesOfTheVisibleAppsOnce() {
        App visibleApp = persisted(new App(), 1L);
        App otherVisibleApp = persisted(new App(), 2L);
        App hiddenApp = persisted(new App(), 3L);
        App uncategorizedApp = persisted(new App(), 4L);

        visibleApp.setCategoryId(42L);
        otherVisibleApp.setCategoryId(42L);
        hiddenApp.setCategoryId(43L);

        when(appService.getApps(7L)).thenReturn(List.of(visibleApp, otherVisibleApp, hiddenApp, uncategorizedApp));
        when(resourceVisibilityResolver.filterVisibleIds(eq("App"), eq(7L), anyCollection()))
            .thenReturn(Set.of(1L, 2L, 4L));
        when(categoryService.getCategories(List.of(42L))).thenReturn(List.of(new Category(42L, "Sales")));

        assertThat(appFacade.getAppCategories(7L)).containsExactly(new AppCategoryDTO(42L, "Sales"));

        verify(categoryService).getCategories(List.of(42L));
    }

    @Test
    void testGetAppCategoriesOfAWorkspaceWithoutCategorizedAppsReadsNoCategories() {
        when(appService.getApps(7L)).thenReturn(List.of(persisted(new App(), 1L)));
        when(resourceVisibilityResolver.filterVisibleIds(eq("App"), eq(7L), anyCollection())).thenReturn(Set.of(1L));

        assertThat(appFacade.getAppCategories(7L)).isEmpty();

        verifyNoInteractions(categoryService);
    }

    @Test
    void testUpdateAppTagsAttachesExistingTagsWithoutRenamingAndCreatesNewOnesByName() {
        App current = persisted(new App(), 1L);
        App updated = persisted(new App(), 1L);

        updated.setTagIds(List.of(5L, 6L));

        when(appService.getApp(1L)).thenReturn(current);
        when(tagService.getTags(List.of(5L))).thenReturn(List.of(new Tag(5L, "crm")));
        when(tagService.save(List.of(new Tag("internal")))).thenReturn(List.of(new Tag(6L, "internal")));
        when(appService.updateTags(1L, List.of(5L, 6L))).thenReturn(updated);

        AppDTO result = appFacade.updateAppTags(
            1L, List.of(new AppTagDTO(5L, "renamed"), new AppTagDTO(null, " internal "), new AppTagDTO(null, "")));

        assertThat(result.tagIds()).containsExactly(5L, 6L);

        ArgumentCaptor<List<Tag>> savedTagsCaptor = ArgumentCaptor.forClass(List.class);

        verify(tagService).save(savedTagsCaptor.capture());

        assertThat(savedTagsCaptor.getValue()).extracting(Tag::getId)
            .containsOnlyNulls();

        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(appAuditPublisher).publish(eq(AppAuditEvent.APP_UPDATED), eq(1L), dataCaptor.capture());

        assertThat(dataCaptor.getValue()).containsOnly(
            Map.entry("changedFields", List.of("tagIds")), Map.entry("tagIds", List.of(5L, 6L)));
    }

    @Test
    void testUpdateAppTagsWithAnUnknownTagIdIsRejectedBeforeAnythingIsStored() {
        when(appService.getApp(1L)).thenReturn(persisted(new App(), 1L));
        when(tagService.getTags(List.of(99L))).thenReturn(List.of());

        assertThatThrownBy(() -> appFacade.updateAppTags(1L, List.of(new AppTagDTO(99L, "gone"))))
            .isInstanceOf(ConfigurationException.class);

        verify(appService, never()).updateTags(anyLong(), any());
        verify(tagService, never()).save(any());
        verifyNoInteractions(appAuditPublisher);
    }

    @Test
    void testUpdateAppTagsToTheStoredTagsStoresAndAuditsNothing() {
        App current = persisted(new App(), 1L);

        current.setTagIds(List.of(5L));

        when(appService.getApp(1L)).thenReturn(current);
        when(tagService.getTags(List.of(5L))).thenReturn(List.of(new Tag(5L, "crm")));

        appFacade.updateAppTags(1L, List.of(new AppTagDTO(5L, "crm")));

        verify(appService, never()).updateTags(anyLong(), any());
        verifyNoInteractions(appAuditPublisher);
    }

    @Test
    void testUpdateAppTagsWithNoTagsClearsThem() {
        App current = persisted(new App(), 1L);

        current.setTagIds(List.of(5L));

        when(appService.getApp(1L)).thenReturn(current);
        when(appService.updateTags(1L, List.of())).thenReturn(persisted(new App(), 1L));

        assertThat(appFacade.updateAppTags(1L, List.of())
            .tagIds()).isEmpty();

        verify(tagService, never()).save(any());
    }

    @Test
    void testGetAppTagsListsTheTagsOfTheVisibleAppsOnce() {
        App visibleApp = persisted(new App(), 1L);
        App otherVisibleApp = persisted(new App(), 2L);
        App hiddenApp = persisted(new App(), 3L);

        visibleApp.setTagIds(List.of(5L, 6L));
        otherVisibleApp.setTagIds(List.of(6L));
        hiddenApp.setTagIds(List.of(7L));

        when(appService.getApps(7L)).thenReturn(List.of(visibleApp, otherVisibleApp, hiddenApp));
        when(resourceVisibilityResolver.filterVisibleIds(eq("App"), eq(7L), anyCollection()))
            .thenReturn(Set.of(1L, 2L));
        when(tagService.getTags(List.of(5L, 6L))).thenReturn(List.of(new Tag(5L, "crm"), new Tag(6L, "internal")));

        assertThat(appFacade.getAppTags(7L)).containsExactly(new AppTagDTO(5L, "crm"), new AppTagDTO(6L, "internal"));
    }

    private void stubCreate() {
        stubCurrentUser(9L);

        when(appService.create(any())).thenAnswer(invocation -> persisted(invocation.getArgument(0), 1L));
        when(appInstanceService.create(any())).thenAnswer(invocation -> {
            AppInstance appInstance = invocation.getArgument(0);

            appInstance.setId(11L);

            return new AppInstanceService.CreatedAppInstance(appInstance, "secret");
        });
    }

    private App captureCreatedApp() {
        ArgumentCaptor<App> appCaptor = ArgumentCaptor.forClass(App.class);

        verify(appService).create(appCaptor.capture());

        return appCaptor.getValue();
    }

    @Test
    void testDeleteRemovesTheAppTheGrantsOfTheAppAndOfItsInstancesAndAudits() {
        when(appService.getApp(4L)).thenReturn(persisted(new App(), 4L));
        when(appInstanceService.getAppInstances(4L))
            .thenReturn(List.of(persistedInstance(40L, 4L), persistedInstance(41L, 4L)));

        appFacade.deleteApp(4L);

        verify(appService).delete(4L);
        verify(resourceGrantService).deleteGrants("App", 4L);
        verify(resourceGrantService).deleteGrants("AppInstance", 40L);
        verify(resourceGrantService).deleteGrants("AppInstance", 41L);
        verify(appAuditPublisher).publish(AppAuditEvent.APP_DELETED, 4L);
    }

    @Test
    void testDeletingAppDeletesIcon() {
        FileEntry icon = iconFileEntry("icon.png");
        App app = persisted(new App(), 4L);

        app.setIcon(icon);

        when(appService.getApp(4L)).thenReturn(app);

        appFacade.deleteApp(4L);

        InOrder inOrder = inOrder(appService, appFileStorage);

        inOrder.verify(appService)
            .delete(4L);
        inOrder.verify(appFileStorage)
            .deleteIcon(icon);
    }

    @Test
    void testDeletingAppWhoseIconFileCannotBeDeletedStillDeletesTheApp() {
        FileEntry icon = iconFileEntry("icon.png");
        App app = persisted(new App(), 4L);

        app.setIcon(icon);

        when(appService.getApp(4L)).thenReturn(app);
        doThrow(new IllegalStateException("storage down")).when(appFileStorage)
            .deleteIcon(icon);

        appFacade.deleteApp(4L);

        verify(appService).delete(4L);
        verify(appAuditPublisher).publish(AppAuditEvent.APP_DELETED, 4L);
    }

    @Test
    void testDeletingAppWithoutIconTouchesNoFile() {
        when(appService.getApp(4L)).thenReturn(persisted(new App(), 4L));

        appFacade.deleteApp(4L);

        verifyNoInteractions(appFileStorage);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("supportedIcons")
    void testSupportedIconIsStoredUnderItsDetectedType(String filename, byte[] content) {
        FileEntry icon = iconFileEntry(filename);

        when(appService.getApp(4L)).thenReturn(persisted(new App(), 4L));
        when(appFileStorage.storeIcon(filename, content)).thenReturn(icon);

        appFacade.updateAppIcon(4L, new ByteArrayInputStream(content));

        verify(appService).updateIcon(4L, icon);
        verify(appFileStorage, never()).deleteIcon(any());
        verify(appAuditPublisher).publish(AppAuditEvent.APP_UPDATED, 4L, Map.of("changedFields", List.of("icon")));
    }

    @Test
    void testIconAtTheLimitIsAccepted() {
        byte[] content = Arrays.copyOf(PNG, ICON_SIZE_LIMIT);

        when(appService.getApp(4L)).thenReturn(persisted(new App(), 4L));
        when(appFileStorage.storeIcon("icon.png", content)).thenReturn(iconFileEntry("icon.png"));

        appFacade.updateAppIcon(4L, new ByteArrayInputStream(content));

        verify(appFileStorage).storeIcon("icon.png", content);
    }

    @Test
    void testIconOfUnsupportedTypeIsRejected() {
        byte[] gif = "GIF89a\u0001\u0000\u0001\u0000".getBytes(StandardCharsets.ISO_8859_1);

        assertIconRejected(gif);
    }

    @Test
    void testSvgIsRejected() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
            .getBytes(StandardCharsets.UTF_8);

        assertIconRejected(svg);
    }

    @Test
    void testEmptyIconIsRejected() {
        assertIconRejected(new byte[0]);
    }

    @Test
    void testIconOverLimitIsRejected() {
        assertIconRejected(Arrays.copyOf(PNG, ICON_SIZE_LIMIT + 1));
    }

    @Test
    void testReplacingIconDeletesOldFile() {
        FileEntry previousIcon = iconFileEntry("icon.jpg");
        FileEntry icon = iconFileEntry("icon.png");
        App app = persisted(new App(), 4L);

        app.setIcon(previousIcon);

        when(appService.getApp(4L)).thenReturn(app);
        when(appFileStorage.storeIcon("icon.png", PNG)).thenReturn(icon);

        appFacade.updateAppIcon(4L, new ByteArrayInputStream(PNG));

        InOrder inOrder = inOrder(appFileStorage, appService);

        inOrder.verify(appFileStorage)
            .storeIcon("icon.png", PNG);
        inOrder.verify(appService)
            .updateIcon(4L, icon);
        inOrder.verify(appFileStorage)
            .deleteIcon(previousIcon);
    }

    @Test
    void testReplacingIconWhoseOldFileCannotBeDeletedStillStoresTheNewIcon() {
        FileEntry previousIcon = iconFileEntry("icon.jpg");
        FileEntry icon = iconFileEntry("icon.png");
        App app = persisted(new App(), 4L);

        app.setIcon(previousIcon);

        when(appService.getApp(4L)).thenReturn(app);
        when(appFileStorage.storeIcon("icon.png", PNG)).thenReturn(icon);
        doThrow(new IllegalStateException("storage down")).when(appFileStorage)
            .deleteIcon(previousIcon);

        appFacade.updateAppIcon(4L, new ByteArrayInputStream(PNG));

        verify(appService).updateIcon(4L, icon);
    }

    @Test
    void testDeleteAppIconClearsTheIconAndDeletesItsFile() {
        FileEntry icon = iconFileEntry("icon.png");
        App app = persisted(new App(), 4L);

        app.setIcon(icon);

        when(appService.getApp(4L)).thenReturn(app);

        appFacade.deleteAppIcon(4L);

        InOrder inOrder = inOrder(appService, appFileStorage);

        inOrder.verify(appService)
            .updateIcon(4L, null);
        inOrder.verify(appFileStorage)
            .deleteIcon(icon);
        verify(appAuditPublisher).publish(AppAuditEvent.APP_UPDATED, 4L, Map.of("changedFields", List.of("icon")));
    }

    @Test
    void testDeleteAppIconOfAnAppWithoutIconChangesNothing() {
        when(appService.getApp(4L)).thenReturn(persisted(new App(), 4L));

        appFacade.deleteAppIcon(4L);

        verify(appService, never()).updateIcon(anyLong(), any());
        verifyNoInteractions(appFileStorage, appAuditPublisher);
    }

    @Test
    void testGetAppIconAnswersTheStoredContentWithItsDetectedType() {
        FileEntry icon = iconFileEntry("icon.webp");
        App app = persisted(new App(), 4L);

        app.setIcon(icon);

        when(appService.getApp(4L)).thenReturn(app);
        when(appFileStorage.readIcon(icon)).thenReturn(WEBP);

        Optional<AppIconDTO> appIcon = appFacade.getAppIcon(4L);

        assertThat(appIcon).hasValueSatisfying(appIconDTO -> {
            assertThat(appIconDTO.content()).isEqualTo(WEBP);
            assertThat(appIconDTO.contentType()).isEqualTo("image/webp");
        });
    }

    @Test
    void testGetAppIconOfStoredContentThatIsNoSupportedImageIsRefused() {
        FileEntry icon = iconFileEntry("icon.png");
        App app = persisted(new App(), 4L);

        app.setIcon(icon);

        when(appService.getApp(4L)).thenReturn(app);
        when(appFileStorage.readIcon(icon)).thenReturn("<svg/>".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> appFacade.getAppIcon(4L))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_ICON_INVALID.getErrorKey()));
    }

    @Test
    void testGetAppIconOfAnAppWithoutIconIsEmpty() {
        when(appService.getApp(4L)).thenReturn(persisted(new App(), 4L));

        assertThat(appFacade.getAppIcon(4L)).isEmpty();

        verifyNoInteractions(appFileStorage);
    }

    @Test
    void testGetAppTellsWhetherTheAppHasAnIconWithoutCarryingTheFileEntry() {
        App app = persisted(new App(), 4L);

        app.setIcon(iconFileEntry("icon.png"));

        when(appService.getApp(4L)).thenReturn(app);
        when(appInstanceService.getAppInstances(4L)).thenReturn(List.of(persistedInstance(40L, 4L)));

        AppDTO appDTO = appFacade.getApp(4L);

        assertThat(appDTO.hasIcon()).isTrue();
        assertThat(appDTO.instances()).singleElement()
            .satisfies(appInstanceDTO -> assertThat(appInstanceDTO.app()
                .hasIcon()).isTrue());
    }

    @Test
    void testDeleteNeedsManageInEveryEnvironment() {
        when(appService.getApp(4L)).thenReturn(persisted(new App(), 4L));
        when(permissionService.hasWorkspaceScopeInEveryEnvironment(7L, "APP_MANAGE")).thenReturn(false);

        assertThatThrownBy(() -> appFacade.deleteApp(4L)).isInstanceOf(AccessDeniedException.class);

        verify(appService, never()).delete(anyLong());
        verifyNoInteractions(resourceGrantService, appAuditPublisher);
    }

    @Test
    void testUpdateStatusNeedsManageInEveryEnvironment() {
        when(appService.getApp(1L)).thenReturn(persisted(new App(), 1L));
        when(permissionService.hasWorkspaceScopeInEveryEnvironment(7L, "APP_MANAGE")).thenReturn(false);

        assertThatThrownBy(() -> appFacade.updateAppStatus(1L, AppStatus.SUSPENDED))
            .isInstanceOf(AccessDeniedException.class);

        verify(appService, never()).updateStatus(anyLong(), any());
        verifyNoInteractions(appAuditPublisher);
    }

    @Test
    void testGetAppShowsInstancesOfOtherWorkspacesAsForeignRows() {
        AppInstance ownAppInstance = persistedInstance(11L, 1L);
        AppInstance foreignAppInstance = persistedInstance(12L, 1L);

        ownAppInstance.setSecretKeyLastUsedDate(Instant.EPOCH);
        foreignAppInstance.setWorkspaceId(8L);
        foreignAppInstance.setName("Germany");
        foreignAppInstance.setCurrentAppDeploymentId(5L);

        when(appService.getApp(1L)).thenReturn(persisted(new App(), 1L));
        when(appInstanceService.getAppInstances(1L)).thenReturn(List.of(ownAppInstance, foreignAppInstance));

        AppDTO appDTO = appFacade.getApp(1L);

        assertThat(appDTO.instances())
            .extracting(
                AppInstanceDTO::id, AppInstanceDTO::workspaceId, AppInstanceDTO::secretKeyHint,
                AppInstanceDTO::secretKeyLastUsedDate)
            .containsExactly(tuple(11L, 7L, "11", Instant.EPOCH));
        assertThat(appDTO.foreignInstances()).containsExactly(
            new ForeignAppInstanceDTO(12L, "Germany", 8L, Environment.PRODUCTION, AppInstanceStatus.ACTIVE, 5L));

        verify(resourceVisibilityResolver).filterVisibleIds(
            eq("AppInstance"), eq(7L),
            eq(List.of(new VisibilityRecord(11L, ResourceVisibility.WORKSPACE, null))));
    }

    @Test
    void testGetAppLeavesOutInstancesTheCallerCannotSee() {
        AppInstance visibleAppInstance = persistedInstance(11L, 1L);
        AppInstance privateAppInstance = persistedInstance(12L, 1L);

        privateAppInstance.setVisibility(ResourceVisibility.PRIVATE);

        when(appService.getApp(1L)).thenReturn(persisted(new App(), 1L));
        when(appInstanceService.getAppInstances(1L)).thenReturn(List.of(visibleAppInstance, privateAppInstance));
        when(resourceVisibilityResolver.filterVisibleIds(eq("AppInstance"), eq(7L), anyCollection()))
            .thenReturn(Set.of(11L));

        AppDTO appDTO = appFacade.getApp(1L);

        assertThat(appDTO.instances()).extracting(AppInstanceDTO::id)
            .containsExactly(11L);
        assertThat(appDTO.foreignInstances()).isEmpty();
    }

    @Test
    void testGetAppLeavesOutInstancesOfAnEnvironmentWithoutAppView() {
        AppInstance developmentAppInstance = persistedInstance(11L, 1L);
        AppInstance productionAppInstance = persistedInstance(12L, 1L);
        AppInstance secondProductionAppInstance = persistedInstance(13L, 1L);

        developmentAppInstance.setEnvironment(Environment.DEVELOPMENT);

        when(appService.getApp(1L)).thenReturn(persisted(new App(), 1L));
        when(appInstanceService.getAppInstances(1L))
            .thenReturn(List.of(developmentAppInstance, productionAppInstance, secondProductionAppInstance));
        when(permissionService.hasWorkspaceScope(7L, "APP_VIEW", Environment.PRODUCTION)).thenReturn(false);

        AppDTO appDTO = appFacade.getApp(1L);

        assertThat(appDTO.instances()).extracting(AppInstanceDTO::id)
            .containsExactly(11L);

        verify(permissionService).hasWorkspaceScope(7L, "APP_VIEW", Environment.PRODUCTION);
        verify(permissionService).hasWorkspaceScope(7L, "APP_VIEW", Environment.DEVELOPMENT);
    }

    @Test
    void testGetAppsReadsTheInstancesOfAllVisibleAppsInOneQuery() {
        AppInstance stagingAppInstance = persistedInstance(11L, 1L);
        AppInstance productionAppInstance = persistedInstance(21L, 2L);

        stagingAppInstance.setEnvironment(Environment.STAGING);

        when(appService.getApps(7L)).thenReturn(List.of(persisted(new App(), 1L), persisted(new App(), 2L)));
        when(resourceVisibilityResolver.filterVisibleIds(eq("App"), eq(7L), anyCollection()))
            .thenReturn(Set.of(1L, 2L));
        when(appInstanceService.getAppInstancesByAppIds(List.of(1L, 2L)))
            .thenReturn(List.of(stagingAppInstance, productionAppInstance));

        List<AppDTO> apps = appFacade.getApps(7L);

        assertThat(apps).extracting(AppDTO::id)
            .containsExactly(1L, 2L);
        assertThat(apps.getFirst()
            .instances()).extracting(AppInstanceDTO::environment)
                .containsExactly(Environment.STAGING);
        assertThat(apps.getLast()
            .instances()).extracting(AppInstanceDTO::environment)
                .containsExactly(Environment.PRODUCTION);

        verify(appInstanceService, never()).getAppInstances(anyLong());
        verify(resourceVisibilityResolver).filterVisibleIds(eq("AppInstance"), eq(7L), anyCollection());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetAppsLeavesOutAppsTheCallerCannotSee() {
        App visible = persisted(new App(), 1L);
        App hidden = persisted(new App(), 2L);

        visible.setVisibility(ResourceVisibility.WORKSPACE);
        hidden.setVisibility(ResourceVisibility.PRIVATE);
        hidden.setCreatedBy("someone-else");

        when(appService.getApps(7L)).thenReturn(List.of(visible, hidden));
        when(resourceVisibilityResolver.filterVisibleIds(eq("App"), eq(7L), anyCollection())).thenReturn(Set.of(1L));
        when(appInstanceService.getAppInstancesByAppIds(List.of(1L))).thenReturn(List.of(persistedInstance(11L, 1L)));

        assertThat(appFacade.getApps(7L)).extracting(AppDTO::id)
            .containsExactly(1L);

        ArgumentCaptor<Collection<VisibilityRecord>> candidates = ArgumentCaptor.forClass(Collection.class);

        verify(resourceVisibilityResolver).filterVisibleIds(eq("App"), eq(7L), candidates.capture());

        assertThat(candidates.getValue())
            .containsExactlyInAnyOrder(
                new VisibilityRecord(1L, ResourceVisibility.WORKSPACE, "admin"),
                new VisibilityRecord(2L, ResourceVisibility.PRIVATE, "someone-else"));
    }

    @Test
    void testGetAppsOfAWorkspaceWithoutVisibleAppsReadsNoInstances() {
        when(appService.getApps(7L)).thenReturn(List.of());
        when(resourceVisibilityResolver.filterVisibleIds(eq("App"), eq(7L), anyCollection())).thenReturn(Set.of());

        assertThat(appFacade.getApps(7L)).isEmpty();

        verifyNoInteractions(appInstanceService);
    }

    @Test
    void testUpdateStatusPublishesTheMatchingLifecycleEvent() {
        when(appService.getApp(anyLong())).thenAnswer(invocation -> persisted(new App(), invocation.getArgument(0)));
        when(appService.updateStatus(1L, AppStatus.SUSPENDED)).thenReturn(persisted(new App(), 1L));
        when(appService.updateStatus(2L, AppStatus.ACTIVE)).thenReturn(persisted(new App(), 2L));
        when(appService.updateStatus(3L, AppStatus.ARCHIVED)).thenReturn(persisted(new App(), 3L));

        appFacade.updateAppStatus(1L, AppStatus.SUSPENDED);
        appFacade.updateAppStatus(2L, AppStatus.ACTIVE);
        appFacade.updateAppStatus(3L, AppStatus.ARCHIVED);

        verify(appAuditPublisher).publish(AppAuditEvent.APP_SUSPENDED, 1L);
        verify(appAuditPublisher).publish(AppAuditEvent.APP_ACTIVATED, 2L);
        verify(appAuditPublisher).publish(AppAuditEvent.APP_ARCHIVED, 3L);
    }

    @Test
    void testGetComponentOperationsReturnsSortedActionAndToolNames() {
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

    @Test
    void testGetConnectionsMarksAiProviderAndUsableConnectionsAndNeverCarriesParameters() {
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

    private void stubCurrentUser(long userId) {
        User user = new User();

        user.setId(userId);

        when(userService.getCurrentUser()).thenReturn(user);
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

    private void assertIconRejected(byte[] content) {
        assertThatThrownBy(() -> appFacade.updateAppIcon(4L, new ByteArrayInputStream(content)))
            .isInstanceOfSatisfying(
                ConfigurationException.class,
                exception -> assertThat(exception.getErrorKey())
                    .isEqualTo(AppErrorType.APP_ICON_INVALID.getErrorKey()));

        verifyNoInteractions(appFileStorage, appAuditPublisher);
        verify(appService, never()).updateIcon(anyLong(), any());
    }

    private static FileEntry iconFileEntry(String filename) {
        return new FileEntry(filename, "base64:///app_icons/" + UUID.randomUUID() + "/" + filename);
    }

    private static List<Arguments> supportedIcons() {
        return List.of(
            Arguments.of("icon.jpg", JPEG), Arguments.of("icon.png", PNG), Arguments.of("icon.webp", WEBP));
    }

    private static App persisted(App app, long id) {
        app.setId(id);
        app.setUuid("uuid");
        app.setName("crm");
        app.setWorkspaceId(7L);
        app.setOwnerUserId(9L);
        app.setStatus(AppStatus.DRAFT);
        app.setCreatedBy("admin");
        app.setCreatedDate(Instant.EPOCH);
        app.setLastModifiedBy("admin");
        app.setLastModifiedDate(Instant.EPOCH);

        return app;
    }

    @Test
    void testFirstInstanceDefaultsToRequired() {
        stubCurrentUser(9L);

        when(appService.create(any())).thenAnswer(invocation -> persisted(invocation.getArgument(0), 1L));
        when(appInstanceService.create(any())).thenAnswer(invocation -> {
            AppInstance appInstance = invocation.getArgument(0);

            appInstance.setId(11L);

            return new AppInstanceService.CreatedAppInstance(appInstance, "secret");
        });

        appFacade.createApp(
            7L, Environment.DEVELOPMENT, "crm", null, AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE, null,
            null, false);

        ArgumentCaptor<AppInstance> appInstanceCaptor = ArgumentCaptor.forClass(AppInstance.class);

        verify(appInstanceService).create(appInstanceCaptor.capture());

        AppInstance createdAppInstance = appInstanceCaptor.getValue();

        assertThat(createdAppInstance.getUserAuthMode()).isEqualTo(AppUserAuthMode.REQUIRED);

        verify(registeredClientService, never()).registerClient(any());
    }

    @Test
    void testFirstInstanceDefaultsToNoneWithAuthorizationServerOff() {
        enableAuthorizationServer(false);
        stubCurrentUser(9L);

        when(appService.create(any())).thenAnswer(invocation -> persisted(invocation.getArgument(0), 1L));
        when(appInstanceService.create(any())).thenAnswer(invocation -> {
            AppInstance appInstance = invocation.getArgument(0);

            appInstance.setId(11L);

            return new AppInstanceService.CreatedAppInstance(appInstance, "secret");
        });

        appFacade.createApp(
            7L, Environment.DEVELOPMENT, "crm", null, AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE, null,
            null, false);

        ArgumentCaptor<AppInstance> appInstanceCaptor = ArgumentCaptor.forClass(AppInstance.class);

        verify(appInstanceService).create(appInstanceCaptor.capture());

        AppInstance createdAppInstance = appInstanceCaptor.getValue();

        assertThat(createdAppInstance.getUserAuthMode()).isEqualTo(AppUserAuthMode.NONE);
    }

    @Test
    void testExplicitRequiredCreateWithAuthorizationServerOffIsRefusedNamingTheProperty() {
        enableAuthorizationServer(false);

        assertThatThrownBy(
            () -> appFacade.createApp(
                7L, Environment.DEVELOPMENT, "crm", null, AppHostingTarget.VERCEL, ResourceVisibility.WORKSPACE,
                AppUserAuthMode.REQUIRED, null, false))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("bytechef.oauth2.authorization-server.enabled")
                    .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(AppErrorType.APP_USER_AUTH_UNAVAILABLE.getErrorKey());

        verifyNoInteractions(appService, appInstanceService, appAuditPublisher);
    }

    @Test
    void testUserAuthAvailabilityFollowsTheAuthorizationServer() {
        assertThat(appFacade.isUserAuthAvailable(7L)).isTrue();

        enableAuthorizationServer(false);

        assertThat(appFacade.isUserAuthAvailable(7L)).isFalse();
    }

    @Test
    void testDeleteDeletesTheClientsOfItsInstances() {
        AppInstance registeredInstance = persistedInstance(40L, 4L);

        registeredInstance.setUserAuthMode(AppUserAuthMode.REQUIRED);
        registeredInstance.setRegisteredClientId("app-registered");

        when(appService.getApp(4L)).thenReturn(persisted(new App(), 4L));
        when(appInstanceService.getAppInstances(4L))
            .thenReturn(List.of(registeredInstance, persistedInstance(41L, 4L)));

        appFacade.deleteApp(4L);

        verify(registeredClientService).deleteClient("app-registered");
        verify(registeredClientService, never()).deleteClient("app-instance-uuid");
    }

    @ParameterizedTest
    @EnumSource(value = AppStatus.class, names = {
        "SUSPENDED", "ARCHIVED"
    })
    void testSuspendingOrArchivingTheAppRevokesTheGrantsOfItsInstances(AppStatus appStatus) {
        AppInstance registeredInstance = persistedInstance(40L, 1L);

        registeredInstance.setUserAuthMode(AppUserAuthMode.REQUIRED);
        registeredInstance.setRegisteredClientId("app-registered");

        when(appService.getApp(1L)).thenReturn(persisted(new App(), 1L));
        when(appService.updateStatus(1L, appStatus)).thenReturn(persisted(new App(), 1L));
        when(appInstanceService.getAppInstances(1L))
            .thenReturn(List.of(registeredInstance, persistedInstance(41L, 1L)));
        when(registeredClientService.fetchRegisteredClientId("app-registered"))
            .thenReturn(Optional.of("registered-id"));

        appFacade.updateAppStatus(1L, appStatus);

        verify(authorizationGrantService).revokeGrants("registered-id");
        verify(registeredClientService, never()).deleteClient(any());
    }

    @Test
    void testActivatingTheAppRevokesNoGrant() {
        when(appService.getApp(1L)).thenReturn(persisted(new App(), 1L));
        when(appService.updateStatus(1L, AppStatus.ACTIVE)).thenReturn(persisted(new App(), 1L));

        appFacade.updateAppStatus(1L, AppStatus.ACTIVE);

        verifyNoInteractions(authorizationGrantService);
    }

    private void enableAuthorizationServer(boolean enabled) {
        lenient().when(registeredClientServiceProvider.getIfAvailable())
            .thenReturn(enabled ? registeredClientService : null);
        lenient().when(registeredClientServiceProvider.getObject())
            .thenReturn(registeredClientService);
        lenient().when(authorizationGrantServiceProvider.getIfAvailable())
            .thenReturn(enabled ? authorizationGrantService : null);
    }

    private static AppInstance persistedInstance(long id, long appId) {
        AppInstance appInstance = new AppInstance();

        appInstance.setId(id);
        appInstance.setAppId(appId);
        appInstance.setUuid("instance-uuid");
        appInstance.setName("Production");
        appInstance.setWorkspaceId(7L);
        appInstance.setOwnerUserId(9L);
        appInstance.setEnvironment(Environment.PRODUCTION);
        appInstance.setHostingTarget(AppHostingTarget.VERCEL);
        appInstance.setSecretKeyHint(String.valueOf(id));

        return appInstance;
    }

    private static String preAuthorize(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = AppFacadeImpl.class.getMethod(methodName, parameterTypes);

        PreAuthorize preAuthorize = Objects.requireNonNull(method.getAnnotation(PreAuthorize.class));

        return preAuthorize.value();
    }
}
