/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.config;

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.facade.JobFacadeImpl;
import com.bytechef.atlas.execution.repository.ContextRepository;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToWebhooksConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.WebhooksToStringConverter;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.ContextServiceImpl;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.automation.ai.mcp.repository.WorkspaceMcpServerRepository;
import com.bytechef.automation.ai.mcp.security.McpServerOwnershipResolver;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerServiceImpl;
import com.bytechef.automation.configuration.callback.ProjectCallback;
import com.bytechef.automation.configuration.callback.ProjectWorkflowCallback;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectFacadeImpl;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.service.PreBuiltTemplateService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentServiceImpl;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowServiceImpl;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectServiceImpl;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowServiceImpl;
import com.bytechef.automation.configuration.service.SharedTemplateService;
import com.bytechef.automation.configuration.util.ComponentDefinitionHelper;
import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.commons.data.jdbc.converter.ExecutionErrorToStringConverter;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToExecutionErrorConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.configuration.service.CurrentUserResolver;
import com.bytechef.ee.automation.configuration.service.PermissionScopeRegistry;
import com.bytechef.ee.automation.configuration.service.PermissionServiceImpl;
import com.bytechef.ee.automation.configuration.service.WorkspaceScopeCacheService;
import com.bytechef.ee.platform.user.remote.client.service.RemoteUserServiceClient;
import com.bytechef.ee.platform.user.remote.grpc.UserRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionImpl;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.connection.service.ConnectionServiceImpl;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.user.callback.UserCallback;
import com.bytechef.platform.user.repository.AuthorityRepository;
import com.bytechef.platform.user.repository.PersistentTokenRepository;
import com.bytechef.platform.user.repository.UserRepository;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.user.service.UserServiceImpl;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacadeImpl;
import com.bytechef.platform.workflow.execution.repository.PrincipalJobRepository;
import com.bytechef.platform.workflow.execution.repository.converter.JobResumeIdToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToJobResumeIdConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToWorkflowExecutionIdConverter;
import com.bytechef.platform.workflow.execution.repository.converter.WorkflowExecutionIdToStringConverter;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.PrincipalJobServiceImpl;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.mockito.Mockito;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.automation.configuration.subflow", "com.bytechef.ee.automation.configuration.security.scope"
    },
    useDefaultFilters = false,
    includeFilters = {
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = {
                "com\\.bytechef\\.automation\\.configuration\\.subflow\\.ChildJobPrincipalFactoryImpl",
                "com\\.bytechef\\.automation\\.configuration\\.subflow\\.SubflowResolverImpl",
                "com\\.bytechef\\.ee\\.automation\\.configuration\\.security\\.scope\\..*PermissionScopeProvider"
            })
    })
@EnableAutoConfiguration
@Import({
    AutomationConfigurationIntTestData.class, JacksonConfiguration.class, LiquibaseConfiguration.class,
    PermissionScopeRegistry.class, PermissionServiceImpl.class, PostgreSQLContainerConfiguration.class,
    ProjectCallback.class, ProjectWorkflowCallback.class, UserCallback.class, WorkspaceScopeCacheService.class,
    AutomationConfigurationRemoteClientIntTestConfiguration.AutomationConfigurationJdbcConfiguration.class
})
@SpringBootConfiguration
public class AutomationConfigurationRemoteClientIntTestConfiguration {

    private static final String USER_GRPC_SERVER_NAME = "automation-configuration-user-" + UUID.randomUUID();

    @Bean
    CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }

    @Bean
    ConnectionService connectionService(ConnectionRepository connectionRepository) {
        return new ConnectionServiceImpl(connectionRepository);
    }

    @Bean
    ContextService contextService(ContextRepository contextRepository) {
        return new ContextServiceImpl(contextRepository);
    }

    @Bean
    CurrentUserResolver currentUserResolver(ManagedChannel userManagedChannel) {
        return new CurrentUserResolver(new RemoteUserServiceClient(new FixedGrpcChannels(userManagedChannel)));
    }

    @Bean
    Encryption encryption(EncryptionKey encryptionKey) {
        return new EncryptionImpl(encryptionKey);
    }

    @Bean
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
    }

    @Bean
    JobFacade jobFacade(
        ApplicationEventPublisher applicationEventPublisher, ContextService contextService, JobService jobService,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage, WorkflowService workflowService) {

        return new JobFacadeImpl(
            applicationEventPublisher, contextService, jobService, taskExecutionService, taskFileStorage,
            workflowService);
    }

    @Bean
    JobService jobService(JobRepository jobRepository) {
        return new JobServiceImpl(jobRepository);
    }

    @Bean
    McpServerOwnershipResolver mcpServerOwnershipResolver(
        WorkspaceMcpServerRepository workspaceMcpServerRepository) {

        return new McpServerOwnershipResolver(new WorkspaceMcpServerServiceImpl(workspaceMcpServerRepository));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    PrincipalJobFacade principalJobFacade(
        JobFacade jobFacade, JobService jobService, PrincipalJobService principalJobService,
        WorkflowService workflowService) {

        return new PrincipalJobFacadeImpl(principalJobService, jobFacade, jobService, workflowService);
    }

    @Bean
    PrincipalJobService principalJobService(PrincipalJobRepository principalJobRepository) {
        return new PrincipalJobServiceImpl(principalJobRepository);
    }

    @Bean
    ProjectDeploymentService projectDeploymentService(ProjectDeploymentRepository projectDeploymentRepository) {
        return new ProjectDeploymentServiceImpl(projectDeploymentRepository);
    }

    @Bean
    ProjectDeploymentWorkflowService projectDeploymentWorkflowService(
        ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository) {

        return new ProjectDeploymentWorkflowServiceImpl(projectDeploymentWorkflowRepository);
    }

    @Bean
    ProjectFacade projectFacade(
        ProjectDeploymentService projectDeploymentService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {

        return new ProjectFacadeImpl(
            new ApplicationProperties(), Mockito.mock(CategoryService.class),
            Mockito.mock(ComponentDefinitionHelper.class), Mockito.mock(PreBuiltTemplateService.class),
            projectWorkflowService, projectDeploymentService, projectService,
            Mockito.mock(ProjectDeploymentFacade.class), Mockito.mock(ProjectWorkflowFacade.class),
            Mockito.mock(SharedTemplateFileStorage.class), Mockito.mock(SharedTemplateService.class),
            Mockito.mock(TagService.class), workflowService, Mockito.mock(WorkflowTestConfigurationService.class),
            Mockito.mock(WorkflowNodeTestOutputService.class), List.of());
    }

    @Bean
    ProjectService projectService(ApplicationContext applicationContext, ProjectRepository projectRepository) {
        return new ProjectServiceImpl(applicationContext, projectRepository);
    }

    @Bean
    ProjectWorkflowService projectWorkflowService(ProjectWorkflowRepository projectWorkflowRepository) {
        return new ProjectWorkflowServiceImpl(projectWorkflowRepository);
    }

    @Bean
    TaskExecutionService taskExecutionService(TaskExecutionRepository taskExecutionRepository) {
        return new TaskExecutionServiceImpl(taskExecutionRepository);
    }

    @Bean
    TaskFileStorage taskFileStorage() {
        return new TaskFileStorageImpl(new Base64FileStorageService());
    }

    @Bean
    UserService userService(
        AuthorityRepository authorityRepository, CacheManager cacheManager, PasswordEncoder passwordEncoder,
        PersistentTokenRepository persistentTokenRepository, UserRepository userRepository) {

        return new UserServiceImpl(
            authorityRepository, cacheManager, passwordEncoder, persistentTokenRepository, userRepository);
    }

    @Bean(destroyMethod = "shutdownNow")
    Server userGrpcServer(UserService userService) throws IOException {
        return InProcessServerBuilder.forName(USER_GRPC_SERVER_NAME)
            .directExecutor()
            .addService(new UserRemoteServiceGrpcServer(userService))
            .build()
            .start();
    }

    @Bean(destroyMethod = "shutdownNow")
    ManagedChannel userManagedChannel(Server userGrpcServer) {
        return InProcessChannelBuilder.forName(USER_GRPC_SERVER_NAME)
            .directExecutor()
            .build();
    }

    @Bean
    WorkflowService workflowService(
        CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
        List<WorkflowRepository> workflowRepositories) {

        return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    static class AutomationConfigurationJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final Encryption encryption;
        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        AutomationConfigurationJdbcConfiguration(Encryption encryption, ObjectMapper objectMapper) {
            this.encryption = encryption;
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return List.of(
                new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
                new EncryptedStringToMapWrapperConverter(encryption, objectMapper),
                new ExecutionErrorToStringConverter(objectMapper), new FileEntryToStringConverter(objectMapper),
                new JobResumeIdToStringConverter(), new MapWrapperToStringConverter(objectMapper),
                new StringToExecutionErrorConverter(objectMapper), new StringToFileEntryConverter(objectMapper),
                new StringToJobResumeIdConverter(), new StringToMapWrapperConverter(objectMapper),
                new StringToWebhooksConverter(objectMapper), new StringToWorkflowExecutionIdConverter(),
                new StringToWorkflowTaskConverter(objectMapper), new WebhooksToStringConverter(objectMapper),
                new WorkflowExecutionIdToStringConverter(), new WorkflowTaskToStringConverter(objectMapper));
        }
    }
}
