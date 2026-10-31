/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;

import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.config.ConnectedUserIntTestConfiguration;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.dto.ConnectedUserDTO;
import com.bytechef.ee.embedded.connected.user.dto.ConnectedUserImportResultDTO;
import com.bytechef.ee.embedded.connected.user.dto.ConnectedUserImportResultDTO.RejectedRow;
import com.bytechef.ee.embedded.connected.user.repository.ConnectedUserRepository;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        ConnectedUserIntTestConfiguration.class, ConnectedUserFacadeImpl.class
    },
    properties = {
        "bytechef.edition=ee", "spring.liquibase.contexts=user"
    })
@Import(PostgreSQLContainerConfiguration.class)
class ConnectedUserFacadeIntTest {

    @Autowired
    private ConnectedUserFacade connectedUserFacade;

    @Autowired
    private ConnectedUserRepository connectedUserRepository;

    @MockitoSpyBean
    private ConnectedUserService connectedUserService;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @MockitoBean
    private ConnectedUserConnectionService connectedUserConnectionService;

    @MockitoBean
    private ConnectedUserProjectFacade connectedUserProjectFacade;

    @MockitoBean
    private ConnectionFacade connectionFacade;

    @MockitoBean
    private ConnectionService connectionService;

    @MockitoBean
    private EnvironmentService environmentService;

    @MockitoBean
    private IntegrationInstanceFacade integrationInstanceFacade;

    @MockitoBean
    private IntegrationInstanceService integrationInstanceService;

    @MockitoBean
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @MockitoBean
    private IntegrationService integrationService;

    @AfterEach
    void afterEach() {
        connectedUserRepository.deleteAll();
    }

    @Test
    void testImportCreatesSkipsAndRejectsRowsInOrder() {
        Environment environment = Environment.DEVELOPMENT;

        connectedUserService.createConnectedUser("existing", environment, "Existing", null, Map.of());

        ConnectedUserImportResultDTO result = connectedUserFacade.importConnectedUsers(
            environment, csv("externalId,name,plan\na,A,pro\nb,,\na,Again,x\n\" bad\",X,\nexisting,E,\n"));

        assertThat(result.createdCount()).isEqualTo(2);
        assertThat(result.skippedExternalIds()).containsExactly("a", "existing");
        assertThat(result.rejectedRows()).hasSize(1);

        RejectedRow rejectedRow = result.rejectedRows()
            .get(0);

        assertThat(rejectedRow.rowNumber()).isEqualTo(5);
        assertThat(rejectedRow.reason()).contains("whitespace");

        ConnectedUser connectedUserA = getConnectedUser("a", environment);

        assertThat(connectedUserA.getName()).isEqualTo("A");
        assertThat(connectedUserA.getMetadata()).isEqualTo(Map.of("plan", "pro"));

        ConnectedUser connectedUserB = getConnectedUser("b", environment);

        assertThat(connectedUserB.getName()).isNull();
        assertThat(connectedUserB.getMetadata()).isEmpty();

        ConnectedUser existingConnectedUser = getConnectedUser("existing", environment);

        assertThat(existingConnectedUser.getName()).isEqualTo("Existing");
        assertThat(connectedUserRepository.count()).isEqualTo(3);
    }

    @Test
    void testImportSkipsAnExternalIdRepeatedInTheSameFile() {
        Environment environment = Environment.DEVELOPMENT;

        ConnectedUserImportResultDTO result = connectedUserFacade.importConnectedUsers(
            environment, csv("externalId\nu1\nu1\n"));

        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.skippedExternalIds()).containsExactly("u1");
        assertThat(result.rejectedRows()).isEmpty();

        ConnectedUser connectedUser = getConnectedUser("u1", environment);

        assertThat(connectedUser.getName()).isNull();
        assertThat(connectedUser.getEmail()).isNull();
        assertThat(connectedUserRepository.count()).isEqualTo(1);
    }

    @Test
    void testImportReportsAServiceValidationFailureAsARejectedRow() {
        Environment environment = Environment.DEVELOPMENT;

        ConnectedUserImportResultDTO result = connectedUserFacade.importConnectedUsers(
            environment, csv("externalId,email\nu1,nope\nu2,\n"));

        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.rejectedRows()).containsExactly(
            new RejectedRow(2, "email 'nope' is not a valid email address"));

        assertThat(connectedUserService.fetchConnectedUser("u1", environment)).isEmpty();
        assertThat(connectedUserService.fetchConnectedUser("u2", environment)).isPresent();
    }

    @Test
    void testImportSkipsARowWhoseExternalIdIsCreatedConcurrently() {
        Environment environment = Environment.DEVELOPMENT;

        TransactionTemplate concurrentTransactionTemplate = new TransactionTemplate(platformTransactionManager);

        concurrentTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        doAnswer(invocation -> {
            concurrentTransactionTemplate.executeWithoutResult(
                transactionStatus -> connectedUserService.createConnectedUser(
                    "u1", environment, "Concurrent", null, Map.of()));

            return Optional.empty();
        }).doCallRealMethod()
            .when(connectedUserService)
            .fetchConnectedUser("u1", environment);

        ConnectedUserImportResultDTO result = connectedUserFacade.importConnectedUsers(
            environment, csv("externalId,name\nu1,Imported\n"));

        assertThat(result.createdCount()).isZero();
        assertThat(result.skippedExternalIds()).containsExactly("u1");
        assertThat(result.rejectedRows()).isEmpty();

        ConnectedUser connectedUser = getConnectedUser("u1", environment);

        assertThat(connectedUser.getName()).isEqualTo("Concurrent");
        assertThat(connectedUserRepository.count()).isEqualTo(1);
    }

    @Test
    void testImportDoesNotCountARejectedRowAsSeen() {
        Environment environment = Environment.DEVELOPMENT;

        ConnectedUserImportResultDTO result = connectedUserFacade.importConnectedUsers(
            environment, csv("externalId,email\nu1,bad-email\nu1,\n"));

        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.skippedExternalIds()).isEmpty();
        assertThat(result.rejectedRows()).containsExactly(
            new RejectedRow(2, "email 'bad-email' is not a valid email address"));

        ConnectedUser connectedUser = getConnectedUser("u1", environment);

        assertThat(connectedUser.getEmail()).isNull();
    }

    @Test
    void testImportRefusesTheWholeFileBeforeCreatingAnything() {
        assertThatThrownBy(
            () -> connectedUserFacade.importConnectedUsers(Environment.DEVELOPMENT, csv("name\nA\n")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(connectedUserRepository.count()).isZero();
    }

    @Test
    void testCreateAndUpdateReturnTheConnectedUserDTO() {
        ConnectedUserDTO createdConnectedUserDTO = connectedUserFacade.createConnectedUser(
            "u1", Environment.DEVELOPMENT, "N", null, Map.of());

        assertThat(createdConnectedUserDTO.id()).isNotNull();
        assertThat(createdConnectedUserDTO.externalId()).isEqualTo("u1");
        assertThat(createdConnectedUserDTO.name()).isEqualTo("N");
        assertThat(createdConnectedUserDTO.integrationInstances()).isEmpty();

        ConnectedUserDTO updatedConnectedUserDTO = connectedUserFacade.updateConnectedUser(
            createdConnectedUserDTO.id(), "N2", null, Map.of("a", "b"), createdConnectedUserDTO.version());

        assertThat(updatedConnectedUserDTO.id()).isEqualTo(createdConnectedUserDTO.id());
        assertThat(updatedConnectedUserDTO.name()).isEqualTo("N2");
        assertThat(updatedConnectedUserDTO.metadata()).isEqualTo(Map.of("a", "b"));

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(createdConnectedUserDTO.id());

        assertThat(connectedUser.getName()).isEqualTo("N2");
        assertThat(connectedUser.getMetadata()).isEqualTo(Map.of("a", "b"));
    }

    @Test
    void testEveryFacadeMethodRequiresTenantAdmin() throws NoSuchMethodException {
        Method[] facadeMethods = ConnectedUserFacade.class.getMethods();

        assertThat(facadeMethods).isNotEmpty();

        List<String> ungatedMethodNames = new ArrayList<>();

        for (Method facadeMethod : facadeMethods) {
            Method implementationMethod = ConnectedUserFacadeImpl.class.getDeclaredMethod(
                facadeMethod.getName(), facadeMethod.getParameterTypes());

            PreAuthorize preAuthorize = implementationMethod.getAnnotation(PreAuthorize.class);

            if (preAuthorize == null || !"isTenantAdmin()".equals(preAuthorize.value())) {
                ungatedMethodNames.add(implementationMethod.toGenericString());
            }
        }

        assertThat(ungatedMethodNames)
            .as("ConnectedUserFacadeImpl methods without @PreAuthorize(\"isTenantAdmin()\")")
            .isEmpty();
    }

    @Test
    void testFacadeExposesTheManagementOperations() {
        List<String> facadeMethodNames = Arrays.stream(ConnectedUserFacade.class.getMethods())
            .map(Method::getName)
            .toList();

        assertThat(facadeMethodNames).contains(
            "deleteConnectedUser", "enableConnectedUser", "getConnectedUser", "getConnectedUserDTO",
            "getConnectedUserDTOs", "getConnectedUsers");
    }

    private static ByteArrayInputStream csv(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private ConnectedUser getConnectedUser(String externalId, Environment environment) {
        Optional<ConnectedUser> connectedUserOptional = connectedUserService.fetchConnectedUser(
            externalId, environment);

        assertThat(connectedUserOptional).isPresent();

        return connectedUserOptional.get();
    }
}
