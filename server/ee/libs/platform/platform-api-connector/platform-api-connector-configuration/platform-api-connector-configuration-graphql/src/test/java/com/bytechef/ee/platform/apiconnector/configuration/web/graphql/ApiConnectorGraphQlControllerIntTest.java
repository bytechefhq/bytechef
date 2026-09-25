/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.platform.apiconnector.configuration.facade.ApiConnectorFacade;
import com.bytechef.ee.platform.apiconnector.configuration.service.ApiConnectorAiService;
import com.bytechef.ee.platform.apiconnector.configuration.service.ApiConnectorGenerationJobService;
import com.bytechef.ee.platform.apiconnector.configuration.service.ApiConnectorService;
import com.bytechef.ee.platform.apiconnector.configuration.service.OpenApiSpecificationGenerator;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.test.config.graphql.GraphQLScalarTypes;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.execution.SecurityDataFetcherExceptionResolver;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;

/**
 * Executes every {@link ApiConnectorGraphQlController} query and mutation through the real GraphQL schema with Spring
 * Security's method interceptor enforcing the controller's real {@code @PreAuthorize} guards through the production
 * {@link AutomationMethodSecurityConfiguration}. API connectors belong to the tenant, so each operation must decide on
 * the caller's {@code ROLE_ADMIN} authority alone and never consult {@link PermissionService}. Every collaborator
 * throws on any call, so an allowed operation proves it entered the controller method by reaching one of them, and a
 * denied one proves it never did.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = ApiConnectorGraphQlControllerIntTest.Config.class)
@GraphQlTest(
    controllers = ApiConnectorGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true", "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
class ApiConnectorGraphQlControllerIntTest {

    private static final String BODY_REACHED = "body reached";

    private static final Map<String, String> DOCUMENTS = Map.ofEntries(
        Map.entry("apiConnector", "query { apiConnector(id: \"1\") { id } }"),
        Map.entry("apiConnectors", "query { apiConnectors { id } }"),
        Map.entry("cancelGenerationJob", "mutation { cancelGenerationJob(jobId: \"job\") }"),
        Map.entry(
            "createApiConnector",
            "mutation { createApiConnector(input: {name: \"name\", connectorVersion: 1}) { id } }"),
        Map.entry("deleteApiConnector", "mutation { deleteApiConnector(id: \"1\") }"),
        Map.entry("enableApiConnector", "mutation { enableApiConnector(id: \"1\", enable: true) }"),
        Map.entry(
            "generateFromDocumentation",
            "mutation { generateFromDocumentation(input: {name: \"name\", documentationUrl: \"url\"}) { id } }"),
        Map.entry(
            "generateSpecification",
            "mutation { generateSpecification(input: {name: \"name\", endpoints: []}) { specification } }"),
        Map.entry("generationJobStatus", "query { generationJobStatus(jobId: \"job\") { jobId } }"),
        Map.entry(
            "importOpenApiSpecification",
            "mutation { importOpenApiSpecification(input: {name: \"name\", specification: \"spec\"}) { id } }"),
        Map.entry(
            "startGenerateFromDocumentationPreview",
            "mutation { startGenerateFromDocumentationPreview(input: {name: \"name\", documentationUrl: \"url\"}) " +
                "{ jobId } }"),
        Map.entry("updateApiConnector", "mutation { updateApiConnector(id: \"1\", input: {name: \"name\"}) { id } }"));

    @Autowired
    private ApiConnectorAiService apiConnectorAiService;

    @Autowired
    private ApiConnectorFacade apiConnectorFacade;

    @Autowired
    private ApiConnectorGenerationJobService apiConnectorGenerationJobService;

    @Autowired
    private ApiConnectorService apiConnectorService;

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private OpenApiSpecificationGenerator openApiSpecificationGenerator;

    @Autowired
    private PermissionService permissionService;

    static Stream<Arguments> endpoints() {
        return DOCUMENTS.keySet()
            .stream()
            .sorted()
            .flatMap(endpointName -> Stream.of(Arguments.of(endpointName, false), Arguments.of(endpointName, true)));
    }

    @BeforeEach
    void beforeEach() {
        clearInvocations(
            apiConnectorAiService, apiConnectorFacade, apiConnectorGenerationJobService, apiConnectorService,
            openApiSpecificationGenerator, permissionService);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testEveryEndpointIsEvaluated() {
        Set<String> endpointNames = Arrays.stream(ApiConnectorGraphQlController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(MutationMapping.class) ||
                method.isAnnotationPresent(QueryMapping.class))
            .map(Method::getName)
            .collect(Collectors.toCollection(TreeSet::new));

        assertThat(endpointNames).isEqualTo(new TreeSet<>(DOCUMENTS.keySet()));
    }

    @ParameterizedTest(name = "{0} admin={1}")
    @MethodSource("endpoints")
    void testEndpointRequiresTheAdminAuthority(String endpointName, boolean admin) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "alice", "credentials",
                List.of(new SimpleGrantedAuthority(admin ? AuthorityConstants.ADMIN : AuthorityConstants.USER))));

        SecurityContextHolder.setContext(securityContext);

        List<ResponseError> responseErrors = graphQlTester.document(DOCUMENTS.get(endpointName))
            .execute()
            .returnResponse()
            .getErrors();

        if (admin) {
            assertThat(responseErrors)
                .as("%s must allow a caller holding ROLE_ADMIN", endpointName)
                .singleElement()
                .extracting(ResponseError::getErrorType)
                .isEqualTo(ErrorType.INTERNAL_ERROR);

            assertThat(
                Stream.of(
                    apiConnectorAiService, apiConnectorFacade, apiConnectorGenerationJobService, apiConnectorService,
                    openApiSpecificationGenerator)
                    .mapToInt(collaborator -> mockingDetails(collaborator).getInvocations()
                        .size())
                    .sum())
                        .as("%s must reach its controller method", endpointName)
                        .isPositive();
        } else {
            assertThat(responseErrors)
                .as("%s must deny a caller without ROLE_ADMIN", endpointName)
                .singleElement()
                .extracting(ResponseError::getErrorType)
                .isEqualTo(ErrorType.FORBIDDEN);

            verifyNoInteractions(
                apiConnectorAiService, apiConnectorFacade, apiConnectorGenerationJobService, apiConnectorService,
                openApiSpecificationGenerator);
        }

        verifyNoMoreInteractions(permissionService);
    }

    @Configuration
    @EnableMethodSecurity
    @ImportAutoConfiguration({
        AopAutoConfiguration.class, AutomationMethodSecurityConfiguration.class
    })
    @Import(ApiConnectorGraphQlController.class)
    static class Config {

        @Bean
        ApiConnectorAiService apiConnectorAiService() {
            return mock(ApiConnectorAiService.class, bodyReachedAnswer());
        }

        @Bean
        ApiConnectorFacade apiConnectorFacade() {
            return mock(ApiConnectorFacade.class, bodyReachedAnswer());
        }

        @Bean
        ApiConnectorGenerationJobService apiConnectorGenerationJobService() {
            return mock(ApiConnectorGenerationJobService.class, bodyReachedAnswer());
        }

        @Bean
        ApiConnectorService apiConnectorService() {
            return mock(ApiConnectorService.class, bodyReachedAnswer());
        }

        @Bean
        OpenApiSpecificationGenerator openApiSpecificationGenerator() {
            return mock(OpenApiSpecificationGenerator.class, bodyReachedAnswer());
        }

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }

        @Bean
        RuntimeWiringConfigurer longScalarRuntimeWiringConfigurer() {
            return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.longScalar());
        }

        @Bean
        SecurityDataFetcherExceptionResolver securityDataFetcherExceptionResolver() {
            return new SecurityDataFetcherExceptionResolver();
        }

        private static Answer<Object> bodyReachedAnswer() {
            return invocation -> {
                if (invocation.getMethod()
                    .getDeclaringClass() == Object.class) {

                    return Answers.RETURNS_DEFAULTS.answer(invocation);
                }

                throw new IllegalStateException(BODY_REACHED);
            };
        }
    }
}
