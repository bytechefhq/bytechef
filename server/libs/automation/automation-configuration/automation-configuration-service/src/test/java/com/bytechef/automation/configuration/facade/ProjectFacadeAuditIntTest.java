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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.automation.configuration.audit.ProjectAuditEvents;
import com.bytechef.automation.configuration.config.ProjectAuditIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.SharedTemplateRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.githubproxy.client.GitHubProxyClient;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        ProjectIntTestConfiguration.class, ProjectAuditIntTestConfiguration.class
    },
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
class ProjectFacadeAuditIntTest {

    private static final String ADMIN_LOGIN = "admin";
    private static final Pattern SECRET_PATTERN = Pattern.compile(
        "(?i)secret|password|token|credential|private.?key|api.?key");
    private static final String WORKFLOW_DEFINITION =
        "{\"label\": \"Audit Workflow\", \"description\": \"Audit Description\", \"tasks\": []}";

    @Autowired
    private GitHubProxyClient gitHubProxyClient;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private ProjectFacade projectFacade;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private SharedTemplateRepository sharedTemplateRepository;

    @Autowired
    private WorkflowCrudRepository workflowCrudRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Workspace workspace;

    @BeforeEach
    void beforeEach() {
        authenticate(AuthorityConstants.ADMIN);

        workspace = workspaceRepository.save(new Workspace("project-audit"));

        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        projectWorkflowRepository.deleteAll();
        projectRepository.deleteAll();

        for (Workflow workflow : workflowCrudRepository.findAll()) {
            workflowCrudRepository.deleteById(workflow.getId());
        }

        sharedTemplateRepository.deleteAll();
        workspaceRepository.deleteAll();
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testCreateProjectRecordsTheNewProject() {
        long projectId = projectFacade.createProject(newProjectDTO("Billing sync"));

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectAuditEvents.PROJECT_CREATED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "createProject",
            Map.of(
                "result", "SUCCESS", "projectId", String.valueOf(projectId), "name", "Billing sync",
                "workspaceId", String.valueOf(workspace.getId())));
    }

    @Test
    void testUpdateProjectRecordsTheRenamedProject() {
        long projectId = projectFacade.createProject(newProjectDTO("Billing sync"));

        ProjectDTO projectDTO = projectFacade.getProject(projectId);

        persistenceAuditEventRepository.deleteAll();

        projectFacade.updateProject(
            ProjectDTO.builder()
                .id(projectId)
                .name("Renamed sync")
                .version(projectDTO.version())
                .workspaceId(workspace.getId())
                .build());

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectAuditEvents.PROJECT_UPDATED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "updateProject",
            Map.of("result", "SUCCESS", "projectId", String.valueOf(projectId), "name", "Renamed sync"));
    }

    @Test
    void testDeleteProjectRecordsTheDeletedProject() {
        long projectId = createProjectWithWorkflow();

        projectFacade.deleteProject(projectId);

        assertThat(projectRepository.findById(projectId)).isEmpty();

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectAuditEvents.PROJECT_DELETED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "deleteProject", Map.of("result", "SUCCESS", "projectId", String.valueOf(projectId)));
    }

    @Test
    void testDeleteOfAnUnknownProjectIsRecordedAsAnError() {
        assertThatThrownBy(() -> projectFacade.deleteProject(Long.MAX_VALUE))
            .isInstanceOf(NoSuchElementException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectAuditEvents.PROJECT_DELETED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertData(
            persistentAuditEvent, "deleteProject",
            Map.of(
                "result", "ERROR", "errorClass", NoSuchElementException.class.getName(), "projectId",
                String.valueOf(Long.MAX_VALUE)));
    }

    @Test
    void testDeniedProjectDeleteIsRecorded() {
        long projectId = projectFacade.createProject(newProjectDTO("Billing sync"));

        persistenceAuditEventRepository.deleteAll();

        authenticate(AuthorityConstants.USER);

        assertThatThrownBy(() -> projectFacade.deleteProject(projectId))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(projectRepository.findById(projectId)).isPresent();

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectAuditEvents.PROJECT_DELETED);

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);
        assertData(
            persistentAuditEvent, "deleteProject", Map.of("result", "DENIED", "projectId", String.valueOf(projectId)));
    }

    @Test
    void testPublishProjectRecordsTheNewVersion() {
        long projectId = createProjectWithWorkflow();

        int projectVersion = projectFacade.publishProject(projectId, "first release", false);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectAuditEvents.PROJECT_PUBLISHED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "publishProject",
            Map.of(
                "result", "SUCCESS", "projectId", String.valueOf(projectId), "projectVersion",
                String.valueOf(projectVersion), "syncWithGit", "false"));
        assertThat(persistentAuditEvent.getData()).doesNotContainValue("first release");
    }

    @Test
    void testDeleteProjectVersionRecordsTheDeletedVersion() {
        long projectId = createProjectWithWorkflow();

        projectFacade.publishProject(projectId, "first", false);
        projectFacade.publishProject(projectId, "second", false);

        persistenceAuditEventRepository.deleteAll();

        projectFacade.deleteProjectVersion(projectId, 1);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectAuditEvents.PROJECT_VERSION_DELETED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "deleteProjectVersion",
            Map.of("result", "SUCCESS", "projectId", String.valueOf(projectId), "projectVersion", "1"));
    }

    @Test
    void testDeleteOfADraftProjectVersionIsRecordedAsAnError() {
        long projectId = createProjectWithWorkflow();

        assertThatThrownBy(() -> projectFacade.deleteProjectVersion(projectId, 1))
            .isInstanceOf(ConfigurationException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectAuditEvents.PROJECT_VERSION_DELETED);

        assertData(
            persistentAuditEvent, "deleteProjectVersion",
            Map.of(
                "result", "ERROR", "errorClass", ConfigurationException.class.getName(), "projectId",
                String.valueOf(projectId), "projectVersion", "1"));
    }

    @Test
    void testExportProjectRecordsTheExportedVersion() {
        long projectId = createProjectWithWorkflow();

        projectFacade.publishProject(projectId, "first", false);

        persistenceAuditEventRepository.deleteAll();

        byte[] projectData = projectFacade.exportProject(projectId, 1);

        assertThat(projectData).isNotEmpty();

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectAuditEvents.PROJECT_EXPORTED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "exportProject",
            Map.of("result", "SUCCESS", "projectId", String.valueOf(projectId), "projectVersion", "1"));
    }

    @Test
    void testExportSharedProjectRecordsTheProject() {
        long projectId = createProjectWithWorkflow();

        projectFacade.exportSharedProject(projectId, "internal sharing notes");

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectAuditEvents.PROJECT_SHARED_EXPORTED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "exportSharedProject",
            Map.of("result", "SUCCESS", "projectId", String.valueOf(projectId)));
        assertThat(persistentAuditEvent.getData()).doesNotContainValue("internal sharing notes");
    }

    @Test
    void testDeleteSharedProjectRecordsTheProject() {
        long projectId = createProjectWithWorkflow();

        projectFacade.exportSharedProject(projectId, "internal sharing notes");

        persistenceAuditEventRepository.deleteAll();

        projectFacade.deleteSharedProject(projectId);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectAuditEvents.PROJECT_SHARED_DELETED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "deleteSharedProject",
            Map.of("result", "SUCCESS", "projectId", String.valueOf(projectId)));
    }

    @Test
    void testDuplicateProjectRecordsTheSourceAndTheNewProject() {
        long projectId = createProjectWithWorkflow();

        ProjectDTO duplicatedProjectDTO = projectFacade.duplicateProject(projectId);

        assertThat(duplicatedProjectDTO.id()).isNotEqualTo(projectId);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectAuditEvents.PROJECT_DUPLICATED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "duplicateProject",
            Map.of(
                "result", "SUCCESS", "sourceProjectId", String.valueOf(projectId), "projectId",
                String.valueOf(duplicatedProjectDTO.id()), "name", duplicatedProjectDTO.name()));
    }

    @Test
    void testRestoreProjectVersionRecordsTheRestoredVersion() {
        long projectId = createProjectWithWorkflow();

        projectFacade.publishProject(projectId, "first", false);

        persistenceAuditEventRepository.deleteAll();

        projectFacade.restoreProjectVersion(projectId, 1);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectAuditEvents.PROJECT_VERSION_RESTORED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "restoreProjectVersion",
            Map.of("result", "SUCCESS", "projectId", String.valueOf(projectId), "projectVersion", "1"));
    }

    @Test
    void testImportProjectRecordsTheImportedProject() {
        long projectId = projectFacade.importProject(createProjectArchive("Imported Project"), workspace.getId());

        assertThat(projectRepository.findById(projectId)).isPresent();

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectAuditEvents.PROJECT_IMPORTED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "importProject",
            Map.of(
                "result", "SUCCESS", "projectId", String.valueOf(projectId), "workspaceId",
                String.valueOf(workspace.getId())));
    }

    @Test
    void testImportProjectTemplateRecordsTheTemplateAndTheImportedProject() {
        String templateId = EncodingUtils.base64EncodeToString("templates/" + UUID.randomUUID() + ".zip");

        when(gitHubProxyClient.getRaw(any(), any(), any(), any(), eq(null), eq(null), eq(null)))
            .thenReturn(createProjectArchive("Template Project"));

        long projectId = projectFacade.importProjectTemplate(templateId, workspace.getId(), false);

        assertThat(projectRepository.findById(projectId)).isPresent();

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectAuditEvents.PROJECT_TEMPLATE_IMPORTED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "importProjectTemplate",
            Map.of(
                "result", "SUCCESS", "templateId", templateId, "projectId", String.valueOf(projectId),
                "workspaceId", String.valueOf(workspace.getId())));
    }

    @Test
    void testImportOfAnUnknownSharedTemplateIsRecordedAsAnError() {
        String templateId = String.valueOf(UUID.randomUUID());

        assertThatThrownBy(() -> projectFacade.importProjectTemplate(templateId, workspace.getId(), true))
            .isExactlyInstanceOf(RuntimeException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectAuditEvents.PROJECT_TEMPLATE_IMPORTED);

        assertData(
            persistentAuditEvent, "importProjectTemplate",
            Map.of(
                "result", "ERROR", "errorClass", RuntimeException.class.getName(), "templateId", templateId,
                "workspaceId", String.valueOf(workspace.getId())));
    }

    private static void assertData(
        PersistentAuditEvent persistentAuditEvent, String methodName, Map<String, String> expectedData) {

        Map<String, String> data = persistentAuditEvent.getData();

        assertThat(data.get("method")).endsWith("." + methodName);
        assertThat(data)
            .hasSize(expectedData.size() + 1)
            .containsAllEntriesOf(expectedData);
        assertThat(data.keySet()).noneMatch(key -> SECRET_PATTERN.matcher(key)
            .find());
        assertThat(data.values()).noneMatch(value -> SECRET_PATTERN.matcher(value)
            .find());
    }

    private static void assertSuccess(PersistentAuditEvent persistentAuditEvent) {
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .doesNotContainKeys("errorClass", "dataError", "mapperError", "captureError");
    }

    private static void authenticate(String authority) {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                ADMIN_LOGIN, "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    private static byte[] createProjectArchive(String projectName) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            zipOutputStream.putNextEntry(new ZipEntry("template.json"));
            zipOutputStream.write(
                "{\"description\":\"Template description\",\"projectVersion\":1}".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("project.json"));
            zipOutputStream.write(
                ("{\"name\":\"" + projectName + "\",\"description\":\"Imported description\"}")
                    .getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("workflow-audit.json"));
            zipOutputStream.write("\"{\\\"tasks\\\":[]}\"".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.finish();

            return byteArrayOutputStream.toByteArray();
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }
    }

    private long createProjectWithWorkflow() {
        long projectId = projectFacade.createProject(newProjectDTO("Billing sync"));

        projectWorkflowFacade.addWorkflow(projectId, WORKFLOW_DEFINITION);

        persistenceAuditEventRepository.deleteAll();

        return projectId;
    }

    private PersistentAuditEvent findSingleEvent(String eventType) {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent()
            .stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .toList();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    private ProjectDTO newProjectDTO(String name) {
        return ProjectDTO.builder()
            .name(name)
            .workspaceId(workspace.getId())
            .build();
    }
}
