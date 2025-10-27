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
import static org.mockito.ArgumentMatchers.any;
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
import com.bytechef.automation.configuration.service.ProjectWorkflowServiceImpl;
import com.bytechef.automation.configuration.service.SharedTemplateService;
import com.bytechef.automation.configuration.util.ProjectDeploymentFacadeHelper;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.bytechef.platform.githubproxy.client.FileItem;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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
            projectTagFacade.getProjectTags()
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
            projectTagFacade.getProjectTags()
                .stream()
                .map(Tag::getName)
                .collect(Collectors.toSet())).contains("tag1", "tag2", "tag3");

        projectRepository.deleteById(Validate.notNull(project.getId(), "id"));

        assertThat(
            projectTagFacade.getProjectTags()
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
        Workflow workflow = new Workflow("{\"tasks\":[]}", Workflow.Format.JSON);

        workflow.setNew(true);

        workflow = workflowRepository.save(workflow);

        Project project = new Project();

        project.setName("name");
        project.setWorkspaceId(workspace.getId());

        project = projectRepository.save(project);

        projectWorkflowRepository.save(
            new ProjectWorkflow(
                project.getId(), project.getLastProjectVersion(), Validate.notNull(workflow.getId(), "id"),
                UUID.randomUUID()));

        List<ProjectWorkflowDTO> workflows = projectWorkflowFacade.getProjectWorkflows(
            Validate.notNull(project.getId(), "id"));

        List<String> ids = workflows.stream()
            .map(ProjectWorkflowDTO::getId)
            .toList();

        assertThat(ids).contains(workflow.getId());
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
}
