/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionEndpointDTO;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.configuration.web.rest.model.ApiCollectionEndpointModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
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
public class ApiCollectionEndpointApiController implements ApiCollectionEndpointApi {

    private final ApiCollectionEndpointService apiCollectionEndpointService;
    private final ApiCollectionFacade apiCollectionFacade;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public ApiCollectionEndpointApiController(
        ApiCollectionEndpointService apiCollectionEndpointService, ApiCollectionFacade apiCollectionFacade,
        ConversionService conversionService) {

        this.apiCollectionEndpointService = apiCollectionEndpointService;
        this.apiCollectionFacade = apiCollectionFacade;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<ApiCollectionEndpointModel> createApiCollectionEndpoint(
        ApiCollectionEndpointModel apiCollectionEndpointModel) {

        validate(apiCollectionEndpointModel);

        ApiCollectionEndpointDTO apiCollectionEndpoint = apiCollectionFacade.createApiCollectionEndpoint(
            Validate.notNull(
                conversionService.convert(apiCollectionEndpointModel, ApiCollectionEndpointDTO.class),
                "apiCollectionEndpoint"));

        return ResponseEntity.ok(conversionService.convert(apiCollectionEndpoint, ApiCollectionEndpointModel.class));
    }

    @Override
    public ResponseEntity<Void> deleteApiCollectionEndpoint(Long id) {
        apiCollectionEndpointService.delete(id);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<ApiCollectionEndpointModel> updateApiCollectionEndpoint(
        Long id, ApiCollectionEndpointModel apiCollectionEndpointModel) {

        validate(apiCollectionEndpointModel);

        apiCollectionEndpointModel = apiCollectionEndpointModel.id(id);

        return ResponseEntity.ok(
            conversionService.convert(
                apiCollectionFacade.updateApiCollectionEndpoint(
                    conversionService.convert(apiCollectionEndpointModel, ApiCollectionEndpointDTO.class)),
                ApiCollectionEndpointModel.class));
    }

    private static void validate(ApiCollectionEndpointModel apiCollectionEndpointModel) {
        String path = apiCollectionEndpointModel.getPath();

        if (StringUtils.isNotEmpty(path) && path.startsWith("/")) {
            throw new IllegalArgumentException("Context path must not start with a slash.");
        }
    }
}
