/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.repository.git.GitWorkflowRepository.GitWorkflows;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.configuration.audit.ProjectGitAuditEvents;
import com.bytechef.ee.automation.configuration.config.EeAutomationConfigurationIntTestConfiguration;
import com.bytechef.ee.automation.configuration.domain.ProjectGitConfiguration;
import com.bytechef.ee.automation.configuration.service.ProjectGitConfigurationService;
import com.bytechef.ee.automation.configuration.service.ProjectGitService;
import com.bytechef.ee.automation.configuration.service.WorkspaceService;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.ee.platform.configuration.dto.GitConfigurationDTO;
import com.bytechef.ee.platform.configuration.facade.GitConfigurationFacade;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        EeAutomationConfigurationIntTestConfiguration.class, ProjectGitFacadeAuditIntTest.AuditConfiguration.class
    },
    properties = {
        "bytechef.edition=ee", "bytechef.audit.enabled=true"
    })
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class ProjectGitFacadeAuditIntTest {

    private static final String BRANCH = "main";
    private static final String COMMIT_HASH = "0f3c2a9";
    private static final String PASSWORD = "confidential-password";
    private static final long PROJECT_ID = 42L;
    private static final String URL = "https://git.example.com/repository.git";
    private static final long WORKSPACE_ID = 1049L;

    @MockitoBean
    private GitConfigurationFacade gitConfigurationFacade;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @MockitoBean
    private ProjectFacade projectFacade;

    @MockitoBean
    private ProjectGitConfigurationService projectGitConfigurationService;

    @Autowired
    private ProjectGitFacade projectGitFacade;

    @MockitoBean
    private ProjectGitService projectGitService;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private ProjectWorkflowFacade projectWorkflowFacade;

    @MockitoBean
    private ProjectWorkflowService projectWorkflowService;

    @MockitoBean
    private WorkflowService workflowService;

    @MockitoBean
    private WorkspaceService workspaceService;

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin@localhost.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testPushRecordsTheProjectAndTheCommitWithoutTheCredentials() {
        stubRepository();

        when(projectService.getProject(PROJECT_ID)).thenReturn(new Project());
        when(projectWorkflowService.getProjectWorkflows(anyLong(), anyInt())).thenReturn(List.of());
        when(projectGitService.save(any(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(COMMIT_HASH);

        projectGitFacade.pushProjectToGit(PROJECT_ID, "confidential message");

        assertThat(findSingleEvent(ProjectGitAuditEvents.PROJECT_GIT_PUSHED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("projectId", String.valueOf(PROJECT_ID))
            .containsEntry("commitId", COMMIT_HASH)
            .doesNotContainValue("confidential message")
            .doesNotContainValue(URL)
            .doesNotContainValue(PASSWORD);
    }

    @Test
    void testPullRecordsTheProject() {
        stubRepository();

        when(projectGitService.getWorkflows(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(new GitWorkflows(List.of(), null));
        when(projectService.getProject(PROJECT_ID)).thenReturn(new Project());
        when(projectWorkflowService.getProjectWorkflows(anyLong(), anyInt())).thenReturn(List.of());

        projectGitFacade.pullProjectFromGit(PROJECT_ID);

        assertThat(findSingleEvent(ProjectGitAuditEvents.PROJECT_GIT_PULLED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("projectId", String.valueOf(PROJECT_ID))
            .doesNotContainValue(URL);
    }

    @Test
    void testPullWithoutAGitConfigurationRecordsAnError() {
        when(workspaceService.getProjectWorkspace(PROJECT_ID)).thenReturn(new Workspace(WORKSPACE_ID, "workspace"));
        when(gitConfigurationFacade.getGitConfiguration(WORKSPACE_ID))
            .thenThrow(new RuntimeException("Git configuration not found"));

        assertThatThrownBy(() -> projectGitFacade.pullProjectFromGit(PROJECT_ID))
            .isInstanceOf(RuntimeException.class);

        assertThat(findSingleEvent(ProjectGitAuditEvents.PROJECT_GIT_PULLED))
            .containsEntry("result", "ERROR")
            .containsEntry("projectId", String.valueOf(PROJECT_ID))
            .containsEntry("errorClass", RuntimeException.class.getName());
    }

    @Test
    void testSaveConfigurationRecordsTheBranchAndTheSyncFlag() {
        ProjectGitConfiguration projectGitConfiguration = new ProjectGitConfiguration();

        projectGitConfiguration.setBranch(BRANCH);
        projectGitConfiguration.setEnabled(true);
        projectGitConfiguration.setProjectId(PROJECT_ID);

        projectGitFacade.saveProjectGitConfiguration(projectGitConfiguration);

        assertThat(findSingleEvent(ProjectGitAuditEvents.PROJECT_GIT_CONFIGURATION_SAVED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("projectId", String.valueOf(PROJECT_ID))
            .containsEntry("branch", BRANCH)
            .containsEntry("enabled", "true");
    }

    private void stubRepository() {
        ProjectGitConfiguration projectGitConfiguration = new ProjectGitConfiguration();

        projectGitConfiguration.setBranch(BRANCH);
        projectGitConfiguration.setProjectId(PROJECT_ID);

        when(workspaceService.getProjectWorkspace(PROJECT_ID)).thenReturn(new Workspace(WORKSPACE_ID, "workspace"));
        when(gitConfigurationFacade.getGitConfiguration(WORKSPACE_ID))
            .thenReturn(new GitConfigurationDTO(URL, "git-user", PASSWORD));
        when(projectGitConfigurationService.getProjectGitConfiguration(PROJECT_ID))
            .thenReturn(projectGitConfiguration);
    }

    private Map<String, String> findSingleEvent(String eventType) {
        Page<PersistentAuditEvent> page =
            persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> pageContent = page.getContent();

        List<PersistentAuditEvent> persistentAuditEvents = pageContent.stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .toList();

        assertThat(persistentAuditEvents).hasSize(1);

        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.getFirst();

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("admin@localhost.com");

        return persistentAuditEvent.getData();
    }

    @Configuration
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class, AuditMapperResolver.class
    })
    static class AuditConfiguration {

        @Bean
        ProjectGitFacade projectGitFacade(
            GitConfigurationFacade gitConfigurationFacade, ProjectFacade projectFacade,
            ProjectGitConfigurationService projectGitConfigurationService, ProjectGitService projectGitService,
            ProjectService projectService, ProjectWorkflowFacade projectWorkflowFacade,
            ProjectWorkflowService projectWorkflowService, WorkflowService workflowService,
            WorkspaceService workspaceService) {

            return new ProjectGitFacadeImpl(
                gitConfigurationFacade, projectFacade, projectGitConfigurationService, projectGitService,
                projectService, projectWorkflowFacade, projectWorkflowService, workflowService, workspaceService);
        }
    }
}
