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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.SharedTemplate;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.dto.SharedWorkflowDTO;
import com.bytechef.automation.configuration.dto.WorkflowTemplateDTO;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.service.SharedTemplateService;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.bytechef.platform.githubproxy.client.FileItem;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ProjectIntTestConfiguration.class,
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
public class ProjectWorkflowFacadeIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private TagRepository tagRepository;

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

    @AfterEach
    public void afterEach() {
        projectWorkflowRepository.deleteAll();
        projectRepository.deleteAll();

        // Clean up workflow records by finding all and deleting individually
        workflowRepository.findAll()
            .forEach(workflow -> workflowRepository.deleteById(workflow.getId()));

        workspaceRepository.deleteAll();

        categoryRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @BeforeEach
    public void beforeEach() {
        workspace = workspaceRepository.save(new Workspace("test"));
    }

    @Test
    public void testExportSharedWorkflow() {
        Project project = new Project();

        project.setName("Test Export Project");
        project.setWorkspaceId(workspace.getId());
        project = projectRepository.save(project);

        String workflowDefinition =
            "{\"label\":\"Test Export Workflow\",\"description\":\"Test workflow for export\",\"tasks\":[]}";
        ProjectWorkflow projectWorkflow = projectWorkflowFacade.addWorkflow(project.getId(), workflowDefinition);
        String workflowId = projectWorkflow.getWorkflowId();

        FileEntry mockFileEntry = new FileEntry(
            "workflow_" + projectWorkflow.getUuid() + ".zip", "zip", "application/zip",
            "http://localhost/shared/workflow_" + projectWorkflow.getUuid() + ".zip");

        when(sharedTemplateFileStorage.storeFileContent(anyString(), any()))
            .thenReturn(mockFileEntry);

        String description = "Test template description";

        projectWorkflowFacade.exportSharedWorkflow(workflowId, description);

        verify(sharedTemplateFileStorage, times(1)).storeFileContent(anyString(), any());
    }

    @Test
    public void testGetSharedWorkflow() {
        byte[] workflowZip;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            zipOutputStream.putNextEntry(new ZipEntry("template.json"));

            String metaJson = "{\"projectVersion\":1,\"description\":\"Shared workflow description\"}";

            zipOutputStream.write(metaJson.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("workflow-1.json"));
            String workflowJson =
                "{\"label\":\"Test Shared Workflow\",\"description\":\"WF desc\",\"tasks\":[]}";
            zipOutputStream.write(workflowJson.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.finish();
            workflowZip = byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        FileEntry templateFileEntry = new FileEntry(
            "workflow.zip", "zip", "application/zip", "http://localhost/shared/workflow.zip");

        SharedTemplate sharedTemplate = new SharedTemplate();
        sharedTemplate.setTemplate(templateFileEntry);

        when(sharedTemplateFileStorage.getInputStream(any(FileEntry.class)))
            .thenReturn(new ByteArrayInputStream(workflowZip));

        String workflowUuid = UUID.randomUUID()
            .toString();

        when(sharedTemplateService.fetchSharedTemplate(UUID.fromString(workflowUuid)))
            .thenReturn(Optional.of(sharedTemplate));

        SharedWorkflowDTO sharedWorkflowDTO = projectWorkflowFacade.getSharedWorkflow(workflowUuid);

        assertThat(sharedWorkflowDTO).isNotNull();
        assertThat(sharedWorkflowDTO.exported()).isTrue();
    }

    @Test
    public void testGetSharedWorkflowWithoutTemplate() {
        UUID uuid = UUID.randomUUID();

        String workflowUuid = uuid.toString();

        SharedTemplate sharedTemplate = new SharedTemplate();
        sharedTemplate.setTemplate(null);

        when(sharedTemplateService.fetchSharedTemplate(UUID.fromString(workflowUuid)))
            .thenReturn(Optional.of(sharedTemplate));

        SharedWorkflowDTO sharedWorkflowDTO = projectWorkflowFacade.getSharedWorkflow(workflowUuid);

        assertThat(sharedWorkflowDTO).isNotNull();
        assertThat(sharedWorkflowDTO.exported()).isFalse();
    }

    @Test
    public void testGetSharedWorkflowNotFound() {
        UUID uuid = UUID.randomUUID();

        String workflowUuid = uuid.toString();

        when(sharedTemplateService.fetchSharedTemplate(UUID.fromString(workflowUuid)))
            .thenReturn(Optional.empty());

        SharedWorkflowDTO sharedWorkflowDTO = projectWorkflowFacade.getSharedWorkflow(
            workflowUuid);

        assertThat(sharedWorkflowDTO).isNull();
    }

    @Test
    public void testGetWorkflowTemplatePreBuilt() {
        byte[] workflowZip;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            zipOutputStream.putNextEntry(new ZipEntry("workflow-1.json"));

            String workflowJson =
                "{\"label\":\"WF Label PB\",\"description\":\"WF Desc PB\",\"tasks\":[]}";

            zipOutputStream.write(workflowJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("template.json"));

            String templateJson = "{\"description\":\"WF Template PB\",\"projectVersion\":3}";

            zipOutputStream.write(templateJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();
            zipOutputStream.finish();

            workflowZip = byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(preBuiltTemplateService.getPrebuiltTemplateData(anyString()))
            .thenReturn(workflowZip);

        WorkflowTemplateDTO importTemplate = projectWorkflowFacade.getWorkflowTemplate("any-id", false);

        assertThat(importTemplate).isNotNull();
        assertThat(importTemplate.description()).isEqualTo("WF Template PB");
        assertThat(importTemplate.projectVersion()).isEqualTo(3);

        WorkflowTemplateDTO.WorkflowInfo workflow = importTemplate.workflow();

        assertThat(workflow).isNotNull();
        assertThat(workflow.label()).isEqualTo("WF Label PB");
    }

    @Test
    public void testGetWorkflowTemplateShared() {
        byte[] workflowZip;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            zipOutputStream.putNextEntry(new ZipEntry("workflow-1.json"));

            String workflowJson =
                "{\"label\":\"WF Label\",\"description\":\"WF Desc\",\"tasks\":[]}";

            zipOutputStream.write(workflowJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("template.json"));

            String templateJson = "{\"description\":\"WF Template\",\"projectVersion\":2}";

            zipOutputStream.write(templateJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();
            zipOutputStream.finish();

            workflowZip = byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        FileEntry templateFileEntry = new FileEntry(
            "workflow.zip", "zip", "application/zip", "http://localhost/shared/workflow.zip");
        SharedTemplate sharedTemplate = new SharedTemplate();

        sharedTemplate.setTemplate(templateFileEntry);

        when(sharedTemplateFileStorage.getInputStream(any(FileEntry.class)))
            .thenReturn(new ByteArrayInputStream(workflowZip));

        UUID uuid = UUID.randomUUID();

        when(sharedTemplateService.getSharedTemplate(uuid))
            .thenReturn(sharedTemplate);

        WorkflowTemplateDTO importTemplate = projectWorkflowFacade.getWorkflowTemplate(uuid.toString(), true);

        assertThat(importTemplate).isNotNull();
        assertThat(importTemplate.description()).isEqualTo("WF Template");
        assertThat(importTemplate.projectVersion()).isEqualTo(2);

        WorkflowTemplateDTO.WorkflowInfo workflow = importTemplate.workflow();

        assertThat(workflow).isNotNull();
        assertThat(workflow.label()).isEqualTo("WF Label");
    }

    @Test
    public void testGetPreBuiltWorkflowTemplates() {
        byte[] workflowZip;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            zipOutputStream.putNextEntry(new ZipEntry("workflow-1.json"));

            String workflowJson =
                "{\"label\":\"PB Label\",\"description\":\"PB Desc\",\"tasks\":[]}";

            zipOutputStream.write(workflowJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("template.json"));

            String templateJson = "{\"description\":\"PB Template\",\"projectVersion\":5}";

            zipOutputStream.write(templateJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();

            zipOutputStream.finish();

            workflowZip = byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(preBuiltTemplateService.getFiles("workflows"))
            .thenReturn(List.of(new FileItem("workflows/pb-workflow.zip", 100L, "sha", "ref", "raw")));
        when(preBuiltTemplateService.getPrebuiltTemplateData(anyString())).thenReturn(workflowZip);

        List<WorkflowTemplateDTO> templates = projectWorkflowFacade.getPreBuiltWorkflowTemplates("", "");

        assertThat(templates).isNotNull();
        assertThat(templates).hasSize(1);
        assertThat(templates.get(0)
            .workflow()
            .label()).isEqualTo("PB Label");

        // simple query filter check
        List<WorkflowTemplateDTO> filtered = projectWorkflowFacade.getPreBuiltWorkflowTemplates("PB Label", "");
        assertThat(filtered).hasSize(1);
    }

    @Test
    public void testImportWorkflowTemplatePreBuilt() {
        Project project = new Project();

        project.setName("Test Project PB");
        project.setWorkspaceId(workspace.getId());

        project = projectRepository.save(project);

        int initialCount = projectWorkflowFacade.getProjectWorkflows(project.getId())
            .size();

        byte[] workflowZip;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            zipOutputStream.putNextEntry(new ZipEntry("workflow-1.json"));

            String workflowJson = "{\"label\":\"PB Workflow\",\"tasks\":[]}";

            zipOutputStream.write(workflowJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("template.json"));

            String metaJson = "{\"projectVersion\":1,\"description\":\"pb desc\"}";

            zipOutputStream.write(metaJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();
            zipOutputStream.finish();

            workflowZip = byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(preBuiltTemplateService.getPrebuiltTemplateData(anyString()))
            .thenReturn(workflowZip);

        projectWorkflowFacade.importWorkflowTemplate(project.getId(), "any-id", false);

        List<ProjectWorkflowDTO> updatedWorkflows = projectWorkflowFacade.getProjectWorkflows(project.getId());

        assertThat(updatedWorkflows).hasSize(initialCount + 1);
    }

    @Test
    public void testImportWorkflowTemplateShared() {
        Project project = new Project();

        project.setName("Test Project");
        project.setWorkspaceId(workspace.getId());

        project = projectRepository.save(project);

        List<ProjectWorkflowDTO> initialWorkflows = projectWorkflowFacade.getProjectWorkflows(project.getId());

        int initialCount = initialWorkflows.size();

        byte[] workflowZip;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            zipOutputStream.putNextEntry(new ZipEntry("workflow-1.json"));

            String workflowJson = "{\"label\":\"Test Workflow\",\"tasks\":[]}";

            zipOutputStream.write(workflowJson.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("template.json"));

            String metaJson = "{\"projectVersion\":1,\"description\":\"desc\"}";

            zipOutputStream.write(metaJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();
            zipOutputStream.finish();

            workflowZip = byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        FileEntry templateFileEntry = new FileEntry(
            "workflow.zip", "zip", "application/zip", "http://localhost/shared/workflow.zip");

        SharedTemplate sharedTemplate = new SharedTemplate();

        sharedTemplate.setTemplate(templateFileEntry);

        when(sharedTemplateFileStorage.getInputStream(any(FileEntry.class)))
            .thenReturn(new ByteArrayInputStream(workflowZip));

        UUID uuid = UUID.randomUUID();

        String workflowUuid = uuid.toString();

        when(sharedTemplateService.getSharedTemplate(UUID.fromString(workflowUuid)))
            .thenReturn(sharedTemplate);

        projectWorkflowFacade.importWorkflowTemplate(project.getId(), workflowUuid, true);

        List<ProjectWorkflowDTO> updatedWorkflows = projectWorkflowFacade.getProjectWorkflows(project.getId());

        assertThat(updatedWorkflows).hasSize(initialCount + 1);
    }
}
