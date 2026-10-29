/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.web.graphql;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.ai.mcp.dto.ProvisionableConnectedUserMcpServerDTO;
import com.bytechef.ee.embedded.ai.mcp.facade.ConnectedUserMcpServerFacade;
import com.bytechef.test.config.graphql.GraphQLScalarTypes;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
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
    ConnectedUserMcpServerGraphQlControllerIntTest.ScalarConfiguration.class,
    ConnectedUserMcpServerGraphQlController.class
})
@GraphQlTest(
    controllers = ConnectedUserMcpServerGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
class ConnectedUserMcpServerGraphQlControllerIntTest {

    @MockitoBean
    private ConnectedUserMcpServerFacade connectedUserMcpServerFacade;

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void testProvisionableConnectedUserMcpServers() {
        when(connectedUserMcpServerFacade.getProvisionableMcpServers(7L))
            .thenReturn(List.of(
                new ProvisionableConnectedUserMcpServerDTO(
                    3L, "Server", false, List.of("gmail"), List.of("slack"))));

        graphQlTester.document("""
            query {
                provisionableConnectedUserMcpServers(connectedUserId: "7") {
                    id
                    name
                    provisioned
                    coveredComponentNames
                    missingComponentNames
                }
            }
            """)
            .execute()
            .path("provisionableConnectedUserMcpServers[0].id")
            .entity(String.class)
            .isEqualTo("3")
            .path("provisionableConnectedUserMcpServers[0].name")
            .entity(String.class)
            .isEqualTo("Server")
            .path("provisionableConnectedUserMcpServers[0].provisioned")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("provisionableConnectedUserMcpServers[0].coveredComponentNames")
            .entityList(String.class)
            .containsExactly("gmail")
            .path("provisionableConnectedUserMcpServers[0].missingComponentNames")
            .entityList(String.class)
            .containsExactly("slack");

        verify(connectedUserMcpServerFacade).getProvisionableMcpServers(7L);
    }

    @Test
    void testProvisionConnectedUserMcpServer() {
        graphQlTester.document("""
            mutation {
                provisionConnectedUserMcpServer(connectedUserId: "7", mcpServerId: "3")
            }
            """)
            .execute()
            .path("provisionConnectedUserMcpServer")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(connectedUserMcpServerFacade).provisionMcpServer(7L, 3L);
    }

    @TestConfiguration
    static class ScalarConfiguration {

        @Bean
        RuntimeWiringConfigurer longScalarWiringConfigurer() {
            return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.longScalar());
        }

        @Bean
        RuntimeWiringConfigurer mapScalarWiringConfigurer() {
            return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.mapScalar());
        }
    }
}
