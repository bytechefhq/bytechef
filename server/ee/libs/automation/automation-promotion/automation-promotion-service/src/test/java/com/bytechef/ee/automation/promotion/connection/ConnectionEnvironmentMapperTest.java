/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectionEnvironmentMapperTest {

    private final ConnectionService connectionService = mock(ConnectionService.class);
    private final WorkspaceConnectionFacade workspaceConnectionFacade = mock(WorkspaceConnectionFacade.class);
    private final ConnectionEnvironmentMapper connectionEnvironmentMapper =
        new ConnectionEnvironmentMapper(connectionService, workspaceConnectionFacade);

    @Test
    void testSuggestMapsUnambiguousNameMatch() {
        Connection sourceConnection = connection(10L, "slack", 1, "Team Slack", Environment.DEVELOPMENT);

        when(connectionService.getConnections(List.of(10L))).thenReturn(List.of(sourceConnection));
        when(workspaceConnectionFacade.getConnections(5L, "slack", 1, 1L, null))
            .thenReturn(List.of(connectionDTO(20L, "slack", 1, "Team Slack"), connectionDTO(21L, "slack", 1, "Other")));

        assertThat(connectionEnvironmentMapper.suggest(5L, Set.of(10L), Environment.STAGING))
            .containsExactly(Map.entry(10L, 20L));
    }

    @Test
    void testSuggestSkipsAmbiguousAndMissing() {
        Connection ambiguousSourceConnection = connection(10L, "slack", 1, "Team Slack", Environment.DEVELOPMENT);
        Connection missingTargetSourceConnection = connection(11L, "github", 1, "GH", Environment.DEVELOPMENT);

        when(connectionService.getConnections(List.of(10L, 11L)))
            .thenReturn(List.of(ambiguousSourceConnection, missingTargetSourceConnection));
        when(workspaceConnectionFacade.getConnections(5L, "slack", 1, 1L, null))
            .thenReturn(
                List.of(connectionDTO(20L, "slack", 1, "Team Slack"), connectionDTO(22L, "slack", 1, "Team Slack")));
        when(workspaceConnectionFacade.getConnections(5L, "github", 1, 1L, null)).thenReturn(List.of());

        assertThat(connectionEnvironmentMapper.suggest(5L, Set.of(10L, 11L), Environment.STAGING)).isEmpty();
    }

    @Test
    void testValidateRejectsWrongEnvironment() {
        Connection sourceConnection = connection(10L, "slack", 1, "Team Slack", Environment.DEVELOPMENT);
        Connection targetConnection = connection(20L, "slack", 1, "Team Slack", Environment.PRODUCTION);

        when(connectionService.getConnections(List.of(10L))).thenReturn(List.of(sourceConnection));
        when(connectionService.getConnections(List.of(20L))).thenReturn(List.of(targetConnection));
        when(workspaceConnectionFacade.getConnections(5L, "slack", 1, 1L, null))
            .thenReturn(List.of(connectionDTO(20L, "slack", 1, "Team Slack")));

        assertThatThrownBy(() -> connectionEnvironmentMapper.validate(5L, Environment.STAGING, Map.of(10L, 20L)))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testValidateRejectsInvisibleTarget() {
        Connection sourceConnection = connection(10L, "slack", 1, "Team Slack", Environment.DEVELOPMENT);
        Connection targetConnection = connection(20L, "slack", 1, "Team Slack", Environment.STAGING);

        when(connectionService.getConnections(List.of(10L))).thenReturn(List.of(sourceConnection));
        when(connectionService.getConnections(List.of(20L))).thenReturn(List.of(targetConnection));
        when(workspaceConnectionFacade.getConnections(5L, "slack", 1, 1L, null)).thenReturn(List.of());

        assertThatThrownBy(() -> connectionEnvironmentMapper.validate(5L, Environment.STAGING, Map.of(10L, 20L)))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testValidateTreatsMissingAndInvisibleTargetsIndistinguishably() {
        Connection sourceConnection = connection(10L, "slack", 1, "Team Slack", Environment.DEVELOPMENT);
        Connection invisibleTargetConnection = connection(20L, "slack", 1, "Team Slack", Environment.STAGING);

        when(connectionService.getConnections(List.of(10L))).thenReturn(List.of(sourceConnection));
        when(connectionService.getConnections(List.of(20L))).thenReturn(List.of(invisibleTargetConnection));
        when(connectionService.getConnections(List.of(21L))).thenReturn(List.of());
        when(workspaceConnectionFacade.getConnections(5L, "slack", 1, 1L, null)).thenReturn(List.of());

        ConfigurationException missingTargetException = catchConfigurationException(
            () -> connectionEnvironmentMapper.validate(5L, Environment.STAGING, Map.of(10L, 21L)));
        ConfigurationException invisibleTargetException = catchConfigurationException(
            () -> connectionEnvironmentMapper.validate(5L, Environment.STAGING, Map.of(10L, 20L)));

        assertThat(missingTargetException.getMessage()).isEqualTo(invisibleTargetException.getMessage());
        assertThat(missingTargetException.getMessage())
            .doesNotContain("slack")
            .doesNotContain("STAGING")
            .doesNotContain("20")
            .doesNotContain("21");
    }

    @Test
    void testValidateAcceptsSameComponentDifferentNameTarget() {
        Connection sourceConnection = connection(10L, "slack", 1, "Team Slack", Environment.DEVELOPMENT);
        Connection targetConnection = connection(20L, "slack", 1, "Prod Slack", Environment.STAGING);

        when(connectionService.getConnections(List.of(10L))).thenReturn(List.of(sourceConnection));
        when(connectionService.getConnections(List.of(20L))).thenReturn(List.of(targetConnection));
        when(workspaceConnectionFacade.getConnections(5L, "slack", 1, 1L, null))
            .thenReturn(List.of(connectionDTO(20L, "slack", 1, "Prod Slack")));

        assertThat(connectionEnvironmentMapper.validate(5L, Environment.STAGING, Map.of(10L, 20L)))
            .containsExactly(Map.entry(10L, 20L));
    }

    @Test
    void testValidateRejectsDifferentComponentTarget() {
        Connection sourceConnection = connection(10L, "slack", 1, "Team Slack", Environment.DEVELOPMENT);
        Connection targetConnection = connection(20L, "github", 1, "Team Slack", Environment.STAGING);

        when(connectionService.getConnections(List.of(10L))).thenReturn(List.of(sourceConnection));
        when(connectionService.getConnections(List.of(20L))).thenReturn(List.of(targetConnection));
        when(workspaceConnectionFacade.getConnections(5L, "github", 1, 1L, null))
            .thenReturn(List.of(connectionDTO(20L, "github", 1, "Team Slack")));

        assertThatThrownBy(() -> connectionEnvironmentMapper.validate(5L, Environment.STAGING, Map.of(10L, 20L)))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testValidateRejectsDifferentConnectionVersionTarget() {
        Connection sourceConnection = connection(10L, "slack", 1, "Team Slack", Environment.DEVELOPMENT);
        Connection targetConnection = connection(20L, "slack", 2, "Team Slack", Environment.STAGING);

        when(connectionService.getConnections(List.of(10L))).thenReturn(List.of(sourceConnection));
        when(connectionService.getConnections(List.of(20L))).thenReturn(List.of(targetConnection));
        when(workspaceConnectionFacade.getConnections(5L, "slack", 2, 1L, null))
            .thenReturn(List.of(connectionDTO(20L, "slack", 2, "Team Slack")));

        assertThatThrownBy(() -> connectionEnvironmentMapper.validate(5L, Environment.STAGING, Map.of(10L, 20L)))
            .isInstanceOf(ConfigurationException.class);
    }

    private static ConfigurationException catchConfigurationException(Runnable validation) {
        try {
            validation.run();
        } catch (ConfigurationException configurationException) {
            return configurationException;
        }

        throw new AssertionError("Expected a ConfigurationException to be thrown, but none was");
    }

    private static Connection connection(
        long id, String componentName, int connectionVersion, String name, Environment environment) {

        Connection connection = new Connection();

        connection.setId(id);
        connection.setComponentName(componentName);
        connection.setConnectionVersion(connectionVersion);
        connection.setName(name);
        connection.setEnvironmentId(environment.ordinal());

        return connection;
    }

    private static ConnectionDTO connectionDTO(long id, String componentName, int connectionVersion, String name) {
        return ConnectionDTO.builder()
            .id(id)
            .componentName(componentName)
            .connectionVersion(connectionVersion)
            .name(name)
            .build();
    }
}
