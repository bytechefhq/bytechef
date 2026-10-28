/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationInstanceConfigurationTest {

    @Test
    void testSetAuthorizationTypeAcceptsNull() {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setAuthorizationType(null);

        assertThat(integrationInstanceConfiguration.getAuthorizationType()).isNull();
    }

    @Test
    void testSetAuthorizationTypeKeepsTheGivenType() {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setAuthorizationType(AuthorizationType.OAUTH2_AUTHORIZATION_CODE);

        assertThat(integrationInstanceConfiguration.getAuthorizationType())
            .isEqualTo(AuthorizationType.OAUTH2_AUTHORIZATION_CODE);
    }
}
