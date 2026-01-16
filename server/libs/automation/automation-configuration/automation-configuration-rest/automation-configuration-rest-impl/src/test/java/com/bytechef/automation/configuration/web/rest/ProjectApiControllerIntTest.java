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

package com.bytechef.automation.configuration.web.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.facade.ProjectCategoryFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectTagFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.web.rest.config.AutomationConfigurationRestConfigurationSharedMocks;
import com.bytechef.automation.configuration.web.rest.config.AutomationConfigurationRestTestConfiguration;
import com.bytechef.automation.configuration.web.rest.mapper.ProjectMapper;
import com.bytechef.automation.configuration.web.rest.model.ProjectModel;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.tag.domain.Tag;
import jakarta.servlet.ServletException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClientRequestException;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = AutomationConfigurationRestTestConfiguration.class)
@WebMvcTest(value = ProjectApiController.class)
@AutomationConfigurationRestConfigurationSharedMocks
public class ProjectApiControllerIntTest {

    @MockitoBean
    private CategoryService categoryService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectCategoryFacade projectCategoryFacade;

    @MockitoBean
    private ProjectDeploymentFacade projectDeploymentFacade;

    @MockitoBean
    private ProjectFacade projectFacade;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private ProjectTagFacade projectTagFacade;

    @MockitoBean
    private ProjectWorkflowFacade projectWorkflowFacade;

    @MockitoBean
    private WorkflowService workflowService;

    @MockitoBean
    private WorkspaceFacade workspaceFacade;

    @Autowired
    private ProjectMapper.ProjectDTOToProjectModelMapper projectMapper;

    private WebTestClient webTestClient;

    @BeforeEach
    public void beforeEach() {
        this.webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();
    }

    @Test
    public void testDeleteProject() {
        try {
            this.webTestClient
                .delete()
                .uri("/internal/projects/1")
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
                .uri("/internal/projects/1")
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
    public void testGetProjects() {
        ProjectDTO projectDTO = getProjectDTO();

        when(projectFacade.getWorkspaceProjects(null, null, true, null, null, null, 1L))
            .thenReturn(List.of(projectDTO));

        this.webTestClient
            .get()
            .uri("/internal/workspaces/1/projects")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ProjectModel.class)
            .contains(projectMapper.convert(projectDTO))
            .hasSize(1);

        // Test with categoryId parameter
        when(projectFacade.getWorkspaceProjects(null, 1L, true, null, null, null, 1L))
            .thenReturn(List.of(projectDTO));

        this.webTestClient
            .get()
            .uri("/internal/workspaces/1/projects?categoryId=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ProjectModel.class)
            .hasSize(1);

        // Test with tagId parameter
        when(projectFacade.getWorkspaceProjects(null, null, true, null, null, 1L, 1L))
            .thenReturn(List.of(projectDTO));

        this.webTestClient
            .get()
            .uri("/internal/workspaces/1/projects?tagId=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ProjectModel.class)
            .hasSize(1);
    }

    @Test
    public void testPostProject() {
        ProjectDTO projectDTO = getProjectDTO();
        ProjectModel projectModel = new ProjectModel()
            .name("name")
            .description("description");

        try {
            assert projectDTO.id() != null;
            this.webTestClient
                .post()
                .uri("/internal/projects")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(projectModel)
                .exchange()
                .expectStatus()
                .isOk();
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<ProjectDTO> integrationDTOArgumentCaptor = ArgumentCaptor.forClass(ProjectDTO.class);

        verify(projectFacade).createProject(integrationDTOArgumentCaptor.capture());

        ProjectDTO capturedProjectDTO = integrationDTOArgumentCaptor.getValue();

        Assertions.assertEquals("name", capturedProjectDTO.name());
        Assertions.assertEquals("description", capturedProjectDTO.description());
    }

    @Test
    public void testPutIntegration() {
        ProjectModel projectModel = new ProjectModel()
            .id(1L)
            .name("name2");

        try {
            this.webTestClient
                .put()
                .uri("/internal/projects/1")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(projectModel)
                .exchange()
                .expectStatus()
                .isNoContent();
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void testExportProject() {
        try {
            ProjectDTO projectDTO = getProjectDTO();
            byte[] mockProjectData = "mock zip data".getBytes(StandardCharsets.UTF_8);

            when(projectFacade.exportProject(1L))
                .thenReturn(mockProjectData);

            Project project = new Project();

            project.setId(1L);
            project.setName(projectDTO.name());

            when(projectService.getProject(1L))
                .thenReturn(project);

            this.webTestClient
                .get()
                .uri("/internal/projects/1/export")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .valueEquals("Content-Disposition", "attachment; filename=\"name.zip\"")
                .expectBody(byte[].class)
                .isEqualTo(mockProjectData);
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        verify(projectFacade).exportProject(1L);
    }

    @Test
    public void testExportProjectNotFound() {
        when(projectFacade.exportProject(999L))
            .thenThrow(new RuntimeException("Project not found"));

        try {
            this.webTestClient
                .get()
                .uri("/internal/projects/999/export")
                .exchange()
                .expectStatus()
                .is5xxServerError();
            Assertions.fail("Expected WebClientRequestException to be thrown");
        } catch (WebClientRequestException e) {
            Assertions.assertInstanceOf(ServletException.class, e.getCause());
        }

        verify(projectFacade).exportProject(999L);
    }

    @Test
    public void testImportProject() {
        try {
            byte[] mockProjectData = "mock zip data".getBytes(StandardCharsets.UTF_8);
            MockMultipartFile mockFile = new MockMultipartFile(
                "file", "project.zip", "application/zip", mockProjectData);

            when(projectFacade.importProject(mockProjectData, 1L))
                .thenReturn(123L);

            this.webTestClient
                .post()
                .uri("/internal/workspaces/1/projects/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", mockFile.getResource()))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Long.class)
                .isEqualTo(123L);
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<byte[]> dataArgumentCaptor = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<Long> workspaceIdArgumentCaptor = ArgumentCaptor.forClass(Long.class);

        verify(projectFacade).importProject(dataArgumentCaptor.capture(), workspaceIdArgumentCaptor.capture());

        Assertions.assertEquals("mock zip data", new String(dataArgumentCaptor.getValue(), StandardCharsets.UTF_8));
        Assertions.assertEquals(1L, workspaceIdArgumentCaptor.getValue());
    }

    @Test
    public void testImportProjectWithInvalidFile() {
        byte[] invalidData = "invalid data".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile mockFile = new MockMultipartFile("file", "project.txt", "text/plain", invalidData);

        when(projectFacade.importProject(invalidData, 1L))
            .thenThrow(new RuntimeException("Invalid project file"));

        try {
            this.webTestClient
                .post()
                .uri("/internal/workspaces/1/projects/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", mockFile.getResource()))
                .exchange()
                .expectStatus()
                .is5xxServerError();
            Assertions.fail("Expected WebClientRequestException to be thrown");
        } catch (WebClientRequestException e) {
            Assertions.assertInstanceOf(ServletException.class, e.getCause());
        }

        ArgumentCaptor<byte[]> dataArgumentCaptor = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<Long> workspaceIdArgumentCaptor = ArgumentCaptor.forClass(Long.class);

        verify(projectFacade).importProject(dataArgumentCaptor.capture(), workspaceIdArgumentCaptor.capture());

        Assertions.assertEquals("invalid data", new String(dataArgumentCaptor.getValue(), StandardCharsets.UTF_8));
        Assertions.assertEquals(1L, workspaceIdArgumentCaptor.getValue());
    }

    @Test
    public void testImportProjectWithEmptyFile() {
        byte[] emptyData = new byte[0];
        MockMultipartFile mockFile = new MockMultipartFile("file", "empty.zip", "application/zip", emptyData);

        when(projectFacade.importProject(emptyData, 1L))
            .thenThrow(new RuntimeException("Empty project file"));

        try {
            this.webTestClient
                .post()
                .uri("/internal/workspaces/1/projects/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", mockFile.getResource()))
                .exchange()
                .expectStatus()
                .is5xxServerError();
            Assertions.fail("Expected WebClientRequestException to be thrown");
        } catch (WebClientRequestException e) {
            Assertions.assertInstanceOf(ServletException.class, e.getCause());
        }

        ArgumentCaptor<byte[]> dataArgumentCaptor = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<Long> workspaceIdArgumentCaptor = ArgumentCaptor.forClass(Long.class);

        verify(projectFacade).importProject(dataArgumentCaptor.capture(), workspaceIdArgumentCaptor.capture());

        Assertions.assertEquals(0, dataArgumentCaptor.getValue().length);
        Assertions.assertEquals(1L, workspaceIdArgumentCaptor.getValue());
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
