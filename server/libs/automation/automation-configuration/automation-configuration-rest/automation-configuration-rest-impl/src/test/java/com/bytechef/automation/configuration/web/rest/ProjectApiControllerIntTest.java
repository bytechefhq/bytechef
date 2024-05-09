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

package com.bytechef.automation.configuration.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.WorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectInstanceFacade;
import com.bytechef.automation.configuration.web.rest.config.ProjectConfigurationRestTestConfiguration;
import com.bytechef.automation.configuration.web.rest.mapper.ProjectMapper;
import com.bytechef.automation.configuration.web.rest.model.CategoryModel;
import com.bytechef.automation.configuration.web.rest.model.ProjectModel;
import com.bytechef.automation.configuration.web.rest.model.TagModel;
import com.bytechef.automation.configuration.web.rest.model.UpdateTagsRequestModel;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.configuration.web.rest.model.WorkflowModel;
import com.bytechef.platform.tag.domain.Tag;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

/**
 * @author Ivica Cardic
 */
@Disabled
@ContextConfiguration(classes = ProjectConfigurationRestTestConfiguration.class)
@WebMvcTest(value = ProjectApiController.class)
public class ProjectApiControllerIntTest {

    @MockBean
    private CategoryService categoryService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectInstanceFacade projectInstanceFacade;

    @MockBean
    private ProjectFacade projectFacade;

    @Autowired
    private ProjectMapper.ProjectDTOToProjectModelMapper projectMapper;

    private WebTestClient webTestClient;

    @BeforeEach
    public void setup() {
        this.webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();
    }

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
    public void testGetProject() {
        try {
            ProjectDTO projectDTO = getProjectDTO();

            when(projectFacade.getProject(1L))
                .thenReturn(projectDTO);

            this.webTestClient
                .get()
                .uri("/projects/1")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ProjectModel.class)
                .isEqualTo(Validate.notNull(projectMapper.convert(projectDTO), "projectModel"));
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void testGetProjectCategories() {
        try {
            when(projectFacade.getProjectCategories())
                .thenReturn(List.of(new Category(1, "name")));

            this.webTestClient
                .get()
                .uri("/categories")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(CategoryModel.class)
                .hasSize(1);
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void testGetProjectTags() {
        when(projectFacade.getProjectTags())
            .thenReturn(List.of(new Tag(1L, "tag1"), new Tag(2L, "tag2")));

        try {
            this.webTestClient
                .get()
                .uri("/projects/tags")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.[0].id")
                .isEqualTo(1)
                .jsonPath("$.[1].id")
                .isEqualTo(2)
                .jsonPath("$.[0].name")
                .isEqualTo("tag1")
                .jsonPath("$.[1].name")
                .isEqualTo("tag2");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void testGetProjectWorkflows() {
        try {
            WorkflowDTO workflow = new WorkflowDTO(new Workflow("workflow1", "{}", Format.JSON), 0);

            when(projectFacade.getProjectWorkflows(1L))
                .thenReturn(List.of(workflow));

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

        when(projectFacade.getProjects(null, false, null, null))
            .thenReturn(List.of(projectDTO));

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

        when(projectFacade.getProjects(1L, false, null, null))
            .thenReturn(List.of(projectDTO));

        this.webTestClient
            .get()
            .uri("/projects?categoryIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ProjectModel.class)
            .hasSize(1);

        when(projectFacade.getProjects(null, false, 1L, null))
            .thenReturn(List.of(projectDTO));

        this.webTestClient
            .get()
            .uri("/projects?tagIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ProjectModel.class)
            .hasSize(1);

        when(projectFacade.getProjects(1L, false, 1L, null))
            .thenReturn(List.of(projectDTO));

        this.webTestClient
            .get()
            .uri("/projects?categoryIds=1&tagIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    public void testPostProject() {
        ProjectDTO projectDTO = getProjectDTO();
        ProjectModel projectModel = new ProjectModel()
            .name("name")
            .description("description");

        when(projectFacade.createProject(any()))
            .thenReturn(projectDTO);

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
    public void testPostIntegrationWorkflows() {
        String definition = "{\"description\": \"My description\", \"label\": \"New Workflow\", \"tasks\": []}";

        WorkflowModel workflowModel = new WorkflowModel().definition(definition);
        WorkflowDTO workflow = new WorkflowDTO(new Workflow("id", definition, Format.JSON), 0);

        when(projectFacade.addWorkflow(anyLong(), any()))
            .thenReturn(workflow);

        try {
            this.webTestClient
                .post()
                .uri("/projects/1/workflows")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(workflowModel)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.description")
                .isEqualTo("My description")
                .jsonPath("$.id")
                .isEqualTo(Validate.notNull(workflow.id(), "id"))
                .jsonPath("$.label")
                .isEqualTo("New Workflow");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<String> nameArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> descriptionArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(projectFacade).addWorkflow(
            anyLong(), isNull());

        Assertions.assertEquals("workflowLabel", nameArgumentCaptor.getValue());
        Assertions.assertEquals("workflowDescription", descriptionArgumentCaptor.getValue());
    }

    @Test
    public void testPutIntegration() {
        ProjectDTO projectDTO = ProjectDTO.builder()
            .category(new Category(1L, "category"))
            .description("description")
            .id(1L)
            .name("name2")
            .tags(List.of(new Tag(1L, "tag1"), new Tag(2L, "tag2")))
            .projectWorkflowIds(List.of(1L))
            .build();
        ProjectModel projectModel = new ProjectModel()
            .id(1L)
            .name("name2");

        when(projectFacade.updateProject(any(ProjectDTO.class)))
            .thenReturn(projectDTO);

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
                .jsonPath("$.projectWorkflowIds[0]")
                .isEqualTo(1L);
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
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
            .projectWorkflowIds(List.of(1L))
            .build();
    }

}
