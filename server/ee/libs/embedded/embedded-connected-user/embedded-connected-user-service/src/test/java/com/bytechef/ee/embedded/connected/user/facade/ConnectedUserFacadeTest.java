/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.dto.ConnectedUserDTO;
import com.bytechef.ee.embedded.connected.user.dto.ConnectedUserImportResultDTO;
import com.bytechef.ee.embedded.connected.user.dto.ConnectedUserImportResultDTO.RejectedRow;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserFacadeTest {

    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final ConnectedUserFacadeImpl connectedUserFacade = new ConnectedUserFacadeImpl(
        mock(ConnectedUserConnectionService.class), mock(ConnectedUserProjectFacade.class), connectedUserService,
        mock(ConnectionFacade.class), mock(ConnectionService.class), mock(EnvironmentService.class),
        mock(IntegrationInstanceFacade.class), mock(IntegrationInstanceService.class),
        mock(IntegrationInstanceConfigurationService.class), mock(IntegrationService.class));

    @Test
    void testImportCreatesSkipsAndRejectsRowsInOrder() {
        Environment environment = Environment.DEVELOPMENT;

        when(connectedUserService.fetchConnectedUser(any(String.class), eq(environment)))
            .thenReturn(Optional.empty());
        when(connectedUserService.fetchConnectedUser("existing", environment))
            .thenReturn(Optional.of(mock(ConnectedUser.class)));

        ConnectedUserImportResultDTO result = connectedUserFacade.importConnectedUsers(
            environment, csv("externalId,name,plan\na,A,pro\nb,,\na,Again,x\n\" bad\",X,\nexisting,E,\n"));

        assertThat(result.createdCount()).isEqualTo(2);
        assertThat(result.skippedExternalIds()).containsExactly("a", "existing");
        assertThat(result.rejectedRows()).hasSize(1);

        RejectedRow rejectedRow = result.rejectedRows()
            .get(0);

        assertThat(rejectedRow.rowNumber()).isEqualTo(5);
        assertThat(rejectedRow.reason()).contains("whitespace");

        verify(connectedUserService).createConnectedUser("a", environment, "A", null, Map.of("plan", "pro"));
        verify(connectedUserService).createConnectedUser("b", environment, null, null, Map.of());
        verify(connectedUserService, times(2)).createConnectedUser(
            any(String.class), eq(environment), any(), any(), anyMap());
    }

    @Test
    void testImportSkipsAnExternalIdRepeatedInTheSameFile() {
        Environment environment = Environment.DEVELOPMENT;

        when(connectedUserService.fetchConnectedUser(any(String.class), eq(environment)))
            .thenReturn(Optional.empty());

        ConnectedUserImportResultDTO result = connectedUserFacade.importConnectedUsers(
            environment, csv("externalId\nu1\nu1\n"));

        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.skippedExternalIds()).containsExactly("u1");
        assertThat(result.rejectedRows()).isEmpty();

        verify(connectedUserService, times(1)).createConnectedUser(
            eq("u1"), eq(environment), isNull(), isNull(), anyMap());
    }

    @Test
    void testImportReportsAServiceValidationFailureAsARejectedRow() {
        Environment environment = Environment.DEVELOPMENT;

        when(connectedUserService.fetchConnectedUser(any(String.class), eq(environment)))
            .thenReturn(Optional.empty());
        when(connectedUserService.createConnectedUser("u1", environment, null, "nope", Map.of()))
            .thenThrow(new IllegalArgumentException("email must contain @"));

        ConnectedUserImportResultDTO result = connectedUserFacade.importConnectedUsers(
            environment, csv("externalId,email\nu1,nope\nu2,\n"));

        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.rejectedRows()).containsExactly(new RejectedRow(2, "email must contain @"));
    }

    @Test
    void testImportDoesNotCountARejectedRowAsSeen() {
        Environment environment = Environment.DEVELOPMENT;

        when(connectedUserService.fetchConnectedUser(any(String.class), eq(environment)))
            .thenReturn(Optional.empty());
        when(connectedUserService.createConnectedUser("u1", environment, null, "bad-email", Map.of()))
            .thenThrow(new IllegalArgumentException("email must contain @"));

        ConnectedUserImportResultDTO result = connectedUserFacade.importConnectedUsers(
            environment, csv("externalId,email\nu1,bad-email\nu1,\n"));

        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.skippedExternalIds()).isEmpty();
        assertThat(result.rejectedRows()).containsExactly(new RejectedRow(2, "email must contain @"));
    }

    @Test
    void testImportRefusesTheWholeFileBeforeCreatingAnything() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> connectedUserFacade.importConnectedUsers(Environment.DEVELOPMENT, csv("name\nA\n")))
            .isInstanceOf(IllegalArgumentException.class);

        verify(connectedUserService, never()).createConnectedUser(
            any(String.class), any(Environment.class), any(), any(), anyMap());
    }

    @Test
    void testCreateAndUpdateReturnTheConnectedUserDTO() {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getId()).thenReturn(7L);
        when(connectedUserService.getConnectedUser(7L)).thenReturn(connectedUser);
        when(connectedUserService.createConnectedUser("u1", Environment.DEVELOPMENT, "N", null, Map.of()))
            .thenReturn(connectedUser);
        when(connectedUserService.updateConnectedUser(7L, "N2", null, Map.of("a", "b"), 3))
            .thenReturn(connectedUser);

        ConnectedUserDTO created = connectedUserFacade.createConnectedUser(
            "u1", Environment.DEVELOPMENT, "N", null, Map.of());
        ConnectedUserDTO updated = connectedUserFacade.updateConnectedUser(7L, "N2", null, Map.of("a", "b"), 3);

        assertThat(created).isNotNull();
        assertThat(updated).isNotNull();
    }

    private static ByteArrayInputStream csv(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
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
}
