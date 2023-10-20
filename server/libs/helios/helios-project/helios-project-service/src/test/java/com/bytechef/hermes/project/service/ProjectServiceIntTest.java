
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

package com.bytechef.hermes.project.service;

import com.bytechef.hermes.project.config.ProjectIntTestConfiguration;
import com.bytechef.category.domain.Category;
import com.bytechef.hermes.project.domain.Project;
import com.bytechef.category.repository.CategoryRepository;
import com.bytechef.hermes.project.repository.ProjectRepository;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.repository.TagRepository;
import com.bytechef.test.annotation.EmbeddedSql;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

/**
 * @author Ivica Cardic
 */
@EmbeddedSql
@SpringBootTest(classes = ProjectIntTestConfiguration.class, properties = {
    "bytechef.context-repository.provider=jdbc", "bytechef.persistence.provider=jdbc"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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

    @BeforeAll
    @SuppressFBWarnings("NP")
    public void beforeAll() {
        categoryRepository.deleteAll();

        category = categoryRepository.save(new Category("name"));
    }

    @BeforeEach
    @SuppressFBWarnings("NP")
    public void beforeEach() {
        projectRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testAddWorkflow() {
        Project project = projectRepository.save(getProject());

        project = projectService.addWorkflow(project.getId(), "workflow2");

        assertThat(project.getWorkflowIds()).contains("workflow2");
    }

    @Test
    public void testCreate() {
        Project project = getProject();

        Tag tag = tagRepository.save(new Tag("tag1"));

        project.setTags(List.of(tag));

        project = projectService.create(project);

        assertThat(project)
            .hasFieldOrPropertyWithValue("category", category)
            .hasFieldOrPropertyWithValue("description", "description")
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("tagIds", List.of(tag.getId()))
            .hasFieldOrPropertyWithValue("workflowIds", List.of("workflow1"));
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testDelete() {
        Project project = projectRepository.save(getProject());

        projectService.delete(project.getId());

        assertThat(projectRepository.findById(project.getId())).isNotPresent();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testGetProject() {
        Project project = projectRepository.save(getProject());

        assertThat(project).isEqualTo(projectService.getProject(project.getId()));
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testGetProjects() {
        Project project = projectRepository.save(getProject());

        assertThat(projectService.searchProjects(null, null)).hasSize(1);

        Category category = new Category("category1");

        category = categoryRepository.save(category);

        project.setCategory(category);

        project = projectRepository.save(project);

        assertThat(projectService.searchProjects(List.of(category.getId()), null)).hasSize(1);

        assertThat(projectService.searchProjects(List.of(Long.MAX_VALUE), null)).hasSize(0);

        Tag tag = new Tag("tag1");

        tag = tagRepository.save(tag);

        project.setTags(List.of(tag));

        projectRepository.save(project);

        assertThat(projectService.searchProjects(null, List.of(tag.getId()))).hasSize(1);

        assertThat(projectService.searchProjects(null, List.of(Long.MAX_VALUE))).hasSize(0);

        assertThatException()
            .isThrownBy(() -> projectService.searchProjects(List.of(Long.MAX_VALUE), List.of(Long.MAX_VALUE)));
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testUpdate() {
        Project project = projectRepository.save(getProject());

        Project updateProject = new Project();

        updateProject.setCategory(category);
        updateProject.setDescription(project.getDescription());
        updateProject.setId(project.getId());
        updateProject.setName(project.getName());
        updateProject.setProjectVersion(project.getProjectVersion());
        updateProject.setStatus(project.getStatus());
        updateProject.setVersion(project.getVersion());
        updateProject.setTags(project.getTags());
        updateProject.setWorkflowIds(project.getWorkflowIds());

        project = projectService.update(updateProject);

        assertThat(project)
            .hasFieldOrPropertyWithValue("category", category)
            .hasFieldOrPropertyWithValue("description", "description")
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("tags", Collections.emptyList())
            .hasFieldOrPropertyWithValue("workflowIds", List.of("workflow1"));

        Tag tag = tagRepository.save(new Tag("tag2"));

        project.setDescription("description2");
        project.setName("name2");
        project.setTags(List.of(tag));
        project.setWorkflowIds(List.of("workflow2"));

        Category category2 = categoryRepository.save(new Category("name2"));

        project.setCategory(category2);

        project = projectService.update(project);

        assertThat(project)
            .hasFieldOrPropertyWithValue("category", category2)
            .hasFieldOrPropertyWithValue("description", "description2")
            .hasFieldOrPropertyWithValue("name", "name2")
            .hasFieldOrPropertyWithValue("tags", List.of(tag))
            .hasFieldOrPropertyWithValue("workflowIds", List.of("workflow2"));
    }

    private Project getProject() {
        Project project = new Project();

        project.addWorkflow("workflow1");
        project.setCategory(category);
        project.setDescription("description");
        project.setName("name");
        project.setProjectVersion(1);
        project.setStatus(Project.Status.UNPUBLISHED);

        return project;
    }
}
