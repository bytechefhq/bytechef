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

package com.bytechef.automation.ai.a2a.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.a2a.audit.A2aAuditEvents;
import com.bytechef.automation.ai.a2a.config.A2aIntTestConfiguration;
import com.bytechef.automation.ai.a2a.config.A2aIntTestConfigurationSharedMocks;
import com.bytechef.automation.ai.a2a.config.A2aIntTestWorkflows;
import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.repository.A2aProjectRepository;
import com.bytechef.automation.ai.a2a.repository.A2aProjectWorkflowRepository;
import com.bytechef.automation.ai.a2a.repository.A2aServerRepository;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        A2aIntTestConfiguration.class, A2aAuditIntTest.A2aAuditIntTestConfiguration.class
    })
@Import(PostgreSQLContainerConfiguration.class)
@A2aIntTestConfigurationSharedMocks
class A2aAuditIntTest {

    private static final String ADMIN_LOGIN = "admin";
    private static final long WORKSPACE_ID = 1L;

    @Autowired
    private A2aProjectFacade a2aProjectFacade;

    @Autowired
    private A2aProjectRepository a2aProjectRepository;

    @Autowired
    private A2aProjectWorkflowFacade a2aProjectWorkflowFacade;

    @Autowired
    private A2aProjectWorkflowRepository a2aProjectWorkflowRepository;

    @Autowired
    private A2aServerFacade a2aServerFacade;

    @Autowired
    private A2aServerRepository a2aServerRepository;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private A2aServer a2aServer;
    private Project project;
    private String workflowId;

    @BeforeEach
    void beforeEach() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                ADMIN_LOGIN, "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        Workspace workspace = workspaceRepository.save(new Workspace("a2a-audit"));

        A2aServer newA2aServer = new A2aServer("audit-agent", null, Environment.DEVELOPMENT);

        newA2aServer.setWorkspaceId(workspace.getId());

        a2aServer = a2aServerRepository.save(newA2aServer);

        Project newProject = Project.builder()
            .description("audit-project")
            .name("audit-project")
            .workspaceId(workspace.getId())
            .build();

        newProject.publish("v1");

        project = projectRepository.save(newProject);

        workflowId = A2aIntTestWorkflows.createNewWorkflowCallWorkflow(workflowService);

        projectWorkflowRepository.save(new ProjectWorkflow(project.getId(), 1, workflowId, UUID.randomUUID()));

        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        a2aProjectWorkflowRepository.deleteAll();
        a2aProjectRepository.deleteAll();
        projectDeploymentWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectWorkflowRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
        a2aServerRepository.deleteAll();

        A2aIntTestWorkflows.deleteWorkflows(workflowService, List.of(workflowId));

        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testCreateA2aProjectRecordsTheProjectAndTheServer() {
        A2aProject a2aProject = a2aProjectFacade.createA2aProject(
            a2aServer.getId(), project.getId(), 1, List.of(workflowId));

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(A2aAuditEvents.A2A_PROJECT_CREATED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "createA2aProject",
            Map.of(
                "result", "SUCCESS", "a2aProjectId", String.valueOf(a2aProject.getId()), "a2aServerId",
                String.valueOf(a2aServer.getId()), "projectId", String.valueOf(project.getId()), "projectVersion",
                "1"));
    }

    @Test
    void testCreateA2aProjectWithWorkflowConfigurationsRecordsTheProjectAndTheServer() {
        A2aProject a2aProject = a2aProjectFacade.createA2aProject(
            a2aServer.getId(), project.getId(), 1, List.of(workflowId), List.of());

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(A2aAuditEvents.A2A_PROJECT_CREATED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "createA2aProject",
            Map.of(
                "result", "SUCCESS", "a2aProjectId", String.valueOf(a2aProject.getId()), "a2aServerId",
                String.valueOf(a2aServer.getId()), "projectId", String.valueOf(project.getId()), "projectVersion",
                "1"));
    }

    @Test
    void testCreateA2aProjectForAnUnpublishedVersionRecordsAnError() {
        assertThatThrownBy(
            () -> a2aProjectFacade.createA2aProject(a2aServer.getId(), project.getId(), 2, List.of(workflowId)))
                .isInstanceOf(IllegalArgumentException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(A2aAuditEvents.A2A_PROJECT_CREATED);

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);
        assertData(
            persistentAuditEvent, "createA2aProject",
            Map.of(
                "result", "ERROR", "errorClass", IllegalArgumentException.class.getName(), "a2aServerId",
                String.valueOf(a2aServer.getId()), "projectId", String.valueOf(project.getId()), "projectVersion",
                "2"));
    }

    @Test
    void testUpdateA2aProjectRecordsTheProject() {
        A2aProject a2aProject = createA2aProject();

        a2aProjectFacade.updateA2aProject(a2aProject.getId(), List.of(workflowId));

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(A2aAuditEvents.A2A_PROJECT_UPDATED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "updateA2aProject",
            Map.of("result", "SUCCESS", "a2aProjectId", String.valueOf(a2aProject.getId())));
    }

    @Test
    void testUpdateA2aProjectWithWorkflowConfigurationsRecordsTheProject() {
        A2aProject a2aProject = createA2aProject();

        a2aProjectFacade.updateA2aProject(a2aProject.getId(), List.of(workflowId), List.of());

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(A2aAuditEvents.A2A_PROJECT_UPDATED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "updateA2aProject",
            Map.of("result", "SUCCESS", "a2aProjectId", String.valueOf(a2aProject.getId())));
    }

    @Test
    void testDeleteA2aProjectRecordsTheProject() {
        A2aProject a2aProject = createA2aProject();

        a2aProjectFacade.deleteA2aProject(a2aProject.getId());

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(A2aAuditEvents.A2A_PROJECT_DELETED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "deleteA2aProject",
            Map.of("result", "SUCCESS", "a2aProjectId", String.valueOf(a2aProject.getId())));
    }

    @Test
    void testDeleteOfAnUnknownA2aProjectRecordsAnError() {
        assertThatThrownBy(() -> a2aProjectFacade.deleteA2aProject(Long.MAX_VALUE))
            .isInstanceOf(IllegalArgumentException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(A2aAuditEvents.A2A_PROJECT_DELETED);

        assertData(
            persistentAuditEvent, "deleteA2aProject",
            Map.of(
                "result", "ERROR", "errorClass", IllegalArgumentException.class.getName(), "a2aProjectId",
                String.valueOf(Long.MAX_VALUE)));
    }

    @Test
    void testCreateA2aServerRecordsTheServerAndTheWorkspace() {
        A2aServer createdA2aServer = a2aServerFacade.createA2aServer(
            WORKSPACE_ID, new A2aServer("created-agent", null, Environment.DEVELOPMENT));

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(A2aAuditEvents.A2A_SERVER_CREATED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "createA2aServer",
            Map.of(
                "result", "SUCCESS", "a2aServerId", String.valueOf(createdA2aServer.getId()), "name",
                "created-agent", "workspaceId", String.valueOf(WORKSPACE_ID)));
        assertThat(persistentAuditEvent.getData()).doesNotContainValue(createdA2aServer.getSecretKey());
    }

    @Test
    void testUpdateA2aServerRecordsTheNewNameAndEnablement() {
        a2aServerFacade.updateA2aServer(a2aServer.getId(), "renamed-agent", null, false, null);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(A2aAuditEvents.A2A_SERVER_UPDATED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "updateA2aServer",
            Map.of(
                "result", "SUCCESS", "a2aServerId", String.valueOf(a2aServer.getId()), "name", "renamed-agent",
                "enabled", "false"));
        assertThat(persistentAuditEvent.getData()).doesNotContainValue(a2aServer.getSecretKey());
    }

    @Test
    void testDeleteA2aServerRecordsTheServer() {
        a2aServerFacade.deleteA2aServer(a2aServer.getId());

        assertThat(a2aServerRepository.findById(a2aServer.getId())).isEmpty();

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(A2aAuditEvents.A2A_SERVER_DELETED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "deleteA2aServer",
            Map.of("result", "SUCCESS", "a2aServerId", String.valueOf(a2aServer.getId())));
    }

    @Test
    void testDeleteOfAnUnknownA2aServerRecordsAnError() {
        assertThatThrownBy(() -> a2aServerFacade.deleteA2aServer(Long.MAX_VALUE))
            .isInstanceOf(IllegalArgumentException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(A2aAuditEvents.A2A_SERVER_DELETED);

        assertData(
            persistentAuditEvent, "deleteA2aServer",
            Map.of(
                "result", "ERROR", "errorClass", IllegalArgumentException.class.getName(), "a2aServerId",
                String.valueOf(Long.MAX_VALUE)));
    }

    @Test
    void testDisablingAnA2aProjectWorkflowRecordsTheWorkflowItsProjectAndTheNewState() {
        A2aProject a2aProject = createA2aProject();

        List<A2aProjectWorkflow> a2aProjectWorkflows = a2aProjectWorkflowRepository.findAllByA2aProjectId(
            a2aProject.getId());

        A2aProjectWorkflow a2aProjectWorkflow = a2aProjectWorkflows.getFirst();

        a2aProjectWorkflowFacade.updateA2aProjectWorkflowEnabled(a2aProjectWorkflow.getId(), false);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(
            A2aAuditEvents.A2A_PROJECT_WORKFLOW_ENABLED_UPDATED);

        assertSuccess(persistentAuditEvent);
        assertData(
            persistentAuditEvent, "updateA2aProjectWorkflowEnabled",
            Map.of(
                "result", "SUCCESS", "a2aProjectWorkflowId", String.valueOf(a2aProjectWorkflow.getId()),
                "a2aProjectId", String.valueOf(a2aProject.getId()), "enabled", "false"));
    }

    private static void assertData(
        PersistentAuditEvent persistentAuditEvent, String methodName, Map<String, String> expectedData) {

        Map<String, String> data = new HashMap<>(persistentAuditEvent.getData());

        assertThat(data.remove("method")).endsWith("." + methodName);
        assertThat(data).containsExactlyInAnyOrderEntriesOf(expectedData);
    }

    private static void assertSuccess(PersistentAuditEvent persistentAuditEvent) {
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);
    }

    private A2aProject createA2aProject() {
        A2aProject a2aProject = a2aProjectFacade.createA2aProject(
            a2aServer.getId(), project.getId(), 1, List.of(workflowId));

        persistenceAuditEventRepository.deleteAll();

        return a2aProject;
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

    static class A2aAuditIntTestConfiguration {

        @Bean
        AuditAspect auditAspect(
            ApplicationProperties applicationProperties, AuditEventService auditEventService,
            AuditMapperResolver auditMapperResolver) {

            return new AuditAspect(applicationProperties, auditEventService, auditMapperResolver);
        }

        @Bean
        AuditCaptureAspect auditCaptureAspect(
            AuditMapperResolver auditMapperResolver,
            ObjectProvider<PlatformTransactionManager> transactionManagerProvider) {

            return new AuditCaptureAspect(auditMapperResolver, transactionManagerProvider);
        }

        @Bean
        AuditEventService auditEventService(PersistenceAuditEventRepository persistenceAuditEventRepository) {
            return new AuditEventService(persistenceAuditEventRepository);
        }

        @Bean
        AuditMapperResolver auditMapperResolver(ListableBeanFactory listableBeanFactory) {
            return new AuditMapperResolver(listableBeanFactory);
        }
    }
}
