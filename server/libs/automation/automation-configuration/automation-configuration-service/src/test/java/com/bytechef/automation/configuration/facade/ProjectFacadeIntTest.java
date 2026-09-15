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

import static com.bytechef.automation.configuration.util.ProjectDeploymentFacadeHelper.PREFIX_CATEGORY;
import static com.bytechef.automation.configuration.util.ProjectDeploymentFacadeHelper.PREFIX_PROJECT_DESCRIPTION;
import static com.bytechef.automation.configuration.util.ProjectDeploymentFacadeHelper.PREFIX_PROJECT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.SharedTemplate;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectTemplateDTO;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowServiceImpl;
import com.bytechef.automation.configuration.service.SharedTemplateService;
import com.bytechef.automation.configuration.util.ProjectDeploymentFacadeHelper;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.bytechef.platform.githubproxy.client.model.FileItem;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.lang3.Validate;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
@SpringBootTest(
    classes = ProjectIntTestConfiguration.class,
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
public class ProjectFacadeIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProjectFacade projectFacade;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectTagFacade projectTagFacade;

    @Autowired
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    private WorkflowCrudRepository workflowRepository;

    @MockitoBean
    private SharedTemplateFileStorage sharedTemplateFileStorage;

    @MockitoBean
    private SharedTemplateService sharedTemplateService;

    @MockitoBean
    private com.bytechef.automation.configuration.service.PreBuiltTemplateService preBuiltTemplateService;

    private Workspace workspace;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ProjectDeploymentFacadeHelper projectFacadeInstanceHelper;

    @Autowired
    private ProjectWorkflowServiceImpl projectWorkflowServiceImpl;

    private static final Random random = new SecureRandom();

    @AfterEach
    public void afterEach() {
        projectWorkflowRepository.deleteAll();
        projectRepository.deleteAll();

        for (Workflow workflow : workflowRepository.findAll()) {
            workflowRepository.deleteById(workflow.getId());
        }

        workspaceRepository.deleteAll();

        categoryRepository.deleteAll();
        tagRepository.deleteAll();

    }

    @BeforeEach
    public void beforeEach() {
        workspace = workspaceRepository.save(new Workspace("test"));
    }

    @Test
    public void testAddWorkflow() {
        ProjectDTO projectDTO = projectFacadeInstanceHelper.createProject(workspace.getId());

        ProjectWorkflowDTO workflowDTO = projectFacadeInstanceHelper.addTestWorkflow(projectDTO);

        ProjectWorkflow projectWorkflow =
            projectWorkflowServiceImpl.getProjectWorkflow(workflowDTO.getProjectWorkflowId());

        Optional<Workflow> workflowOptional = workflowRepository.findById(projectWorkflow.getWorkflowId());

        Assertions.assertTrue(workflowOptional.isPresent(), "Workflow not found");

        Workflow workflow = workflowOptional.get();

        assertThat(workflowDTO.getDescription()).isEqualTo(workflow.getDescription());
        assertThat(workflowDTO.getLabel()).isEqualTo(workflow.getLabel());
    }

    @Test
    public void testCreate() {
        ProjectDTO projectDTO = projectFacadeInstanceHelper.createProject(workspace.getId());

        Category category = projectDTO.category();

        assertThat(category.getName()).startsWith(PREFIX_CATEGORY);

        assertThat(projectDTO.description()).startsWith(PREFIX_PROJECT_DESCRIPTION);
        assertThat(projectDTO.name()).startsWith(PREFIX_PROJECT_NAME);
        assertThat(projectDTO.id()).isNotNull();
        assertThat(projectDTO.tags()).hasSize(3);
        assertThat(projectDTO.projectWorkflowIds()).hasSize(0);
        assertThat(categoryRepository.count()).isEqualTo(1);
        assertThat(tagRepository.count()).isEqualTo(3);
    }

    @Test
    public void testDelete() {
        ProjectDTO projectDTO1 = projectFacadeInstanceHelper.createProject(workspace.getId());
        ProjectDTO projectDTO2 = projectFacadeInstanceHelper.createProject(workspace.getId());

        ProjectWorkflowDTO projectWorkflowDTO1 = projectFacadeInstanceHelper.addTestWorkflow(projectDTO1);
        ProjectWorkflowDTO projectWorkflowDTO2 = projectFacadeInstanceHelper.addTestWorkflow(projectDTO2);

        String workflowId1 = projectWorkflowDTO1.getId();
        String workflowId2 = projectWorkflowDTO2.getId();

        Long projectWorkflowId1 = projectWorkflowDTO1.getProjectWorkflowId();
        Long projectWorkflowId2 = projectWorkflowDTO2.getProjectWorkflowId();

        // Verify initial state - both workflow and project_workflow tables have records
        assertThat(projectRepository.count()).isEqualTo(2);
        assertThat(tagRepository.count()).isEqualTo(6);

        List<Workflow> workflows = workflowRepository.findAll();

        assertThat(workflows.size()).isEqualTo(2);

        assertThat(projectWorkflowRepository.count()).isEqualTo(2);

        // Verify specific workflow records exist in workflow table
        assertThat(workflowRepository.findById(workflowId1)).isPresent();
        assertThat(workflowRepository.findById(workflowId2)).isPresent();

        // Verify specific project workflow records exist in project_workflow table
        assertThat(projectWorkflowRepository.findById(projectWorkflowId1)).isPresent();
        assertThat(projectWorkflowRepository.findById(projectWorkflowId2)).isPresent();

        projectFacade.deleteProject(projectDTO1.id());

        assertThat(projectRepository.count()).isEqualTo(1);

        workflows = workflowRepository.findAll();

        assertThat(workflows.size()).isEqualTo(1);

        assertThat(projectWorkflowRepository.count()).isEqualTo(1);

        // Verify specific records for project1 are deleted from workflow table
        assertThat(workflowRepository.findById(workflowId1)).isEmpty();
        // Verify project2 workflow record still exists in workflow table
        assertThat(workflowRepository.findById(workflowId2)).isPresent();

        // Verify specific records for project1 are deleted from project_workflow table
        assertThat(projectWorkflowRepository.findById(projectWorkflowId1)).isEmpty();
        // Verify project2 project workflow record still exists in project_workflow table
        assertThat(projectWorkflowRepository.findById(projectWorkflowId2)).isPresent();

        projectFacade.deleteProject(projectDTO2.id());

        assertThat(projectRepository.count()).isEqualTo(0);
        assertThat(tagRepository.count()).isEqualTo(6);

        workflows = workflowRepository.findAll();

        assertThat(workflows.size()).isEqualTo(0);

        assertThat(projectWorkflowRepository.count()).isEqualTo(0);

        // Verify all specific workflow records are deleted from workflow table
        assertThat(workflowRepository.findById(workflowId1)).isEmpty();
        assertThat(workflowRepository.findById(workflowId2)).isEmpty();

        // Verify all specific project workflow records are deleted from project_workflow table
        assertThat(projectWorkflowRepository.findById(projectWorkflowId1)).isEmpty();
        assertThat(projectWorkflowRepository.findById(projectWorkflowId2)).isEmpty();
    }

    @Test
    public void testExportProject() {
        ProjectDTO projectDTO = projectFacadeInstanceHelper.createProject(workspace.getId());

        projectFacadeInstanceHelper.addTestWorkflow(projectDTO);

        byte[] exportedData = projectFacade.exportProject(projectDTO.id());

        assertThat(exportedData).isNotNull();
        assertThat(exportedData.length).isGreaterThan(0);

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(exportedData))) {
            ZipEntry zipEntry;
            boolean foundProjectJson = false;
            boolean foundWorkflowFile = false;

            while ((zipEntry = zis.getNextEntry()) != null) {
                String name = zipEntry.getName();

                if ("project.json".equals(name)) {
                    foundProjectJson = true;
                } else if (name.startsWith("workflow-")) {
                    foundWorkflowFile = true;
                }

                zis.closeEntry();
            }

            assertThat(foundProjectJson).isTrue();
            assertThat(foundWorkflowFile).isTrue();
        } catch (Exception e) {
            Assertions.fail("Failed to read exported ZIP file", e);
        }
    }

    @Test
    public void testExportProjectInvalidId() {
        Assertions.assertThrows(
            Exception.class,
            () -> projectFacade.exportProject(999999L));
    }

    @Test
    public void testExportSharedProject() {
        ProjectDTO projectDTO = projectFacadeInstanceHelper.createProject(workspace.getId());

        projectFacadeInstanceHelper.addTestWorkflow(projectDTO);

        FileEntry mockFileEntry = new FileEntry(
            "project_test.zip", "zip", "application/zip", "http://localhost/shared/project_test.zip");

        when(sharedTemplateFileStorage.storeFileContent(any(String.class), any()))
            .thenReturn(mockFileEntry);

        projectFacade.exportSharedProject(projectDTO.id(), null);
    }

    @Test
    public void testGetPreBuiltProjectTemplates() {
        byte[] projectZip;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            zipOutputStream.putNextEntry(new ZipEntry("template.json"));

            String templateJson =
                "{\"description\":\"Catalog description\",\"projectVersion\":1,\"categories\":[\"sales\"]}";

            zipOutputStream.write(templateJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("project.json"));

            String projectJson = "{\"name\":\"Catalog Project\",\"description\":\"Catalog imported project\"}";

            zipOutputStream.write(projectJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("workflow-test.json"));

            String workflowJson = "\"{\\\"tasks\\\":[]}\"";

            zipOutputStream.write(workflowJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();
            zipOutputStream.finish();

            projectZip = byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(preBuiltTemplateService.getFiles("projects"))
            .thenReturn(List.of(new FileItem("projects/catalog-project.zip", 0, "sha", "ref", null)));

        when(preBuiltTemplateService.getPrebuiltTemplateData(any(String.class))).thenReturn(projectZip);

        List<ProjectTemplateDTO> projectTemplateDTOs = projectFacade.getPreBuiltProjectTemplates(null, null);

        assertThat(projectTemplateDTOs).isNotNull();
        assertThat(projectTemplateDTOs).hasSize(1);

        ProjectTemplateDTO projectTemplateDTO = projectTemplateDTOs.getFirst();

        ProjectTemplateDTO.ProjectInfo project = projectTemplateDTO.project();

        assertThat(project.name()).isEqualTo("Catalog Project");

        assertThat(projectTemplateDTO.categories()).contains("sales");
    }

    @Test
    public void testGetProject() {
        Project project = new Project();

        project.setWorkspaceId(workspace.getId());

        Category category = categoryRepository.save(new Category("category1"));

        project.setCategory(category);
        project.setName("name");

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        project.setTags(List.of(tag1, tag2));

        project = projectRepository.save(project);

        assertThat(projectFacade.getProject(Validate.notNull(project.getId(), "id")))
            .hasFieldOrPropertyWithValue("category", category)
            .hasFieldOrPropertyWithValue("id", Validate.notNull(project.getId(), "id"))
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));
    }

    @Test
    public void testGetProjects() {
        List<ProjectDTO> testProjectDTOs = new ArrayList<>();

        testProjectDTOs.add(projectFacadeInstanceHelper.createProject(workspace.getId()));
        testProjectDTOs.add(projectFacadeInstanceHelper.createProject(workspace.getId()));
        testProjectDTOs.add(projectFacadeInstanceHelper.createProject(workspace.getId()));
        testProjectDTOs.add(projectFacadeInstanceHelper.createProject(workspace.getId()));

        List<ProjectDTO> projectsDTOs = projectFacade.getProjects(null, null, null, null);

        assertThat(projectsDTOs).hasSize(testProjectDTOs.size());

        ProjectDTO projectDTO = projectsDTOs.get(random.nextInt(testProjectDTOs.size()));

        Project project = projectDTO.toProject();

        Category category = projectDTO.category();

        projectsDTOs = projectFacade.getProjects(category.getId(), null, null, null);

        assertThat(projectsDTOs).hasSize(1);

        assertThat(projectFacade.getProject(Validate.notNull(project.getId(), "id")))
            .isEqualTo(projectDTO)
            .hasFieldOrPropertyWithValue("category", category)
            .hasFieldOrPropertyWithValue("tags", projectDTO.tags());
    }

    @Test
    public void testGetProjectTags() {
        Project project = new Project();

        project.setWorkspaceId(workspace.getId());

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        project.setName("name");
        project.setTags(List.of(tag1, tag2));

        projectRepository.save(project);

        assertThat(
            projectTagFacade.getProjectTags(Validate.notNull(workspace.getId(), "id"))
                .stream()
                .map(Tag::getName)
                .collect(Collectors.toSet())).contains("tag1", "tag2");

        project = new Project();

        project.setName("name2");
        project.setWorkspaceId(workspace.getId());

        tag1 = tagRepository.findById(Validate.notNull(tag1.getId(), "id"))
            .orElseThrow();

        project.setTags(List.of(tag1, tagRepository.save(new Tag("tag3"))));

        projectRepository.save(project);

        assertThat(
            projectTagFacade.getProjectTags(Validate.notNull(workspace.getId(), "id"))
                .stream()
                .map(Tag::getName)
                .collect(Collectors.toSet())).contains("tag1", "tag2", "tag3");

        projectRepository.deleteById(Validate.notNull(project.getId(), "id"));

        assertThat(
            projectTagFacade.getProjectTags(Validate.notNull(workspace.getId(), "id"))
                .stream()
                .map(Tag::getName)
                .collect(Collectors.toSet())).contains("tag1", "tag2");
    }

    @Test
    public void testGetProjectTemplatePreBuilt() {
        byte[] projectZip;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            zipOutputStream.putNextEntry(new ZipEntry("template.json"));

            String templateJson = "{\"description\":\"PB description\",\"projectVersion\":1}";

            zipOutputStream.write(templateJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("project.json"));

            String projectJson = "{\"name\":\"PB Project\",\"description\":\"PB imported project\"}";

            zipOutputStream.write(projectJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("workflow-test.json"));

            String workflowJson = "\"{\\\"tasks\\\":[]}\"";

            zipOutputStream.write(workflowJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();
            zipOutputStream.finish();

            projectZip = byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(preBuiltTemplateService.getPrebuiltTemplateData(any(String.class)))
            .thenReturn(projectZip);

        ProjectTemplateDTO projectTemplateDTO = projectFacade.getProjectTemplate("any-id", false);

        assertThat(projectTemplateDTO).isNotNull();
        assertThat(projectTemplateDTO.description()).isEqualTo("PB description");
        assertThat(projectTemplateDTO.project()
            .name()).isEqualTo("PB Project");
        assertThat(projectTemplateDTO.workflows()).hasSize(1);
        assertThat(projectTemplateDTO.components()).isNotNull();
    }

    @Test
    public void testGetProjectTemplateShared() {
        byte[] projectZip;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            zipOutputStream.putNextEntry(new ZipEntry("template.json"));

            String templateJson = "{\"description\":\"Shared description\",\"projectVersion\":1}";

            zipOutputStream.write(templateJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("project.json"));

            String projectJson = "{\"name\":\"Imported Project\",\"description\":\"Test imported project\"}";

            zipOutputStream.write(projectJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("workflow-test.json"));

            String workflowJson = "\"{\\\"tasks\\\":[]}\""; // a JSON string containing workflow definition

            zipOutputStream.write(workflowJson.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.finish();

            projectZip = byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        FileEntry templateFileEntry = new FileEntry(
            "project.zip", "zip", "application/zip", "http://localhost/shared/project.zip");

        SharedTemplate sharedTemplate = new SharedTemplate();

        sharedTemplate.setTemplate(templateFileEntry);

        when(sharedTemplateFileStorage.getInputStream(any(FileEntry.class)))
            .thenReturn(new ByteArrayInputStream(projectZip));

        String projectUuid = "11111111-2222-3333-4444-555555555557";

        when(sharedTemplateService.getSharedTemplate(UUID.fromString(projectUuid)))
            .thenReturn(sharedTemplate);

        ProjectTemplateDTO projectTemplateDTO = projectFacade.getProjectTemplate(projectUuid, true);

        assertThat(projectTemplateDTO).isNotNull();
        assertThat(projectTemplateDTO.description()).isEqualTo("Shared description");

        ProjectTemplateDTO.ProjectInfo projectInfo = projectTemplateDTO.project();

        assertThat(projectInfo).isNotNull();
        assertThat(projectInfo.name()).isEqualTo("Imported Project");

        assertThat(projectTemplateDTO.workflows()).hasSize(1);
        assertThat(projectTemplateDTO.components()).isNotNull();
    }

    @Test
    public void testGetProjectWorkflows() {
        List<Workflow> allWorkflows = workflowRepository.findAll();

        int initialWorkflowsCount = allWorkflows.size();

        createTestProjectWorkflows(3, 7, 10);

        List<Workspace> workspaces = workspaceRepository.findAll();

        for (Workspace workspace : workspaces) {
            String workspaceName = workspace.getName();

            if (!workspaceName.startsWith("test_workspace_")) {
                continue;
            }

            List<ProjectDTO> workspaceProjects = projectFacade.getWorkspaceProjects(null, null, false, null,
                null, null, workspace.getId());

            assertThat(workspaceProjects.size()).isEqualTo(7);

            for (ProjectDTO projectDTO : workspaceProjects) {
                List<ProjectWorkflowDTO> projectWorkflows = projectWorkflowFacade.getProjectWorkflows(projectDTO.id());

                assertThat(projectWorkflows.size()).isEqualTo(10);
            }

            List<ProjectWorkflowDTO> workspaceProjectWorkflows =
                projectFacade.getWorkspaceProjectWorkflows(workspace.getId());

            assertThat(workspaceProjectWorkflows.size()).isEqualTo(70);

            ProjectDTO firstWorkspaceProject = workspaceProjects.getFirst();

            projectFacade.deleteProject(firstWorkspaceProject.id());

            workspaceProjectWorkflows = projectFacade.getWorkspaceProjectWorkflows(workspace.getId());

            assertThat(workspaceProjectWorkflows.size()).isEqualTo(60);

            for (ProjectDTO workspaceProject : workspaceProjects) {
                projectFacade.deleteProject(workspaceProject.id());
            }
        }

        allWorkflows = workflowRepository.findAll();

        assertThat(allWorkflows.size()).isEqualTo(initialWorkflowsCount);

    }

    private void createTestProjectWorkflows(int workspaceCount, int projectCount, int workflowCount) {
        for (int i = 0; i < workspaceCount; i++) {
            Workspace testWorkspace = workspaceRepository.save(new Workspace("test_workspace_" + i));
            for (int j = 0; j < projectCount; j++) {
                Project project = new Project();

                project.setName("test_project_" + j);

                project.setWorkspaceId(testWorkspace.getId());

                project = projectRepository.save(project);

                for (int k = 0; k < workflowCount; k++) {
                    String workflowLabel = "test_workflow_label_" + k;
                    Workflow workflow =
                        new Workflow("{\"label\":\"" + workflowLabel + "\",\"tasks\":[]}", Workflow.Format.JSON);

                    workflow.setNew(true);

                    workflow = workflowRepository.save(workflow);

                    ProjectWorkflow projectWorkflow = new ProjectWorkflow(
                        project.getId(), project.getLastProjectVersion(), workflow.getId(), UUID.randomUUID());

                    projectWorkflowRepository.save(projectWorkflow);
                }
            }
        }
    }

    @Test
    public void testGetSharedProject() {
        byte[] projectZip;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            zipOutputStream.putNextEntry(new ZipEntry("template.json"));
            String templateJson = "{\"description\":\"Shared description\",\"projectVersion\":1}";
            zipOutputStream.write(templateJson.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("project.json"));
            String projectJson = "{\"name\":\"Imported Project\",\"description\":\"Test imported project\"}";
            zipOutputStream.write(projectJson.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("workflow-test.json"));
            String wfJson = "\"{\\\"tasks\\\":[]}\"";
            zipOutputStream.write(wfJson.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
            zipOutputStream.finish();

            projectZip = byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        FileEntry templateFileEntry = new FileEntry(
            "project.zip", "zip", "application/zip", "http://localhost/shared/project.zip");

        SharedTemplate sharedTemplate = new SharedTemplate();
        sharedTemplate.setTemplate(templateFileEntry);

        when(sharedTemplateFileStorage.getInputStream(any(FileEntry.class)))
            .thenReturn(new ByteArrayInputStream(projectZip));

        String projectUuid = "11111111-2222-3333-4444-555555555555";

        when(sharedTemplateService.fetchSharedTemplate(UUID.fromString(projectUuid)))
            .thenReturn(Optional.of(sharedTemplate));

        com.bytechef.automation.configuration.dto.SharedProjectDTO sharedProjectDTO =
            projectFacade.getSharedProject(projectUuid);

        assertThat(sharedProjectDTO).isNotNull();
        assertThat(sharedProjectDTO.exported()).isTrue();
    }

    @Test
    public void testGetSharedProjectWithoutTemplate() {
        String projectUuid = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

        SharedTemplate sharedTemplate = new SharedTemplate();
        sharedTemplate.setTemplate(null);

        when(sharedTemplateService.fetchSharedTemplate(UUID.fromString(projectUuid)))
            .thenReturn(Optional.of(sharedTemplate));

        com.bytechef.automation.configuration.dto.SharedProjectDTO sharedProjectDTO =
            projectFacade.getSharedProject(projectUuid);

        assertThat(sharedProjectDTO).isNotNull();
        assertThat(sharedProjectDTO.exported()).isFalse();
    }

    @Test
    public void testGetSharedProjectNotFound() {
        String projectUuid = "dc8040d8-52dc-41ed-8507-3b2cf652d3cd";

        when(sharedTemplateService.fetchSharedTemplate(UUID.fromString(projectUuid)))
            .thenReturn(Optional.empty());

        com.bytechef.automation.configuration.dto.SharedProjectDTO sharedProjectDTO =
            projectFacade.getSharedProject(projectUuid);

        assertThat(sharedProjectDTO).isNull();
    }

    @Test
    public void testImportProject() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);

        ZipEntry sharedEntry = new ZipEntry("shared.json");

        zipOutputStream.putNextEntry(sharedEntry);

        String sharedJson = "{\"name\":\"Imported Project\",\"description\":\"Test imported project\"}";

        zipOutputStream.write(sharedJson.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();

        ZipEntry projectEntry = new ZipEntry("project.json");

        zipOutputStream.putNextEntry(projectEntry);

        String projectJson = "{\"name\":\"Imported Project\",\"description\":\"Test imported project\"}";

        zipOutputStream.write(projectJson.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();

        ZipEntry workflowEntry = new ZipEntry("workflow-test.json");

        zipOutputStream.putNextEntry(workflowEntry);

        String workflowJson = "\"{\\\"tasks\\\":[]}\"";

        zipOutputStream.write(workflowJson.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();

        zipOutputStream.finish();

        byte[] zipData = byteArrayOutputStream.toByteArray();

        long importedProjectId = projectFacade.importProject(zipData, workspace.getId());

        assertThat(importedProjectId).isGreaterThan(0);

        ProjectDTO importedProject = projectFacade.getProject(importedProjectId);

        assertThat(importedProject.name()).isEqualTo("Imported Project");
        assertThat(importedProject.description()).isEqualTo("Test imported project");

        List<ProjectWorkflowDTO> workflows = projectWorkflowFacade.getProjectWorkflows(importedProjectId);

        assertThat(workflows).hasSize(1);
    }

    @Test
    public void testImportProjectTemplatePreBuilt() {
        List<ProjectDTO> initialProjects = projectFacade.getProjects(null, null, null, null);
        int initialCount = initialProjects.size();

        byte[] projectZip;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            zipOutputStream.putNextEntry(new ZipEntry("template.json"));
            String templateJson = "{\"description\":\"PB description\",\"projectVersion\":1}";

            zipOutputStream.write(templateJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("project.json"));

            String projectJson = "{\"name\":\"PB Project\",\"description\":\"PB imported project\"}";

            zipOutputStream.write(projectJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("workflow-test.json"));

            String workflowJson = "\"{\\\"tasks\\\":[]}\"";

            zipOutputStream.write(workflowJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();
            zipOutputStream.finish();

            projectZip = byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(preBuiltTemplateService.getPrebuiltTemplateData(any(String.class)))
            .thenReturn(projectZip);

        long importedProjectId = projectFacade.importProjectTemplate("any-id", workspace.getId(), false);

        assertThat(importedProjectId).isGreaterThan(0);

        List<ProjectDTO> updatedProjects = projectFacade.getProjects(null, null, null, null);
        assertThat(updatedProjects).hasSize(initialCount + 1);
    }

    @Test
    public void testImportProjectTemplateShared() {
        List<ProjectDTO> initialProjects = projectFacade.getProjects(null, null, null, null);
        int initialCount = initialProjects.size();

        byte[] projectZip;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            zipOutputStream.putNextEntry(new ZipEntry("template.json"));

            String templateJson = "{\"description\":\"Shared description\",\"projectVersion\":1}";

            zipOutputStream.write(templateJson.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("project.json"));

            String projectJson = "{\"name\":\"Imported Project\",\"description\":\"Test imported project\"}";

            zipOutputStream.write(projectJson.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("workflow-test.json"));

            String wfJson = "\"{\\\"tasks\\\":[]}\"";

            zipOutputStream.write(wfJson.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
            zipOutputStream.finish();

            projectZip = byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        FileEntry templateFileEntry = new FileEntry(
            "project.zip", "zip", "application/zip", "http://localhost/shared/project.zip");

        SharedTemplate sharedTemplate = new SharedTemplate();
        sharedTemplate.setTemplate(templateFileEntry);

        when(sharedTemplateFileStorage.getInputStream(any(FileEntry.class)))
            .thenReturn(new ByteArrayInputStream(projectZip));

        String projectUuid = "dc8040d8-52dc-41ed-8507-3b2cf652d3cc";

        when(sharedTemplateService.getSharedTemplate(UUID.fromString(projectUuid)))
            .thenReturn(sharedTemplate);

        long importedProjectId = projectFacade.importProjectTemplate(projectUuid, workspace.getId(), true);

        assertThat(importedProjectId).isGreaterThan(0);

        List<ProjectDTO> updatedProjects = projectFacade.getProjects(null, null, null, null);

        assertThat(updatedProjects).hasSize(initialCount + 1);
    }

    @Test
    public void testImportProjectTemplateWithoutTemplateJson() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            ZipEntry zipEntry = new ZipEntry("workflow-test.json");

            zipOutputStream.putNextEntry(zipEntry);
            zipOutputStream.write("\"{\\\"tasks\\\":[]}\"".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
            zipOutputStream.finish();
        } catch (Exception e) {
            Assertions.fail("Failed to create test ZIP", e);
        }

        byte[] zipData = byteArrayOutputStream.toByteArray();

        // Prepare mocks to return the crafted ZIP for a valid UUID
        FileEntry templateFileEntry = new FileEntry(
            "project_missing_files.zip", "zip", "application/zip", "http://localhost/shared/project_missing_files.zip");

        SharedTemplate sharedTemplate = new SharedTemplate();
        sharedTemplate.setTemplate(templateFileEntry);

        when(sharedTemplateFileStorage.getInputStream(any(FileEntry.class)))
            .thenReturn(new ByteArrayInputStream(zipData));

        String projectUuid = "11111111-2222-3333-4444-555555555556";

        when(sharedTemplateService.getSharedTemplate(UUID.fromString(projectUuid)))
            .thenReturn(sharedTemplate);

        RuntimeException exception = Assertions.assertThrows(
            RuntimeException.class, () -> projectFacade.importProjectTemplate(projectUuid, workspace.getId(), true));

        assertThat(exception.getMessage()).contains("Missing files in a shared project file");
    }

    @Test
    public void testUpdate() {
        ProjectDTO projectDTO = projectFacadeInstanceHelper.createProject(workspace.getId());

        projectFacadeInstanceHelper.addTestWorkflow(projectDTO);

        assertThat(projectDTO.tags()).hasSize(3);

        assertThat(projectDTO.projectWorkflowIds()).hasSize(0);

        projectDTO = ProjectDTO.builder()
            .id(projectDTO.id())
            .name("Updated Name")
            .tags(List.of(new Tag("TAG_UPDATE")))
            .projectWorkflowIds(projectDTO.projectWorkflowIds())
            .version(projectDTO.version())
            .workspaceId(workspace.getId())
            .build();

        projectFacade.updateProject(projectDTO);

        projectDTO = projectFacade.getProject(projectDTO.id());

        assertThat(projectDTO.tags()).hasSize(1);
        assertThat(projectDTO.name()).isEqualTo("Updated Name");
    }

    /**
     * Calls every guarded {@link ProjectFacadeImpl} method through the real method-security interceptor, backed by the
     * production {@code AutomationMethodSecurityExpressionHandler} and {@code AutomationPermissionEvaluator}. Each
     * denial withholds only the scope the guard needs while granting every other one, and each allowed case grants only
     * that scope and proves the body ran.
     * <p>
     * Duplicating is a create: the body calls {@code projectService.create} and
     * {@code projectWorkflowService.addWorkflow}, and {@code ProjectServiceImpl.create} carries no gate of its own, so
     * the guard on {@code duplicateProject} is the only barrier. It named {@code WORKFLOW_VIEW} alone — a read scope —
     * which let a view-only member create a project and its workflows by duplicating any project they could open,
     * bypassing the {@code PROJECT_CREATE} that {@code createProject}, {@code importProject} and
     * {@code importProjectTemplate} all demand.
     * <p>
     * The guard is a conjunction, so every combination is asserted rather than only the denial. Holding the read alone
     * must not create; holding the create alone must not reach a project the caller cannot read; holding both must
     * pass.
     * <p>
     * Only the outcome and the scopes actually consulted are asserted, never the order the two halves are evaluated in:
     * SpEL {@code and} short-circuits, so pinning which half runs first would redden a harmless reordering of an
     * expression that admits exactly the same principals either way.
     */
    @Nested
    @Import({
        MethodSecurityEnforcement.Config.class, PostgreSQLContainerConfiguration.class
    })
    class MethodSecurityEnforcement {

        private static final String BODY_REACHED = "body reached";
        private static final Set<String> GUARDED_METHOD_NAMES = Set.of(
            "createProject", "deleteProject", "deleteSharedProject", "duplicateProject", "exportProject",
            "exportSharedProject", "getProject", "getProjects", "getWorkspaceProjectWorkflows", "getWorkspaceProjects",
            "importProject", "importProjectTemplate", "publishProject", "updateProject");
        private static final String PROJECT_CREATE = "PROJECT_CREATE";
        private static final String PROJECT_DELETE = "PROJECT_DELETE";
        private static final long PROJECT_ID = 42L;
        private static final String PROJECT_SETTINGS = "PROJECT_SETTINGS";
        private static final String PROJECT_TYPE = "Project";
        private static final String WORKFLOW_EDIT = "WORKFLOW_EDIT";
        private static final String WORKFLOW_VIEW = "WORKFLOW_VIEW";
        private static final long WORKSPACE_ID = 7L;
        private static final String WORKSPACE_TYPE = "Workspace";

        @MockitoBean
        private PermissionService permissionService;

        @MockitoBean
        private ProjectDeploymentService projectDeploymentService;

        @MockitoBean
        private ProjectService projectService;

        @BeforeEach
        void authenticateAsNonAdmin() {
            SecurityContextHolder.getContext()
                .setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                        "member", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

            when(projectDeploymentService.getProjectDeployments(anyLong()))
                .thenThrow(new IllegalStateException(BODY_REACHED));
            when(projectService.create(any(Project.class))).thenThrow(new IllegalStateException(BODY_REACHED));
            when(projectService.getProject(anyLong())).thenThrow(new IllegalStateException(BODY_REACHED));
            when(projectService.getProjects(any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException(BODY_REACHED));
            when(projectService.getWorkspaceProjectIds(anyLong())).thenThrow(new IllegalStateException(BODY_REACHED));
            when(projectService.update(any(Project.class))).thenThrow(new IllegalStateException(BODY_REACHED));
            when(preBuiltTemplateService.getPrebuiltTemplateData(anyString()))
                .thenThrow(new IllegalStateException(BODY_REACHED));
        }

        @AfterEach
        void clearSecurityContext() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void testEveryGuardedMethodHasACase() {
            Set<String> guardedMethodNames = Arrays.stream(ProjectFacadeImpl.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
                .map(Method::getName)
                .collect(Collectors.toSet());

            assertThat(guardedMethodNames).isEqualTo(GUARDED_METHOD_NAMES);

            for (Method method : ProjectFacade.class.getMethods()) {
                if (GUARDED_METHOD_NAMES.contains(method.getName())) {
                    assertThat(getImplementation(method).isAnnotationPresent(PreAuthorize.class))
                        .as("every public overload of %s must be guarded", method.getName())
                        .isTrue();
                }
            }
        }

        @Test
        void testCreateProjectDeniesWithoutTheWorkspaceCreateScope() {
            denyOnly(WORKSPACE_ID, WORKSPACE_TYPE, PROJECT_CREATE);

            assertDenied(() -> projectFacade.createProject(newProjectDTO(null)));
        }

        @Test
        void testCreateProjectAllowsWithTheWorkspaceCreateScope() {
            grant(WORKSPACE_ID, WORKSPACE_TYPE, PROJECT_CREATE);

            assertBodyReached(() -> projectFacade.createProject(newProjectDTO(null)));
        }

        @Test
        void testDeleteProjectDeniesWithoutTheDeleteScope() {
            denyOnly(PROJECT_ID, PROJECT_TYPE, PROJECT_DELETE);

            assertDenied(() -> projectFacade.deleteProject(PROJECT_ID));
        }

        @Test
        void testDeleteProjectAllowsWithTheDeleteScope() {
            grant(PROJECT_ID, PROJECT_TYPE, PROJECT_DELETE);

            assertBodyReached(() -> projectFacade.deleteProject(PROJECT_ID));
        }

        @Test
        void testDeleteSharedProjectDeniesWithoutTheSettingsScope() {
            denyOnly(PROJECT_ID, PROJECT_TYPE, PROJECT_SETTINGS);

            assertDenied(() -> projectFacade.deleteSharedProject(PROJECT_ID));
        }

        @Test
        void testDeleteSharedProjectAllowsWithTheSettingsScope() {
            grant(PROJECT_ID, PROJECT_TYPE, PROJECT_SETTINGS);

            assertBodyReached(() -> projectFacade.deleteSharedProject(PROJECT_ID));
        }

        @Test
        void testDuplicateProjectDeniesWhenTheCallerHoldsOnlyTheViewScope() {
            when(permissionService.hasResourceScope(PROJECT_ID, PROJECT_TYPE, WORKFLOW_VIEW)).thenReturn(true);
            when(permissionService.hasResourceScope(PROJECT_ID, PROJECT_TYPE, PROJECT_CREATE)).thenReturn(false);

            assertThatThrownBy(() -> projectFacade.duplicateProject(PROJECT_ID))
                .as("a member holding WORKFLOW_VIEW but not PROJECT_CREATE must not create a project by duplicating " +
                    "one")
                .isInstanceOf(AccessDeniedException.class);

            verify(permissionService).hasResourceScope(PROJECT_ID, PROJECT_TYPE, PROJECT_CREATE);
        }

        @Test
        void testDuplicateProjectAllowsWhenTheCallerHoldsBothScopes() {
            when(permissionService.hasResourceScope(PROJECT_ID, PROJECT_TYPE, WORKFLOW_VIEW)).thenReturn(true);
            when(permissionService.hasResourceScope(PROJECT_ID, PROJECT_TYPE, PROJECT_CREATE)).thenReturn(true);

            assertThatThrownBy(() -> projectFacade.duplicateProject(PROJECT_ID))
                .as("a member holding both WORKFLOW_VIEW and PROJECT_CREATE must be allowed to duplicate a project")
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);

            verify(permissionService).hasResourceScope(PROJECT_ID, PROJECT_TYPE, WORKFLOW_VIEW);
            verify(permissionService).hasResourceScope(PROJECT_ID, PROJECT_TYPE, PROJECT_CREATE);
        }

        @Test
        void testDuplicateProjectDeniesWhenTheCallerCannotReadTheSourceProject() {
            when(permissionService.hasResourceScope(PROJECT_ID, PROJECT_TYPE, WORKFLOW_VIEW)).thenReturn(false);
            when(permissionService.hasResourceScope(PROJECT_ID, PROJECT_TYPE, PROJECT_CREATE)).thenReturn(true);

            assertThatThrownBy(() -> projectFacade.duplicateProject(PROJECT_ID))
                .as("PROJECT_CREATE must not admit a caller who cannot read the project being copied")
                .isInstanceOf(AccessDeniedException.class);

            verify(permissionService).hasResourceScope(PROJECT_ID, PROJECT_TYPE, WORKFLOW_VIEW);
        }

        @Test
        void testDuplicateProjectDeniesWhenTheCallerHoldsNeitherScope() {
            assertThatThrownBy(() -> projectFacade.duplicateProject(PROJECT_ID))
                .as("a caller holding neither scope must be denied")
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testExportProjectDeniesWithoutTheViewScope() {
            denyOnly(PROJECT_ID, PROJECT_TYPE, WORKFLOW_VIEW);

            assertDenied(() -> projectFacade.exportProject(PROJECT_ID));
        }

        @Test
        void testExportProjectAllowsWithTheViewScope() {
            grant(PROJECT_ID, PROJECT_TYPE, WORKFLOW_VIEW);

            assertBodyReached(() -> projectFacade.exportProject(PROJECT_ID));
        }

        @Test
        void testExportSharedProjectDeniesWithoutTheSettingsScope() {
            denyOnly(PROJECT_ID, PROJECT_TYPE, PROJECT_SETTINGS);

            assertDenied(() -> projectFacade.exportSharedProject(PROJECT_ID, "description"));
        }

        @Test
        void testExportSharedProjectAllowsWithTheSettingsScope() {
            grant(PROJECT_ID, PROJECT_TYPE, PROJECT_SETTINGS);

            assertBodyReached(() -> projectFacade.exportSharedProject(PROJECT_ID, "description"));
        }

        @Test
        void testGetProjectDeniesWithoutTheViewScope() {
            denyOnly(PROJECT_ID, PROJECT_TYPE, WORKFLOW_VIEW);

            assertDenied(() -> projectFacade.getProject(PROJECT_ID));
        }

        @Test
        void testGetProjectAllowsWithTheViewScope() {
            grant(PROJECT_ID, PROJECT_TYPE, WORKFLOW_VIEW);

            assertBodyReached(() -> projectFacade.getProject(PROJECT_ID));
        }

        @Test
        void testGetProjectsDeniesANonAdminHoldingEveryResourceScope() {
            when(permissionService.hasResourceScope(any(), anyString(), anyString())).thenReturn(true);

            assertDenied(() -> projectFacade.getProjects(null, null, null, null));

            verify(permissionService).isTenantAdmin();
        }

        @Test
        void testGetProjectsAllowsATenantAdmin() {
            when(permissionService.isTenantAdmin()).thenReturn(true);

            assertBodyReached(() -> projectFacade.getProjects(null, null, null, null));
        }

        @Test
        void testGetWorkspaceProjectsDeniesWithoutTheWorkspaceViewScope() {
            denyOnly(WORKSPACE_ID, WORKSPACE_TYPE, WORKFLOW_VIEW);

            assertDenied(() -> projectFacade.getWorkspaceProjects(null, null, true, null, null, null, WORKSPACE_ID));
        }

        @Test
        void testGetWorkspaceProjectsAllowsWithTheWorkspaceViewScope() {
            grant(WORKSPACE_ID, WORKSPACE_TYPE, WORKFLOW_VIEW);

            assertBodyReached(
                () -> projectFacade.getWorkspaceProjects(null, null, true, null, null, null, WORKSPACE_ID));
        }

        @Test
        void testGetWorkspaceProjectWorkflowsDeniesWithoutTheWorkspaceViewScope() {
            denyOnly(WORKSPACE_ID, WORKSPACE_TYPE, WORKFLOW_VIEW);

            assertDenied(() -> projectFacade.getWorkspaceProjectWorkflows(WORKSPACE_ID));
        }

        @Test
        void testGetWorkspaceProjectWorkflowsAllowsWithTheWorkspaceViewScope() {
            grant(WORKSPACE_ID, WORKSPACE_TYPE, WORKFLOW_VIEW);

            assertBodyReached(() -> projectFacade.getWorkspaceProjectWorkflows(WORKSPACE_ID));
        }

        @Test
        void testImportProjectDeniesWithoutTheWorkspaceCreateScope() throws Exception {
            byte[] projectData = createProjectData();

            denyOnly(WORKSPACE_ID, WORKSPACE_TYPE, PROJECT_CREATE);

            assertDenied(() -> projectFacade.importProject(projectData, WORKSPACE_ID));
        }

        @Test
        void testImportProjectAllowsWithTheWorkspaceCreateScope() throws Exception {
            byte[] projectData = createProjectData();

            grant(WORKSPACE_ID, WORKSPACE_TYPE, PROJECT_CREATE);

            assertBodyReached(() -> projectFacade.importProject(projectData, WORKSPACE_ID));
        }

        @Test
        void testImportProjectTemplateDeniesWithoutTheWorkspaceCreateScope() {
            denyOnly(WORKSPACE_ID, WORKSPACE_TYPE, PROJECT_CREATE);

            assertDenied(() -> projectFacade.importProjectTemplate("template", WORKSPACE_ID, false));
        }

        @Test
        void testImportProjectTemplateAllowsWithTheWorkspaceCreateScope() {
            grant(WORKSPACE_ID, WORKSPACE_TYPE, PROJECT_CREATE);

            assertBodyReached(() -> projectFacade.importProjectTemplate("template", WORKSPACE_ID, false));
        }

        @Test
        void testPublishProjectDeniesWithoutTheEditScope() {
            denyOnly(PROJECT_ID, PROJECT_TYPE, WORKFLOW_EDIT);

            assertDenied(() -> projectFacade.publishProject(PROJECT_ID, "description", false));
        }

        @Test
        void testPublishProjectAllowsWithTheEditScope() {
            grant(PROJECT_ID, PROJECT_TYPE, WORKFLOW_EDIT);

            assertBodyReached(() -> projectFacade.publishProject(PROJECT_ID, "description", false));
        }

        @Test
        void testUpdateProjectDeniesWithoutTheEditScope() {
            denyOnly(PROJECT_ID, PROJECT_TYPE, WORKFLOW_EDIT);

            assertDenied(() -> projectFacade.updateProject(newProjectDTO(PROJECT_ID)));
        }

        @Test
        void testUpdateProjectAllowsWithTheEditScope() {
            grant(PROJECT_ID, PROJECT_TYPE, WORKFLOW_EDIT);

            assertBodyReached(() -> projectFacade.updateProject(newProjectDTO(PROJECT_ID)));
        }

        private void assertBodyReached(ThrowingCallable call) {
            assertThatThrownBy(call)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
        }

        private void assertDenied(ThrowingCallable call) {
            assertThatThrownBy(call).isInstanceOf(AccessDeniedException.class);
        }

        private byte[] createProjectData() throws Exception {
            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
                    zipOutputStream.putNextEntry(new ZipEntry("project.json"));
                    zipOutputStream.write("{\"name\":\"imported\"}".getBytes(StandardCharsets.UTF_8));
                    zipOutputStream.closeEntry();

                    zipOutputStream.putNextEntry(new ZipEntry("workflow-1.json"));
                    zipOutputStream.write("\"{}\"".getBytes(StandardCharsets.UTF_8));
                    zipOutputStream.closeEntry();
                }

                return byteArrayOutputStream.toByteArray();
            }
        }

        private void denyOnly(long targetId, String targetType, String scope) {
            when(permissionService.isTenantAdmin()).thenReturn(false);
            when(permissionService.hasResourceScope(any(), anyString(), anyString())).thenReturn(true);
            when(permissionService.hasResourceScope(targetId, targetType, scope)).thenReturn(false);
        }

        private Method getImplementation(Method method) {
            try {
                return ProjectFacadeImpl.class.getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException noSuchMethodException) {
                throw new IllegalStateException(noSuchMethodException);
            }
        }

        private void grant(long targetId, String targetType, String scope) {
            when(permissionService.hasResourceScope(targetId, targetType, scope)).thenReturn(true);
        }

        private ProjectDTO newProjectDTO(Long id) {
            return ProjectDTO.builder()
                .id(id)
                .name("project")
                .workspaceId(WORKSPACE_ID)
                .build();
        }

        @EnableMethodSecurity
        static class Config {
        }
    }
}
