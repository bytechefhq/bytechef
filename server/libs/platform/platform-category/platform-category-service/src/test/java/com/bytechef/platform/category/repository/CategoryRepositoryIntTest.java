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

package com.bytechef.platform.category.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.category.config.CategoryIntTestConfiguration;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.apache.commons.lang3.Validate;
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
public class CategoryRepositoryIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @AfterEach
    public void afterEach() {
        categoryRepository.deleteAll();
    }

    @Test
    public void testCreate() {
        Category category = categoryRepository.save(new Category("name"));

        assertThat(category).isEqualTo(
            OptionalUtils.get(categoryRepository.findById(Validate.notNull(category.getId(), "id"))));
    }

    @Test
    public void testDelete() {
        Category category = categoryRepository.save(new Category("name"));

        Assertions.assertThat(categoryRepository.findById(Validate.notNull(category.getId(), "id")))
            .hasValue(category);

        categoryRepository.deleteById(Validate.notNull(category.getId(), "id"));

        Assertions.assertThat(categoryRepository.findById(Validate.notNull(category.getId(), "id")))
            .isEmpty();
    }

    @Test
    public void testFindById() {
        Category category = categoryRepository.save(new Category("name"));

        Assertions.assertThat(categoryRepository.findById(Validate.notNull(category.getId(), "id")))
            .hasValue(category);
    }

    @Test
    public void testUpdate() {
        Category category = categoryRepository.save(new Category("name"));

        category.setName("name2");

        categoryRepository.save(category);

        Assertions.assertThat(categoryRepository.findById(category.getId()))
            .hasValue(category);
    }
}
