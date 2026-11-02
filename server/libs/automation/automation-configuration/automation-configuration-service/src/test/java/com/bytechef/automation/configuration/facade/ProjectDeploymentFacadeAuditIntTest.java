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
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.automation.configuration.audit.ProjectDeploymentAuditEvents;
import com.bytechef.automation.configuration.config.ProjectAuditIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.dto.ProjectDeploymentWorkflowDTO;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
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
class ProjectDeploymentFacadeAuditIntTest {

    private static final String ADMIN_LOGIN = "admin";
    private static final String DEPLOYMENT_NAME = "Billing deployment";
    private static final Pattern SECRET_PATTERN = Pattern.compile(
        "(?i)secret|password|token|credential|private.?key|api.?key");
    private static final String WORKFLOW_DEFINITION =
        "{\"label\": \"Audit Workflow\", \"description\": \"Audit Description\", \"tasks\": []}";

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

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
    private ProjectWorkflow publishedProjectWorkflow;

    @BeforeEach
    void beforeEach() {
        authenticate(AuthorityConstants.ADMIN);

        Workspace workspace = workspaceRepository.save(new Workspace("deployment-audit"));

        projectId = projectFacade.createProject(
            ProjectDTO.builder()
                .name("Deployment audit")
                .workspaceId(workspace.getId())
                .build());

        projectWorkflowFacade.addWorkflow(projectId, WORKFLOW_DEFINITION);

        projectFacade.publishProject(projectId, "first", false);

        List<ProjectWorkflow> projectWorkflows = projectWorkflowRepository.findAllByProjectIdAndProjectVersion(
            projectId, 1);

        publishedProjectWorkflow = projectWorkflows.getFirst();

        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        projectDeploymentWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectWorkflowRepository.deleteAll();
        projectRepository.deleteAll();

        for (Workflow workflow : workflowCrudRepository.findAll()) {
            workflowCrudRepository.deleteById(workflow.getId());
        }

        workspaceRepository.deleteAll();
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testCreateProjectDeploymentRecordsTheNewDeployment() {
        long projectDeploymentId = projectDeploymentFacade.createProjectDeployment(newProjectDeploymentDTO());

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectDeploymentAuditEvents.DEPLOYMENT_CREATED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "createProjectDeployment",
            Map.of(
                "result", "SUCCESS", "projectDeploymentId", String.valueOf(projectDeploymentId), "projectId",
                String.valueOf(projectId), "name", DEPLOYMENT_NAME));
    }

    @Test
    void testCreateProjectDeploymentForAWorkflowRecordsTheWorkflow() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName(DEPLOYMENT_NAME);
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(1);

        String workflowId = publishedProjectWorkflow.getWorkflowId();

        long projectDeploymentId = projectDeploymentFacade.createProjectDeployment(
            projectDeployment, workflowId, List.of());

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectDeploymentAuditEvents.DEPLOYMENT_CREATED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "createProjectDeployment",
            Map.of(
                "result", "SUCCESS", "projectDeploymentId", String.valueOf(projectDeploymentId), "projectId",
                String.valueOf(projectId), "name", DEPLOYMENT_NAME, "workflowId", workflowId));
    }

    @Test
    void testCreateProjectDeploymentOfADraftVersionIsRecordedAsAnError() {
        ProjectDeploymentDTO projectDeploymentDTO = ProjectDeploymentDTO.builder()
            .environment(Environment.DEVELOPMENT)
            .name(DEPLOYMENT_NAME)
            .projectId(projectId)
            .projectVersion(2)
            .projectDeploymentWorkflows(List.of())
            .build();

        assertThatThrownBy(() -> projectDeploymentFacade.createProjectDeployment(projectDeploymentDTO))
            .isInstanceOf(ConfigurationException.class);

        assertThat(projectDeploymentRepository.count()).isZero();

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectDeploymentAuditEvents.DEPLOYMENT_CREATED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertData(
            persistentAuditEvent, "createProjectDeployment",
            Map.of(
                "result", "ERROR", "errorClass", ConfigurationException.class.getName(), "projectId",
                String.valueOf(projectId), "name", DEPLOYMENT_NAME));
    }

    @Test
    void testUpdateProjectDeploymentRecordsTheRenamedDeployment() {
        long projectDeploymentId = createProjectDeployment();

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacade.getProjectDeployment(projectDeploymentId);

        projectDeploymentFacade.updateProjectDeployment(
            ProjectDeploymentDTO.builder()
                .environment(Environment.DEVELOPMENT)
                .id(projectDeploymentId)
                .name("Renamed deployment")
                .projectId(projectId)
                .projectVersion(1)
                .projectDeploymentWorkflows(projectDeploymentDTO.projectDeploymentWorkflows())
                .version(projectDeploymentDTO.version())
                .build());

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectDeploymentAuditEvents.DEPLOYMENT_UPDATED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "updateProjectDeployment",
            Map.of(
                "result", "SUCCESS", "projectDeploymentId", String.valueOf(projectDeploymentId), "projectId",
                String.valueOf(projectId), "name", "Renamed deployment"));
    }

    @Test
    void testUpdateProjectDeploymentWorkflowConnectionsRecordsTheWorkflowAndEnvironment() {
        createProjectDeployment();

        long environmentId = Environment.DEVELOPMENT.ordinal();

        when(environmentService.getEnvironment(environmentId)).thenReturn(Environment.DEVELOPMENT);

        String workflowUuid = publishedProjectWorkflow.getUuidAsString();

        projectDeploymentFacade.updateProjectDeployment(projectId, 1, workflowUuid, List.of(), environmentId);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectDeploymentAuditEvents.DEPLOYMENT_UPDATED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "updateProjectDeployment",
            Map.of(
                "result", "SUCCESS", "projectId", String.valueOf(projectId), "projectVersion", "1",
                "workflowUuid", workflowUuid, "environmentId", String.valueOf(environmentId)));
    }

    @Test
    void testDeleteProjectDeploymentRecordsTheDeletedDeployment() {
        long projectDeploymentId = createProjectDeployment();

        projectDeploymentFacade.deleteProjectDeployment(projectDeploymentId);

        assertThat(projectDeploymentRepository.findById(projectDeploymentId)).isEmpty();

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectDeploymentAuditEvents.DEPLOYMENT_DELETED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "deleteProjectDeployment",
            Map.of("result", "SUCCESS", "projectDeploymentId", String.valueOf(projectDeploymentId)));
    }

    @Test
    void testDeleteOfAnUnknownProjectDeploymentIsRecordedAsAnError() {
        assertThatThrownBy(() -> projectDeploymentFacade.deleteProjectDeployment(Long.MAX_VALUE))
            .isInstanceOf(NoSuchElementException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ProjectDeploymentAuditEvents.DEPLOYMENT_DELETED);

        assertData(
            persistentAuditEvent, "deleteProjectDeployment",
            Map.of(
                "result", "ERROR", "errorClass", NoSuchElementException.class.getName(), "projectDeploymentId",
                String.valueOf(Long.MAX_VALUE)));
    }

    @Test
    void testDisablingADeploymentRecordsTheNewState() {
        long projectDeploymentId = createProjectDeployment();

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentId, false);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(
            ProjectDeploymentAuditEvents.DEPLOYMENT_ENABLED_UPDATED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "enableProjectDeployment",
            Map.of(
                "result", "SUCCESS", "projectDeploymentId", String.valueOf(projectDeploymentId), "enabled", "false"));
    }

    @Test
    void testDisablingADeploymentWorkflowRecordsTheWorkflowAndTheNewState() {
        long projectDeploymentId = createProjectDeployment();

        String workflowId = publishedProjectWorkflow.getWorkflowId();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentId, workflowId, false);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(
            ProjectDeploymentAuditEvents.DEPLOYMENT_WORKFLOW_ENABLED_UPDATED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "enableProjectDeploymentWorkflow",
            Map.of(
                "result", "SUCCESS", "projectDeploymentId", String.valueOf(projectDeploymentId), "workflowId",
                workflowId, "enabled", "false"));
    }

    @Test
    void testDisablingADeploymentWorkflowByEnvironmentRecordsTheProjectAndEnvironment() {
        createProjectDeployment();

        String workflowId = publishedProjectWorkflow.getWorkflowId();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectId, workflowId, false, Environment.DEVELOPMENT);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(
            ProjectDeploymentAuditEvents.DEPLOYMENT_WORKFLOW_ENABLED_UPDATED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "enableProjectDeploymentWorkflow",
            Map.of(
                "result", "SUCCESS", "projectId", String.valueOf(projectId), "workflowId", workflowId,
                "environment", Environment.DEVELOPMENT.name(), "enabled", "false"));
    }

    @Test
    void testDisablingAWorkflowOfAnUnknownDeploymentIsRecordedAsAnError() {
        String workflowId = publishedProjectWorkflow.getWorkflowId();

        assertThatThrownBy(
            () -> projectDeploymentFacade.enableProjectDeploymentWorkflow(Long.MAX_VALUE, workflowId, false))
                .isInstanceOf(NoSuchElementException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(
            ProjectDeploymentAuditEvents.DEPLOYMENT_WORKFLOW_ENABLED_UPDATED);

        assertData(
            persistentAuditEvent, "enableProjectDeploymentWorkflow",
            Map.of(
                "result", "ERROR", "errorClass", NoSuchElementException.class.getName(), "projectDeploymentId",
                String.valueOf(Long.MAX_VALUE), "workflowId", workflowId, "enabled", "false"));
    }

    private long createProjectDeployment() {
        long projectDeploymentId = projectDeploymentFacade.createProjectDeployment(newProjectDeploymentDTO());

        persistenceAuditEventRepository.deleteAll();

        return projectDeploymentId;
    }

    private ProjectDeploymentDTO newProjectDeploymentDTO() {
        ProjectDeploymentWorkflowDTO projectDeploymentWorkflowDTO = new ProjectDeploymentWorkflowDTO(
            List.of(), null, null, Map.of(), true, null, null, null, null, null, null, null, 0,
            publishedProjectWorkflow.getWorkflowId(), publishedProjectWorkflow.getUuidAsString());

        return ProjectDeploymentDTO.builder()
            .environment(Environment.DEVELOPMENT)
            .name(DEPLOYMENT_NAME)
            .projectId(projectId)
            .projectVersion(1)
            .projectDeploymentWorkflows(List.of(projectDeploymentWorkflowDTO))
            .build();
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
