/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.repository.AppInstanceRepository;
import com.bytechef.ee.automation.app.repository.AppRepository;
import com.bytechef.ee.automation.app.service.AppInstanceServiceImpl;
import com.bytechef.ee.automation.app.service.AppServiceImpl;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionImpl;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.facade.ConnectionFacadeImpl;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.connection.service.ConnectionServiceImpl;
import com.bytechef.platform.connection.usage.ConnectionUsageChecker;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

/**
 * Runs {@link AppDeploymentConnectionUsageChecker} against real {@code app}, {@code app_instance} and
 * {@code connection} rows, wired into a real {@link ConnectionFacadeImpl} through the context's
 * {@link ConnectionUsageChecker} provider, as {@code AppConnectionUsageCheckerIntTest} does for bindings.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(
    classes = AppDeploymentConnectionUsageCheckerIntTest.AppDeploymentConnectionUsageCheckerIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class AppDeploymentConnectionUsageCheckerIntTest {

    @Autowired
    private AppDeploymentConnectionUsageChecker appDeploymentConnectionUsageChecker;

    @Autowired
    private AppInstanceRepository appInstanceRepository;

    @Autowired
    private AppRepository appRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private ObjectProvider<ConnectionUsageChecker> connectionUsageCheckerProvider;

    private ConnectionFacade connectionFacade;

    @BeforeEach
    void beforeEach() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        JobPrincipalAccessor jobPrincipalAccessor = mock(JobPrincipalAccessor.class);

        when(jobPrincipalAccessor.getType()).thenReturn(PlatformType.AUTOMATION);

        connectionFacade = new ConnectionFacadeImpl(
            mock(ConnectionDefinitionService.class), emptyProvider(), connectionService, "EE",
            new JobPrincipalAccessorRegistry(List.of(jobPrincipalAccessor)), mock(OAuth2Service.class),
            mock(TagService.class), mock(WorkflowTestConfigurationService.class), emptyProvider(),
            connectionUsageCheckerProvider);
    }

    @AfterEach
    void afterEach() {
        appRepository.deleteAll();
        connectionRepository.deleteAll();

        SecurityContextHolder.clearContext();
    }

    @Test
    void testTheCheckerIsTheRegisteredConnectionUsageChecker() {
        assertThat(connectionUsageCheckerProvider.orderedStream()
            .toList()).containsExactly(appDeploymentConnectionUsageChecker);
    }

    @Test
    void testDescribeUsagesNamesEveryInstanceDeployingWithTheConnection() {
        long connectionId = saveConnection("vercel");

        saveAppInstance("crm-dashboard", "Production", connectionId);
        saveAppInstance("support-portal", "Staging", connectionId);
        saveAppInstance("billing", "Production", null);

        assertThat(appDeploymentConnectionUsageChecker.describeUsages(connectionId))
            .containsExactlyInAnyOrder(
                "app 'crm-dashboard' instance 'Production' (deployment)",
                "app 'support-portal' instance 'Staging' (deployment)");
    }

    @Test
    void testDeploymentConnectionCannotBeDeleted() {
        long connectionId = saveConnection("vercel");

        saveAppInstance("crm-dashboard", "Production", connectionId);

        assertThatThrownBy(() -> connectionFacade.delete(connectionId))
            .isInstanceOfSatisfying(ConfigurationException.class, exception -> assertThat(
                exception.getErrorKey()).isEqualTo(ConnectionErrorType.CONNECTION_IS_USED.getErrorKey()))
            .hasMessageContaining("app 'crm-dashboard' instance 'Production' (deployment)");

        assertThat(connectionRepository.findById(connectionId)).isPresent();
    }

    @Test
    void testConnectionNoInstanceDeploysWithCanBeDeleted() {
        long connectionId = saveConnection("vercel");

        saveAppInstance("crm-dashboard", "Production", null);

        connectionFacade.delete(connectionId);

        assertThat(connectionRepository.findById(connectionId)).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        ObjectProvider<T> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(null);

        return objectProvider;
    }

    private long saveConnection(String componentName) {
        Connection connection = new Connection();

        connection.setComponentName(componentName);
        connection.setConnectionVersion(1);
        connection.setEnvironmentId(Environment.PRODUCTION.ordinal());
        connection.setName(componentName + " connection");
        connection.setParameters(Map.of());
        connection.setType(PlatformType.AUTOMATION);

        return Objects.requireNonNull(connectionRepository.save(connection)
            .getId());
    }

    private void saveAppInstance(String appName, String appInstanceName, @Nullable Long deploymentConnectionId) {
        App app = new App();

        app.setUuid(UUID.randomUUID()
            .toString());
        app.setName(appName);
        app.setWorkspaceId(1L);
        app.setOwnerUserId(1L);
        app.setStatus(AppStatus.ACTIVE);
        app.setVisibility(ResourceVisibility.WORKSPACE);

        App savedApp = appRepository.save(app);

        AppInstance appInstance = new AppInstance();

        String secretKeyHash = UUID.randomUUID()
            .toString()
            .replace("-", "")
            .repeat(2);

        appInstance.setUuid(UUID.randomUUID()
            .toString());
        appInstance.setAppId(savedApp.getId());
        appInstance.setName(appInstanceName);
        appInstance.setWorkspaceId(1L);
        appInstance.setOwnerUserId(1L);
        appInstance.setEnvironment(Environment.PRODUCTION);
        appInstance.setStatus(AppInstanceStatus.ACTIVE);
        appInstance.setHostingTarget(AppHostingTarget.VERCEL);
        appInstance.setDeploymentConnectionId(deploymentConnectionId);
        appInstance.setDeploymentSettings(Map.of("projectId", "prj_1"));
        appInstance.setSecretKeyHash(secretKeyHash);
        appInstance.setSecretKeyHint(secretKeyHash.substring(0, 8));

        appInstanceRepository.save(appInstance);
    }

    @EnableAutoConfiguration
    @Import({
        AppDeploymentConnectionUsageChecker.class, AppInstanceServiceImpl.class,
        AppDeploymentConnectionUsageCheckerIntTestJdbcConfiguration.class, AppServiceImpl.class,
        ConnectionServiceImpl.class, EncryptionImpl.class, JacksonConfiguration.class, LiquibaseConfiguration.class
    })
    static class AppDeploymentConnectionUsageCheckerIntTestConfiguration {

        @Bean
        EncryptionKey encryptionKey() {
            return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
        }

        @Bean
        WorkspaceConnectionService workspaceConnectionService() {
            return mock(WorkspaceConnectionService.class);
        }
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    static class AppDeploymentConnectionUsageCheckerIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final Encryption encryption;
        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        AppDeploymentConnectionUsageCheckerIntTestJdbcConfiguration(Encryption encryption, ObjectMapper objectMapper) {
            this.encryption = encryption;
            this.objectMapper = objectMapper;
        }

        @Override
        protected @NonNull List<?> userConverters() {
            return List.of(
                new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
                new EncryptedStringToMapWrapperConverter(encryption, objectMapper),
                new FileEntryToStringConverter(objectMapper), new MapWrapperToStringConverter(objectMapper),
                new StringToFileEntryConverter(objectMapper), new StringToMapWrapperConverter(objectMapper));
        }
    }
}
