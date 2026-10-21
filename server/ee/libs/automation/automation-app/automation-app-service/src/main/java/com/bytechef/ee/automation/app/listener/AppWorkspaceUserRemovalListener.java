/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.listener;

import com.bytechef.automation.configuration.event.WorkspaceUserRemovedEvent;
import com.bytechef.ee.automation.app.audit.AppAuditEvent;
import com.bytechef.ee.automation.app.audit.AppAuditPublisher;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.security.AppInstanceClients;
import com.bytechef.ee.automation.app.service.AppFavoriteService;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.automation.app.service.AppInstanceUsageService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Reacts to {@link WorkspaceUserRemovedEvent} inside the removing transaction by revoking the removed member's grants
 * on the apps of the workspace and on the app instances that live in it, deleting their usage rows of those instances
 * and their portal favorites of the apps those instances belong to, unless another workspace the member still belongs
 * to hosts an ACTIVE instance of the app, and after commit their OAuth2 authorizations on those instances' clients.
 * Ownership and visibility are left alone; a PRIVATE app or instance the member owned is reported through an owner-left
 * audit event, because only a tenant admin can manage it from then on.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AppWorkspaceUserRemovalListener {

    private static final String APP_INSTANCE_ID = "appInstanceId";
    private static final String APP_INSTANCE_RESOURCE_TYPE = "AppInstance";
    private static final String APP_RESOURCE_TYPE = "App";
    private static final String USER_ID = "userId";
    private static final String WORKSPACE_ID = "workspaceId";

    private final AppAuditPublisher appAuditPublisher;
    private final AppFavoriteService appFavoriteService;
    private final AppInstanceClients appInstanceClients;
    private final AppInstanceService appInstanceService;
    private final AppInstanceUsageService appInstanceUsageService;
    private final AppService appService;
    private final ResourceGrantService resourceGrantService;
    private final WorkspaceUserService workspaceUserService;

    @SuppressFBWarnings("EI")
    public AppWorkspaceUserRemovalListener(
        AppAuditPublisher appAuditPublisher, AppFavoriteService appFavoriteService,
        AppInstanceClients appInstanceClients, AppInstanceService appInstanceService,
        AppInstanceUsageService appInstanceUsageService, AppService appService,
        ResourceGrantService resourceGrantService, WorkspaceUserService workspaceUserService) {

        this.appAuditPublisher = appAuditPublisher;
        this.appFavoriteService = appFavoriteService;
        this.appInstanceClients = appInstanceClients;
        this.appInstanceService = appInstanceService;
        this.appInstanceUsageService = appInstanceUsageService;
        this.appService = appService;
        this.resourceGrantService = resourceGrantService;
        this.workspaceUserService = workspaceUserService;
    }

    @EventListener
    public void onWorkspaceUserRemoved(WorkspaceUserRemovedEvent event) {
        long userId = event.userId();
        long workspaceId = event.workspaceId();

        List<App> apps = appService.getApps(workspaceId);

        revokeGrants(APP_RESOURCE_TYPE, userId, apps.stream()
            .map(App::getId)
            .toList());

        for (App app : apps) {
            if (isPrivateOwnedBy(app.getVisibility(), app.getOwnerUserId(), userId)) {
                appAuditPublisher.publish(
                    AppAuditEvent.APP_OWNER_LEFT, app.getId(), Map.of(USER_ID, userId, WORKSPACE_ID, workspaceId));
            }
        }

        List<AppInstance> appInstances = new ArrayList<>();

        for (Environment environment : Environment.values()) {
            appInstances.addAll(appInstanceService.getWorkspaceAppInstances(workspaceId, environment));
        }

        List<Long> appInstanceIds = appInstances.stream()
            .map(AppInstance::getId)
            .toList();

        revokeGrants(APP_INSTANCE_RESOURCE_TYPE, userId, appInstanceIds);

        appInstanceUsageService.deleteUsages(userId, appInstanceIds);

        appFavoriteService.deleteFavorites(
            userId, appInstances.stream()
                .map(AppInstance::getAppId)
                .distinct()
                .filter(appId -> !isReachableThroughAnotherWorkspace(appId, userId, workspaceId))
                .toList());

        for (AppInstance appInstance : appInstances) {
            if (isPrivateOwnedBy(appInstance.getVisibility(), appInstance.getOwnerUserId(), userId)) {
                appAuditPublisher.publish(
                    AppAuditEvent.APP_INSTANCE_OWNER_LEFT, appInstance.getAppId(),
                    Map.of(APP_INSTANCE_ID, appInstance.getId(), USER_ID, userId, WORKSPACE_ID, workspaceId));
            }
        }

        List<String> clientIds = appInstances.stream()
            .map(AppInstance::getRegisteredClientId)
            .filter(Objects::nonNull)
            .toList();

        if (!clientIds.isEmpty()) {
            revokeAuthorizationsAfterCommit(clientIds, event.userLogin());
        }
    }

    private boolean isReachableThroughAnotherWorkspace(long appId, long userId, long removedWorkspaceId) {
        return appInstanceService.getAppInstances(appId)
            .stream()
            .filter(appInstance -> appInstance.getStatus() == AppInstanceStatus.ACTIVE)
            .map(AppInstance::getWorkspaceId)
            .filter(Objects::nonNull)
            .filter(workspaceId -> workspaceId != removedWorkspaceId)
            .distinct()
            .anyMatch(workspaceId -> workspaceUserService.isWorkspaceMember(userId, workspaceId));
    }

    private void revokeAuthorizations(List<String> clientIds, String userLogin) {
        for (String clientId : clientIds) {
            appInstanceClients.revokeGrant(clientId, userLogin);
        }
    }

    private void revokeAuthorizationsAfterCommit(List<String> clientIds, String userLogin) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            revokeAuthorizations(clientIds, userLogin);

            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCommit() {
                revokeAuthorizations(clientIds, userLogin);
            }
        });
    }

    private static boolean isPrivateOwnedBy(ResourceVisibility visibility, Long ownerUserId, long userId) {
        return visibility == ResourceVisibility.PRIVATE && ownerUserId != null && ownerUserId == userId;
    }

    private void revokeGrants(String resourceType, long userId, List<Long> resourceIds) {
        if (resourceIds.isEmpty()) {
            return;
        }

        resourceGrantService.revokeUserGrants(resourceType, userId, resourceIds);
    }
}
