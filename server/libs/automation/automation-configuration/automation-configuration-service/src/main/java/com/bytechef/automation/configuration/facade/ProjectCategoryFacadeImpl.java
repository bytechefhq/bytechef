/*
 * Copyright 2025 ByteChef
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

package com.bytechef.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ProjectCategoryFacadeImpl implements ProjectCategoryFacade {

    private final CategoryService categoryService;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public ProjectCategoryFacadeImpl(CategoryService categoryService, ProjectService projectService) {
        this.categoryService = categoryService;
        this.projectService = projectService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getProjectCategories() {
        List<Project> projects = projectService.getProjects();

        return categoryService.getCategories(
            CollectionUtils.filter(CollectionUtils.map(projects, Project::getCategoryId), Objects::nonNull));
    }
}
