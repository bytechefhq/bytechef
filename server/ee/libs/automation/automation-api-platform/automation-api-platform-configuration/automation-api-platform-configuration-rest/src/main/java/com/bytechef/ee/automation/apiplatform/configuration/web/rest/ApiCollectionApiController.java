/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.web.rest.model.EnvironmentModel;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import com.bytechef.ee.automation.apiplatform.configuration.web.rest.model.ApiCollectionModel;
import com.bytechef.platform.constant.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/api-platform/internal")
@ConditionalOnCoordinator
public class ApiCollectionApiController implements ApiCollectionApi {

    private final ApiCollectionFacade apiCollectionFacade;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public ApiCollectionApiController(
        ApiCollectionFacade apiCollectionFacade, ConversionService conversionService) {

        this.apiCollectionFacade = apiCollectionFacade;
        this.conversionService = conversionService;
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<ApiCollectionModel> createApiCollection(ApiCollectionModel apiCollectionModel) {
        validate(apiCollectionModel);

        ApiCollectionDTO apiCollection = conversionService.convert(apiCollectionModel, ApiCollectionDTO.class);

        return ResponseEntity.ok(
            conversionService.convert(
                apiCollectionFacade.createApiCollection(apiCollection), ApiCollectionModel.class));
    }

    @Override
    public ResponseEntity<Void> deleteApiCollection(Long id) {
        apiCollectionFacade.deleteApiCollection(id);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<ApiCollectionModel> getApiCollection(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(apiCollectionFacade.getApiCollection(id), ApiCollectionModel.class));
    }

    @Override
    public ResponseEntity<List<ApiCollectionModel>> getWorkspaceApiCollections(
        Long id, EnvironmentModel environment, Long projectId, Long tagId) {

        return ResponseEntity.ok(
            CollectionUtils.map(
                apiCollectionFacade.getApiCollections(
                    id, environment == null ? null : Environment.valueOf(environment.getValue()), projectId, tagId),
                openApiCollection -> conversionService.convert(openApiCollection, ApiCollectionModel.class)));
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<ApiCollectionModel> updateApiCollection(Long id, ApiCollectionModel apiCollectionModel) {
        validate(apiCollectionModel);

        return ResponseEntity.ok(
            conversionService.convert(
                apiCollectionFacade.updateApiCollection(
                    conversionService.convert(apiCollectionModel.id(id), ApiCollectionDTO.class)),
                ApiCollectionModel.class));
    }
    private static void validate(ApiCollectionModel apiCollectionModel) {
        String contextPath = apiCollectionModel.getContextPath();

        if (contextPath.startsWith("/")) {
            throw new IllegalArgumentException("Context path must not start with a slash.");
        }
    }
}
