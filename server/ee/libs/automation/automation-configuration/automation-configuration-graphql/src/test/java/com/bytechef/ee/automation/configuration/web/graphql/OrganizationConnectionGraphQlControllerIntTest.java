/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.automation.configuration.facade.OrganizationConnectionFacade;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.test.config.graphql.GraphQLScalarTypes;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    OrganizationConnectionGraphQlControllerIntTest.OrganizationConnectionGraphQlControllerIntTestConfiguration.class,
    OrganizationConnectionGraphQlController.class
})
@GraphQlTest(
    controllers = OrganizationConnectionGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
class OrganizationConnectionGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private OrganizationConnectionFacade organizationConnectionFacade;

    @Test
    void testCreateOrganizationConnectionForwardsAuthorizationTypeAndTags() {
        when(organizationConnectionFacade.create(any(ConnectionDTO.class))).thenReturn(42L);

        graphQlTester
            .document("""
                mutation {
                    createOrganizationConnection(input: {
                        name: "Shared Slack"
                        authorizationType: OAUTH2_AUTHORIZATION_CODE
                        componentName: "slack"
                        connectionVersion: 1
                        environmentId: 2
                        parameters: {code: "abc"}
                        tags: [{id: 5, name: "existing"}, {name: "new"}]
                    })
                }
                """)
            .execute()
            .path("createOrganizationConnection")
            .entity(String.class)
            .isEqualTo("42");

        ArgumentCaptor<ConnectionDTO> connectionDTOCaptor = ArgumentCaptor.forClass(ConnectionDTO.class);

        verify(organizationConnectionFacade).create(connectionDTOCaptor.capture());

        ConnectionDTO connectionDTO = connectionDTOCaptor.getValue();

        assertThat(connectionDTO.authorizationType()).isEqualTo(AuthorizationType.OAUTH2_AUTHORIZATION_CODE);
        assertThat(connectionDTO.visibility()).isEqualTo(ResourceVisibility.ORGANIZATION);
        assertThat(connectionDTO.tags()).extracting(Tag::getId, Tag::getName)
            .containsExactly(tuple(5L, "existing"), tuple(null, "new"));
    }

    @Test
    void testCreateOrganizationConnectionWithoutTags() {
        when(organizationConnectionFacade.create(any(ConnectionDTO.class))).thenReturn(43L);

        graphQlTester
            .document("""
                mutation {
                    createOrganizationConnection(input: {
                        name: "Shared API"
                        componentName: "http"
                        connectionVersion: 1
                        environmentId: 2
                        parameters: {}
                    })
                }
                """)
            .execute()
            .path("createOrganizationConnection")
            .entity(String.class)
            .isEqualTo("43");

        ArgumentCaptor<ConnectionDTO> connectionDTOCaptor = ArgumentCaptor.forClass(ConnectionDTO.class);

        verify(organizationConnectionFacade).create(connectionDTOCaptor.capture());

        ConnectionDTO connectionDTO = connectionDTOCaptor.getValue();

        assertThat(connectionDTO.authorizationType()).isNull();
        assertThat(connectionDTO.tags()).isEmpty();
    }

    @Configuration
    static class OrganizationConnectionGraphQlControllerIntTestConfiguration {

        @Bean
        RuntimeWiringConfigurer scalarWiringConfigurer() {
            return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.longScalar())
                .scalar(GraphQLScalarTypes.mapScalar());
        }
    }
}
