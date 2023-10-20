
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

package com.bytechef.helios.project.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.category.domain.Category;
import com.bytechef.helios.project.web.rest.config.ProjectRestTestConfiguration;
import com.bytechef.helios.project.web.rest.mapper.ProjectMapper;
import com.bytechef.helios.project.dto.ProjectDTO;
import com.bytechef.helios.project.facade.ProjectFacade;
import com.bytechef.category.service.CategoryService;
import com.bytechef.helios.project.web.rest.model.ProjectModel;
import com.bytechef.helios.project.web.rest.model.CreateProjectWorkflowRequestModel;
import com.bytechef.helios.project.web.rest.model.UpdateTagsRequestModel;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.web.rest.model.TagModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = ProjectRestTestConfiguration.class)
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
            ProjectDTO projectDTO = getProjectDTO();

            when(projectFacade.getProject(1L)).thenReturn(projectDTO);

            this.webTestClient
                .get()
                .uri("/projects/1")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ProjectModel.class)
                .isEqualTo(projectMapper.convert(projectDTO));
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void testGetProjectWorkflows() {
        try {
            Workflow workflow = new Workflow("{}", Workflow.Format.JSON, "workflow1", Map.of());

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
        ProjectDTO projectDTO = getProjectDTO();

        when(projectFacade.searchProjects(null, false, null)).thenReturn(List.of(projectDTO));

        this.webTestClient
            .get()
            .uri("/projects")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ProjectModel.class)
            .contains(projectMapper.convert(projectDTO))
            .hasSize(1);

        when(projectFacade.searchProjects(List.of(1L), false, null)).thenReturn(List.of(projectDTO));

        this.webTestClient
            .get()
            .uri("/projects?categoryIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ProjectModel.class)
            .hasSize(1);

        when(projectFacade.searchProjects(null, false, List.of(1L))).thenReturn(List.of(projectDTO));

        this.webTestClient
            .get()
            .uri("/projects?tagIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ProjectModel.class)
            .hasSize(1);

        when(projectFacade.searchProjects(List.of(1L), false, List.of(1L))).thenReturn(List.of(projectDTO));

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
        ProjectDTO projectDTO = getProjectDTO();
        ProjectModel projectModel = new ProjectModel()
            .name("name")
            .description("description");

        when(projectFacade.createProject(any())).thenReturn(projectDTO);

        try {
            assert projectDTO.id() != null;
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
                .isEqualTo(projectDTO.description())
                .jsonPath("$.id")
                .isEqualTo(projectDTO.id())
                .jsonPath("$.name")
                .isEqualTo(projectDTO.name())
                .jsonPath("$.workflowIds[0]")
                .isEqualTo("workflow1");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<ProjectDTO> integrationDTOArgumentCaptor = ArgumentCaptor.forClass(ProjectDTO.class);

        verify(projectFacade).createProject(integrationDTOArgumentCaptor.capture());

        ProjectDTO capturedProjectDTO = integrationDTOArgumentCaptor.getValue();

        Assertions.assertEquals(capturedProjectDTO.name(), "name");
        Assertions.assertEquals(capturedProjectDTO.description(), "description");
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testPostIntegrationWorkflows() throws Exception {
        CreateProjectWorkflowRequestModel createProjectWorkflowRequestModel = new CreateProjectWorkflowRequestModel()
            .label("workflowLabel")
            .description("workflowDescription");
        Workflow workflow = new Workflow(
            "{\"description\": \"My description\", \"label\": \"New Workflow\", \"tasks\": []}", "id",
            Workflow.Format.JSON);

        when(projectFacade.addWorkflow(anyLong(), any(), any(), any()))
            .thenReturn(workflow);

        try {
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
                .isEqualTo("My description")
                .jsonPath("$.id")
                .isEqualTo(workflow.getId())
                .jsonPath("$.label")
                .isEqualTo("New Workflow");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<String> nameArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> descriptionArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(projectFacade).addWorkflow(anyLong(), nameArgumentCaptor.capture(),
            descriptionArgumentCaptor.capture(), isNull());

        Assertions.assertEquals("workflowLabel", nameArgumentCaptor.getValue());
        Assertions.assertEquals("workflowDescription", descriptionArgumentCaptor.getValue());
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testPutIntegration() {
        ProjectDTO projectDTO = ProjectDTO.builder()
            .category(new Category(1L, "category"))
            .description("description")
            .id(1L)
            .name("name2")
            .tags(List.of(new Tag(1L, "tag1"), new Tag(2L, "tag2")))
            .workflowIds(List.of("workflow1"))
            .build();
        ProjectModel projectModel = new ProjectModel()
            .id(1L)
            .name("name2");

        when(projectFacade.update(any(ProjectDTO.class))).thenReturn(projectDTO);

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
                .isEqualTo(projectDTO.id())
                .jsonPath("$.description")
                .isEqualTo(projectDTO.description())
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

    private static ProjectDTO getProjectDTO() {
        return ProjectDTO.builder()
            .category(new Category(1L, "category"))
            .description("description")
            .id(1L)
            .name("name")
            .tags(List.of(new Tag(1L, "tag1"), new Tag(2L, "tag2")))
            .workflowIds(List.of("workflow1"))
            .build();
    }

}
