
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

package com.bytechef.hermes.project.facade;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.repository.WorkflowCrudRepository;
import com.bytechef.hermes.project.config.ProjectIntTestConfiguration;
import com.bytechef.category.domain.Category;
import com.bytechef.hermes.project.domain.Project;
import com.bytechef.category.repository.CategoryRepository;
import com.bytechef.hermes.project.repository.ProjectRepository;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.repository.TagRepository;
import com.bytechef.test.annotation.EmbeddedSql;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
@EmbeddedSql
@SpringBootTest(
    classes = ProjectIntTestConfiguration.class,
    properties = "bytechef.workflow.workflow-repository.jdbc.enabled=true")
public class ProjectFacadeIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProjectFacade projectFacade;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    private WorkflowCrudRepository workflowRepository;

    @BeforeEach
    @SuppressFBWarnings("NP")
    public void beforeEach() {
        projectRepository.deleteAll();

        categoryRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testAddWorkflow() {
        Project project = new Project();

        project.setName("name");
        project.setStatus(Project.Status.UNPUBLISHED);

        project = projectRepository.save(project);

        project = projectFacade.addWorkflow(project.getId(), "Workflow 1", "Description", null);

        List<String> workflowIds = project.getWorkflowIds();

        Workflow workflow = workflowRepository.findById(workflowIds.iterator()
            .next())
            .orElseThrow();

        assertThat(workflow.getDescription()).isEqualTo("Description");
        assertThat(workflow.getLabel()).isEqualTo("Workflow 1");
    }

    @Test
    public void testCreate() {
        Project project = new Project();

        project.setName("name1");
        project.setDescription("description");
        project.setStatus(Project.Status.UNPUBLISHED);

        Category category = categoryRepository.save(new Category("name"));

        project.setCategory(category);
        project.setTags(List.of(new Tag("tag1")));

        project = projectFacade.create(project);

        assertThat(project.getCategoryId()).isEqualTo(category.getId());
        assertThat(project.getDescription()).isEqualTo("description");
        assertThat(project.getName()).isEqualTo("name1");
        assertThat(project.getId()).isNotNull();
        assertThat(project.getTagIds()).hasSize(1);
        assertThat(project.getWorkflowIds()).hasSize(1);

        project = new Project();

        project.setName("name2");
        project.setStatus(Project.Status.UNPUBLISHED);
        project.setWorkflowIds(List.of("workflow2"));

        project = projectFacade.create(project);

        assertThat(project.getWorkflowIds()).hasSize(1);
        assertThat(project.getWorkflowIds()).contains("workflow2");
    }

    @Test
    public void testDelete() {
        Project project1 = new Project();

        project1.setName("name1");
        project1.setStatus(Project.Status.UNPUBLISHED);
        project1.setTags(List.of(new Tag("tag1")));

        project1 = projectFacade.create(project1);

        Project project2 = new Project();

        project2.setName("name2");
        project2.setStatus(Project.Status.UNPUBLISHED);
        project2.setTags(List.of(new Tag("tag1")));

        project2 = projectFacade.create(project2);

        assertThat(projectRepository.count()).isEqualTo(2);
        assertThat(tagRepository.count()).isEqualTo(1);

        projectFacade.delete(project1.getId());

        assertThat(projectRepository.count()).isEqualTo(1);

        projectFacade.delete(project2.getId());

        assertThat(projectRepository.count()).isEqualTo(0);
        assertThat(tagRepository.count()).isEqualTo(1);
    }

    @Test
    public void testGetProject() {
        Project project = new Project();

        Category category = categoryRepository.save(new Category("category1"));

        project.setCategory(category);
        project.setName("name");
        project.setStatus(Project.Status.UNPUBLISHED);

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        project.setTags(List.of(tag1, tag2));

        project = projectRepository.save(project);

        assertThat(projectFacade.getProject(project.getId()))
            .isEqualTo(project)
            .hasFieldOrPropertyWithValue("category", category)
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));
    }

    @Test
    public void testGetProjects() {
        Project project = new Project();

        Category category = categoryRepository.save(new Category("category1"));

        project.setCategory(category);
        project.setName("name");
        project.setStatus(Project.Status.UNPUBLISHED);

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        project.setTags(List.of(tag1, tag2));

        project = projectRepository.save(project);

        List<Project> projects = projectFacade.getProjects(null, null);

        assertThat(projects).isEqualTo(List.of(project));

        project = projects.get(0);

        assertThat(projectFacade.getProject(project.getId()))
            .isEqualTo(project)
            .hasFieldOrPropertyWithValue("category", category)
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testGetProjectTags() {
        Project project = new Project();

        Tag tag1 = tagRepository.save(new Tag("tag1"));

        project.setName("name");
        project.setStatus(Project.Status.UNPUBLISHED);
        project.setTags(List.of(tag1, tagRepository.save(new Tag("tag2"))));

        projectRepository.save(project);

        assertThat(projectFacade.getProjectTags()
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet())).contains("tag1", "tag2");

        project = new Project();

        project.setName("name2");
        project.setStatus(Project.Status.UNPUBLISHED);

        tag1 = tagRepository.findById(tag1.getId())
            .orElseThrow();

        project.setTags(List.of(tag1, tagRepository.save(new Tag("tag3"))));

        projectRepository.save(project);

        assertThat(projectFacade.getProjectTags()
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet())).contains("tag1", "tag2", "tag3");

        projectRepository.deleteById(project.getId());

        assertThat(projectFacade.getProjectTags()
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet())).contains("tag1", "tag2");
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testGetProjectWorkflows() {
        Workflow workflow = new Workflow("{}", Workflow.Format.JSON);

        workflow.setNew(true);

        workflow = workflowRepository.save(workflow);

        Project project = new Project();

        project.setName("name");
        project.setStatus(Project.Status.UNPUBLISHED);
        project.setWorkflowIds(List.of(workflow.getId()));

        project = projectRepository.save(project);

        List<Workflow> workflows = projectFacade.getProjectWorkflows(project.getId());

        assertThat(workflows).contains(workflow);
    }

    @Test
    public void testUpdate() {
        Project project = new Project();

        project.setName("name");
        project.setStatus(Project.Status.UNPUBLISHED);

        Tag tag1 = new Tag("tag1");

        project.setTags(List.of(tag1, tagRepository.save(new Tag("tag2"))));

        project = projectFacade.create(project);

        assertThat(project.getTagIds()).hasSize(2);
        assertThat(project.getWorkflowIds()).hasSize(1);

        project.setTags(List.of(tag1));

        projectRepository.save(project);

        project = projectFacade.update(project);

        assertThat(project.getTagIds()).hasSize(1);
    }
}
