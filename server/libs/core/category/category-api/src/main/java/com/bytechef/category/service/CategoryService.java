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

package com.bytechef.category.service;

import com.bytechef.category.domain.Category;
import java.util.List;

public interface CategoryService {

    Category create(Category category);

    void delete(long id);

    List<Category> getCategories();

    Category getCategory(long id);

    List<Category> getCategories(List<Long> ids);

    Category save(Category category);

    Category update(Category category);
}
