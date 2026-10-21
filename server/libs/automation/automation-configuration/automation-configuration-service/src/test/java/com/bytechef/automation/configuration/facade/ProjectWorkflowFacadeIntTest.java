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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.SharedTemplate;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.dto.SharedWorkflowDTO;
import com.bytechef.automation.configuration.dto.WorkflowTemplateDTO;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.PreBuiltTemplateService;
import com.bytechef.automation.configuration.service.SharedTemplateService;
import com.bytechef.automation.configuration.util.ProjectDeploymentFacadeHelper;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.dto.WorkflowTaskDTO;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.bytechef.platform.githubproxy.client.model.WorkflowTemplate;
import com.bytechef.platform.githubproxy.client.model.WorkflowTemplateSummary;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

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
class ProjectWorkflowFacadeIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProjectDeploymentFacadeHelper projectDeploymentFacadeHelper;

    @Autowired
    private ProjectFacade projectFacade;

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

    @Autowired
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    @MockitoBean
    private SharedTemplateFileStorage sharedTemplateFileStorage;

    @MockitoBean
    private SharedTemplateService sharedTemplateService;

    @MockitoBean
    private PreBuiltTemplateService preBuiltTemplateService;

    private Workspace workspace;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @AfterEach
    void afterEach() {
        projectWorkflowRepository.deleteAll();
        projectRepository.deleteAll();

        // Clean up workflow records by finding all and deleting individually
        workflowRepository.findAll()
            .forEach(workflow -> {
                workflowTestConfigurationService.delete(workflow.getId());

                workflowRepository.deleteById(workflow.getId());
            });

        workspaceRepository.deleteAll();

        categoryRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @BeforeEach
    void beforeEach() {
        workspace = workspaceRepository.save(new Workspace("test"));
    }

    @Test
    void testDuplicateWorkflowCopiesWorkflowTestConfigurationInputs() {
        Project project = new Project();

        project.setName("Test Duplicate Project");
        project.setWorkspaceId(workspace.getId());
        project = projectRepository.save(project);

        ProjectWorkflow projectWorkflow = projectWorkflowFacade.addWorkflow(
            project.getId(), "{\"label\":\"Test Duplicate Workflow\",\"inputs\":[{\"name\":\"count\"}],\"tasks\":[]}");

        String workflowId = projectWorkflow.getWorkflowId();

        workflowTestConfigurationService.saveWorkflowTestConfigurationInputs(workflowId, "count", "42", 0);

        String duplicatedWorkflowId = projectWorkflowFacade.duplicateWorkflow(project.getId(), workflowId);

        assertThat(duplicatedWorkflowId).isNotEqualTo(workflowId);
        assertThat(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(duplicatedWorkflowId, 0))
            .isEqualTo(Map.of("count", "42"));
        assertThat(workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, 0))
            .isEqualTo(Map.of("count", "42"));
    }

    @Test
    void testExportSharedWorkflow() {
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
    void testGetSharedWorkflow() {
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
    void testGetSharedWorkflowWithoutTemplate() {
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
    void testGetSharedWorkflowNotFound() {
        UUID uuid = UUID.randomUUID();

        String workflowUuid = uuid.toString();

        when(sharedTemplateService.fetchSharedTemplate(UUID.fromString(workflowUuid)))
            .thenReturn(Optional.empty());

        SharedWorkflowDTO sharedWorkflowDTO = projectWorkflowFacade.getSharedWorkflow(
            workflowUuid);

        assertThat(sharedWorkflowDTO).isNull();
    }

    @Test
    void testGetWorkflowTemplatePreBuilt() {
        ObjectNode workflowDefinition = JsonMapper.builder()
            .build()
            .createObjectNode();

        workflowDefinition.put("label", "WF Label PB");
        workflowDefinition.put("description", "WF Desc PB");

        WorkflowTemplate workflowTemplate = new WorkflowTemplate(
            "pb-workflow-slug", "WF Label PB", "WF Template PB", null, "ai",
            List.of(), null, null, List.of(), List.of(), null, List.of(),
            workflowDefinition, null, null);

        when(preBuiltTemplateService.getWorkflowTemplate(anyString()))
            .thenReturn(workflowTemplate);

        WorkflowTemplateDTO importTemplate = projectWorkflowFacade.getWorkflowTemplate("pb-workflow-slug", false);

        assertThat(importTemplate).isNotNull();
        assertThat(importTemplate.id()).isEqualTo("pb-workflow-slug");
        assertThat(importTemplate.description()).isEqualTo("WF Template PB");
        assertThat(importTemplate.categories()).containsExactly("ai");

        WorkflowDTO workflow = importTemplate.workflow();

        assertThat(workflow).isNotNull();
        assertThat(workflow.getLabel()).isEqualTo("WF Label PB");
    }

    @Test
    void testGetWorkflowTemplateShared() {
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

        WorkflowDTO workflow = importTemplate.workflow();

        assertThat(workflow).isNotNull();
        assertThat(workflow.getLabel()).isEqualTo("WF Label");
    }

    @Test
    void testGetPreBuiltWorkflowTemplates() {
        WorkflowTemplateSummary workflowTemplateSummary = new WorkflowTemplateSummary(
            "pb-workflow-slug", "PB Label", "PB Template", "ai", List.of(), null, null);

        when(preBuiltTemplateService.getWorkflowTemplates())
            .thenReturn(List.of(workflowTemplateSummary));

        List<WorkflowTemplateDTO> templates = projectWorkflowFacade.getPreBuiltWorkflowTemplates("", "");

        assertThat(templates).isNotNull();
        assertThat(templates).hasSize(1);

        WorkflowTemplateDTO workflowTemplateDTO = templates.getFirst();
        assertThat(workflowTemplateDTO.id()).isEqualTo("pb-workflow-slug");

        WorkflowDTO workflow = workflowTemplateDTO.workflow();

        assertThat(workflow.getLabel()).isEqualTo("PB Label");

        assertThat(workflowTemplateDTO.categories()).containsExactly("ai");

        // simple query filter check
        List<WorkflowTemplateDTO> filtered = projectWorkflowFacade.getPreBuiltWorkflowTemplates("PB Label", "");

        assertThat(filtered).hasSize(1);
    }

    @Test
    void testImportWorkflowTemplatePreBuilt() {
        Project project = new Project();

        project.setName("Test Project PB");
        project.setWorkspaceId(workspace.getId());

        project = projectRepository.save(project);

        int initialCount = projectWorkflowFacade.getProjectWorkflows(project.getId())
            .size();

        ObjectNode workflowDefinition = JsonMapper.builder()
            .build()
            .createObjectNode();

        workflowDefinition.put("label", "PB Workflow");

        WorkflowTemplate workflowTemplate = new WorkflowTemplate(
            "pb-workflow-slug", "PB Workflow", "pb desc", null, null, List.of(), null, null, List.of(), List.of(), null,
            List.of(), workflowDefinition, null, null);

        when(preBuiltTemplateService.getWorkflowTemplate(anyString()))
            .thenReturn(workflowTemplate);

        projectWorkflowFacade.importWorkflowTemplate(project.getId(), "pb-workflow-slug", false);

        List<ProjectWorkflowDTO> updatedWorkflows = projectWorkflowFacade.getProjectWorkflows(project.getId());

        assertThat(updatedWorkflows).hasSize(initialCount + 1);
    }

    @Test
    void testImportWorkflowTemplateShared() {
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

    @Test
    void testGetWorkflowTemplateSharedFlattensTaskDispatcherChildTasks() {
        byte[] workflowZip;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            zipOutputStream.putNextEntry(new ZipEntry("workflow-1.json"));

            String workflowJson = """
                {
                    "label": "WF Label",
                    "description": "WF Desc",
                    "tasks": [
                        {
                            "name": "branch_1",
                            "type": "branch/v1",
                            "parameters": {
                                "cases": [
                                    {
                                        "key": "sales",
                                        "tasks": [
                                            {"name": "slack_1", "type": "slack/v1/sendMessage", "parameters": {}}
                                        ]
                                    }
                                ],
                                "default": [
                                    {"name": "googleMail_1", "type": "googleMail/v1/sendEmail", "parameters": {}}
                                ]
                            }
                        }
                    ]
                }
                """;

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

        WorkflowDTO workflow = importTemplate.workflow();

        assertThat(workflow.getTasks())
            .extracting(WorkflowTaskDTO::getName)
            .containsExactlyInAnyOrder("branch_1", "slack_1", "googleMail_1");
    }

    @Test
    public void testGetProjectWorkflowVersions() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectWorkflowDTO projectWorkflowDTO = projectDeploymentFacadeHelper.addTestWorkflow(projectDTO);

        String workflowUuid = projectWorkflowDTO.getWorkflowUuid();

        projectFacade.publishProject(projectDTO.id(), "first", false);

        projectDeploymentFacadeHelper.addTestWorkflow(projectDTO);

        projectFacade.publishProject(projectDTO.id(), "second", false);

        assertThat(projectWorkflowFacade.getProjectWorkflowVersions(projectDTO.id(), workflowUuid))
            .extracting(ProjectVersion::getVersion)
            .containsExactly(2, 1);
    }

    @Test
    public void testRestoreProjectVersionWorkflow() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectWorkflowDTO projectWorkflowDTO = projectDeploymentFacadeHelper.addTestWorkflow(
            projectDTO, "{\"label\": \"Version 1\", \"description\": \"Test Description\", \"tasks\": []}");
        ProjectWorkflowDTO otherProjectWorkflowDTO = projectDeploymentFacadeHelper.addTestWorkflow(projectDTO);

        String workflowUuid = projectWorkflowDTO.getWorkflowUuid();

        projectFacade.publishProject(projectDTO.id(), "first", false);

        ProjectWorkflow draftProjectWorkflow = projectWorkflowRepository
            .findByProjectIdAndProjectVersionAndUuid(projectDTO.id(), 2, UUID.fromString(workflowUuid))
            .orElseThrow();
        ProjectWorkflow otherDraftProjectWorkflow = projectWorkflowRepository
            .findByProjectIdAndProjectVersionAndUuid(
                projectDTO.id(), 2, UUID.fromString(otherProjectWorkflowDTO.getWorkflowUuid()))
            .orElseThrow();

        String draftWorkflowId = draftProjectWorkflow.getWorkflowId();

        Workflow draftWorkflow = workflowRepository.findById(draftWorkflowId)
            .orElseThrow();

        projectWorkflowFacade.updateWorkflow(
            draftWorkflowId, "{\"label\": \"Version 2\", \"description\": \"Test Description\", \"tasks\": []}",
            draftWorkflow.getVersion());

        String otherDraftDefinition = workflowRepository.findById(otherDraftProjectWorkflow.getWorkflowId())
            .orElseThrow()
            .getDefinition();

        projectWorkflowFacade.restoreProjectVersionWorkflow(projectDTO.id(), 1, workflowUuid);

        ProjectWorkflow restoredProjectWorkflow = projectWorkflowRepository
            .findByProjectIdAndProjectVersionAndUuid(projectDTO.id(), 2, UUID.fromString(workflowUuid))
            .orElseThrow();

        assertThat(restoredProjectWorkflow.getId()).isEqualTo(draftProjectWorkflow.getId());
        assertThat(restoredProjectWorkflow.getWorkflowId()).isEqualTo(draftWorkflowId);
        assertThat(workflowRepository.findById(draftWorkflowId)
            .orElseThrow()
            .getLabel()).isEqualTo("Version 1");
        assertThat(workflowRepository.findById(otherDraftProjectWorkflow.getWorkflowId())
            .orElseThrow()
            .getDefinition()).isEqualTo(otherDraftDefinition);
    }

    @Test
    public void testRestoreProjectVersionWorkflowReaddsWorkflowDeletedFromDraft() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectWorkflowDTO projectWorkflowDTO = projectDeploymentFacadeHelper.addTestWorkflow(projectDTO);

        String workflowUuid = projectWorkflowDTO.getWorkflowUuid();

        projectFacade.publishProject(projectDTO.id(), "first", false);

        ProjectWorkflow draftProjectWorkflow = projectWorkflowRepository
            .findByProjectIdAndProjectVersionAndUuid(projectDTO.id(), 2, UUID.fromString(workflowUuid))
            .orElseThrow();

        projectWorkflowFacade.deleteWorkflow(draftProjectWorkflow.getWorkflowId());

        projectWorkflowFacade.restoreProjectVersionWorkflow(projectDTO.id(), 1, workflowUuid);

        assertThat(
            projectWorkflowRepository.findByProjectIdAndProjectVersionAndUuid(
                projectDTO.id(), 2, UUID.fromString(workflowUuid)))
                    .isPresent();
    }

    @Test
    public void testRestoreProjectVersionWorkflowRejectsInvalidVersion() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectWorkflowDTO projectWorkflowDTO = projectDeploymentFacadeHelper.addTestWorkflow(projectDTO);

        String workflowUuid = projectWorkflowDTO.getWorkflowUuid();

        assertThatThrownBy(() -> projectWorkflowFacade.restoreProjectVersionWorkflow(projectDTO.id(), 1, workflowUuid))
            .isInstanceOf(ConfigurationException.class);

        projectFacade.publishProject(projectDTO.id(), "first", false);

        String unknownWorkflowUuid = UUID.randomUUID()
            .toString();

        assertThatThrownBy(
            () -> projectWorkflowFacade.restoreProjectVersionWorkflow(projectDTO.id(), 1, unknownWorkflowUuid))
                .isInstanceOf(ConfigurationException.class);
    }

    @Nested
    @ContextConfiguration(classes = Authorization.MethodSecurityConfiguration.class)
    class Authorization {

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void testDeleteWorkflowIsDeniedWithoutWorkflowScope() {
            String workflowId = addWorkflow("Delete Denied Project");

            authenticate("user", AuthorityConstants.USER);

            assertThatThrownBy(() -> projectWorkflowFacade.deleteWorkflow(workflowId))
                .isInstanceOf(AccessDeniedException.class);

            assertThat(workflowRepository.findById(workflowId)).isPresent();
        }

        @Test
        void testDeleteWorkflowAllowsTenantAdmin() {
            String workflowId = addWorkflow("Delete Allowed Project");

            projectWorkflowFacade.deleteWorkflow(workflowId);

            assertThat(workflowRepository.findById(workflowId)).isEmpty();
        }

        private String addWorkflow(String projectName) {
            authenticate("admin", AuthorityConstants.ADMIN);

            Project project = new Project();

            project.setName(projectName);
            project.setWorkspaceId(workspace.getId());

            project = projectRepository.save(project);

            ProjectWorkflow projectWorkflow = projectWorkflowFacade.addWorkflow(
                project.getId(), "{\"label\":\"" + projectName + " Workflow\",\"tasks\":[]}");

            return projectWorkflow.getWorkflowId();
        }

        private static void authenticate(String login, String authority) {
            SecurityContextHolder.getContext()
                .setAuthentication(
                    new UsernamePasswordAuthenticationToken(login, "n/a",
                        List.of(new SimpleGrantedAuthority(authority))));
        }

        @EnableMethodSecurity
        static class MethodSecurityConfiguration {
        }
    }

    @Nested
    @ContextConfiguration(classes = PreAuthorizeEnforcement.MethodSecurityConfiguration.class)
    class PreAuthorizeEnforcement {

        private static final long PROJECT_ID = 11L;
        private static final String UUID_STRING = "00000000-0000-0000-0000-000000000041";
        private static final String WORKFLOW_ID = "workflow-41";

        @MockitoBean
        private PermissionService permissionService;

        @BeforeEach
        void beforeEach() {
            reset(permissionService);

            authenticate(AuthorityConstants.USER);
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void testWorkflowWriteMethodsAreProtected() {
            assertRequiresScope(
                PROJECT_ID, "Project", "WORKFLOW_CREATE", () -> projectWorkflowFacade.addWorkflow(PROJECT_ID, "{}"));
            assertRequiresScope(
                PROJECT_ID, "Project", "WORKFLOW_CREATE",
                () -> projectWorkflowFacade.duplicateWorkflow(PROJECT_ID, WORKFLOW_ID));
            assertRequiresScope(
                WORKFLOW_ID, "Workflow", "WORKFLOW_DELETE", () -> projectWorkflowFacade.deleteWorkflow(WORKFLOW_ID));
            assertRequiresScope(
                PROJECT_ID, "Project", "WORKFLOW_EDIT",
                () -> projectWorkflowFacade.restoreProjectVersionWorkflow(PROJECT_ID, 1, UUID_STRING));
        }

        @Test
        void testWorkflowVersionReadIsProtected() {
            assertRequiresScope(
                PROJECT_ID, "Project", "WORKFLOW_VIEW",
                () -> projectWorkflowFacade.getProjectWorkflowVersions(PROJECT_ID, UUID_STRING));
        }

        private void
            assertRequiresScope(Serializable targetId, String targetType, String scope, ThrowingCallable call) {
            reset(permissionService);

            assertDenied(call);

            when(permissionService.hasResourceScope(targetId, targetType, scope)).thenReturn(true);

            assertNotDenied(call);

            reset(permissionService);
        }

        private static void assertDenied(ThrowingCallable call) {
            assertThatThrownBy(call).isInstanceOf(AccessDeniedException.class);
        }

        private static void assertNotDenied(ThrowingCallable call) {
            Throwable throwable = catchThrowable(call);

            assertThat(throwable)
                .as("Expected the gate to let the call through, but it was denied: %s", throwable)
                .satisfiesAnyOf(
                    actual -> assertThat(actual).isNull(),
                    actual -> assertThat(actual).isNotInstanceOf(AccessDeniedException.class));
        }

        private static void authenticate(String authority) {
            SecurityContextHolder.getContext()
                .setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                        "user", "n/a", List.of(new SimpleGrantedAuthority(authority))));
        }

        @EnableMethodSecurity
        static class MethodSecurityConfiguration {
        }
    }
}
