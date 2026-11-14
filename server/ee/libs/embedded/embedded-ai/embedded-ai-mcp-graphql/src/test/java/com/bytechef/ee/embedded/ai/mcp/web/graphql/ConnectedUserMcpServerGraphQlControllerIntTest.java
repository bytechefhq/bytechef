/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.web.graphql;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.ai.mcp.dto.ConnectedUserMcpServerAutomationDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.ConnectedUserMcpServerDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.ProvisionableConnectedUserMcpServerDTO;
import com.bytechef.ee.embedded.ai.mcp.facade.ConnectedUserMcpServerFacade;
import com.bytechef.test.config.graphql.GraphQLScalarTypes;
import java.time.Instant;
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
                    3L, "Server", false, List.of("gmail"), List.of("slack"), List.of("Send mail"),
                    List.of("Archive mail"))));

        graphQlTester.document("""
            query {
                provisionableConnectedUserMcpServers(connectedUserId: "7") {
                    id
                    name
                    provisioned
                    coveredComponentNames
                    missingComponentNames
                    coveredAutomationNames
                    missingAutomationNames
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
            .containsExactly("slack")
            .path("provisionableConnectedUserMcpServers[0].coveredAutomationNames")
            .entityList(String.class)
            .containsExactly("Send mail")
            .path("provisionableConnectedUserMcpServers[0].missingAutomationNames")
            .entityList(String.class)
            .containsExactly("Archive mail");

        verify(connectedUserMcpServerFacade).getProvisionableMcpServers(7L);
    }

    @Test
    void testConnectedUserMcpServersReturnsAutomations() {
        when(connectedUserMcpServerFacade.getConnectedUserMcpServers(7L))
            .thenReturn(List.of(
                new ConnectedUserMcpServerDTO(
                    3L, "Server", true, 0L, Instant.parse("2026-11-01T10:00:00Z"), List.of(), List.of(),
                    List.of(
                        new ConnectedUserMcpServerAutomationDTO(
                            11L, 21L, "Template", "uuid-1", "Send mail", "Sends mail", true, false,
                            Instant.parse("2026-11-02T10:00:00Z"))))));

        graphQlTester.document("""
            query {
                connectedUserMcpServers(connectedUserId: "7") {
                    automations {
                        id
                        connectedUserProjectWorkflowId
                        automationName
                        workflowUuid
                        name
                        description
                        enabled
                        available
                        lastExecutionDate
                    }
                }
            }
            """)
            .execute()
            .path("connectedUserMcpServers[0].automations[0].id")
            .entity(String.class)
            .isEqualTo("11")
            .path("connectedUserMcpServers[0].automations[0].connectedUserProjectWorkflowId")
            .entity(String.class)
            .isEqualTo("21")
            .path("connectedUserMcpServers[0].automations[0].automationName")
            .entity(String.class)
            .isEqualTo("Template")
            .path("connectedUserMcpServers[0].automations[0].workflowUuid")
            .entity(String.class)
            .isEqualTo("uuid-1")
            .path("connectedUserMcpServers[0].automations[0].name")
            .entity(String.class)
            .isEqualTo("Send mail")
            .path("connectedUserMcpServers[0].automations[0].description")
            .entity(String.class)
            .isEqualTo("Sends mail")
            .path("connectedUserMcpServers[0].automations[0].enabled")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("connectedUserMcpServers[0].automations[0].available")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("connectedUserMcpServers[0].automations[0].lastExecutionDate")
            .entity(String.class)
            .isEqualTo("2026-11-02T10:00:00Z");
    }

    @Test
    void testEnableConnectedUserMcpAutomation() {
        when(connectedUserMcpServerFacade.enableMcpAutomation(7L, 11L, false))
            .thenReturn(
                new ConnectedUserMcpServerAutomationDTO(
                    11L, 21L, "Template", "uuid-1", "Send mail", null, false, true, null));

        graphQlTester.document("""
            mutation {
                enableConnectedUserMcpAutomation(connectedUserId: "7", id: "11", enable: false) {
                    id
                    enabled
                    description
                }
            }
            """)
            .execute()
            .path("enableConnectedUserMcpAutomation.id")
            .entity(String.class)
            .isEqualTo("11")
            .path("enableConnectedUserMcpAutomation.enabled")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("enableConnectedUserMcpAutomation.description")
            .valueIsNull();

        verify(connectedUserMcpServerFacade).enableMcpAutomation(7L, 11L, false);
    }

    @Test
    void testEnableConnectedUserMcpWorkflow() {
        graphQlTester.document("""
            mutation {
                enableConnectedUserMcpWorkflow(id: "11", enable: false)
            }
            """)
            .execute()
            .path("enableConnectedUserMcpWorkflow")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(connectedUserMcpServerFacade).enableMcpWorkflow(11L, false);
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
