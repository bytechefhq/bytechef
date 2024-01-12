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

package com.bytechef.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.category.domain.Category;
import com.bytechef.category.repository.CategoryRepository;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class ProjectServiceIntTest {

    private Category category;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TagRepository tagRepository;

    @BeforeEach
    public void beforeEach() {
        category = categoryRepository.save(new Category("name"));
    }

    @AfterEach
    public void afterEach() {
        projectRepository.deleteAll();

        categoryRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    public void testAddWorkflow() {
        Project project = projectRepository.save(getProject());

        project = projectService.addWorkflow(Validate.notNull(project.getId(), "id"), "workflow2");

        assertThat(project.getWorkflowIds()).contains("workflow2");
    }

    @Test
    public void testCreate() {
        Project project = getProject();

        Tag tag = tagRepository.save(new Tag("tag1"));

        project.setTags(List.of(tag));

        project = projectService.create(project);

        assertThat(project)
            .hasFieldOrPropertyWithValue("categoryId", category.getId())
            .hasFieldOrPropertyWithValue("description", "description")
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("tagIds", List.of(Validate.notNull(tag.getId(), "id")))
            .hasFieldOrPropertyWithValue("workflowIds", List.of("workflow1"));
    }

    @Test
    public void testDelete() {
        Project project = projectRepository.save(getProject());

        projectService.delete(Validate.notNull(project.getId(), "id"));

        Assertions.assertThat(projectRepository.findById(project.getId()))
            .isNotPresent();
    }

    @Test
    public void testGetProject() {
        Project project = projectRepository.save(getProject());

        assertThat(project).isEqualTo(projectService.getProject(Validate.notNull(project.getId(), "id")));
    }

    @Test
    public void testGetProjects() {
        Project project = projectRepository.save(getProject());

        assertThat(projectService.getProjects()).hasSize(1);

        Category category = new Category("category1");

        category = categoryRepository.save(category);

        project.setCategory(category);

        project = projectRepository.save(project);

        assertThat(projectService.getProjects(category.getId(), null, null, null)).hasSize(1);

        assertThat(projectService.getProjects(Long.MAX_VALUE, null, null, null)).hasSize(0);

        Tag tag = new Tag("tag1");

        tag = tagRepository.save(tag);

        project.setTags(List.of(tag));

        projectRepository.save(project);

        assertThat(projectService.getProjects(null, null, tag.getId(), null)).hasSize(1);
        assertThat(projectService.getProjects(null, null, Long.MAX_VALUE, null)).hasSize(0);
        assertThat(projectService.getProjects(Long.MAX_VALUE, null, Long.MAX_VALUE, null)).hasSize(0);
    }

    @Test
    public void testUpdate() {
        Project project = projectRepository.save(getProject());

        Tag tag = tagRepository.save(new Tag("tag2"));

        project.setDescription("description2");
        project.setName("name2");
        project.setTagIds(List.of(tag.getId()));
        project.setWorkflowIds(List.of("workflow2"));

        project = projectService.update(project);

        Category category2 = categoryRepository.save(new Category("name2"));

        project.setCategory(category2);
        project.setDescription("description2");
        project.setName("name2");
        project.setTagIds(List.of(tag.getId()));
        project.setWorkflowIds(List.of("workflow2"));

        project = projectService.update(project);

        assertThat(project)
            .hasFieldOrPropertyWithValue("categoryId", category2.getId())
            .hasFieldOrPropertyWithValue("description", "description2")
            .hasFieldOrPropertyWithValue("name", "name2")
            .hasFieldOrPropertyWithValue("tagIds", List.of(tag.getId()))
            .hasFieldOrPropertyWithValue("workflowIds", List.of("workflow2"));
    }

    private Project getProject() {
        return Project.builder()
            .categoryId(category.getId())
            .description("description")
            .name("name")
            .projectVersion(1)
            .publishedDate(LocalDateTime.now())
            .status(Project.Status.UNPUBLISHED)
            .workflowIds(List.of("workflow1"))
            .build();
    }
}
