/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.client.config;

import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.automation.configuration.instance.accessor.ProjectDeploymentJobPrincipalAccessor;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowServiceImpl;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.Authorization.RefreshTokenResponse;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionImpl;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.aspect.TokenRefreshHandler;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.context.ContextFactoryImpl;
import com.bytechef.platform.component.log.EditorLogFileStorage;
import com.bytechef.platform.component.log.LogFileStorage;
import com.bytechef.platform.component.log.TriggerLogFileStorage;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionServiceImpl;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationConnectionRepository;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationRepository;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationServiceImpl;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.facade.ConnectionFacadeImpl;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.connection.service.ConnectionServiceImpl;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.file.storage.EditorTempFileStorageImpl;
import com.bytechef.platform.file.storage.TempFileStorageImpl;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.oauth2.service.OAuth2ServiceImpl;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.tag.service.TagServiceImpl;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.tracing.Tracer;
import java.util.List;
import java.util.Map;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@EnableAutoConfiguration
@Import({
    LiquibaseConfiguration.class, ConnectionRemoteClientIntTestConfiguration.ConnectionJdbcConfiguration.class,
    PostgreSQLContainerConfiguration.class
})
@SpringBootConfiguration
public class ConnectionRemoteClientIntTestConfiguration {

    public static final String ACCESS_TOKEN_PREFIX = "access-for-";
    public static final String BASE_URI = "https://api.grpc-test.example.com";
    public static final String COMPONENT_NAME = "grpcTestConnection";
    public static final String REDIRECT_URI = "https://bytechef.example.com/callback";
    public static final String REFRESHED_ACCESS_TOKEN = "refreshed-access-token";
    public static final int REFRESHED_EXPIRES_IN = 7200;

    @Bean
    ApplicationProperties applicationProperties() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Oauth2 oauth2 = applicationProperties.getOauth2();

        oauth2.setRedirectUri(REDIRECT_URI);

        return applicationProperties;
    }

    @Bean
    CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }

    @Bean
    ComponentDefinitionRegistry componentDefinitionRegistry(ApplicationProperties applicationProperties) {
        return new ComponentDefinitionRegistry(
            applicationProperties, List.of(new GrpcTestConnectionComponentHandler()), List::of, List.of());
    }

    @Bean
    ConnectionDefinitionService connectionDefinitionService(
        ComponentDefinitionRegistry componentDefinitionRegistry, ContextFactory contextFactory,
        @Lazy TokenRefreshHandler tokenRefreshHandler) {

        return new ConnectionDefinitionServiceImpl(componentDefinitionRegistry, contextFactory, tokenRefreshHandler);
    }

    @Bean
    ConnectionFacade connectionFacade(
        ConnectionDefinitionService connectionDefinitionService, ConnectionService connectionService,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, OAuth2Service oAuth2Service, TagService tagService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        return new ConnectionFacadeImpl(
            connectionDefinitionService, connectionService, jobPrincipalAccessorRegistry, oAuth2Service, tagService,
            workflowTestConfigurationService);
    }

    @Bean
    ConnectionService connectionService(ConnectionRepository connectionRepository) {
        return new ConnectionServiceImpl(connectionRepository);
    }

    @Bean
    ContextFactory contextFactory(
        ApplicationContext applicationContext, ApplicationProperties applicationProperties, CacheManager cacheManager,
        ObjectProvider<ApprovalTokens> approvalTokensProvider) {

        Base64FileStorageService base64FileStorageService = new Base64FileStorageService();

        return new ContextFactoryImpl(
            applicationContext, applicationProperties, cacheManager, Mockito.mock(DataStorage.class),
            Mockito.mock(EditorLogFileStorage.class), new EditorTempFileStorageImpl(base64FileStorageService),
            applicationContext, approvalTokensProvider, Mockito.mock(LogFileStorage.class),
            new TempFileStorageImpl(base64FileStorageService), Mockito.mock(Tracer.class),
            Mockito.mock(TriggerLogFileStorage.class));
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
    JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry(
        ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository) {

        return new JobPrincipalAccessorRegistry(
            List.of(
                new ProjectDeploymentJobPrincipalAccessor(
                    Mockito.mock(ProjectDeploymentService.class),
                    new ProjectDeploymentWorkflowServiceImpl(projectDeploymentWorkflowRepository),
                    Mockito.mock(ProjectService.class), Mockito.mock(ProjectWorkflowService.class))));
    }

    @Bean
    OAuth2Service oAuth2Service(ApplicationProperties applicationProperties) {
        return new OAuth2ServiceImpl(applicationProperties);
    }

    @Bean
    TagService tagService(TagRepository tagRepository) {
        return new TagServiceImpl(tagRepository);
    }

    @Bean
    TokenRefreshHandler tokenRefreshHandler(
        CacheManager cacheManager, @Lazy ConnectionDefinitionService connectionDefinitionService,
        ConnectionService connectionService) {

        return new TokenRefreshHandler(cacheManager, connectionDefinitionService, connectionService);
    }

    @Bean
    WorkflowTestConfigurationService workflowTestConfigurationService(
        WorkflowTestConfigurationRepository workflowTestConfigurationRepository,
        WorkflowTestConfigurationConnectionRepository workflowTestConfigurationConnectionRepository) {

        return new WorkflowTestConfigurationServiceImpl(
            workflowTestConfigurationRepository, workflowTestConfigurationConnectionRepository);
    }

    public static class GrpcTestConnectionComponentHandler implements ComponentHandler {

        private static final ComponentDefinition COMPONENT_DEFINITION = component(COMPONENT_NAME)
            .title("gRPC Test Connection")
            .connection(
                connection()
                    .baseUri((connectionParameters, context) -> BASE_URI)
                    .properties(string("region"))
                    .authorizations(
                        authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                            .title("OAuth2 Authorization Code")
                            .properties(string(Authorization.CLIENT_ID), string(Authorization.CLIENT_SECRET))
                            .authorizationCallback(
                                (
                                    connectionParameters, code, redirectUri, codeVerifier,
                                    context) -> new AuthorizationCallbackResponse(
                                        Map.of(
                                            Authorization.ACCESS_TOKEN, ACCESS_TOKEN_PREFIX + code,
                                            Authorization.REFRESH_TOKEN, "refresh-token",
                                            "redirect_uri_used", redirectUri)))
                            .refresh(
                                (connectionParameters, context) -> new RefreshTokenResponse(
                                    REFRESHED_ACCESS_TOKEN, "refreshed-refresh-token", REFRESHED_EXPIRES_IN))));

        @Override
        public ComponentDefinition getDefinition() {
            return COMPONENT_DEFINITION;
        }
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    static class ConnectionJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final Encryption encryption;
        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        ConnectionJdbcConfiguration(Encryption encryption, ObjectMapper objectMapper) {
            this.encryption = encryption;
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return List.of(
                new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
                new EncryptedStringToMapWrapperConverter(encryption, objectMapper),
                new MapWrapperToStringConverter(objectMapper), new StringToMapWrapperConverter(objectMapper));
        }
    }
}
