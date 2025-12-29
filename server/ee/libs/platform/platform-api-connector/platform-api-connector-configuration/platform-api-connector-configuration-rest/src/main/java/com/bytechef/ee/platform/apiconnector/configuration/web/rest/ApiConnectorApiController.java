/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import com.bytechef.ee.platform.apiconnector.configuration.facade.ApiConnectorFacade;
import com.bytechef.ee.platform.apiconnector.configuration.service.ApiConnectorService;
import com.bytechef.ee.platform.apiconnector.configuration.web.rest.model.ApiConnectorModel;
import com.bytechef.ee.platform.apiconnector.configuration.web.rest.model.ImportOpenApiSpecificationRequestModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ApiConnectorApiController implements ApiConnectorApi {

    private final ApiConnectorFacade apiConnectorFacade;
    private final ApiConnectorService apiConnectorService;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public ApiConnectorApiController(
        ApiConnectorFacade apiConnectorFacade, ApiConnectorService apiConnectorService,
        ConversionService conversionService) {

        this.apiConnectorFacade = apiConnectorFacade;
        this.apiConnectorService = apiConnectorService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<ApiConnectorModel> createApiConnector(ApiConnectorModel apiConnectorModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                apiConnectorService.create(conversionService.convert(apiConnectorModel, ApiConnector.class)),
                ApiConnectorModel.class));
    }

    @Override
    public ResponseEntity<Void> deleteApiConnector(Long id) {
        apiConnectorService.delete(id);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<Void> enableApiConnector(Long id, Boolean enable) {
        apiConnectorService.enableApiConnector(id, enable);

        return ResponseEntity.ok()
            .build();
    }

    @Override
    public ResponseEntity<ApiConnectorModel> getApiConnector(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(apiConnectorService.getApiConnector(id), ApiConnectorModel.class));
    }

    @Override
    public ResponseEntity<List<ApiConnectorModel>> getApiConnectors() {
        return ResponseEntity.ok(
            CollectionUtils.map(
                apiConnectorFacade.getApiConnectors(),
                openApiConnector -> conversionService.convert(openApiConnector, ApiConnectorModel.class)));
    }

    @Override
    public ResponseEntity<ApiConnectorModel> importOpenApiSpecification(
        ImportOpenApiSpecificationRequestModel importOpenAPISpecificationRequestModel) {

        return ResponseEntity.ok(
            conversionService.convert(
                apiConnectorFacade.importOpenApiSpecification(
                    importOpenAPISpecificationRequestModel.getName(),
                    importOpenAPISpecificationRequestModel.getSpecification()),
                ApiConnectorModel.class));
    }

    @Override
    public ResponseEntity<ApiConnectorModel> updateApiConnector(
        Long id, ApiConnectorModel apiConnectorModel) {

        return ResponseEntity.ok(
            conversionService.convert(
                apiConnectorService.update(
                    conversionService.convert(apiConnectorModel, ApiConnector.class)),
                ApiConnectorModel.class));
    }
}
