
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

package com.bytechef.category.repository;

import com.bytechef.category.config.CategoryIntTestConfiguration;
import com.bytechef.category.domain.Category;
import com.bytechef.test.annotation.EmbeddedSql;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
@EmbeddedSql
@SpringBootTest(classes = CategoryIntTestConfiguration.class)
public class CategoryRepositoryIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    public void beforeEach() {
        categoryRepository.deleteAll();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testCreate() {
        Category category = categoryRepository.save(new Category("name"));

        assertThat(category).isEqualTo(categoryRepository.findById(category.getId())
            .get());
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testDelete() {
        Category category = categoryRepository.save(new Category("name"));

        assertThat(categoryRepository.findById(category.getId())).hasValue(category);

        categoryRepository.deleteById(category.getId());

        assertThat(categoryRepository.findById(category.getId())).isEmpty();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testFindById() {
        Category category = categoryRepository.save(new Category("name"));

        assertThat(categoryRepository.findById(category.getId())).hasValue(category);
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testUpdate() {
        Category category = categoryRepository.save(new Category("name"));

        category.setName("name2");

        categoryRepository.save(category);

        assertThat(categoryRepository.findById(category.getId())).hasValue(category);
    }
}
