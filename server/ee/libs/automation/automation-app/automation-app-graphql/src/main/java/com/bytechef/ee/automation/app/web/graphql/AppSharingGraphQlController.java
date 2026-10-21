/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.app.facade.AppSharingFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for app and app instance visibility and sharing.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
class AppSharingGraphQlController {

    private final AppSharingFacade appSharingFacade;

    @SuppressFBWarnings("EI")
    AppSharingGraphQlController(AppSharingFacade appSharingFacade) {
        this.appSharingFacade = appSharingFacade;
    }

    @QueryMapping
    public List<Long> appGrants(@Argument long workspaceId, @Argument long appId) {
        return appSharingFacade.getAppGrants(workspaceId, appId);
    }

    @QueryMapping
    public List<Long> appInstanceGrants(@Argument long workspaceId, @Argument long appInstanceId) {
        return appSharingFacade.getAppInstanceGrants(workspaceId, appInstanceId);
    }

    @MutationMapping
    public boolean setAppVisibility(
        @Argument long workspaceId, @Argument long appId, @Argument ResourceVisibility visibility) {

        appSharingFacade.setAppVisibility(workspaceId, appId, visibility);

        return true;
    }

    @MutationMapping
    public boolean grantAppAccess(@Argument long workspaceId, @Argument long appId, @Argument long userId) {
        appSharingFacade.grantAppAccess(workspaceId, appId, userId);

        return true;
    }

    @MutationMapping
    public boolean revokeAppAccess(@Argument long workspaceId, @Argument long appId, @Argument long userId) {
        appSharingFacade.revokeAppAccess(workspaceId, appId, userId);

        return true;
    }

    @MutationMapping
    public boolean setAppInstanceVisibility(
        @Argument long workspaceId, @Argument long appInstanceId, @Argument ResourceVisibility visibility) {

        appSharingFacade.setAppInstanceVisibility(workspaceId, appInstanceId, visibility);

        return true;
    }

    @MutationMapping
    public boolean grantAppInstanceAccess(
        @Argument long workspaceId, @Argument long appInstanceId, @Argument long userId) {

        appSharingFacade.grantAppInstanceAccess(workspaceId, appInstanceId, userId);

        return true;
    }

    @MutationMapping
    public boolean revokeAppInstanceAccess(
        @Argument long workspaceId, @Argument long appInstanceId, @Argument long userId) {

        appSharingFacade.revokeAppInstanceAccess(workspaceId, appInstanceId, userId);

        return true;
    }
}
