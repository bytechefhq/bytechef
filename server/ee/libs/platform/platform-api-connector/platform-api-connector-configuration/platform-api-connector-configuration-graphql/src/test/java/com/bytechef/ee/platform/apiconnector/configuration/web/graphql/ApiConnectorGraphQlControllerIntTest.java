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
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnectorGenerationJob;
import com.bytechef.ee.platform.apiconnector.configuration.dto.GenerateSpecificationRequestDTO;
import com.bytechef.ee.platform.apiconnector.configuration.facade.ApiConnectorFacade;
import com.bytechef.test.config.graphql.GraphQLScalarTypes;
import java.util.Optional;
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

    @Nested
    class GenerationJobs {

        @Test
        void testStartGenerateFromDocumentationPreviewRoutesThroughFacade() {
            when(apiConnectorFacade.startGenerateFromDocumentationPreview("https://docs.example.com", "Only pets", 3))
                .thenReturn(new ApiConnectorGenerationJob("job-1", "https://docs.example.com"));

            graphQlTester.document("""
                mutation {
                    startGenerateFromDocumentationPreview(input: {
                        name: "petstore", documentationUrl: "https://docs.example.com", userPrompt: "Only pets",
                        maxPages: 3}) {
                        jobId
                        status
                    }
                }
                """)
                .execute()
                .path("startGenerateFromDocumentationPreview.status")
                .entity(String.class)
                .isEqualTo("PENDING");

            verify(apiConnectorFacade).startGenerateFromDocumentationPreview(
                "https://docs.example.com", "Only pets", 3);
        }

        @Test
        void testCancelGenerationJobRoutesThroughFacade() {
            when(apiConnectorFacade.cancelGenerationJob("job-1")).thenReturn(true);

            graphQlTester.document("""
                mutation {
                    cancelGenerationJob(jobId: "job-1")
                }
                """)
                .execute()
                .path("cancelGenerationJob")
                .entity(Boolean.class)
                .isEqualTo(true);

            verify(apiConnectorFacade).cancelGenerationJob("job-1");
        }

        @Test
        void testGenerationJobStatusRoutesThroughFacade() {
            when(apiConnectorFacade.fetchGenerationJob("job-1"))
                .thenReturn(Optional.of(new ApiConnectorGenerationJob("job-1", "https://docs.example.com")));

            graphQlTester.document("""
                query {
                    generationJobStatus(jobId: "job-1") {
                        jobId
                        status
                    }
                }
                """)
                .execute()
                .path("generationJobStatus.jobId")
                .entity(String.class)
                .isEqualTo("job-1");

            verify(apiConnectorFacade).fetchGenerationJob("job-1");
        }

        @Test
        void testGenerationJobStatusReturnsNullForUnknownJob() {
            when(apiConnectorFacade.fetchGenerationJob("missing")).thenReturn(Optional.empty());

            graphQlTester.document("""
                query {
                    generationJobStatus(jobId: "missing") {
                        jobId
                    }
                }
                """)
                .execute()
                .path("generationJobStatus")
                .valueIsNull();
        }
    }

    @Nested
    class GenerateSpecification {

        @Test
        void testGenerateSpecificationRoutesThroughFacade() {
            when(apiConnectorFacade.generateSpecification(any(GenerateSpecificationRequestDTO.class)))
                .thenReturn("openapi: 3.0.0");

            graphQlTester.document("""
                mutation {
                    generateSpecification(input: {name: "petstore", endpoints: []}) {
                        specification
                    }
                }
                """)
                .execute()
                .path("generateSpecification.specification")
                .entity(String.class)
                .isEqualTo("openapi: 3.0.0");

            verify(apiConnectorFacade).generateSpecification(any(GenerateSpecificationRequestDTO.class));
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
