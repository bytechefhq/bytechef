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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.automation.configuration.audit.ProjectWorkflowAuditEvents;
import com.bytechef.automation.configuration.config.ProjectAuditIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
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
class ProjectWorkflowFacadeAuditIntTest {

    private static final String ADMIN_LOGIN = "admin";
    private static final Pattern SECRET_PATTERN = Pattern.compile(
        "(?i)secret|password|token|credential|private.?key|api.?key");
    private static final String WORKFLOW_DEFINITION =
        "{\"label\": \"Audit Workflow\", \"description\": \"Audit Description\", \"tasks\": []}";

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
    private WorkflowCrudRepository workflowCrudRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private long projectId;

    @BeforeEach
    void beforeEach() {
        authenticate(AuthorityConstants.ADMIN);

        Workspace workspace = workspaceRepository.save(new Workspace("workflow-audit"));

        projectId = projectFacade.createProject(
            ProjectDTO.builder()
                .name("Workflow audit")
                .workspaceId(workspace.getId())
                .build());

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

        workspaceRepository.deleteAll();
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testAddWorkflowRecordsTheProjectAndTheNewWorkflow() {
        ProjectWorkflow projectWorkflow = projectWorkflowFacade.addWorkflow(projectId, WORKFLOW_DEFINITION);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectWorkflowAuditEvents.WORKFLOW_CREATED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "addWorkflow",
            Map.of(
                "result", "SUCCESS", "projectId", String.valueOf(projectId), "workflowId",
                projectWorkflow.getWorkflowId()));
        assertThat(persistentAuditEvent.getData()).doesNotContainValue(WORKFLOW_DEFINITION);
    }

    @Test
    void testAddWorkflowToAnUnknownProjectIsRecordedAsAnError() {
        assertThatThrownBy(() -> projectWorkflowFacade.addWorkflow(Long.MAX_VALUE, WORKFLOW_DEFINITION))
            .isInstanceOf(NoSuchElementException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectWorkflowAuditEvents.WORKFLOW_CREATED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertData(
            persistentAuditEvent, "addWorkflow",
            Map.of(
                "result", "ERROR", "errorClass", NoSuchElementException.class.getName(), "projectId",
                String.valueOf(Long.MAX_VALUE)));
    }

    @Test
    void testDeleteWorkflowRecordsTheDeletedWorkflow() {
        String workflowId = addWorkflow();

        projectWorkflowFacade.deleteWorkflow(workflowId);

        assertThat(workflowCrudRepository.findById(workflowId)).isEmpty();

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectWorkflowAuditEvents.WORKFLOW_DELETED);

        assertSuccess(persistentAuditEvent);
        assertData(persistentAuditEvent, "deleteWorkflow", Map.of("result", "SUCCESS", "workflowId", workflowId));
    }

    @Test
    void testDeleteOfAnUnknownWorkflowIsRecordedAsAnError() {
        assertThatThrownBy(() -> projectWorkflowFacade.deleteWorkflow("unknown-workflow"))
            .isInstanceOf(NoSuchElementException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectWorkflowAuditEvents.WORKFLOW_DELETED);

        assertData(
            persistentAuditEvent, "deleteWorkflow",
            Map.of(
                "result", "ERROR", "errorClass", NoSuchElementException.class.getName(), "workflowId",
                "unknown-workflow"));
    }

    @Test
    void testDeniedWorkflowDeleteIsRecorded() {
        String workflowId = addWorkflow();

        authenticate(AuthorityConstants.USER);

        assertThatThrownBy(() -> projectWorkflowFacade.deleteWorkflow(workflowId))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(workflowCrudRepository.findById(workflowId)).isPresent();

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectWorkflowAuditEvents.WORKFLOW_DELETED);

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);
        assertData(persistentAuditEvent, "deleteWorkflow", Map.of("result", "DENIED", "workflowId", workflowId));
    }

    @Test
    void testDuplicateWorkflowRecordsTheSourceAndTheNewWorkflow() {
        String workflowId = addWorkflow();

        String duplicatedWorkflowId = projectWorkflowFacade.duplicateWorkflow(projectId, workflowId);

        assertThat(duplicatedWorkflowId).isNotEqualTo(workflowId);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectWorkflowAuditEvents.WORKFLOW_DUPLICATED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "duplicateWorkflow",
            Map.of(
                "result", "SUCCESS", "projectId", String.valueOf(projectId), "sourceWorkflowId", workflowId,
                "workflowId", duplicatedWorkflowId));
    }

    @Test
    void testRestoreProjectVersionWorkflowRecordsTheRestoredWorkflow() {
        ProjectWorkflow projectWorkflow = projectWorkflowFacade.addWorkflow(projectId, WORKFLOW_DEFINITION);

        projectFacade.publishProject(projectId, "first", false);

        persistenceAuditEventRepository.deleteAll();

        String workflowUuid = projectWorkflow.getUuidAsString();

        projectWorkflowFacade.restoreProjectVersionWorkflow(projectId, 1, workflowUuid);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(
            ProjectWorkflowAuditEvents.WORKFLOW_VERSION_RESTORED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "restoreProjectVersionWorkflow",
            Map.of(
                "result", "SUCCESS", "projectId", String.valueOf(projectId), "projectVersion", "1",
                "workflowUuid", workflowUuid));
    }

    private String addWorkflow() {
        ProjectWorkflow projectWorkflow = projectWorkflowFacade.addWorkflow(projectId, WORKFLOW_DEFINITION);

        persistenceAuditEventRepository.deleteAll();

        return projectWorkflow.getWorkflowId();
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
}
