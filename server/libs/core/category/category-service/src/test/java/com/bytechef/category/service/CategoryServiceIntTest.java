
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

package com.bytechef.category.service;

import com.bytechef.category.config.CategoryIntTestConfiguration;
import com.bytechef.category.repository.CategoryRepository;
import com.bytechef.category.servicee.CategoryService;
import com.bytechef.category.domain.Category;
import com.bytechef.test.annotation.EmbeddedSql;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
@EmbeddedSql
@SpringBootTest(classes = CategoryIntTestConfiguration.class)
public class CategoryServiceIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryService categoryService;

    @BeforeEach
    @SuppressFBWarnings("NP")
    public void beforeEach() {
        categoryRepository.deleteAll();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testGetCategories() {
        Category category = categoryService.save(new Category("name"));

        assertThat(categoryService.getCategories()).isEqualTo(List.of(category));

        assertThat(categoryService.getCategories(List.of())).isEmpty();

        assertThat(categoryService.getCategories(List.of(category.getId()))).isEqualTo(List.of(category));
    }

    @Test
    public void testSave() {
        Category category = categoryService.save(new Category("name"));

        Iterable<Category> categoryIterable = categoryRepository.findAll();

        Stream<Category> categoryStream = StreamSupport.stream(categoryIterable.spliterator(), false);

        assertThat(categoryStream.toList()).contains(category);
    }
}
