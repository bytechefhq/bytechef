/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.athena.configuration.web.rest;

import com.bytechef.athena.configuration.facade.IntegrationFacade;
import com.bytechef.athena.configuration.web.rest.model.CategoryModel;
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
@RestController("com.bytechef.athena.configuration.web.rest.categoryApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}")
public class CategoryApiController implements CategoryApi {

    private final IntegrationFacade integrationFacade;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI2")
    public CategoryApiController(
        IntegrationFacade integrationFacade, ConversionService conversionService) {

        this.integrationFacade = integrationFacade;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<List<CategoryModel>> getIntegrationCategories() {
        return ResponseEntity.ok(integrationFacade.getIntegrationCategories()
            .stream()
            .map(category -> conversionService.convert(category, CategoryModel.class))
            .toList());
    }
}
