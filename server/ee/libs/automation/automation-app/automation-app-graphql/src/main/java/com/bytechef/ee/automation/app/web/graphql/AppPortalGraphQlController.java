/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.app.dto.PortalAppDTO;
import com.bytechef.ee.automation.app.dto.PortalAppInstanceDTO;
import com.bytechef.ee.automation.app.facade.AppPortalFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller exposing the App Portal: the apps the current user may launch, favorites and launch recording.
 *
 * <p>
 * {@link AppPortalFacade} owns the access rule and every refusal, so this controller carries no {@code @PreAuthorize}:
 * it only maps arguments and responses, and every operation makes one facade call.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
class AppPortalGraphQlController {

    private final AppPortalFacade appPortalFacade;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    AppPortalGraphQlController(AppPortalFacade appPortalFacade, EnvironmentService environmentService) {
        this.appPortalFacade = appPortalFacade;
        this.environmentService = environmentService;
    }

    @QueryMapping
    public List<PortalAppResponse> portalApps(@Argument long environmentId) {
        Environment environment = environmentService.getEnvironment(environmentId);

        return appPortalFacade.getPortalApps(environment)
            .stream()
            .map(AppPortalGraphQlController::toResponse)
            .toList();
    }

    @MutationMapping
    public boolean setAppFavorite(@Argument long appId, @Argument boolean favorite) {
        appPortalFacade.setAppFavorite(appId, favorite);

        return true;
    }

    @MutationMapping
    public boolean recordAppLaunch(@Argument long appInstanceId) {
        appPortalFacade.recordAppLaunch(appInstanceId);

        return true;
    }

    private static PortalAppResponse toResponse(PortalAppDTO portalApp) {
        List<PortalAppInstanceResponse> instances = portalApp.instances()
            .stream()
            .map(AppPortalGraphQlController::toResponse)
            .toList();

        return new PortalAppResponse(
            portalApp.id(), portalApp.name(), portalApp.description(), portalApp.categoryId(), portalApp.categoryName(),
            portalApp.hasIcon(), portalApp.favorite(), AppGraphQlMapper.toDateString(portalApp.lastUsedDate()),
            instances);
    }

    private static PortalAppInstanceResponse toResponse(PortalAppInstanceDTO portalAppInstance) {
        return new PortalAppInstanceResponse(
            portalAppInstance.id(), portalAppInstance.name(), portalAppInstance.workspaceId(),
            portalAppInstance.workspaceName(), portalAppInstance.url(),
            AppGraphQlMapper.toDateString(portalAppInstance.lastUsedDate()), portalAppInstance.useCount());
    }

    public record PortalAppResponse(
        long id, String name, @Nullable String description, @Nullable Long categoryId, @Nullable String categoryName,
        boolean hasIcon, boolean favorite, @Nullable String lastUsedDate, List<PortalAppInstanceResponse> instances) {

        public PortalAppResponse {
            instances = List.copyOf(instances);
        }
    }

    public record PortalAppInstanceResponse(
        long id, String name, long workspaceId, String workspaceName, String url, @Nullable String lastUsedDate,
        long useCount) {
    }
}
