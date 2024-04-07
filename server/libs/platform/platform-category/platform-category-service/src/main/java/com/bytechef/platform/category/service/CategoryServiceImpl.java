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

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.repository.CategoryRepository;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public Category create(Category category) {
        Validate.notNull(category, "'category' must not be null");
        Validate.isTrue(category.getId() == null, "'category.id' must be null");

        return categoryRepository.save(category);
    }

    @Override
    public void delete(long id) {
        categoryRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getCategories() {
        return categoryRepository.findAll(Sort.by("name"))
            .stream()
            .sorted(Comparator.comparing(Category::getName))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Category getCategory(long id) {
        return OptionalUtils.get(categoryRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getCategories(List<Long> ids) {
        Validate.notNull(ids, "'ids' must not be null");

        if (ids.isEmpty()) {
            return Collections.emptyList();
        } else {

            return categoryRepository.findAllById(ids)
                .stream()
                .sorted(Comparator.comparing(Category::getName))
                .toList();
        }
    }

    @Override
    public Category save(Category category) {
        if (category.isNew()) {
            if (StringUtils.isNotBlank(category.getName())) {
                Category finalCategory = category;

                category = OptionalUtils.orElseGet(
                    categoryRepository.findByName(category.getName()),
                    () -> categoryRepository.save(finalCategory));
            }
        } else {
            Category curCategory = OptionalUtils.get(
                categoryRepository.findById(Validate.notNull(category.getId(), "id")));

            if (!Objects.equals(category.getName(), curCategory.getName())) {
                curCategory.setName(category.getName());
                curCategory.setVersion(category.getVersion());

                category = categoryRepository.save(curCategory);
            }
        }

        return category;
    }

    @Override
    public Category update(Category category) {
        Validate.notNull(category, "'category' must not be null");
        Validate.notNull(category.getId(), "'category.id' must not be null");

        return categoryRepository.save(category);
    }
}
