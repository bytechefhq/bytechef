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
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppConnection;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.repository.AppConnectionRepository;
import com.bytechef.ee.automation.app.repository.AppRepository;
import com.bytechef.ee.automation.app.service.AppConnectionServiceImpl;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
 * Runs {@link AppConnectionUsageChecker} against real {@code app}, {@code app_connection} and {@code connection} rows,
 * and wires it into a real {@link ConnectionFacadeImpl} over a real {@link ConnectionServiceImpl} the way production
 * does: through the context's {@link ConnectionUsageChecker} provider, not a hand-built list. A refused delete is
 * checked by the connection row still being there.
 *
 * <p>
 * This class assembles its own context rather than reusing {@code AppIntTestConfiguration}: reading a
 * {@link Connection} through Spring Data JDBC needs the encrypted-parameters converters, which that configuration's
 * JDBC setup does not register (it stubs {@code ConnectionService}). The configuration below is deliberately a plain
 * class, not a {@code @Configuration}, so that {@code AppIntTestConfiguration}'s component scan of
 * {@code com.bytechef.ee.automation.app} never picks it up.
 *
 * <p>
 * The facade is constructed in the test rather than scanned: its other collaborators (OAuth2, tags, workflow test
 * configuration, job principal accessors) live in modules this one does not carry, and none of them is reached on the
 * delete path except the two "used by workflows" checks, which are stubbed to report the connection unused so a refusal
 * can only come from the app binding.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppConnectionUsageCheckerIntTest.AppConnectionUsageCheckerIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class AppConnectionUsageCheckerIntTest {

    @Autowired
    private AppConnectionRepository appConnectionRepository;

    @Autowired
    private AppConnectionUsageChecker appConnectionUsageChecker;

    @Autowired
    private AppRepository appRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private ObjectProvider<ConnectionUsageChecker> connectionUsageCheckerProvider;

    @AfterEach
    void afterEach() {
        appRepository.deleteAll();
        connectionRepository.deleteAll();

        SecurityContextHolder.clearContext();
    }

    @Nested
    class DescribeUsages {

        @Test
        void testDescribeUsagesNamesEveryBindingOfTheConnection() {
            long connectionId = saveConnection("slack");

            saveBinding(saveApp("crm-dashboard", AppStatus.ENABLED), connectionId, "sales-slack");
            saveBinding(saveApp("support-portal", AppStatus.ENABLED), connectionId, "alerts");

            assertThat(appConnectionUsageChecker.describeUsages(connectionId))
                .containsExactlyInAnyOrder(
                    "app 'crm-dashboard' (binding 'sales-slack')", "app 'support-portal' (binding 'alerts')");
        }

        @Test
        void testDescribeUsagesCountsDisabledApps() {
            long connectionId = saveConnection("slack");

            saveBinding(saveApp("crm-dashboard", AppStatus.DISABLED), connectionId, "crm");

            assertThat(appConnectionUsageChecker.describeUsages(connectionId))
                .containsExactly("app 'crm-dashboard' (binding 'crm')");
        }

        @Test
        void testDescribeUsagesIsEmptyForUnboundConnection() {
            long boundConnectionId = saveConnection("slack");
            long unboundConnectionId = saveConnection("github");

            saveBinding(saveApp("crm-dashboard", AppStatus.ENABLED), boundConnectionId, "crm");

            assertThat(appConnectionUsageChecker.describeUsages(unboundConnectionId)).isEmpty();
        }

        @Test
        void testDescribeUsagesIsEmptyOnceTheAppIsDeleted() {
            long connectionId = saveConnection("slack");

            App app = saveApp("crm-dashboard", AppStatus.ENABLED);

            saveBinding(app, connectionId, "crm");

            appRepository.deleteById(Objects.requireNonNull(app.getId()));

            assertThat(appConnectionUsageChecker.describeUsages(connectionId)).isEmpty();
        }
    }

    @Nested
    class ConnectionDelete {

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

        @Test
        void testTheRealCheckerIsTheRegisteredConnectionUsageChecker() {
            assertThat(connectionUsageCheckerProvider.orderedStream()
                .toList()).containsExactly(appConnectionUsageChecker);
        }

        @Test
        void testDeleteIsRefusedWhileAnAppBindsTheConnection() {
            long connectionId = saveConnection("slack");

            saveBinding(saveApp("crm-dashboard", AppStatus.ENABLED), connectionId, "sales-slack");

            assertThatThrownBy(() -> connectionFacade.delete(connectionId))
                .isInstanceOfSatisfying(ConfigurationException.class, exception -> assertThat(
                    exception.getErrorKey()).isEqualTo(ConnectionErrorType.CONNECTION_IS_USED.getErrorKey()))
                .hasMessageContaining("app 'crm-dashboard' (binding 'sales-slack')");

            assertThat(connectionRepository.findById(connectionId)).isPresent();
        }

        @Test
        void testDeleteIsRefusedForADisabledApp() {
            long connectionId = saveConnection("slack");

            saveBinding(saveApp("crm-dashboard", AppStatus.DISABLED), connectionId, "crm");

            assertThatThrownBy(() -> connectionFacade.delete(connectionId))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("app 'crm-dashboard' (binding 'crm')");

            assertThat(connectionRepository.findById(connectionId)).isPresent();
        }

        @Test
        void testDeleteSucceedsOnceTheBindingIsGone() {
            long connectionId = saveConnection("slack");

            AppConnection binding = saveBinding(saveApp("crm-dashboard", AppStatus.ENABLED), connectionId, "crm");

            appConnectionRepository.deleteById(Objects.requireNonNull(binding.getId()));

            connectionFacade.delete(connectionId);

            assertThat(connectionRepository.findById(connectionId)).isEmpty();
        }

        @SuppressWarnings("unchecked")
        private static <T> ObjectProvider<T> emptyProvider() {
            ObjectProvider<T> objectProvider = mock(ObjectProvider.class);

            when(objectProvider.getIfAvailable()).thenReturn(null);

            return objectProvider;
        }
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

    private App saveApp(String name, AppStatus status) {
        App app = new App();

        String secretKeyHash = UUID.randomUUID()
            .toString()
            .replace("-", "")
            .repeat(2);

        app.setUuid(UUID.randomUUID()
            .toString());
        app.setName(name);
        app.setWorkspaceId(1L);
        app.setEnvironment(Environment.PRODUCTION);
        app.setOwnerUserId(1L);
        app.setStatus(status);
        app.setVisibility(ResourceVisibility.WORKSPACE);
        app.setHostingTarget(AppHostingTarget.VERCEL);
        app.setSecretKeyHash(secretKeyHash);
        app.setSecretKeyHint(secretKeyHash.substring(0, 8));

        return appRepository.save(app);
    }

    private AppConnection saveBinding(App app, long connectionId, String bindingKey) {
        AppConnection appConnection = new AppConnection();

        appConnection.setAppId(app.getId());
        appConnection.setConnectionId(connectionId);
        appConnection.setBindingKey(bindingKey);

        return appConnectionRepository.save(appConnection);
    }

    /**
     * The real app services and checker, the real connection service, and the converters that let Spring Data JDBC read
     * a connection's encrypted parameters. Only the workspace-connection service (which the checker never reaches) are
     * stubbed.
     */
    @EnableAutoConfiguration
    @Import({
        AppConnectionServiceImpl.class, AppConnectionUsageChecker.class,
        AppConnectionUsageCheckerIntTestJdbcConfiguration.class,
        AppServiceImpl.class, ConnectionServiceImpl.class, EncryptionImpl.class, JacksonConfiguration.class,
        LiquibaseConfiguration.class
    })
    static class AppConnectionUsageCheckerIntTestConfiguration {

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
    static class AppConnectionUsageCheckerIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final Encryption encryption;
        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        AppConnectionUsageCheckerIntTestJdbcConfiguration(Encryption encryption, ObjectMapper objectMapper) {
            this.encryption = encryption;
            this.objectMapper = objectMapper;
        }

        @Override
        protected @NonNull List<?> userConverters() {
            return List.of(
                new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
                new EncryptedStringToMapWrapperConverter(encryption, objectMapper));
        }
    }
}
