/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.facade.IntegrationCategoryFacade;
import com.bytechef.ee.embedded.configuration.web.rest.model.CategoryModel;
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
@RestController("com.bytechef.ee.embedded.configuration.web.rest.CategoryApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class CategoryApiController implements CategoryApi {

    private final ConversionService conversionService;
    private final IntegrationCategoryFacade integrationCategoryFacade;

    @SuppressFBWarnings("EI2")
    public CategoryApiController(
        ConversionService conversionService, IntegrationCategoryFacade integrationCategoryFacade) {

        this.conversionService = conversionService;
        this.integrationCategoryFacade = integrationCategoryFacade;
    }

    @Override
    public ResponseEntity<List<CategoryModel>> getIntegrationCategories() {
        return ResponseEntity.ok(
            integrationCategoryFacade.getIntegrationCategories()
                .stream()
                .map(category -> conversionService.convert(category, CategoryModel.class))
                .toList());
    }
}
