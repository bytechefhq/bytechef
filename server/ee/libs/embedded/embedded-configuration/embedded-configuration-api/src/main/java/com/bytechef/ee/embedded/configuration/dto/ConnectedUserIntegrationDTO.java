/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.platform.component.domain.Authorization;
import com.bytechef.platform.component.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.ValueProperty;
import com.bytechef.platform.connection.domain.Connection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ConnectedUserIntegrationDTO(
    ConnectionConfig connectionConfig, IntegrationInstanceConfigurationDTO integrationInstanceConfiguration,
    List<ConnectedUserIntegrationInstance> integrationInstances,
    OAuth2AuthorizationParameters oAuth2AuthorizationParameters, String redirectUri) {

    public ConnectedUserIntegrationDTO(
        List<Connection> connections, IntegrationInstanceConfigurationDTO integrationInstanceConfiguration,
        List<IntegrationInstance> integrationInstances,
        List<IntegrationInstanceWorkflow> integrationInstanceWorkflows) {

        this(
            null, integrationInstanceConfiguration,
            toIntegrationInstances(
                connections, integrationInstanceConfiguration.integrationInstanceConfigurationWorkflows(),
                integrationInstances, integrationInstanceWorkflows),
            null, null);
    }

    public ConnectedUserIntegrationDTO(
        Authorization authorization, List<Connection> connections,
        IntegrationInstanceConfigurationDTO integrationInstanceConfiguration,
        List<IntegrationInstance> integrationInstances, List<IntegrationInstanceWorkflow> integrationInstanceWorkflows,
        @Nullable OAuth2AuthorizationParameters oAuth2AuthorizationParameters, String redirectUri) {

        this(
            new ConnectionConfig(
                authorization.getType(),
                oAuth2AuthorizationParameters == null ? null : new OAuth2(oAuth2AuthorizationParameters, redirectUri),
                authorization.getProperties()),
            integrationInstanceConfiguration,
            toIntegrationInstances(
                connections, integrationInstanceConfiguration.integrationInstanceConfigurationWorkflows(),
                integrationInstances, integrationInstanceWorkflows),
            oAuth2AuthorizationParameters, redirectUri);
    }

    private static List<ConnectedUserIntegrationInstance> toIntegrationInstances(
        List<Connection> connections,
        List<IntegrationInstanceConfigurationWorkflowDTO> integrationInstanceConfigurationWorkflowDTOs,
        List<IntegrationInstance> integrationInstances,
        List<IntegrationInstanceWorkflow> integrationInstanceWorkflows) {

        return integrationInstances.stream()
            .map(integrationInstance -> new ConnectedUserIntegrationInstance(
                getConnection(connections, integrationInstance), integrationInstance,
                toIntegrationInstanceWorkflows(
                    integrationInstanceConfigurationWorkflowDTOs, integrationInstance.getId(),
                    integrationInstanceWorkflows)))
            .toList();
    }

    public record ConnectedUserIntegrationInstance(
        Connection connection, IntegrationInstance integrationInstance,
        List<ConnectedUserIntegrationInstanceWorkflow> workflows) {
    }

    public record ConnectedUserIntegrationInstanceWorkflow(
        IntegrationInstanceWorkflow integrationInstanceWorkflow, String workflowUuid) {
    }

    public record ConnectionConfig(AuthorizationType authorizationType, List<Input> inputs, OAuth2 oauth2) {

        public ConnectionConfig(
            AuthorizationType authorizationType, OAuth2 oauth2, List<? extends Property> properties) {

            this(authorizationType, getInputs(authorizationType, properties), oauth2);
        }
    }

    public record Input(String name, String label, Type type, boolean required) {

        public enum Type {
            BOOLEAN,
            DATE,
            DATE_TIME,
            INTEGER,
            NUMBER,
            STRING,
            TIME;
        }
    }

    public record OAuth2(OAuth2AuthorizationParameters oAuth2AuthorizationParameters, String redirectUri) {
    }

    private static List<Input> getInputs(AuthorizationType authorizationType, List<? extends Property> properties) {
        if (StringUtils.contains(authorizationType.name(), "OAUTH2")) {
            return List.of();
        }

        return properties.stream()
            .map(property -> new Input(
                property.getName(), ((ValueProperty<?>) property).getLabel(),
                Input.Type.valueOf(StringUtils.upperCase(String.valueOf(property.getType()))),
                property.getRequired()))
            .toList();
    }

    private static Connection getConnection(List<Connection> connections, IntegrationInstance integrationInstance) {
        return connections.stream()
            .filter(connection -> Objects.equals(connection.getId(), integrationInstance.getConnectionId()))
            .findFirst()
            .orElseThrow();
    }

    private static String getWorkflowUuid(
        List<IntegrationInstanceConfigurationWorkflowDTO> integrationInstanceConfigurationWorkflowDTOs,
        IntegrationInstanceWorkflow integrationInstanceWorkflow) {

        return integrationInstanceConfigurationWorkflowDTOs
            .stream()
            .filter(integrationInstanceConfigurationWorkflowDTO -> Objects.equals(
                integrationInstanceConfigurationWorkflowDTO.id(),
                integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId()))
            .map(IntegrationInstanceConfigurationWorkflowDTO::workflowUuid)
            .findFirst()
            .orElseThrow();
    }

    private static List<ConnectedUserIntegrationInstanceWorkflow> toIntegrationInstanceWorkflows(
        List<IntegrationInstanceConfigurationWorkflowDTO> integrationInstanceConfigurationWorkflowDTOs,
        long integrationInstanceId, List<IntegrationInstanceWorkflow> integrationInstanceWorkflows) {

        return integrationInstanceWorkflows.stream()
            .filter(integrationInstanceWorkflow -> Objects.equals(
                integrationInstanceWorkflow.getIntegrationInstanceId(), integrationInstanceId))
            .map(integrationInstanceWorkflow -> new ConnectedUserIntegrationInstanceWorkflow(
                integrationInstanceWorkflow,
                getWorkflowUuid(integrationInstanceConfigurationWorkflowDTOs, integrationInstanceWorkflow)))
            .toList();
    }
}
