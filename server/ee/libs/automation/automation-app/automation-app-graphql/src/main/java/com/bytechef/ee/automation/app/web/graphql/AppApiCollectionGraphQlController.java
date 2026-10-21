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
 * GraphQL controller exposing the API collection bindings of an {@code App} for the admin UI.
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
    public List<AppApiCollectionBindingResponse> appApiCollections(@Argument long appId) {
        return appApiCollectionFacade.getApiCollectionBindings(appId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @QueryMapping
    public List<AppBindableApiCollectionResponse> appBindableApiCollections(@Argument long appId) {
        return appApiCollectionFacade.getBindableApiCollections(appId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @MutationMapping
    public AppApiCollectionBindingResponse bindAppApiCollection(@Argument long appId, @Argument long apiCollectionId) {
        return toResponse(appApiCollectionFacade.bindApiCollection(appId, apiCollectionId));
    }

    @MutationMapping
    public boolean unbindAppApiCollection(@Argument long appId, @Argument long apiCollectionId) {
        appApiCollectionFacade.unbindApiCollection(appId, apiCollectionId);

        return true;
    }

    private AppApiCollectionBindingResponse toResponse(AppApiCollectionBindingDTO binding) {
        return new AppApiCollectionBindingResponse(
            binding.id(), binding.appId(), binding.apiCollectionId(), binding.name(), binding.contextPath(),
            binding.collectionVersion(), binding.projectId() == 0 ? null : binding.projectId(), binding.projectName(),
            binding.available());
    }

    private AppBindableApiCollectionResponse toResponse(AppBindableApiCollectionDTO apiCollection) {
        return new AppBindableApiCollectionResponse(
            apiCollection.apiCollectionId(), apiCollection.name(), apiCollection.contextPath(),
            apiCollection.collectionVersion(), apiCollection.projectId(), apiCollection.projectName(),
            apiCollection.available());
    }

    public record AppApiCollectionBindingResponse(
        long id, long appId, long apiCollectionId, String name, @Nullable String contextPath, int collectionVersion,
        @Nullable Long projectId, @Nullable String projectName, boolean available) {
    }

    public record AppBindableApiCollectionResponse(
        long apiCollectionId, String name, @Nullable String contextPath, int collectionVersion, long projectId,
        @Nullable String projectName, boolean available) {
    }
}
