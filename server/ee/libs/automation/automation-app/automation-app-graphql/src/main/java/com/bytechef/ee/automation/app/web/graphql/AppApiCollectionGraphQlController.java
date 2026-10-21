/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.app.dto.AppApiCollectionBindingDTO;
import com.bytechef.ee.automation.app.dto.AppBindableApiCollectionDTO;
import com.bytechef.ee.automation.app.facade.AppApiCollectionFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller exposing the API collection bindings of an app instance for the admin UI.
 *
 * <p>
 * {@link AppApiCollectionFacade} owns all authorization and lookups, so this controller carries no
 * {@code @PreAuthorize}: it only maps arguments and responses.
 *
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
class AppApiCollectionGraphQlController {

    private final AppApiCollectionFacade appApiCollectionFacade;

    @SuppressFBWarnings("EI")
    AppApiCollectionGraphQlController(AppApiCollectionFacade appApiCollectionFacade) {
        this.appApiCollectionFacade = appApiCollectionFacade;
    }

    @QueryMapping
    public List<AppApiCollectionBindingResponse> appApiCollections(@Argument long appInstanceId) {
        return appApiCollectionFacade.getApiCollectionBindings(appInstanceId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @QueryMapping
    public List<AppBindableApiCollectionResponse> appBindableApiCollections(@Argument long appInstanceId) {
        return appApiCollectionFacade.getBindableApiCollections(appInstanceId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @MutationMapping
    public AppApiCollectionBindingResponse
        bindAppApiCollection(@Argument long appInstanceId, @Argument long apiCollectionId) {
        return toResponse(appApiCollectionFacade.bindApiCollection(appInstanceId, apiCollectionId));
    }

    @MutationMapping
    public boolean unbindAppApiCollection(@Argument long appInstanceId, @Argument long apiCollectionId) {
        appApiCollectionFacade.unbindApiCollection(appInstanceId, apiCollectionId);

        return true;
    }

    private AppApiCollectionBindingResponse toResponse(AppApiCollectionBindingDTO binding) {
        return new AppApiCollectionBindingResponse(
            binding.id(), binding.appInstanceId(), binding.apiCollectionId(), binding.name(), binding.contextPath(),
            binding.collectionVersion(), binding.projectId() == 0 ? null : binding.projectId(), binding.projectName(),
            binding.available(), binding.projectDeploymentId(), binding.projectVersion());
    }

    private AppBindableApiCollectionResponse toResponse(AppBindableApiCollectionDTO apiCollection) {
        return new AppBindableApiCollectionResponse(
            apiCollection.apiCollectionId(), apiCollection.name(), apiCollection.contextPath(),
            apiCollection.collectionVersion(), apiCollection.projectId(), apiCollection.projectName(),
            apiCollection.available());
    }

    public record AppApiCollectionBindingResponse(
        long id, long appInstanceId, long apiCollectionId, String name, @Nullable String contextPath,
        int collectionVersion,
        @Nullable Long projectId, @Nullable String projectName, boolean available,
        @Nullable Long projectDeploymentId, @Nullable Integer projectVersion) {
    }

    public record AppBindableApiCollectionResponse(
        long apiCollectionId, String name, @Nullable String contextPath, int collectionVersion, long projectId,
        @Nullable String projectName, boolean available) {
    }
}
