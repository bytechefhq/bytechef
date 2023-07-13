
/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.dione.configuration.web.rest;

import com.bytechef.dione.configuration.facade.IntegrationFacade;
import com.bytechef.dione.configuration.web.rest.model.CategoryModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/embedded")
public class IntegrationCategoryController implements IntegrationCategoriesApi {

    private final IntegrationFacade integrationFacade;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI2")
    public IntegrationCategoryController(
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
