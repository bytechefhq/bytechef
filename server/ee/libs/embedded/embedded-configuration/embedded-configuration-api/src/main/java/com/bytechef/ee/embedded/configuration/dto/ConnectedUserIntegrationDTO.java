/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.platform.component.domain.Authorization;
import com.bytechef.platform.component.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.ValueProperty;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ConnectedUserIntegrationDTO(
    ConnectionConfig connectionConfig, CredentialStatus credentialStatus, boolean enabled,
    IntegrationInstanceConfigurationDTO integrationInstanceConfiguration) {

    public ConnectedUserIntegrationDTO(
        @Nullable Connection connection, @Nullable IntegrationInstance integrationInstance,
        IntegrationInstanceConfigurationDTO integrationInstanceConfiguration) {

        this(
            null, connection == null ? null : connection.getCredentialStatus(),
            integrationInstance != null && integrationInstance.isEnabled(), integrationInstanceConfiguration);
    }

    public ConnectedUserIntegrationDTO(
        Authorization authorization, @Nullable Connection connection, @Nullable IntegrationInstance integrationInstance,
        IntegrationInstanceConfigurationDTO integrationInstanceConfiguration,
        OAuth2AuthorizationParameters oAuth2AuthorizationParameters, String redirectUri) {

        this(
            new ConnectionConfig(
                authorization.getType(), new OAuth2(oAuth2AuthorizationParameters, redirectUri),
                authorization.getProperties()),
            connection == null ? null : connection.getCredentialStatus(),
            integrationInstance != null && integrationInstance.isEnabled(), integrationInstanceConfiguration);
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
            TIME
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
}
