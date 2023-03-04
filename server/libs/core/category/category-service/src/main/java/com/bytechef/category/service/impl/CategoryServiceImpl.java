
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

package com.bytechef.category.service.impl;

import com.bytechef.category.domain.Category;
import com.bytechef.category.repository.CategoryRepository;
import com.bytechef.category.service.CategoryService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
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
    public Category create(Category category) {
        Assert.notNull(category, "'category' must not be null");
        Assert.isNull(category.getId(), "'category.id' must be null");

        return categoryRepository.save(category);
    }

    @Override
    public void delete(long id) {
        categoryRepository.deleteById(id);
    }

    @Override
    public List<Category> getCategories() {
        return StreamSupport.stream(categoryRepository.findAll(Sort.by("name"))
            .spliterator(), false)
            .sorted(Comparator.comparing(Category::getName))
            .toList();
    }

    @Override
    public Category getCategory(long id) {
        return categoryRepository.findById(id)
            .orElseThrow();
    }

    @Override
    public List<Category> getCategories(List<Long> ids) {
        Assert.notNull(ids, "'ids' must not be null");

        if (ids.isEmpty()) {
            return Collections.emptyList();
        } else {
            return StreamSupport.stream(categoryRepository.findAllById(ids)
                .spliterator(),
                false)
                .sorted(Comparator.comparing(Category::getName))
                .toList();
        }
    }

    @Override
    @SuppressFBWarnings("NP")
    public Category save(Category category) {
        if (category.isNew()) {
            if (StringUtils.hasText(category.getName())) {
                Category finalCategory = category;

                category = categoryRepository.findByName(category.getName())
                    .orElseGet(() -> categoryRepository.save(finalCategory));
            }
        } else {
            Category curCategory = categoryRepository.findById(category.getId())
                .orElseThrow();

            curCategory.setName(category.getName());
            curCategory.setVersion(category.getVersion());

            category = categoryRepository.save(curCategory);
        }

        return category;
    }

    @Override
    public Category update(Category category) {
        Assert.notNull(category, "'category' must not be null");
        Assert.notNull(category.getId(), "'category.id' must not be null");

        return categoryRepository.save(category);
    }
}
