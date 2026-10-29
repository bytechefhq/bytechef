/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.callback.ProjectCallback;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectServiceImpl;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.ee.automation.configuration.repository.ProjectCodeWorkflowRepository;
import com.bytechef.ee.automation.configuration.repository.ProjectGitConfigurationRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowServiceImpl;
import com.bytechef.ee.automation.configuration.service.ProjectGitConfigurationService;
import com.bytechef.ee.automation.configuration.service.ProjectGitConfigurationServiceImpl;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacadeImpl;
import com.bytechef.ee.platform.codeworkflow.configuration.repository.CodeWorkflowContainerRepository;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerServiceImpl;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorageImpl;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectBeforeDeleteEventListenerIntTest.Config.class, properties = "bytechef.edition=ee")
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = WorkflowService.class)
class ProjectBeforeDeleteEventListenerIntTest {

    private static final String CODE_WORKFLOWS_DIR = "code_workflows";

    @Autowired
    private CodeWorkflowContainerFacade codeWorkflowContainerFacade;

    @Autowired
    private CodeWorkflowContainerRepository codeWorkflowContainerRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ProjectCodeWorkflowService projectCodeWorkflowService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Test
    void testDeletingAProjectDeletesItsCodeWorkflowsContainersAndFiles() {
        Workspace workspace = workspaceRepository.save(new Workspace("Code Workflows"));

        Project project = new Project();

        project.setName("Uploaded Code Workflow Project");
        project.setWorkspaceId(workspace.getId());

        project = projectService.create(project);

        CodeWorkflowContainer codeWorkflowContainer = codeWorkflowContainerFacade.create(
            "container", "1", List.of(), Language.JAVASCRIPT,
            "export default {}".getBytes(StandardCharsets.UTF_8), PlatformType.AUTOMATION);

        projectCodeWorkflowService.create(codeWorkflowContainer, project);

        FileEntry workflowsFileEntry = codeWorkflowContainer.getWorkflows();

        assertThat(fileStorageService.fileExists(CODE_WORKFLOWS_DIR, workflowsFileEntry)).isTrue();

        projectService.delete(project.getId());

        assertThat(projectService.fetchProject(project.getId())).isEmpty();
        assertThat(projectCodeWorkflowService.getProjectCodeWorkflows(project.getId())).isEmpty();
        assertThat(codeWorkflowContainerRepository.findById(codeWorkflowContainer.getId())).isEmpty();
        assertThat(fileStorageService.fileExists(CODE_WORKFLOWS_DIR, workflowsFileEntry)).isFalse();
    }

    @EnableAutoConfiguration
    @Import({
        LiquibaseConfiguration.class, ProjectCallback.class,
        ProjectBeforeDeleteEventListenerIntTest.Config.ProjectBeforeDeleteEventListenerIntTestJdbcConfiguration.class
    })
    @SpringBootConfiguration
    static class Config {

        @Bean
        CodeWorkflowContainerFacade codeWorkflowContainerFacade(
            CodeWorkflowContainerService codeWorkflowContainerService, CodeWorkflowFileStorage codeWorkflowFileStorage,
            ObjectMapper objectMapper, WorkflowService workflowService) {

            return new CodeWorkflowContainerFacadeImpl(
                codeWorkflowContainerService, codeWorkflowFileStorage, objectMapper, workflowService);
        }

        @Bean
        CodeWorkflowContainerService codeWorkflowContainerService(
            CodeWorkflowContainerRepository codeWorkflowContainerRepository) {

            return new CodeWorkflowContainerServiceImpl(codeWorkflowContainerRepository);
        }

        @Bean
        CodeWorkflowFileStorage codeWorkflowFileStorage(FileStorageService fileStorageService) {
            return new CodeWorkflowFileStorageImpl(fileStorageService);
        }

        @Bean
        FileStorageService fileStorageService() throws IOException {
            return new FilesystemFileStorageService(
                String.valueOf(Files.createTempDirectory("project-before-delete-event-listener-int-test")));
        }

        @Bean
        ProjectBeforeDeleteEventListener projectBeforeDeleteEventListener(
            CodeWorkflowContainerFacade codeWorkflowContainerFacade,
            ProjectCodeWorkflowService projectCodeWorkflowService,
            ProjectGitConfigurationService projectGitConfigurationService) {

            return new ProjectBeforeDeleteEventListener(
                codeWorkflowContainerFacade, projectCodeWorkflowService, projectGitConfigurationService);
        }

        @Bean
        ProjectCodeWorkflowService projectCodeWorkflowService(
            ProjectCodeWorkflowRepository projectCodeWorkflowRepository) {

            return new ProjectCodeWorkflowServiceImpl(projectCodeWorkflowRepository);
        }

        @Bean
        ProjectGitConfigurationService projectGitConfigurationService(
            ProjectGitConfigurationRepository projectGitConfigurationRepository) {

            return new ProjectGitConfigurationServiceImpl(projectGitConfigurationRepository);
        }

        @Bean
        ProjectService projectService(ApplicationContext applicationContext, ProjectRepository projectRepository) {
            return new ProjectServiceImpl(applicationContext, projectRepository);
        }

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        static class ProjectBeforeDeleteEventListenerIntTestJdbcConfiguration
            extends AbstractIntTestJdbcConfiguration {

            private final ObjectMapper objectMapper;

            @SuppressFBWarnings("EI2")
            ProjectBeforeDeleteEventListenerIntTestJdbcConfiguration(ObjectMapper objectMapper) {
                this.objectMapper = objectMapper;
            }

            @Override
            protected List<?> userConverters() {
                return List.of(
                    new FileEntryToStringConverter(objectMapper), new MapWrapperToStringConverter(objectMapper),
                    new StringToFileEntryConverter(objectMapper), new StringToMapWrapperConverter(objectMapper));
            }
        }
    }
}
