/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.app.dto.AppApiCollectionDTO;
import com.bytechef.ee.automation.app.dto.AppApiEndpointDTO;
import com.bytechef.ee.automation.app.facade.AppApiExecutionFacade;
import com.bytechef.ee.automation.app.public_.web.rest.model.AppApiCollectionModel;
import com.bytechef.ee.automation.app.public_.web.rest.model.AppApiEndpointModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lists the API collections bound to the calling app, with the endpoints it can call under {@code /api/app/v1/api}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.automation.app.public_.web.rest.AppApiCollectionApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.app:/api/app}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class AppApiCollectionApiController extends AbstractAppApiController implements AppApiCollectionApi {

    private final AppApiExecutionFacade appApiExecutionFacade;

    @SuppressFBWarnings("EI2")
    public AppApiCollectionApiController(AppApiExecutionFacade appApiExecutionFacade) {
        this.appApiExecutionFacade = appApiExecutionFacade;
    }

    @Override
    public ResponseEntity<List<AppApiCollectionModel>> getApiCollections() {
        return ResponseEntity.ok(
            appApiExecutionFacade.getApiCollections()
                .stream()
                .map(AppApiCollectionApiController::toModel)
                .toList());
    }

    private static AppApiCollectionModel toModel(AppApiCollectionDTO appApiCollectionDTO) {
        AppApiCollectionModel appApiCollectionModel = new AppApiCollectionModel();

        appApiCollectionModel.setApiCollectionId(appApiCollectionDTO.apiCollectionId());
        appApiCollectionModel.setAvailable(appApiCollectionDTO.available());
        appApiCollectionModel.setCollectionVersion(appApiCollectionDTO.collectionVersion());
        appApiCollectionModel.setContextPath(JsonNullable.of(appApiCollectionDTO.contextPath()));
        appApiCollectionModel.setEndpoints(
            appApiCollectionDTO.endpoints()
                .stream()
                .map(AppApiCollectionApiController::toModel)
                .toList());
        appApiCollectionModel.setName(appApiCollectionDTO.name());

        return appApiCollectionModel;
    }

    private static AppApiEndpointModel toModel(AppApiEndpointDTO appApiEndpointDTO) {
        AppApiEndpointModel appApiEndpointModel = new AppApiEndpointModel();

        appApiEndpointModel.setHttpMethod(AppApiEndpointModel.HttpMethodEnum.fromValue(appApiEndpointDTO.httpMethod()));
        appApiEndpointModel.setName(appApiEndpointDTO.name());
        appApiEndpointModel.setPath(JsonNullable.of(appApiEndpointDTO.path()));
        appApiEndpointModel.setRequestSchema(JsonNullable.of(appApiEndpointDTO.requestSchema()));
        appApiEndpointModel.setResponseSchema(JsonNullable.of(appApiEndpointDTO.responseSchema()));

        return appApiEndpointModel;
    }
}
