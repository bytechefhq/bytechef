
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

package com.bytechef.hermes.project.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.category.domain.Category;
import com.bytechef.hermes.project.domain.Project;
import com.bytechef.hermes.project.facade.ProjectFacade;
import com.bytechef.category.service.CategoryService;
import com.bytechef.hermes.project.web.rest.mapper.ProjectMapper;
import com.bytechef.hermes.project.web.rest.model.ProjectModel;
import com.bytechef.hermes.project.web.rest.model.CreateProjectWorkflowRequestModel;
import com.bytechef.hermes.project.web.rest.model.UpdateTagsRequestModel;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.web.rest.model.TagModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@WebFluxTest(value = ProjectController.class)
public class ProjectControllerIntTest {

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private ProjectFacade projectFacade;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testDeleteProject() {
        try {
            this.webTestClient
                .delete()
                .uri("/projects/1")
                .exchange()
                .expectStatus()
                .isOk();
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<Long> argument = ArgumentCaptor.forClass(Long.class);

        verify(projectFacade).deleteProject(argument.capture());

        Assertions.assertEquals(1L, argument.getValue());
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testGetProject() {
        try {
            Project project = getProject();

            when(projectFacade.getProject(1L)).thenReturn(project);

            this.webTestClient
                .get()
                .uri("/projects/1")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ProjectModel.class)
                .isEqualTo(projectMapper.convert(project));
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void testGetProjectWorkflows() {
        try {
            Workflow workflow = new Workflow("workflow1", "{}", Workflow.Format.JSON, Map.of());

            when(projectFacade.getProjectWorkflows(1L)).thenReturn(List.of(workflow));

            this.webTestClient
                .get()
                .uri("/projects/1/workflows")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.[0].id")
                .isEqualTo("workflow1");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void testGetProjects() {
        Project project = getProject();

        when(projectFacade.searchProjects(null, false, null)).thenReturn(List.of(project));

        this.webTestClient
            .get()
            .uri("/projects")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ProjectModel.class)
            .contains(projectMapper.convert(project))
            .hasSize(1);

        when(projectFacade.searchProjects(List.of(1L), false, null)).thenReturn(List.of(project));

        this.webTestClient
            .get()
            .uri("/projects?categoryIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ProjectModel.class)
            .hasSize(1);

        when(projectFacade.searchProjects(null, false, List.of(1L))).thenReturn(List.of(project));

        this.webTestClient
            .get()
            .uri("/projects?tagIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ProjectModel.class)
            .hasSize(1);

        when(projectFacade.searchProjects(List.of(1L), false, List.of(1L))).thenReturn(List.of(project));

        this.webTestClient
            .get()
            .uri("/projects?categoryIds=1&tagIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testPostProject() {
        Project project = getProject();
        ProjectModel projectModel = new ProjectModel()
            .name("name")
            .description("description");

        when(projectFacade.createProject(any())).thenReturn(project);

        try {
            assert project.getId() != null;
            this.webTestClient
                .post()
                .uri("/projects")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(projectModel)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.description")
                .isEqualTo(project.getDescription())
                .jsonPath("$.id")
                .isEqualTo(project.getId())
                .jsonPath("$.name")
                .isEqualTo(project.getName())
                .jsonPath("$.workflowIds[0]")
                .isEqualTo("workflow1");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<Project> integrationArgumentCaptor = ArgumentCaptor.forClass(Project.class);

        verify(projectFacade).createProject(integrationArgumentCaptor.capture());

        Project capturedProject = integrationArgumentCaptor.getValue();

        Assertions.assertEquals(capturedProject.getName(), "name");
        Assertions.assertEquals(capturedProject.getDescription(), "description");
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testPostIntegrationWorkflows() {
        Project project = getProject();
        CreateProjectWorkflowRequestModel createProjectWorkflowRequestModel = new CreateProjectWorkflowRequestModel()
            .name("workflowName")
            .description("workflowDescription");

        when(projectFacade.addWorkflow(1L, "workflowName", "workflowDescription", null))
            .thenReturn(project);

        try {
            assert project.getId() != null;
            this.webTestClient
                .post()
                .uri("/projects/1/workflows")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createProjectWorkflowRequestModel)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.description")
                .isEqualTo(project.getDescription())
                .jsonPath("$.id")
                .isEqualTo(project.getId())
                .jsonPath("$.name")
                .isEqualTo(project.getName())
                .jsonPath("$.workflowIds[0]")
                .isEqualTo("workflow1");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<String> nameArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> descriptionArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(projectFacade).addWorkflow(anyLong(), nameArgumentCaptor.capture(),
            descriptionArgumentCaptor.capture(), isNull());

        Assertions.assertEquals("workflowName", nameArgumentCaptor.getValue());
        Assertions.assertEquals("workflowDescription", descriptionArgumentCaptor.getValue());
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testPutIntegration() {
        Project project = getProject();
        ProjectModel projectModel = new ProjectModel()
            .id(1L)
            .name("name2");

        project.setName("name2");

        when(projectFacade.update(project)).thenReturn(project);

        try {
            this.webTestClient
                .put()
                .uri("/projects/1")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(projectModel)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo(project.getId())
                .jsonPath("$.description")
                .isEqualTo(project.getDescription())
                .jsonPath("$.name")
                .isEqualTo("name2")
                .jsonPath("$.workflowIds[0]")
                .isEqualTo("workflow1");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    @SuppressFBWarnings("NP")
    public void testPutIntegrationTags() {
        try {
            this.webTestClient
                .put()
                .uri("/projects/1/tags")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateTagsRequestModel().tags(List.of(new TagModel().name("tag1"))))
                .exchange()
                .expectStatus()
                .is2xxSuccessful();
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<List<Tag>> tagsArgumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(projectFacade).updateProjectTags(anyLong(), tagsArgumentCaptor.capture());

        List<Tag> capturedTags = tagsArgumentCaptor.getValue();

        Iterator<Tag> tagIterator = capturedTags.iterator();

        Tag capturedTag = tagIterator.next();

        Assertions.assertEquals("tag1", capturedTag.getName());
    }

    private static Project getProject() {
        Project project = new Project();

        project.addWorkflow("workflow1");

        project.setCategory(new Category(1L, "category"));
        project.setDescription("description");
        project.setId(1L);
        project.setName("name");
        project.setTags(List.of(new Tag(1L, "tag1"), new Tag(2L, "tag2")));

        return project;
    }

    @ComponentScan(basePackages = {
        "com.bytechef.atlas.web.rest.mapper",
        "com.bytechef.category.web.rest.mapper",
        "com.bytechef.hermes.project.web.rest",
        "com.bytechef.tag.web.rest.mapper"

    })
    @SpringBootConfiguration
    public static class IntegrationRestTestConfiguration {
    }
}
