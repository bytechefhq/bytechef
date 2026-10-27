/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.web.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import com.bytechef.ee.platform.apiconnector.configuration.facade.ApiConnectorFacade;
import com.bytechef.ee.platform.apiconnector.configuration.service.ApiConnectorGenerationJobService;
import com.bytechef.ee.platform.apiconnector.configuration.service.OpenApiSpecificationGenerator;
import com.bytechef.test.config.graphql.GraphQLScalarTypes;
import org.junit.jupiter.api.Nested;
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
    ApiConnectorGraphQlControllerIntTest.ScalarConfiguration.class,
    ApiConnectorGraphQlController.class
})
@GraphQlTest(
    controllers = ApiConnectorGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
class ApiConnectorGraphQlControllerIntTest {

    @MockitoBean
    private ApiConnectorFacade apiConnectorFacade;

    @MockitoBean
    private ApiConnectorGenerationJobService apiConnectorGenerationJobService;

    @MockitoBean
    private OpenApiSpecificationGenerator openApiSpecificationGenerator;

    @Autowired
    private GraphQlTester graphQlTester;

    @Nested
    class EnableApiConnector {

        @Test
        void testEnableApiConnectorRoutesThroughFacade() {
            graphQlTester.document("""
                mutation {
                    enableApiConnector(id: "1", enable: true)
                }
                """)
                .execute()
                .path("enableApiConnector")
                .entity(Boolean.class)
                .isEqualTo(true);

            verify(apiConnectorFacade).enableApiConnector(1L, true);
        }
    }

    @Nested
    class UpdateApiConnector {

        @Test
        void testUpdateApiConnectorWithoutSpecificationRoutesMetadataThroughFacade() {
            when(apiConnectorFacade.updateApiConnectorMetadata(1L, null, "Pet Store", "Pets", "icon.svg", 2))
                .thenReturn(createApiConnector("Pet Store"));

            graphQlTester.document("""
                mutation {
                    updateApiConnector(
                        id: "1",
                        input: {title: "Pet Store", description: "Pets", icon: "icon.svg", connectorVersion: 2}) {
                        id
                        title
                    }
                }
                """)
                .execute()
                .path("updateApiConnector.title")
                .entity(String.class)
                .isEqualTo("Pet Store");

            verify(apiConnectorFacade).updateApiConnectorMetadata(1L, null, "Pet Store", "Pets", "icon.svg", 2);
        }

        @Test
        void testUpdateApiConnectorWithSpecificationRegeneratesDefinitionThroughFacade() {
            when(apiConnectorFacade.updateApiConnector(1L, "petstore", null, "openapi: 3.0.0", 3))
                .thenReturn(createApiConnector("Petstore"));

            graphQlTester.document("""
                mutation {
                    updateApiConnector(
                        id: "1", input: {name: "petstore", specification: "openapi: 3.0.0", version: 3}) {
                        id
                        title
                    }
                }
                """)
                .execute()
                .path("updateApiConnector.title")
                .entity(String.class)
                .isEqualTo("Petstore");

            verify(apiConnectorFacade).updateApiConnector(1L, "petstore", null, "openapi: 3.0.0", 3);
            verify(apiConnectorFacade, never()).updateApiConnectorMetadata(
                anyLong(), any(), any(), any(), any(), any());
        }
    }

    private static ApiConnector createApiConnector(String title) {
        ApiConnector apiConnector = new ApiConnector();

        apiConnector.setId(1L);
        apiConnector.setName("petstore");
        apiConnector.setTitle(title);
        apiConnector.setConnectorVersion(1);

        return apiConnector;
    }

    @TestConfiguration
    static class ScalarConfiguration {

        @Bean
        RuntimeWiringConfigurer longScalarWiringConfigurer() {
            return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.longScalar());
        }
    }
}
