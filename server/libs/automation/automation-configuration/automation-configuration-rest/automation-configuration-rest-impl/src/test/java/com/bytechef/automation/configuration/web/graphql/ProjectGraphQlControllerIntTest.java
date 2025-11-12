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

package com.bytechef.automation.configuration.web.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.dto.ProjectTemplateDTO;
import com.bytechef.automation.configuration.dto.SharedProjectDTO;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.web.graphql.config.AutomationConfigurationGraphQlTestConfiguration;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationConfigurationGraphQlTestConfiguration.class,
    ProjectGraphQlController.class
})
@GraphQlTest(
    controllers = ProjectGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
public class ProjectGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProjectFacade projectFacade;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TagService tagService;

    @Test
    void testCategorySchemaMapping() {
        // Given
        Project mockProject = createMockProject(1L, "Test Project");
        mockProject.setCategoryId(10L);
        Category mockCategory = createMockCategory(10L, "Test Category");

        when(projectService.getProject(1L)).thenReturn(mockProject);
        when(categoryService.getCategory(10L)).thenReturn(mockCategory);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    project(id: 1) {
                        id
                        name
                        category {
                            id
                            name
                        }
                    }
                }
                """)
            .execute()
            .path("project.category")
            .matchesJson("""
                {
                    "id": "10",
                    "name": "Test Category"
                }
                """);
    }

    @Test
    void testDeleteSharedProject() {
        // Given
        doNothing().when(projectFacade)
            .deleteSharedProject(anyLong());

        // When & Then
        this.graphQlTester
            .documentName("deleteSharedProject")
            .variable("id", 123L)
            .execute()
            .path("deleteSharedProject")
            .entity(Boolean.class)
            .isEqualTo(true);
    }

    @Test
    void testExportSharedProject() {
        // Given
        doNothing().when(projectFacade)
            .exportSharedProject(anyLong(), anyString());

        // When & Then
        this.graphQlTester
            .documentName("exportSharedProject")
            .variable("id", 123L)
            .variable("description", "Test template")
            .execute()
            .path("exportSharedProject")
            .valueIsNull();
    }

    @Test
    void testGetProjectById() {
        // Given
        Project mockProject = createMockProject(1051L, "Effective Java");
        when(projectService.getProject(1051L)).thenReturn(mockProject);

        // When & Then
        this.graphQlTester
            .documentName("projectById")
            .variable("id", 1051)
            .execute()
            .path("project")
            .matchesJson("""
                {
                    "id": "1051",
                    "name": "Effective Java"
                }
                """);
    }

    @Test
    void testGetProjects() {
        // Given
        List<Project> mockProjects = List.of(
            createMockProject(1L, "Project 1"),
            createMockProject(2L, "Project 2"));
        when(projectService.getProjects()).thenReturn(mockProjects);

        // When & Then
        this.graphQlTester
            .documentName("projects")
            .execute()
            .path("projects")
            .entityList(Project.class)
            .hasSize(2)
            .path("projects[0]")
            .matchesJson("""
                {
                    "id": "1",
                    "name": "Project 1"
                }
                """)
            .path("projects[1]")
            .matchesJson("""
                {
                    "id": "2",
                    "name": "Project 2"
                }
                """);
    }

    @Test
    void testGetSharedProject() {
        // Given
        SharedProjectDTO dto = new SharedProjectDTO("Template desc", true, 3, "http://public");

        when(projectFacade.getSharedProject(anyString())).thenReturn(dto);

        // When & Then
        this.graphQlTester
            .documentName("sharedProject")
            .variable("projectUuid", "some-uuid")
            .execute()
            .path("sharedProject.description")
            .entity(String.class)
            .isEqualTo("Template desc")
            .path("sharedProject.exported")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("sharedProject.projectVersion")
            .entity(Integer.class)
            .isEqualTo(3);
    }

    @Test
    void testImportProjectTemplate() {
        // Given
        when(projectFacade.importProjectTemplate(anyString(), anyLong(), eq(true))).thenReturn(456L);

        // When & Then
        this.graphQlTester
            .documentName("importProjectTemplate")
            .variable("id", "test-uuid-123")
            .variable("workspaceId", 789L)
            .execute()
            .path("importProjectTemplate")
            .entity(Long.class)
            .isEqualTo(456L);
    }

    @Test
    void testProjectTemplate() {
        // Given
        ProjectTemplateDTO dto = new ProjectTemplateDTO(
            "Alice", "alice@example.com", "DEV", null, List.of("cat1"), List.of(), "PT Desc", "tpl-1", Instant.now(),
            new ProjectTemplateDTO.ProjectInfo("Proj One", "Proj Desc"), 1, "http://public-url",
            List.of(new ProjectTemplateDTO.WorkflowInfo("wf-1", "WF Label", "WF Desc")));

        when(projectFacade.getProjectTemplate(eq("tpl-1"), eq(true))).thenReturn(dto);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    projectTemplate(id: \"tpl-1\", sharedProject: true) {
                        id
                        description
                        projectVersion
                        project { name }
                    }
                }
                """)
            .execute()
            .path("projectTemplate.id")
            .entity(String.class)
            .isEqualTo("tpl-1")
            .path("projectTemplate.description")
            .entity(String.class)
            .isEqualTo("PT Desc")
            .path("projectTemplate.projectVersion")
            .entity(Integer.class)
            .isEqualTo(1)
            .path("projectTemplate.project.name")
            .entity(String.class)
            .isEqualTo("Proj One");
    }

    @Test
    void testPreBuiltProjectTemplates() {
        // Given
        ProjectTemplateDTO dto1 = new ProjectTemplateDTO(
            null, null, null, null, List.of(), List.of(), "Desc 1", "tpl-1", Instant.now(),
            new ProjectTemplateDTO.ProjectInfo("P1", null), 1, null, List.of());
        ProjectTemplateDTO dto2 = new ProjectTemplateDTO(
            null, null, null, null, List.of(), List.of(),
            "Desc 2", "tpl-2", Instant.now(),
            new ProjectTemplateDTO.ProjectInfo("P2", null), 1, null,
            List.of());

        when(projectFacade.getPreBuiltProjectTemplates(anyString(), anyString())).thenReturn(List.of(dto1, dto2));

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    preBuiltProjectTemplates(query: \"\", category: \"\") {
                        id
                        description
                    }
                }
                """)
            .execute()
            .path("preBuiltProjectTemplates")
            .entityList(Object.class)
            .hasSize(2)
            .path("preBuiltProjectTemplates[0].id")
            .entity(String.class)
            .isEqualTo("tpl-1")
            .path("preBuiltProjectTemplates[1].id")
            .entity(String.class)
            .isEqualTo("tpl-2");
    }

    @Test
    void testTagsBatchMapping() {
        // Given
        Project mockProject1 = createMockProject(1L, "Project 1");

        mockProject1.setTagIds(List.of(1L, 2L));

        Project mockProject2 = createMockProject(2L, "Project 2");

        mockProject2.setTagIds(List.of(2L, 3L));

        List<Project> mockProjects = List.of(mockProject1, mockProject2);
        List<Tag> mockTags = List.of(
            createMockTag(1L, "Tag 1"),
            createMockTag(2L, "Tag 2"),
            createMockTag(3L, "Tag 3"));

        when(projectService.getProjects()).thenReturn(mockProjects);
        when(tagService.getTags(any())).thenReturn(mockTags);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    projects {
                        id
                        name
                        tags {
                            id
                            name
                        }
                    }
                }
                """)
            .execute()
            .path("projects[0].tags")
            .entityList(Tag.class)
            .hasSize(2)
            .path("projects[1].tags")
            .entityList(Tag.class)
            .hasSize(2);
    }

    private Project createMockProject(Long id, String name) {
        Project project = new Project();
        project.setId(id);
        project.setName(name);
        return project;
    }

    private Category createMockCategory(Long id, String name) {
        return new Category(id, name);
    }

    private Tag createMockTag(Long id, String name) {
        return new Tag(id, name);
    }
}
