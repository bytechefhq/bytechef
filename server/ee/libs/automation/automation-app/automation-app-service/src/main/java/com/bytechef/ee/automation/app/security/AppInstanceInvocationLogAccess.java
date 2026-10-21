/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.ee.platform.external.invocation.log.security.ExternalInvocationLogAppInstanceAccess;
import com.bytechef.ee.platform.external.invocation.log.security.ViewableAppInstances;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AppInstanceInvocationLogAccess implements ExternalInvocationLogAppInstanceAccess {

    private static final String APP_INSTANCE = "AppInstance";
    private static final String APP_VIEW = "APP_VIEW";

    private final AppInstanceService appInstanceService;
    private final PermissionService permissionService;
    private final ResourceVisibilityResolver resourceVisibilityResolver;

    @SuppressFBWarnings("EI")
    public AppInstanceInvocationLogAccess(
        AppInstanceService appInstanceService, PermissionService permissionService,
        ResourceVisibilityResolver resourceVisibilityResolver) {

        this.appInstanceService = appInstanceService;
        this.permissionService = permissionService;
        this.resourceVisibilityResolver = resourceVisibilityResolver;
    }

    @Override
    public ViewableAppInstances getViewableAppInstances(long workspaceId, int environment) {
        if (permissionService.isTenantAdmin()) {
            return ViewableAppInstances.all();
        }

        return ViewableAppInstances.only(getViewableAppInstanceIds(workspaceId, environment));
    }

    private Set<Long> getViewableAppInstanceIds(long workspaceId, int environment) {
        Environment[] environments = Environment.values();

        if (environment < 0 || environment >= environments.length) {
            return Set.of();
        }

        Environment appInstanceEnvironment = environments[environment];

        if (!permissionService.hasWorkspaceScope(workspaceId, APP_VIEW, appInstanceEnvironment)) {
            return Set.of();
        }

        List<AppInstance> appInstances = appInstanceService.getWorkspaceAppInstances(
            workspaceId, appInstanceEnvironment);

        if (appInstances.isEmpty()) {
            return Set.of();
        }

        return resourceVisibilityResolver.filterVisibleIds(
            APP_INSTANCE, workspaceId,
            appInstances.stream()
                .map(appInstance -> new VisibilityRecord(
                    appInstance.getId(), appInstance.getVisibility(), appInstance.getCreatedBy()))
                .toList());
    }
}
