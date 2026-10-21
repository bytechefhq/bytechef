/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.ee.automation.app.audit.AppAuditEvent;
import com.bytechef.ee.automation.app.audit.AppAuditPublisher;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class AppSharingFacadeImpl implements AppSharingFacade {

    private static final String APP = "App";
    private static final String APP_INSTANCE = "AppInstance";
    private static final String APP_INSTANCE_ID = "appInstanceId";
    private static final String PREVIOUS_VISIBILITY = "previousVisibility";
    private static final String USER_ID = "userId";
    private static final String VISIBILITY = "visibility";

    private final AppAuditPublisher appAuditPublisher;
    private final AppInstanceService appInstanceService;
    private final AppService appService;
    private final ResourceGrantService resourceGrantService;
    private final ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry;
    private final WorkspaceUserService workspaceUserService;

    @SuppressFBWarnings("EI")
    public AppSharingFacadeImpl(
        AppAuditPublisher appAuditPublisher, AppInstanceService appInstanceService, AppService appService,
        ResourceGrantService resourceGrantService, ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry,
        WorkspaceUserService workspaceUserService) {

        this.appAuditPublisher = appAuditPublisher;
        this.appInstanceService = appInstanceService;
        this.appService = appService;
        this.resourceGrantService = resourceGrantService;
        this.resourceVisibilityPolicyRegistry = resourceVisibilityPolicyRegistry;
        this.workspaceUserService = workspaceUserService;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("@permissionService.isResourceOwner('App', #appId) || " +
        "hasPermission(#appId, 'App', 'APP_MANAGE')")
    public List<Long> getAppGrants(long workspaceId, long appId) {
        validateAppBelongsToWorkspace(workspaceId, appId);

        return resourceGrantService.getGrantedUserIds(APP, appId);
    }

    @Override
    @PreAuthorize("@permissionService.isResourceOwner('App', #appId) || " +
        "hasPermission(#appId, 'App', 'APP_MANAGE')")
    public void grantAppAccess(long workspaceId, long appId, long userId) {
        validateAppBelongsToWorkspace(workspaceId, appId);

        if (!workspaceUserService.isWorkspaceMember(userId, workspaceId)) {
            throw newNotAMemberException(userId, workspaceId, AppErrorType.APP_NOT_FOUND);
        }

        resourceGrantService.grant(APP, appId, userId);

        appAuditPublisher.publish(AppAuditEvent.APP_ACCESS_GRANTED, appId, Map.of(USER_ID, userId));
    }

    @Override
    @PreAuthorize("@permissionService.isResourceOwner('App', #appId) || " +
        "hasPermission(#appId, 'App', 'APP_MANAGE')")
    public void revokeAppAccess(long workspaceId, long appId, long userId) {
        validateAppBelongsToWorkspace(workspaceId, appId);

        resourceGrantService.revoke(APP, appId, userId);

        appAuditPublisher.publish(AppAuditEvent.APP_ACCESS_REVOKED, appId, Map.of(USER_ID, userId));
    }

    @Override
    @PreAuthorize("@permissionService.isResourceOwner('App', #appId) || " +
        "hasPermission(#appId, 'App', 'APP_MANAGE')")
    public void setAppVisibility(long workspaceId, long appId, ResourceVisibility visibility) {
        checkVisibilitySupported(APP, visibility, "App");

        App app = validateAppBelongsToWorkspace(workspaceId, appId);

        ResourceVisibility previousVisibility = app.getVisibility();

        if (visibility == ResourceVisibility.PRIVATE && previousVisibility != ResourceVisibility.PRIVATE) {
            resourceGrantService.deleteGrants(APP, appId);
        }

        appService.updateVisibility(appId, visibility);

        appAuditPublisher.publish(
            AppAuditEvent.APP_VISIBILITY_CHANGED, appId,
            Map.of(VISIBILITY, visibility.name(), PREVIOUS_VISIBILITY, String.valueOf(previousVisibility)));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("@permissionService.isResourceOwner('AppInstance', #appInstanceId) || " +
        "hasPermission(#appInstanceId, 'AppInstance', 'APP_MANAGE')")
    public List<Long> getAppInstanceGrants(long workspaceId, long appInstanceId) {
        validateAppInstanceBelongsToWorkspace(workspaceId, appInstanceId);

        return resourceGrantService.getGrantedUserIds(APP_INSTANCE, appInstanceId);
    }

    @Override
    @PreAuthorize("@permissionService.isResourceOwner('AppInstance', #appInstanceId) || " +
        "hasPermission(#appInstanceId, 'AppInstance', 'APP_MANAGE')")
    public void grantAppInstanceAccess(long workspaceId, long appInstanceId, long userId) {
        AppInstance appInstance = validateAppInstanceBelongsToWorkspace(workspaceId, appInstanceId);

        if (!workspaceUserService.isWorkspaceMember(userId, appInstance.getWorkspaceId())) {
            throw newNotAMemberException(userId, appInstance.getWorkspaceId(), AppErrorType.APP_INSTANCE_NOT_FOUND);
        }

        resourceGrantService.grant(APP_INSTANCE, appInstanceId, userId);

        appAuditPublisher.publish(
            AppAuditEvent.APP_INSTANCE_ACCESS_GRANTED, appInstance.getAppId(),
            Map.of(APP_INSTANCE_ID, appInstanceId, USER_ID, userId));
    }

    @Override
    @PreAuthorize("@permissionService.isResourceOwner('AppInstance', #appInstanceId) || " +
        "hasPermission(#appInstanceId, 'AppInstance', 'APP_MANAGE')")
    public void revokeAppInstanceAccess(long workspaceId, long appInstanceId, long userId) {
        AppInstance appInstance = validateAppInstanceBelongsToWorkspace(workspaceId, appInstanceId);

        resourceGrantService.revoke(APP_INSTANCE, appInstanceId, userId);

        appAuditPublisher.publish(
            AppAuditEvent.APP_INSTANCE_ACCESS_REVOKED, appInstance.getAppId(),
            Map.of(APP_INSTANCE_ID, appInstanceId, USER_ID, userId));
    }

    @Override
    @PreAuthorize("@permissionService.isResourceOwner('AppInstance', #appInstanceId) || " +
        "hasPermission(#appInstanceId, 'AppInstance', 'APP_MANAGE')")
    public void setAppInstanceVisibility(long workspaceId, long appInstanceId, ResourceVisibility visibility) {
        checkVisibilitySupported(APP_INSTANCE, visibility, "App instance");

        AppInstance appInstance = validateAppInstanceBelongsToWorkspace(workspaceId, appInstanceId);

        ResourceVisibility previousVisibility = appInstance.getVisibility();

        if (visibility == ResourceVisibility.PRIVATE && previousVisibility != ResourceVisibility.PRIVATE) {
            resourceGrantService.deleteGrants(APP_INSTANCE, appInstanceId);
        }

        appInstanceService.updateVisibility(appInstanceId, visibility);

        appAuditPublisher.publish(
            AppAuditEvent.APP_INSTANCE_VISIBILITY_CHANGED, appInstance.getAppId(),
            Map.of(
                APP_INSTANCE_ID, appInstanceId, VISIBILITY, visibility.name(), PREVIOUS_VISIBILITY,
                String.valueOf(previousVisibility)));
    }

    private void checkVisibilitySupported(String resourceType, ResourceVisibility visibility, String label) {
        if (!resourceVisibilityPolicyRegistry.supports(resourceType, visibility)) {
            throw new ConfigurationException(
                "%s does not support %s visibility".formatted(label, visibility),
                AppErrorType.APP_VISIBILITY_UNSUPPORTED);
        }
    }

    private App validateAppBelongsToWorkspace(long workspaceId, long appId) {
        App app = appService.fetchApp(appId)
            .orElseThrow(() -> newNotInWorkspaceException("App", appId, workspaceId, AppErrorType.APP_NOT_FOUND));

        if (!Objects.equals(app.getWorkspaceId(), workspaceId)) {
            throw newNotInWorkspaceException("App", appId, workspaceId, AppErrorType.APP_NOT_FOUND);
        }

        return app;
    }

    private AppInstance validateAppInstanceBelongsToWorkspace(long workspaceId, long appInstanceId) {
        AppInstance appInstance = appInstanceService.fetchAppInstance(appInstanceId)
            .orElseThrow(() -> newNotInWorkspaceException(
                "App instance", appInstanceId, workspaceId, AppErrorType.APP_INSTANCE_NOT_FOUND));

        if (!Objects.equals(appInstance.getWorkspaceId(), workspaceId)) {
            throw newNotInWorkspaceException(
                "App instance", appInstanceId, workspaceId, AppErrorType.APP_INSTANCE_NOT_FOUND);
        }

        return appInstance;
    }

    private static ConfigurationException newNotAMemberException(
        long userId, long workspaceId, AppErrorType appErrorType) {

        return new ConfigurationException(
            "User id=%s is not a member of workspace id=%s".formatted(userId, workspaceId), appErrorType);
    }

    private static ConfigurationException newNotInWorkspaceException(
        String label, long id, long workspaceId, AppErrorType appErrorType) {

        return new ConfigurationException(
            "%s id=%s does not belong to workspace id=%s".formatted(label, id, workspaceId), appErrorType);
    }
}
