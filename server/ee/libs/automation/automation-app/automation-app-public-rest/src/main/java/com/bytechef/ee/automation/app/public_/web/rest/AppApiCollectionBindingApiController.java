/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.app.dto.AppApiCollectionBindingDTO;
import com.bytechef.ee.automation.app.dto.AppBindableApiCollectionDTO;
import com.bytechef.ee.automation.app.facade.AppApiCollectionFacade;
import com.bytechef.ee.automation.app.facade.AppInstanceFacade;
import com.bytechef.ee.automation.app.public_.web.rest.management.AppApiCollectionBindingApi;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppApiCollectionBindingModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppBindableApiCollectionModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.EnvironmentModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The App API collection binding management endpoints of the automation public API.
 *
 * <p>
 * {@link AppApiCollectionFacade} owns all authorization and lookups, so this controller carries no
 * {@code @PreAuthorize}: it maps models to and from DTOs. Bind and unbind are refused outside DEVELOPMENT before any
 * facade is called. Every route then requires the instance to be in the request's environment through
 * {@link AppInstanceFacade}, before any permission check, and answers an instance of another environment as not found.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.app.public_.web.rest.AppApiCollectionBindingApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AppApiCollectionBindingApiController extends AbstractAppApiController
    implements AppApiCollectionBindingApi {

    private final AppApiCollectionFacade appApiCollectionFacade;
    private final AppInstanceFacade appInstanceFacade;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI2")
    public AppApiCollectionBindingApiController(
        AppApiCollectionFacade appApiCollectionFacade, AppInstanceFacade appInstanceFacade,
        EnvironmentService environmentService) {

        this.appApiCollectionFacade = appApiCollectionFacade;
        this.appInstanceFacade = appInstanceFacade;
        this.environmentService = environmentService;
    }

    @Override
    public ResponseEntity<List<AppApiCollectionBindingModel>> getAppInstanceApiCollectionBindings(
        Long id, @Nullable EnvironmentModel xEnvironment) {

        requireInEnvironment(id, AppEnvironmentScope.getEnvironment(environmentService, xEnvironment));

        return ResponseEntity.ok(
            appApiCollectionFacade.getApiCollectionBindings(id)
                .stream()
                .map(this::toModel)
                .toList());
    }

    @Override
    public ResponseEntity<List<AppBindableApiCollectionModel>> getAppInstanceBindableApiCollections(
        Long id, @Nullable EnvironmentModel xEnvironment) {

        requireInEnvironment(id, AppEnvironmentScope.getEnvironment(environmentService, xEnvironment));

        return ResponseEntity.ok(
            appApiCollectionFacade.getBindableApiCollections(id)
                .stream()
                .map(this::toModel)
                .toList());
    }

    @Override
    public ResponseEntity<AppApiCollectionBindingModel> bindAppInstanceApiCollection(
        Long id, Long apiCollectionId, @Nullable EnvironmentModel xEnvironment) {

        requireInEnvironment(id, AppEnvironmentScope.requireDevelopment(environmentService, xEnvironment));

        return ResponseEntity.ok(toModel(appApiCollectionFacade.bindApiCollection(id, apiCollectionId)));
    }

    @Override
    public ResponseEntity<Void> unbindAppInstanceApiCollection(
        Long id, Long apiCollectionId, @Nullable EnvironmentModel xEnvironment) {

        requireInEnvironment(id, AppEnvironmentScope.requireDevelopment(environmentService, xEnvironment));

        appApiCollectionFacade.unbindApiCollection(id, apiCollectionId);

        return ResponseEntity.noContent()
            .build();
    }

    private void requireInEnvironment(long id, Environment environment) {
        AppEnvironmentScope.requireInEnvironment(appInstanceFacade, id, environment);
    }

    private AppApiCollectionBindingModel toModel(AppApiCollectionBindingDTO binding) {
        return new AppApiCollectionBindingModel()
            .id(binding.id())
            .appInstanceId(binding.appInstanceId())
            .apiCollectionId(binding.apiCollectionId())
            .name(binding.name())
            .contextPath(binding.contextPath())
            .collectionVersion(binding.collectionVersion())
            .projectId(binding.projectId() == 0 ? null : binding.projectId())
            .projectName(binding.projectName())
            .available(binding.available());
    }

    private AppBindableApiCollectionModel toModel(AppBindableApiCollectionDTO apiCollection) {
        return new AppBindableApiCollectionModel()
            .apiCollectionId(apiCollection.apiCollectionId())
            .name(apiCollection.name())
            .contextPath(apiCollection.contextPath())
            .collectionVersion(apiCollection.collectionVersion())
            .projectId(apiCollection.projectId())
            .projectName(apiCollection.projectName())
            .available(apiCollection.available());
    }
}
