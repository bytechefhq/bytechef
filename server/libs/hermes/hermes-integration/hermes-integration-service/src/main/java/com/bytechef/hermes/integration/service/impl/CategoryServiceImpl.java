
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

package com.bytechef.hermes.integration.service.impl;

import com.bytechef.hermes.integration.domain.Category;
import com.bytechef.hermes.integration.repository.CategoryRepository;
import com.bytechef.hermes.integration.service.CategoryService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * @author Ivica Cardic
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Optional<Category> fetchCategory(Long id) {
        return categoryRepository.findById(id);
    }

    @Override
    public List<Category> getCategories() {
        return StreamSupport.stream(categoryRepository.findAll()
            .spliterator(), false)
            .toList();
    }

    @Override
    public List<Category> getCategories(List<Long> ids) {
        Assert.notNull(ids, "'ids' must not be null");

        if (ids.isEmpty()) {
            return Collections.emptyList();
        } else {
            return categoryRepository.findByIdIn(ids);
        }
    }

    @Override
    @SuppressFBWarnings("NP")
    public Category save(Category category) {
        if (category.isNew()) {
            category = categoryRepository.save(category);
        } else {
            Category curCategory = categoryRepository.findById(category.getId())
                .orElseThrow();

            curCategory.setName(category.getName());
            curCategory.setVersion(category.getVersion());

            category = categoryRepository.save(curCategory);
        }

        return category;
    }
}
