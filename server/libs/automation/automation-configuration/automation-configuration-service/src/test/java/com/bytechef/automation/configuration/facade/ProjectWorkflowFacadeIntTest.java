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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
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
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.SharedTemplateService;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.dto.WorkflowTaskDTO;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.bytechef.platform.githubproxy.client.model.WorkflowTemplate;
import com.bytechef.platform.githubproxy.client.model.WorkflowTemplateSummary;
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
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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

        WorkflowDTO workflow = importTemplate.workflow();

        assertThat(workflow).isNotNull();
        assertThat(workflow.getLabel()).isEqualTo("WF Label");
    }

    @Test
    public void testGetPreBuiltWorkflowTemplates() {
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
    public void testImportWorkflowTemplatePreBuilt() {
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

    @Test
    public void testGetWorkflowTemplateSharedFlattensTaskDispatcherChildTasks() {
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

    @Nested
    @Import({
        MethodSecurityEnforcement.Config.class, PostgreSQLContainerConfiguration.class
    })
    class MethodSecurityEnforcement {

        private static final String BODY_REACHED = "body reached";
        private static final long PROJECT_ID = 42L;
        private static final long PROJECT_WORKFLOW_ID = 7L;
        private static final String WORKFLOW_ID = "workflow-1";

        @MockitoBean
        private PermissionService permissionService;

        @MockitoBean
        private ProjectService projectService;

        @MockitoBean
        private ProjectWorkflowService projectWorkflowService;

        @MockitoBean
        private WorkflowFacade workflowFacade;

        @BeforeEach
        void authenticateAsNonAdmin() {
            SecurityContextHolder.getContext()
                .setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                        "viewer", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

            when(projectService.getProject(anyLong())).thenThrow(new IllegalStateException(BODY_REACHED));
            when(projectService.getWorkflowProject(anyString())).thenThrow(new IllegalStateException(BODY_REACHED));
            when(projectWorkflowService.getProjectWorkflow(anyLong()))
                .thenThrow(new IllegalStateException(BODY_REACHED));
            when(projectWorkflowService.getProjectWorkflows()).thenThrow(new IllegalStateException(BODY_REACHED));
            when(projectWorkflowService.getProjectWorkflows(anyLong(), anyInt()))
                .thenThrow(new IllegalStateException(BODY_REACHED));
            when(projectWorkflowService.getWorkflowProjectWorkflow(anyString()))
                .thenThrow(new IllegalStateException(BODY_REACHED));
            when(preBuiltTemplateService.getWorkflowTemplate(anyString()))
                .thenThrow(new IllegalStateException(BODY_REACHED));

            doThrow(new IllegalStateException(BODY_REACHED)).when(workflowFacade)
                .update(anyString(), anyString(), anyInt());
        }

        @AfterEach
        void clearSecurityContext() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void testUpdateWorkflowDeniesWhenTheWorkflowEditScopeIsRefused() {
            assertDenied(() -> projectWorkflowFacade.updateWorkflow(WORKFLOW_ID, "{}", 1));
        }

        @Test
        void testUpdateWorkflowAllowsWhenTheWorkflowEditScopeIsGranted() {
            grantWorkflowScope("WORKFLOW_EDIT", Environment.DEVELOPMENT);

            assertBodyReached(() -> projectWorkflowFacade.updateWorkflow(WORKFLOW_ID, "{}", 1));
        }

        @Test
        void testUpdateWorkflowDeniesAMemberWhoHoldsTheWorkflowEditScopeOnlyInProduction() {
            grantWorkflowScope("WORKFLOW_EDIT", Environment.PRODUCTION);

            assertDenied(() -> projectWorkflowFacade.updateWorkflow(WORKFLOW_ID, "{}", 1));
        }

        @Test
        void testExportSharedWorkflowDeniesWhenTheWorkflowEditScopeIsRefused() {
            assertDenied(() -> projectWorkflowFacade.exportSharedWorkflow(WORKFLOW_ID, "description"));
        }

        @Test
        void testExportSharedWorkflowAllowsWhenTheWorkflowEditScopeIsGranted() {
            grantWorkflowScope("WORKFLOW_EDIT", Environment.DEVELOPMENT);

            assertBodyReached(() -> projectWorkflowFacade.exportSharedWorkflow(WORKFLOW_ID, "description"));
        }

        @Test
        void testDeleteSharedWorkflowDeniesWhenTheWorkflowDeleteScopeIsRefused() {
            assertDenied(() -> projectWorkflowFacade.deleteSharedWorkflow(WORKFLOW_ID));
        }

        @Test
        void testDeleteSharedWorkflowAllowsWhenTheWorkflowDeleteScopeIsGranted() {
            grantWorkflowScope("WORKFLOW_DELETE", Environment.DEVELOPMENT);

            assertBodyReached(() -> projectWorkflowFacade.deleteSharedWorkflow(WORKFLOW_ID));
        }

        @ParameterizedTest
        @ValueSource(booleans = {
            false, true
        })
        void testGetProjectWorkflowByWorkflowIdRequiresWorkflowView(boolean granted) {
            when(permissionService.hasWorkflowScope(WORKFLOW_ID, "WORKFLOW_VIEW")).thenReturn(granted);

            assertGuarded(() -> projectWorkflowFacade.getProjectWorkflow(WORKFLOW_ID), granted);
        }

        @ParameterizedTest
        @ValueSource(booleans = {
            false, true
        })
        void testGetProjectWorkflowByProjectWorkflowIdRequiresWorkflowView(boolean granted) {
            when(permissionService.hasResourceScope(PROJECT_WORKFLOW_ID, "ProjectWorkflow", "WORKFLOW_VIEW"))
                .thenReturn(granted);

            assertGuarded(() -> projectWorkflowFacade.getProjectWorkflow(PROJECT_WORKFLOW_ID), granted);
        }

        @ParameterizedTest
        @ValueSource(booleans = {
            false, true
        })
        void testGetProjectWorkflowsRequiresWorkflowViewOnTheProject(boolean granted) {
            when(permissionService.hasResourceScope(PROJECT_ID, "Project", "WORKFLOW_VIEW")).thenReturn(granted);

            assertGuarded(() -> projectWorkflowFacade.getProjectWorkflows(PROJECT_ID), granted);
        }

        @ParameterizedTest
        @ValueSource(booleans = {
            false, true
        })
        void testGetProjectVersionWorkflowsRequiresWorkflowViewOnTheProject(boolean granted) {
            when(permissionService.hasResourceScope(PROJECT_ID, "Project", "WORKFLOW_VIEW")).thenReturn(granted);

            assertGuarded(() -> projectWorkflowFacade.getProjectVersionWorkflows(PROJECT_ID, 1, true), granted);
        }

        @ParameterizedTest
        @ValueSource(booleans = {
            false, true
        })
        void testGetProjectWorkflowsOfTheWholeTenantRequiresATenantAdmin(boolean granted) {
            when(permissionService.isTenantAdmin()).thenReturn(granted);

            assertGuarded(() -> projectWorkflowFacade.getProjectWorkflows(), granted);
        }

        @Test
        void testAddWorkflowDeniesWhenTheWorkflowCreateScopeIsRefused() {
            assertDenied(() -> projectWorkflowFacade.addWorkflow(PROJECT_ID, "{}"));
        }

        @Test
        void testAddWorkflowAllowsWhenTheWorkflowCreateScopeIsGranted() {
            grantProjectWorkflowCreateScope();

            assertBodyReached(() -> projectWorkflowFacade.addWorkflow(PROJECT_ID, "{}"));
        }

        @Test
        void testImportWorkflowTemplateDeniesWhenTheWorkflowCreateScopeIsRefused() {
            assertDenied(() -> projectWorkflowFacade.importWorkflowTemplate(PROJECT_ID, "template", false));
        }

        @Test
        void testImportWorkflowTemplateAllowsWhenTheWorkflowCreateScopeIsGranted() {
            grantProjectWorkflowCreateScope();

            assertBodyReached(() -> projectWorkflowFacade.importWorkflowTemplate(PROJECT_ID, "template", false));
        }

        @Test
        void testDuplicateWorkflowAllowsWhenCreateOnTheTargetAndViewOnTheSourceAreGranted() {
            grantProjectWorkflowCreateScope();
            grantWorkflowScope("WORKFLOW_VIEW", Environment.DEVELOPMENT);

            assertBodyReached(() -> projectWorkflowFacade.duplicateWorkflow(PROJECT_ID, WORKFLOW_ID));
        }

        @Test
        void testDuplicateWorkflowDeniesWhenTheWorkflowCreateScopeIsRefused() {
            grantWorkflowScope("WORKFLOW_VIEW", Environment.DEVELOPMENT);

            assertDenied(() -> projectWorkflowFacade.duplicateWorkflow(PROJECT_ID, WORKFLOW_ID));
        }

        @Test
        void testDuplicateWorkflowDeniesWhenTheSourceWorkflowViewScopeIsRefused() {
            grantProjectWorkflowCreateScope();

            assertDenied(() -> projectWorkflowFacade.duplicateWorkflow(PROJECT_ID, WORKFLOW_ID));
        }

        @Test
        void testDeleteWorkflowDeniesWhenTheWorkflowDeleteScopeIsRefused() {
            assertDenied(() -> projectWorkflowFacade.deleteWorkflow(WORKFLOW_ID));
        }

        @Test
        void testDeleteWorkflowAllowsWhenTheWorkflowDeleteScopeIsGranted() {
            grantWorkflowScope("WORKFLOW_DELETE", Environment.DEVELOPMENT);

            assertBodyReached(() -> projectWorkflowFacade.deleteWorkflow(WORKFLOW_ID));
        }

        private void grantProjectWorkflowCreateScope() {
            when(permissionService.hasResourceScopeInEnvironment(
                PROJECT_ID, "Project", "WORKFLOW_CREATE", Environment.DEVELOPMENT)).thenReturn(true);
        }

        private void grantWorkflowScope(String scope, Environment environment) {
            when(permissionService.hasWorkflowScope(WORKFLOW_ID, scope, environment)).thenReturn(true);
        }

        private static void assertBodyReached(ThrowingCallable throwingCallable) {
            assertThatThrownBy(throwingCallable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
        }

        private static void assertDenied(ThrowingCallable throwingCallable) {
            assertThatThrownBy(throwingCallable).isInstanceOf(AccessDeniedException.class);
        }

        private static void assertGuarded(ThrowingCallable throwingCallable, boolean granted) {
            if (granted) {
                assertBodyReached(throwingCallable);
            } else {
                assertDenied(throwingCallable);
            }
        }

        @EnableMethodSecurity
        static class Config {
        }
    }
}
