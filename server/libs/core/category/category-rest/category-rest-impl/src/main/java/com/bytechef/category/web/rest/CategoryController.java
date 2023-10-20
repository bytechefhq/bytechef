
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

package com.bytechef.category.web.rest;

import com.bytechef.category.domain.Category;
import com.bytechef.category.service.CategoryService;
import com.bytechef.category.web.rest.model.CategoryModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author Ivica Cardic
 */
@RestController

@RequestMapping("${openapi.openAPIDefinition.base-path:}/core")
public class CategoryController implements CategoriesApi {

    private final CategoryService categoryService;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI2")
    public CategoryController(CategoryService categoryService, ConversionService conversionService) {
        this.categoryService = categoryService;
        this.conversionService = conversionService;
    }

    @Override
    public Mono<ResponseEntity<CategoryModel>> getCategory(Long id, ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(
            conversionService.convert(categoryService.getCategory(id), CategoryModel.class)));
    }

    @Override
    public Mono<ResponseEntity<CategoryModel>> updateCategory(
        Long id, Mono<CategoryModel> categoryModelMono, ServerWebExchange exchange) {

        return categoryModelMono.map(
            tagModel -> conversionService.convert(
                categoryService.update(conversionService.convert(tagModel.id(id), Category.class)),
                CategoryModel.class))
            .map(ResponseEntity::ok);
    }
}
