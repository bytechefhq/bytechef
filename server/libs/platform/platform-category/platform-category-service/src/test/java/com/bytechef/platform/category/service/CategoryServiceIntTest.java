/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.category.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.category.config.CategoryIntTestConfiguration;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = CategoryIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class CategoryServiceIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryService categoryService;

    @AfterEach
    public void afterEach() {
        categoryRepository.deleteAll();
    }

    @Test
    public void testGetCategories() {
        Category category = categoryService.save(new Category("name"));

        Assertions.assertThat(categoryService.getCategories())
            .isEqualTo(List.of(category));

        Assertions.assertThat(categoryService.getCategories(List.of()))
            .isEmpty();

        Assertions.assertThat(categoryService.getCategories(List.of(category.getId())))
            .isEqualTo(List.of(category));
    }

    @Test
    public void testSave() {
        Category category = categoryService.save(new Category("name"));

        Iterable<Category> categoryIterable = categoryRepository.findAll();

        Stream<Category> categoryStream = StreamSupport.stream(categoryIterable.spliterator(), false);

        assertThat(categoryStream.toList()).contains(category);
    }
}
